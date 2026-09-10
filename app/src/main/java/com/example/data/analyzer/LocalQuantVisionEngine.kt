package com.example.data.analyzer

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import com.example.audio.AudioSignalEngine
import com.example.data.models.MetricSnapshot
import com.example.data.models.StrengthLevel
import com.example.data.models.TradeDirection
import com.example.data.models.TradingAnalysis
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Ultra-strict 100% On-Device OCR & Quantitative Vision Engine.
 * Extracts "5 min change" (Target A) and "60 min change" (Target B) strictly with optical bounding box verification,
 * respects color polarity (Red = Negative, Green = Positive), and executes canonical mathematical rules.
 * Strictly avoids synthetic or heuristic fallbacks if optical clarity is lacking.
 */
object LocalQuantVisionEngine {

    private const val TAG = "LocalQuantEngine"
    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    @Volatile
    private var lastVerboseLogTime = 0L

    private inline fun logThrottled(crossinline block: () -> Unit) {
        val now = System.currentTimeMillis()
        if (now - lastVerboseLogTime >= 5000L) {
            lastVerboseLogTime = now
            block()
        }
    }

    suspend fun analyzeBitmap(bitmap: Bitmap, history: List<MetricSnapshot> = emptyList()): TradingAnalysis = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val width = bitmap.width
        val height = bitmap.height

        if (width <= 0 || height <= 0) {
            return@withContext TradingAnalysis(
                isSuccess = false,
                errorMessage = "No image detected in frame (Empty Frame)",
                latencyMs = System.currentTimeMillis() - startTime,
                rawResponse = "LocalEngine: Frame empty",
                engineSource = "On-Device OCR",
                audioEvent = AudioSignalEngine.SOUND_NONE
            )
        }

        // Fast resolution scaling: Downscale high-res camera frames to max 960px for ultra-low latency (<50ms) ML Kit recognition
        val maxDim = 960
        val needsScale = width > maxDim || height > maxDim
        val targetBitmap = if (needsScale) {
            val scale = maxDim.toFloat() / kotlin.math.max(width, height)
            val sw = (width * scale).toInt().coerceAtLeast(1)
            val sh = (height * scale).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, sw, sh, true)
        } else {
            bitmap
        }

        try {
            val inputImage = InputImage.fromBitmap(targetBitmap, 0)
            val visionText: Text = Tasks.await(textRecognizer.process(inputImage))

            logThrottled {
                Log.d(TAG, "OCR Detected raw text:\n${visionText.text}")
            }

            // Extract Target A (5m change), Target B (60m change), and Optional Context (1d change)
            val ocrValues = extractValuesStrict(visionText, targetBitmap)

            if (ocrValues != null && (ocrValues.val5m != null || ocrValues.val60m != null || ocrValues.val1d != null)) {
                val latency = System.currentTimeMillis() - startTime

                if (ocrValues.isAmbiguous) {
                    return@withContext TradingAnalysis(
                        isSuccess = false,
                        isValid = false,
                        errorMessage = "Sign or color ambiguous (Ambiguous Sign)",
                        rawResponse = "OCR Ambiguous: Missing sign with unclear color or approximate symbol (~)",
                        latencyMs = latency,
                        engineSource = "On-Device OCR",
                        audioEvent = AudioSignalEngine.SOUND_NONE,
                        dataQuality = com.example.data.models.DataQualityState.AMBIGUOUS,
                        direction = TradeDirection.NEUTRAL,
                        isNoTradeZone = false
                    )
                }

                return@withContext createAnalysisFromMetrics(
                    est5m = ocrValues.val5m,
                    est60m = ocrValues.val60m,
                    est1d = null,
                    prefix5m = ocrValues.prefix5m,
                    prefix60m = ocrValues.prefix60m,
                    details = ocrValues.details,
                    isApprox = ocrValues.isApproximate,
                    is1dExplicitlyRejected = false,
                    latencyMs = latency,
                    history = history
                )
            }

            // HARD FAILURE: If no valid numeric metrics (5m or 60m) can be extracted, stop immediately and return isSuccess: false.
            val latency = System.currentTimeMillis() - startTime
            return@withContext TradingAnalysis(
                isSuccess = false,
                isValid = false,
                errorMessage = "Valid metrics (5m or 60m) could not be detected with optical clarity. Check camera alignment.",
                rawResponse = "OCR Extraction Incomplete: No valid timeframe (5m, 60m) found in bounding scan.",
                latencyMs = latency,
                engineSource = "On-Device OCR",
                audioEvent = AudioSignalEngine.SOUND_NONE,
                dataQuality = com.example.data.models.DataQualityState.UNAVAILABLE,
                direction = TradeDirection.NEUTRAL,
                isNoTradeZone = false
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e(TAG, "OCR recognition exception", e)
            val latency = System.currentTimeMillis() - startTime
            return@withContext TradingAnalysis(
                isSuccess = false,
                isValid = false,
                errorMessage = "OCR Error: ${e.localizedMessage ?: "Unknown error"}",
                rawResponse = "OCR Exception: ${e.message}",
                latencyMs = latency,
                engineSource = "On-Device OCR",
                audioEvent = AudioSignalEngine.SOUND_NONE,
                dataQuality = com.example.data.models.DataQualityState.UNAVAILABLE,
                direction = TradeDirection.NEUTRAL,
                isNoTradeZone = false
            )
        } finally {
            if (needsScale && targetBitmap != bitmap) {
                try {
                    if (!targetBitmap.isRecycled) {
                        targetBitmap.recycle()
                    }
                } catch (ignored: Exception) {}
            }
        }
    }

    /**
     * Builds a full canonical TradingAnalysis object from parsed metrics using the unified calculation engine.
     * This provides a pure, deterministic path testable directly without ML Kit hardware dependencies.
     */
    internal fun createAnalysisFromMetrics(
        est5m: Double?,
        est60m: Double?,
        est1d: Double? = null,
        prefix5m: String = "",
        prefix60m: String = "",
        details: String = "On-Device OCR",
        isApprox: Boolean = false,
        is1dExplicitlyRejected: Boolean = false,
        latencyMs: Long = 0L,
        history: List<MetricSnapshot> = emptyList()
    ): TradingAnalysis {
        val valid5m = if (TradingOutputParser.isValidMetricValue(est5m)) est5m else null
        val valid60m = if (TradingOutputParser.isValidMetricValue(est60m)) est60m else null
        val valid1d = if (TradingOutputParser.isValidMetricValue(est1d)) est1d else null

        return ReactiveMarketPressureEngine.buildTradingAnalysis(
            val5m = valid5m,
            val60m = valid60m,
            val1d = valid1d,
            prefix5m = prefix5m,
            prefix60m = prefix60m,
            isApprox = isApprox,
            is1dExplicitlyRejected = is1dExplicitlyRejected,
            latencyMs = latencyMs,
            engineSource = "On-Device OCR ($details)",
            history = history
        )
    }

    data class CandidatePolarityResult(
        val resolvedValue: Double,
        val isAmbiguous: Boolean,
        val isApproximate: Boolean,
        val prefix: String
    )

    fun resolveCandidatePolarity(
        tokenText: String,
        polarity: Int, // -1 = Red (Negative), 1 = Green (Positive), 0 = Neutral / Unknown
        parsedValue: Double
    ): CandidatePolarityResult {
        val hasMinus = tokenText.contains("-") || tokenText.contains("—") || tokenText.contains("–") || tokenText.contains("−")
        val hasPlus = tokenText.contains("+")
        val hasApprox = tokenText.contains("~") || tokenText.contains("≈")
        val prefix = if (hasApprox) (if (tokenText.contains("≈")) "≈" else "~") else ""
        val isApproximate = hasApprox

        if (hasMinus && hasPlus) {
            // Conflicting explicit signs on same token
            return CandidatePolarityResult(parsedValue, isAmbiguous = true, isApproximate = isApproximate, prefix = prefix)
        }

        if (hasMinus) {
            // Explicit minus
            return if (polarity > 0) {
                // Conflict: Explicit minus on confirmed green text
                CandidatePolarityResult(-abs(parsedValue), isAmbiguous = true, isApproximate = isApproximate, prefix = prefix)
            } else {
                CandidatePolarityResult(-abs(parsedValue), isAmbiguous = false, isApproximate = isApproximate, prefix = prefix)
            }
        }

        if (hasPlus) {
            // Explicit plus
            return if (polarity < 0) {
                // Conflict: Explicit plus on confirmed red text
                CandidatePolarityResult(abs(parsedValue), isAmbiguous = true, isApproximate = isApproximate, prefix = prefix)
            } else {
                CandidatePolarityResult(abs(parsedValue), isAmbiguous = false, isApproximate = isApproximate, prefix = prefix)
            }
        }

        // No explicit sign:
        if (hasApprox) {
            // Approximate without explicit sign is ambiguous (never assume sign)
            return CandidatePolarityResult(parsedValue, isAmbiguous = true, isApproximate = true, prefix = prefix)
        }

        // Without explicit sign (including zero): MUST rely on confirmed color polarity
        return when (polarity) {
            -1 -> CandidatePolarityResult(-abs(parsedValue), isAmbiguous = false, isApproximate = false, prefix = "")
            1 -> CandidatePolarityResult(abs(parsedValue), isAmbiguous = false, isApproximate = false, prefix = "")
            else -> {
                // Unknown / neutral color with NO sign: Ambiguous/invalid! NEVER assume positive or valid!
                CandidatePolarityResult(parsedValue, isAmbiguous = true, isApproximate = false, prefix = "")
            }
        }
    }

    private data class OcrExtractedRates(
        val val5m: Double?,
        val val60m: Double?,
        val val1d: Double?,
        val details: String,
        val isAmbiguous: Boolean = false,
        val isApproximate: Boolean = false,
        val prefix5m: String = "",
        val prefix60m: String = "",
        val is1dRejected: Boolean = false
    )

    /**
     * Strict spatial and semantic extraction of Target A (5m change) and Target B (60m change),
     * and optional display context for 1 day change.
     * Strictly prevents column bleeding and ensures 5m and 60m receive distinct, optically verified values.
     */
    private fun extractValuesStrict(visionText: Text, bitmap: Bitmap): OcrExtractedRates? {
        var val5m: Double? = null
        var val60m: Double? = null
        var val1d: Double? = null

        // Collect all lines across blocks
        val allLines = visionText.textBlocks.flatMap { it.lines }

        // 1. Identify all "1 day", "Traders' Sentiment", "Profit" header lines and boxes
        val headers1d = allLines.filter { line ->
            val text = line.text.lowercase(Locale.US)
            val isForbidden = text.contains("month") || text.contains("year") || text.contains("ytd") || text.contains("sentiment")
            if (isForbidden) return@filter false

            text.contains("1 day") ||
            text.contains("1day") ||
            text.contains("1 d ") ||
            text.contains("1d ch") ||
            text.contains("1 d ch") ||
            text.contains("1d change") ||
            text.contains("1 day change") ||
            text.contains("24h") ||
            text.contains("24 h") ||
            text.contains("24hr") ||
            text.contains("24 hr") ||
            text.contains("24hour") ||
            text.contains("24 hour") ||
            text.contains("১ দিন") ||
            text.contains("১দিনের পরিবর্তন") ||
            text.contains("১দিন")
        }
        val headerBox1d = headers1d.mapNotNull { it.boundingBox }.minByOrNull { it.left }

        val forbiddenHeaders = allLines.filter { line ->
            val text = line.text.lowercase(Locale.US)
            text.contains("traders' sentiment") ||
            text.contains("sentiment") ||
            text.contains("profit") ||
            text.contains("payout") ||
            text.contains("balance") ||
            text.contains("investment") ||
            text.contains("deposit") ||
            text.contains("account") ||
            text.contains("demo") ||
            text.contains("real") ||
            text.contains("month") ||
            text.contains("year") ||
            text.contains("ytd") ||
            text.contains("মাস") ||
            text.contains("বছর")
        }
        val forbiddenBoxes = forbiddenHeaders.mapNotNull { it.boundingBox }

        // Find candidate header lines for 5m and 60m (with boundary-aware regex to prevent matching 15m, 25m, etc.)
        val regexHeader5m = Regex("""(?<!\d)(?:5\s*min|5\s*m|5\s*chg|5\s*change|5m\s*chg|5m\s*change|5-min|5-m|sm\s*in|sm\s*chg|৫\s*মিনিট|৫মি)(?!\w)""", RegexOption.IGNORE_CASE)
        val regexHeader60m = Regex("""(?<!\d)(?:60\s*min|60\s*m|60\s*chg|60\s*change|60m\s*chg|60m\s*change|60-min|60-m|1\s*h|1\s*hr|1\s*hour|1h\s*chg|1h\s*change|৬০\s*মিনিট|৬০মি|১\s*ঘণ্টা|১\s*ঘন্টা)(?!\w)""", RegexOption.IGNORE_CASE)

        val headers5m = allLines.filter { line ->
            val text = line.text.lowercase(Locale.US)
            val isForbidden = text.contains("60") || text.contains("50") || text.contains("1 day") || 
                              text.contains("1day") || text.contains("sentiment") || text.contains("profit") || 
                              text.contains("payout") || text.contains("balance") || text.contains("deposit") ||
                              text.contains("month") || text.contains("year") || text.contains("ytd")
            if (isForbidden) return@filter false

            regexHeader5m.containsMatchIn(text)
        }

        val headers60m = allLines.filter { line ->
            val text = line.text.lowercase(Locale.US)
            val isForbidden = text.contains("1 day") || text.contains("1day") || text.contains("sentiment") || 
                              text.contains("profit") || text.contains("payout") || text.contains("balance") || text.contains("deposit") ||
                              text.contains("month") || text.contains("year") || text.contains("ytd")
            if (isForbidden) return@filter false

            regexHeader60m.containsMatchIn(text)
        }

        // Primary Header Boxes (take the clearest header box)
        val headerBox5m = headers5m.mapNotNull { it.boundingBox }.minByOrNull { it.left }
        val headerBox60m = headers60m.mapNotNull { it.boundingBox }.minByOrNull { it.left }

        // Extract all candidate number lines across the entire image with their parsed values and color polarity
        data class CandidateNumber(
            val line: Text.Line,
            val box: Rect,
            val rawValue: Double,
            val resolvedValue: Double,
            val isAmbiguous: Boolean = false,
            val isApproximate: Boolean = false,
            val prefix: String = ""
        )

        // Helper to extract true percentage change rate from text (ignoring timeframe numbers like 5 in '5 min' or 60 in '60 min')
        fun extractChangeRatesWithPositions(line: Text.Line): List<CandidateNumber> {
            val results = mutableListOf<CandidateNumber>()
            val lineBox = line.boundingBox ?: return results
            val text = line.text
            val lower = text.lowercase(Locale.US)

            // Strictly skip metadata lines, time clocks, balances, payouts, month/year/ytd
            if (lower.contains("sentiment") || lower.contains("profit") || lower.contains("payout") || 
                lower.contains("investment") || lower.contains("balance") || lower.contains("deposit") ||
                lower.contains("account") || lower.contains("demo") || lower.contains("real") ||
                lower.contains("utc") || lower.contains("gmt") || lower.contains("$") || lower.contains("€") ||
                lower.contains("₹") || lower.contains("৳") ||
                lower.contains("month") || lower.contains("year") || lower.contains("ytd") ||
                lower.contains("মাস") || lower.contains("বছর") ||
                (lower.contains("trade") && !lower.contains("change") && !lower.contains("chg"))
            ) {
                return results
            }

            // Skip clock timestamps like 12:34:56 or 14:35
            if (Regex("""\b\d{1,2}:\d{2}(?::\d{2})?\b""").containsMatchIn(text)) {
                return results
            }

            // Check individual elements first for precise bounding boxes
            val elements = line.elements
            if (elements.isNotEmpty()) {
                for (elem in elements) {
                    val elemBox = elem.boundingBox ?: lineBox
                    val elemText = elem.text
                    val elemLower = elemText.lowercase(Locale.US)

                    // Skip headers
                    if (elemLower == "5m" || elemLower == "60m" || elemLower == "1d" || elemLower == "5min" || elemLower == "60min" || elemLower == "1day" ||
                        elemLower == "1h" || elemLower == "24h" || elemLower == "sm" || elemLower == "change" || elemLower == "chg" ||
                        elemLower.contains("month") || elemLower.contains("year") || elemLower.contains("ytd")) {
                        continue
                    }

                    val normalized = TradingOutputParser.normalizeBengaliAndDashes(elemText)
                    
                    // A genuine trading percentage change is signed (+/-) or explicitly contains '%'
                    val elemHasSign = normalized.contains("+") || normalized.contains("-") ||
                            normalized.contains("—") || normalized.contains("–") || normalized.contains("−")
                    val elemHasPercent = normalized.contains("%") || elemText.contains("%") || line.text.contains("%")
                    if (!elemHasSign && !elemHasPercent) {
                        continue
                    }

                    // Match signed rate (+0.05%, -0.08%, +5.15), decimal with percent (0.05%), or integer percentage (0%, +1%, -2%)
                    val match = Regex("""([~≈]?\s*[+\-—–−]?\s*\d+\.\d+)\s*%?""").find(normalized)
                        ?: Regex("""([~≈]?\s*[+\-—–−]?\s*\d+)\s*%""").find(normalized)

                    if (match != null) {
                        val tokenStr = match.groupValues[1]
                        val tokenHasSign = tokenStr.contains("+") || tokenStr.contains("-") ||
                                tokenStr.contains("—") || tokenStr.contains("–") || tokenStr.contains("−")
                        if (!tokenHasSign && !elemHasPercent) {
                            continue
                        }
                        val clean = tokenStr.replace(Regex("[~≈\\s]"), "")
                        val parsed = clean.toDoubleOrNull()
                        if (parsed != null && abs(parsed) <= TradingOutputParser.MAX_VALID_PERCENTAGE) {
                            val polarity = determineColorPolarity(bitmap, elemBox)
                            val res = resolveCandidatePolarity(tokenStr, polarity, parsed)
                            results.add(CandidateNumber(line, elemBox, parsed, res.resolvedValue, res.isAmbiguous, res.isApproximate, res.prefix))
                        }
                    }
                }
            }

            // If elements didn't yield candidates, match whole line
            if (results.isEmpty()) {
                val normalized = TradingOutputParser.normalizeBengaliAndDashes(text)
                val lineHasSign = normalized.contains("+") || normalized.contains("-") ||
                        normalized.contains("—") || normalized.contains("–") || normalized.contains("−")
                val lineHasPercent = normalized.contains("%") || line.text.contains("%")

                if (lineHasSign || lineHasPercent) {
                    val matches = (Regex("""([~≈]?\s*[+\-—–−]?\s*\d+\.\d+)\s*%?""").findAll(normalized) +
                                   Regex("""([~≈]?\s*[+\-—–−]?\s*\d+)\s*%""").findAll(normalized)).toList()
                    for (m in matches) {
                        val tokenStr = m.groupValues[1]
                        val tokenHasSign = tokenStr.contains("+") || tokenStr.contains("-") ||
                                tokenStr.contains("—") || tokenStr.contains("–") || tokenStr.contains("−")
                        if (!tokenHasSign && !lineHasPercent) continue

                        val clean = tokenStr.replace(Regex("[~≈\\s]"), "")
                        val parsed = clean.toDoubleOrNull()
                        if (parsed != null && abs(parsed) <= TradingOutputParser.MAX_VALID_PERCENTAGE) {
                            val polarity = determineColorPolarity(bitmap, lineBox)
                            // Note: pass tokenStr (the matched token) to strictly isolate signs and prevent bleeding across multiple numbers
                            val res = resolveCandidatePolarity(tokenStr, polarity, parsed)
                            results.add(CandidateNumber(line, lineBox, parsed, res.resolvedValue, res.isAmbiguous, res.isApproximate, res.prefix))
                        }
                    }
                }
            }

            return results
        }

        val candidateNumbers = mutableListOf<CandidateNumber>()

        for (line in allLines) {
            val candidates = extractChangeRatesWithPositions(line)
            candidateNumbers.addAll(candidates)
        }

        logThrottled {
            Log.d(TAG, "Candidate numbers found: ${candidateNumbers.size} -> ${candidateNumbers.map { "${it.resolvedValue} at (${it.box.left},${it.box.top})" }}")
        }

        // STRATEGY A: Direct Header Proximity (STRICT: ONLY accept numbers directly underneath 5 min change, 60 min change, and 1 day change headers)
        if (headerBox5m != null && headerBox60m != null) {
            val center5m = headerBox5m.centerX()
            val center60m = headerBox60m.centerX()
            val center1d = headerBox1d?.centerX() ?: (center60m + abs(center60m - center5m))
            val colSpacing = abs(center60m - center5m).coerceAtLeast(40)

            // Candidates strictly for Column 1 (5 min change): must be directly underneath 5m header
            val col1Candidates = candidateNumbers.filter { cand ->
                val box = cand.box
                val isBelow = box.top >= headerBox5m.top - 10 && box.top <= headerBox5m.bottom + 220
                val dist5m = abs(box.centerX() - center5m)
                val dist60m = abs(box.centerX() - center60m)
                isBelow && dist5m < dist60m && dist5m < colSpacing * 0.70
            }.sortedBy { it.box.top }

            // Candidates strictly for Column 2 (60 min change): must be directly underneath 60m header
            val col2Candidates = candidateNumbers.filter { cand ->
                val box = cand.box
                val isBelow = box.top >= headerBox60m.top - 10 && box.top <= headerBox60m.bottom + 220
                val dist60m = abs(box.centerX() - center60m)
                val dist5m = abs(box.centerX() - center5m)
                val dist1d = abs(box.centerX() - center1d)
                isBelow && dist60m < dist5m && dist60m < dist1d && dist60m < colSpacing * 0.70
            }.sortedBy { it.box.top }

            // Candidates strictly for Column 3 (1 day change): must be directly underneath 1d header
            val col3Candidates = candidateNumbers.filter { cand ->
                val box = cand.box
                val isBelow = if (headerBox1d != null) (box.top >= headerBox1d.top - 10 && box.top <= headerBox1d.bottom + 220) else (box.top >= headerBox60m.top - 10 && box.top <= headerBox60m.bottom + 220)
                val dist1d = abs(box.centerX() - center1d)
                val dist60m = abs(box.centerX() - center60m)
                isBelow && dist1d < dist60m && dist1d < colSpacing * 0.90
            }.sortedBy { it.box.top }

            val chosen5m = col1Candidates.firstOrNull()
            val chosen60m = col2Candidates.firstOrNull { it.line != chosen5m?.line && it.box != chosen5m?.box }
            // Ambiguous or approximate or out-of-range 1D is dropped, never guessed
            val chosen1d = col3Candidates.firstOrNull { 
                it.line != chosen5m?.line && it.line != chosen60m?.line && 
                it.box != chosen5m?.box && it.box != chosen60m?.box && 
                !it.isAmbiguous && !it.isApproximate &&
                TradingOutputParser.isValidMetricValue(it.resolvedValue)
            }

            // MANDATORY DUAL-TIMEFRAME: Both 5m and 60m must be present directly under their respective headers
            if (chosen5m != null && chosen60m != null) {
                logThrottled {
                    Log.d(TAG, "Strict Header Proximity Match: 5m=${chosen5m?.resolvedValue}, 60m=${chosen60m?.resolvedValue}, 1d=${chosen1d?.resolvedValue}")
                }
                val isAmbiguous = (chosen5m?.isAmbiguous == true) || (chosen60m?.isAmbiguous == true)
                val isApprox = (chosen5m?.isApproximate == true) || (chosen60m?.isApproximate == true)
                val is1dRejected = col3Candidates.isNotEmpty() && chosen1d == null
                return OcrExtractedRates(
                    chosen5m?.resolvedValue,
                    chosen60m?.resolvedValue,
                    chosen1d?.resolvedValue,
                    "Strict Header Proximity OCR",
                    isAmbiguous = isAmbiguous,
                    isApproximate = isApprox,
                    prefix5m = chosen5m?.prefix ?: "",
                    prefix60m = chosen60m?.prefix ?: "",
                    is1dRejected = is1dRejected
                )
            }
        }

        // SECONDARY RECOVERY: Check if header and value were combined in the same line/element text
        // (e.g. "5 min change 0%", "60 min change -0.53%", "1 day change +3.72%")
        val validLines = allLines.filter { line ->
            val text = line.text.lowercase(Locale.US)
            val isForbiddenText = text.contains("traders' sentiment") ||
                text.contains("sentiment") ||
                text.contains("profit") ||
                text.contains("payout") ||
                text.contains("balance") ||
                text.contains("deposit") ||
                text.contains("month") ||
                text.contains("year") ||
                text.contains("ytd") ||
                text.contains("মাস") ||
                text.contains("বছর")

            if (isForbiddenText) return@filter false

            val box = line.boundingBox
            if (box != null) {
                for (fBox in forbiddenBoxes) {
                    val isUnderForbidden = box.top >= fBox.top - 10 && box.top <= fBox.bottom + 160 &&
                            abs(box.centerX() - fBox.centerX()) < 120
                    if (isUnderForbidden) return@filter false
                }
            }
            true
        }

        val sanitizedText = validLines.joinToString(" ") { it.text }
        val fullNormalized = TradingOutputParser.normalizeBengaliAndDashes(sanitizedText)
        
        val regex5m = Regex("""(?<!\d)(?:[৫5]\s*(?:min|m|minute|মিনিট)(?:\s*change|\s*chg|\s*পরিবর্তন)?)\s*[:=]?\s*([~≈]?\s*[+\-—–−]?\s*\d+(?:\.\d+)?)\s*%?""", RegexOption.IGNORE_CASE)
        val regex60m = Regex("""(?<!\d)(?:(?:৬০|60)\s*(?:min|m|minute|মিনিট)|(?:১|1)\s*(?:h|hour|hr|ঘণ্টা|ঘন্টা))(?:\s*change|\s*chg|\s*পরিবর্তন)?\s*[:=]?\s*([~≈]?\s*[+\-—–−]?\s*\d+(?:\.\d+)?)\s*%?""", RegexOption.IGNORE_CASE)
        val regex1d = Regex("""(?<!\d)(?:(?:১|1)\s*(?:day|d|দিন)|24\s*h(?:our)?)(?:\s*change|\s*chg|\s*পরিবর্তন)?\s*[:=]?\s*([~≈]?\s*[+\-—–−]?\s*\d+(?:\.\d+)?)\s*%?""", RegexOption.IGNORE_CASE)

        val match5m = regex5m.find(fullNormalized)
        val match60m = regex60m.find(fullNormalized)
        val match1d = regex1d.find(fullNormalized)

        var isAmbiguousCombined = false
        var isApprox = false
        var prefix5m = ""
        var prefix60m = ""

        if (match5m != null && val5m == null) {
            val tokenStr = match5m.groupValues[1]
            val rawStr = tokenStr.replace(Regex("[~≈\\s]"), "")
            val parsed = rawStr.toDoubleOrNull()
            if (parsed != null && TradingOutputParser.isValidMetricValue(parsed)) {
                val line5m = validLines.firstOrNull { regex5m.containsMatchIn(TradingOutputParser.normalizeBengaliAndDashes(it.text)) }
                val polarity5m = line5m?.boundingBox?.let { determineColorPolarity(bitmap, it) } ?: 0
                val res = resolveCandidatePolarity(tokenStr, polarity5m, parsed)
                val5m = res.resolvedValue
                if (res.isAmbiguous) isAmbiguousCombined = true
                if (res.isApproximate) {
                    isApprox = true
                    prefix5m = res.prefix
                }
            }
        }

        if (match60m != null && val60m == null) {
            val tokenStr = match60m.groupValues[1]
            val rawStr = tokenStr.replace(Regex("[~≈\\s]"), "")
            val parsed = rawStr.toDoubleOrNull()
            if (parsed != null && TradingOutputParser.isValidMetricValue(parsed)) {
                val line60m = validLines.firstOrNull { regex60m.containsMatchIn(TradingOutputParser.normalizeBengaliAndDashes(it.text)) }
                val polarity60m = line60m?.boundingBox?.let { determineColorPolarity(bitmap, it) } ?: 0
                val res = resolveCandidatePolarity(tokenStr, polarity60m, parsed)
                val60m = res.resolvedValue
                if (res.isAmbiguous) isAmbiguousCombined = true
                if (res.isApproximate) {
                    isApprox = true
                    prefix60m = res.prefix
                }
            }
        }

        if (match1d != null && val1d == null) {
            val tokenStr = match1d.groupValues[1]
            val rawStr = tokenStr.replace(Regex("[~≈\\s]"), "")
            val parsed = rawStr.toDoubleOrNull()
            if (parsed != null && TradingOutputParser.isValidMetricValue(parsed)) {
                val line1d = validLines.firstOrNull { regex1d.containsMatchIn(TradingOutputParser.normalizeBengaliAndDashes(it.text)) }
                val polarity1d = line1d?.boundingBox?.let { determineColorPolarity(bitmap, it) } ?: 0
                val res = resolveCandidatePolarity(tokenStr, polarity1d, parsed)
                // Ambiguous, approximate, or out-of-range 1D must be dropped, not guessed
                val1d = if (res.isAmbiguous || res.isApproximate || !TradingOutputParser.isValidMetricValue(res.resolvedValue)) null else res.resolvedValue
            }
        }

        // MANDATORY DUAL-TIMEFRAME: Both 5m and 60m must be present
        if (val5m != null && val60m != null) {
            val is1dRejected = match1d != null && val1d == null
            return OcrExtractedRates(
                val5m,
                val60m,
                val1d,
                "Combined Line OCR",
                isAmbiguous = isAmbiguousCombined,
                isApproximate = isApprox,
                prefix5m = prefix5m,
                prefix60m = prefix60m,
                is1dRejected = is1dRejected
            )
        }

        // STRICT CONSTRAINT: NEVER guess or extract random numbers outside 5m / 60m / 1d headers
        return null
    }

    private fun findNumberDirectlyUnderneathOrBeside(targetLine: Text.Line, allLines: List<Text.Line>, bitmap: Bitmap): Double? {
        val targetBox = targetLine.boundingBox ?: return null
        var bestMatch: Double? = null
        var minDistance = Int.MAX_VALUE

        val numRegex = Regex("""([~≈]?\s*[+\-—–−]?\s*\d+(?:\.\d+)?)\s*%?""")

        for (other in allLines) {
            if (other == targetLine) continue
            val otherBox = other.boundingBox ?: continue
            val text = TradingOutputParser.normalizeBengaliAndDashes(other.text)

            val match = numRegex.find(text) ?: continue
            val tokenStr = match.groupValues[1]
            val cleanStr = tokenStr.replace(Regex("[~≈\\s]"), "")
            val parsedNumber = cleanStr.toDoubleOrNull() ?: continue

            // Determine color polarity of the detected region: Red = Negative, Green = Positive
            val polarity = determineColorPolarity(bitmap, otherBox)
            val res = resolveCandidatePolarity(tokenStr, polarity, parsedNumber)
            if (res.isAmbiguous) continue
            val resolvedNumber = res.resolvedValue

            // Check if element is directly below (y > targetBox.bottom) or aligned beside
            val isBelow = otherBox.top >= targetBox.bottom - 10 && otherBox.top <= targetBox.bottom + 160
            val xAligned = abs(otherBox.centerX() - targetBox.centerX()) < 220
            val isBeside = abs(otherBox.centerY() - targetBox.centerY()) < 60 && otherBox.left >= targetBox.right - 10

            if ((isBelow && xAligned) || isBeside) {
                val dist = if (isBelow) (otherBox.top - targetBox.bottom) else (otherBox.left - targetBox.right)
                if (dist < minDistance) {
                    minDistance = dist
                    bestMatch = resolvedNumber
                }
            }
        }
        return bestMatch
    }

    fun determineColorPolarity(bitmap: Bitmap, box: Rect): Int {
        val left = box.left.coerceIn(0, bitmap.width - 1)
        val top = box.top.coerceIn(0, bitmap.height - 1)
        val right = box.right.coerceIn(0, bitmap.width - 1)
        val bottom = box.bottom.coerceIn(0, bitmap.height - 1)

        if (right <= left || bottom <= top) return 0

        var redPixelScore = 0
        var greenPixelScore = 0

        val stepX = ((right - left) / 30).coerceAtLeast(1)
        val stepY = ((bottom - top) / 30).coerceAtLeast(1)

        for (x in left..right step stepX) {
            for (y in top..bottom step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                val maxC = maxOf(r, g, b)
                if (maxC > 50) {
                    if (r > g + 20 && r > 70) {
                        redPixelScore++
                    } else if (g > r + 20 && g > 70) {
                        greenPixelScore++
                    }
                }
            }
        }

        return when {
            redPixelScore >= 3 && redPixelScore > greenPixelScore -> -1 // RED -> Negative
            greenPixelScore >= 3 && greenPixelScore > redPixelScore -> 1 // GREEN -> Positive
            else -> 0
        }
    }

    private fun determineRegionColorPolarity(bitmap: Bitmap, isRightSide: Boolean): Int {
        val width = bitmap.width
        val height = bitmap.height
        val minX = if (isRightSide) (width * 0.50).toInt() else (width * 0.05).toInt()
        val maxX = if (isRightSide) (width * 0.95).toInt() else (width * 0.50).toInt()
        val minY = (height * 0.20).toInt()
        val maxY = (height * 0.80).toInt()

        var redCount = 0
        var greenCount = 0

        val stepX = ((maxX - minX) / 25).coerceAtLeast(1)
        val stepY = ((maxY - minY) / 25).coerceAtLeast(1)

        for (x in minX..maxX step stepX) {
            for (y in minY..maxY step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)

                if (maxOf(r, g, b) > 50) {
                    if (r > g + 20 && r > 70) redCount++
                    else if (g > r + 20 && g > 70) greenCount++
                }
            }
        }

        return when {
            redCount >= 4 && redCount > greenCount -> -1
            greenCount >= 4 && greenCount > redCount -> 1
            else -> 0
        }
    }
}
