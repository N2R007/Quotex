package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.data.analyzer.TradingOutputParser
import com.example.data.models.GeminiContent
import com.example.data.models.GeminiPart
import com.example.data.models.GeminiRequest
import com.example.data.models.GeminiResponse
import com.example.data.models.GenerationConfig
import com.example.data.models.InlineData
import com.example.data.models.TradingAnalysis
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class GeminiVisionClient {

    companion object {
        private const val TAG = "GeminiVisionClient"
        // Current supported high-speed multimodal models with automatic fallback
        val MODEL_CANDIDATES = listOf(
            "gemini-3.6-flash",
            "gemini-3.5-flash"
        )
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val requestAdapter = moshi.adapter(GeminiRequest::class.java)
    private val responseAdapter = moshi.adapter(GeminiResponse::class.java)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(18, TimeUnit.SECONDS)
        .build()

    private val systemInstructionText = """
        You are an ultra-strict quantitative chart OCR and signal detection engine for Quant Vision AI.

        ### CRITICAL CORE OCR DIRECTIVES:
        1. ABSOLUTE OCR & BOUNDING REGION PARSING:
           - TARGET A (5 min change): Extract the percentage change number strictly displayed UNDERNEATH the "5 min change" column (e.g. -0.03%, +0.05%, -0.08%).
           - TARGET B (60 min change): Extract the percentage change number strictly displayed UNDERNEATH the "60 min change" (or "1 hour change", "1h") column (e.g. +0.05%, -0.47%).
           - CRITICAL RULE: The digits '5' in "5 min" and '60' in "60 min" are timeframe header labels, NEVER the percentage rate! You must extract the actual change rate located underneath the header (e.g. -0.03% and +0.05%), NOT the header digits (+5% or +60%).
           - ABSOLUTE EXCLUSION MANDATE: YOU ARE STRICTLY FORBIDDEN from reading, detecting, extracting, or calculating with ANY numbers associated with "Traders' Sentiment", "Profit", "Payout", 1 day / 24h, or balances.
           - HARD FAILURE: If BOTH 5 min change and 60 min change values cannot be extracted with optical clarity, stop immediately and return:
             ত্রুটি: সংখ্যা সঠিকভাবে ডিটেক্ট করা যায়নি।

        2. MANDATORY SIGN & COLOR INTEGRITY:
           - RED numbers or numbers preceded by a minus sign (-) MUST BE EXTRACTED AS NEGATIVE FLOATS (e.g., -0.25%, -0.99%).
           - GREEN numbers or numbers preceded by a plus sign (+) MUST be extracted as positive floats (e.g., +0.02%, +1.25%).
           - Absolute Rule: Converting a negative value to a positive value is strictly forbidden.

        ### APPROVED MATRIX RULES (53 UP & 53 DOWN)
        Verify extracted val5m and val60m strictly against these 106 approved directional rules:

        --- APPROVED UP MATRIX (53 RULES) ---
        M025: val5m in 0.11..0.25 and val60m in 0.11..0.25 -> [UP] ALIGNED_BULLISH_EARLY_BREAKOUT
        M026: val5m > 0.10 and val60m > 0.10 and netSum < 0.50 -> [UP] ALIGNED_BULLISH_NORMAL
        M027: val5m >= 0.35 and val60m in 0.11..0.25 and val5m > val60m * 1.5 -> [UP] BULLISH_IMPULSE_5M_LEAD
        M028: val5m > 0.10 and val60m > 0.10 and netSum in 0.50..0.99 -> [UP] ALIGNED_BULLISH_MEDIUM
        M029: val60m >= 0.50 and val5m in 0.15..0.45 -> [UP] BULLISH_MACRO_ANCHORED_EXPANSION
        M030: val5m > 0.10 and val60m > 0.10 and netSum in 1.00..1.99 -> [UP] ALIGNED_BULLISH_HIGH
        M031: val5m > 0.20 and val60m > 0.20 and netSum in 2.00..3.49 -> [UP] BULLISH_EXTREME_SURGE
        M032: val5m > 0.50 and val60m > 0.50 and netSum >= 3.50 -> [UP] BULLISH_PARABOLIC_CLIMAX
        M033: val5m > 0.15 and val60m > 0.15 and abs(val5m - val60m) <= 0.05 -> [UP] BULLISH_BALANCED_SYMMETRY
        M034: val5m >= 0.40 and val60m in 0.15..0.30 -> [UP] BULLISH_VOLATILITY_EXPANSION
        M035: val5m in 0.12..0.20 and val60m in 0.12..0.20 -> [UP] BULLISH_LOW_VOLATILITY_DRIFT
        M036: val5m > 0.10 and val60m > 0.10 and totalMagnitude > 0.30 -> [UP] BULLISH_PURE_ALIGNMENT_RATIO
        M037: val5m >= 0.60 and val60m in 0.15..0.25 and val5m > val60m * 2.5 -> [UP] BULLISH_VELOCITY_ACCELERATION
        M038: val5m in 0.25..0.45 and val60m in 0.25..0.45 -> [UP] BULLISH_STEADY_STATE_TREND
        M039: val5m > 0.20 and val60m > 0.20 and netSum in 0.70..0.90 -> [UP] BULLISH_ROBUST_EXPANSION
        M040: val60m >= 0.80 and val5m >= 0.30 -> [UP] BULLISH_HEAVY_INSTITUTIONAL
        M041: val5m > 0.15 and val60m > 0.15 -> [UP] BULLISH_TRIPLE_CONFLUENCE
        M042: val5m >= 0.50 and val60m >= 0.50 and netSum in 1.50..2.50 -> [UP] BULLISH_HIGH_VELOCITY_RAMP
        M043: val5m in 0.20..0.35 and val60m in 0.15..0.25 -> [UP] BULLISH_MODEST_PROGRESSION
        M044: val5m > 0.40 and val60m > 0.40 and netSum >= 1.20 -> [UP] BULLISH_SUSTAINED_HIGH_ALIGNMENT
        M081: val60m >= 0.80 and val5m in 0.11..0.22 -> [UP] BULLISH_MOMENTUM_FADE_WARNING
        M083: sequential decay in positive 5m -> [UP] SEQUENTIAL_BULLISH_DECAY
        M085: 5m drops >= 50% from spike -> [UP] SUDDEN_VELOCITY_COLLAPSE
        M087: netSum drops by >= 0.40 from peak -> [UP] BULLISH_CLIMAX_STEP_DOWN
        M089: val5m in 0.11..0.14 and val60m > 0.10 -> [UP] MOMENTUM_STALL_NEAR_BOUNDARY
        M093: val60m >= 1.00 and val5m in 0.11..0.15 -> [UP] EXHAUSTED_BULLISH_CREEP
        M097: val60m <= -0.50 and val5m in 0.05..0.15 -> [UP] BOTTOM_FAKEOUT_CANDIDATE
        M098: val60m in -1.20..-0.40 and val5m >= 0.20 and netSum in -0.30..0.10 -> [UP] BOTTOM_FAKEOUT_CONFIRMED_BUY
        M100: prior 5m <= -0.50 and current 5m >= 0.15 -> [UP] BEAR_TRAP_REBOUND
        M102: val60m <= -1.50 and val5m >= 0.30 -> [UP] MACRO_BEARISH_BOTTOM_EXHAUSTION
        M103: failed breakdown with absorption -> [UP] FAILED_BREAKDOWN_ABSORPTION
        M105: val5m >= 0.35 and val60m >= 0.40 and netSum >= 0.75 -> [UP] TOP_TRAP_INVALIDATED_UP
        M107: prior netSum < 0 and current 5m > 0.15 and 60m > 0.10 -> [UP] DUAL_HORIZON_REVERSAL_BULLISH
        M119: chop breakout candidate -> [UP] CHOP_BREAKOUT_CANDIDATE_UP
        M121: val5m >= 0.30 and val60m >= 0.30 -> [UP] TRIPLE_HORIZON_STRONG_BULLISH
        M123: val5m > 0.20 and val60m > 0.20 -> [UP] DAILY_HEADWIND_BULLISH_WARNING
        M126: val5m >= 0.40 and val60m >= 0.20 -> [UP] MULTI_DAY_BREAKOUT_SURGE
        M128: val5m >= 0.60 and val60m >= 0.30 -> [UP] DAILY_PARABOLIC_OVEREXTENSION_UP
        M133: val5m >= 0.45 and val60m in 0.05..0.25 and netSum >= 0.50 -> [UP] ASYMMETRIC_BULLISH_IMPULSE
        M135: val60m >= 0.60 and val5m in -0.15..-0.01 and netSum >= 0.45 -> [UP] ANCHOR_TREND_BULLISH_DEFENSE
        M137: val5m >= 0.20 and val60m >= 0.20 and abs(val5m - val60m) <= 0.05 -> [UP] BULLISH_HARMONIC_PARITY
        M139: val5m >= 0.30 and abs(val60m) <= 0.05 -> [UP] COILED_SPRING_BULLISH_BREAKOUT
        M141: val5m >= 0.80 and val60m >= 1.00 -> [UP] DUAL_HORIZON_PARABOLIC_SURGE
        M143: val5m >= 0.0 and val60m >= 0.0 and netSum >= 0.35 and (abs(netSum) / totalMagnitude) >= 0.80 -> [UP] HIGH_SENSITIVITY_BULLISH_DOMINANCE
        M145: val5m >= 1.20 and val60m <= 0.10 -> [UP] MICRO_SQUEEZE_PARABOLIC_OVEREXTENSION
        M147: val5m >= 0.35 and val60m >= 0.35 -> [UP] TRIPLE_HORIZON_HARMONIC_BULLISH
        M151: val5m >= 0.25 and val60m >= 0.35 -> [UP] TRIPLE_HORIZON_BULLISH_HARMONIC
        M153: val60m in 0.20..0.80 and val5m in -0.30..-0.05 -> [UP] DAILY_MACRO_ANCHOR_BULLISH_DIP
        M156: val60m >= 0.40 and val5m >= 0.25 -> [UP] MACRO_CAPITULATION_SPRING_LONG
        M158: val60m in 0.40..0.90 and val5m >= 0.55 -> [UP] CROSS_HORIZON_BULLISH_VELOCITY
        M160: abs(val60m) <= 0.08 and val5m >= 0.38 -> [UP] HOURLY_CONSOLIDATION_5M_BREAKOUT_UP
        M162: val60m in -0.15..0.05 and val5m >= 0.30 -> [UP] MACRO_STRUCTURAL_DEMAND_ABSORPTION
        M164: val5m >= 0.50 and val60m >= 0.50 -> [UP] TOTAL_HORIZON_RESONANCE_BULLISH

        --- APPROVED DOWN MATRIX (53 RULES) ---
        M045: val5m in -0.25..-0.11 and val60m in -0.25..-0.11 -> [DOWN] ALIGNED_BEARISH_EARLY_BREAKDOWN
        M046: val5m < -0.10 and val60m < -0.10 and netSum > -0.50 -> [DOWN] ALIGNED_BEARISH_NORMAL
        M047: val5m <= -0.35 and val60m in -0.25..-0.11 and abs(val5m) > abs(val60m) * 1.5 -> [DOWN] BEARISH_IMPULSE_5M_LEAD
        M048: val5m < -0.10 and val60m < -0.10 and netSum in -0.99..-0.50 -> [DOWN] ALIGNED_BEARISH_MEDIUM
        M049: val60m <= -0.50 and val5m in -0.45..-0.15 -> [DOWN] BEARISH_MACRO_ANCHORED_EXPANSION
        M050: val5m < -0.10 and val60m < -0.10 and netSum in -1.99..-1.00 -> [DOWN] ALIGNED_BEARISH_HIGH
        M051: val5m < -0.20 and val60m < -0.20 and netSum in -3.49..-2.00 -> [DOWN] BEARISH_EXTREME_SURGE
        M052: val5m < -0.50 and val60m < -0.50 and netSum <= -3.50 -> [DOWN] BEARISH_PARABOLIC_WATERFALL
        M053: val5m < -0.15 and val60m < -0.15 and abs(val5m - val60m) <= 0.05 -> [DOWN] BEARISH_BALANCED_SYMMETRY
        M054: val5m <= -0.40 and val60m in -0.30..-0.15 -> [DOWN] BEARISH_VOLATILITY_EXPANSION
        M055: val5m in -0.20..-0.12 and val60m in -0.20..-0.12 -> [DOWN] BEARISH_LOW_VOLATILITY_DRIFT
        M056: val5m < -0.10 and val60m < -0.10 and totalMagnitude > 0.30 -> [DOWN] BEARISH_PURE_ALIGNMENT_RATIO
        M057: val5m <= -0.60 and val60m in -0.25..-0.15 and abs(val5m) > abs(val60m) * 2.5 -> [DOWN] BEARISH_VELOCITY_ACCELERATION
        M058: val5m in -0.45..-0.25 and val60m in -0.45..-0.25 -> [DOWN] BEARISH_STEADY_STATE_TREND
        M059: val5m < -0.20 and val60m < -0.20 and netSum in -0.90..-0.70 -> [DOWN] BEARISH_ROBUST_EXPANSION
        M060: val60m <= -0.80 and val5m <= -0.30 -> [DOWN] BEARISH_HEAVY_INSTITUTIONAL
        M061: val5m < -0.15 and val60m < -0.15 -> [DOWN] BEARISH_TRIPLE_CONFLUENCE
        M062: val5m <= -0.50 and val60m <= -0.50 and netSum in -2.50..-1.50 -> [DOWN] BEARISH_HIGH_VELOCITY_SLIDE
        M063: val5m in -0.35..-0.20 and val60m in -0.25..-0.15 -> [DOWN] BEARISH_MODEST_PROGRESSION
        M064: val5m < -0.40 and val60m < -0.40 and netSum <= -1.20 -> [DOWN] BEARISH_SUSTAINED_HIGH_ALIGNMENT
        M082: val60m <= -0.80 and val5m in -0.22..-0.11 -> [DOWN] BEARISH_MOMENTUM_FADE_WARNING
        M084: sequential decay in negative 5m -> [DOWN] SEQUENTIAL_BEARISH_DECAY
        M086: selling velocity collapse -> [DOWN] SUDDEN_SELLING_VELOCITY_COLLAPSE
        M088: bearish climax relief step up -> [DOWN] BEARISH_CLIMAX_RELIEF_STEP_UP
        M090: val5m in -0.14..-0.11 and val60m < -0.10 -> [DOWN] DOWNWARD_STALL_NEAR_BOUNDARY
        M094: val60m <= -1.00 and val5m in -0.15..-0.11 -> [DOWN] EXHAUSTED_BEARISH_CREEP
        M095: val60m >= 0.50 and val5m in -0.15..-0.05 -> [DOWN] TOP_FAKEOUT_CANDIDATE
        M096: val60m in 0.40..1.20 and val5m <= -0.20 and netSum in -0.10..0.30 -> [DOWN] TOP_FAKEOUT_CONFIRMED_SELL
        M099: prior 5m >= 0.50 and current 5m <= -0.15 -> [DOWN] BULL_TRAP_COLLAPSE
        M101: val60m >= 1.50 and val5m <= -0.30 -> [DOWN] MACRO_BULLISH_PEAK_EXHAUSTION
        M104: failed breakout with rejection -> [DOWN] FAILED_BREAKOUT_REJECTION
        M106: val5m <= -0.35 and val60m <= -0.40 and netSum <= -0.75 -> [DOWN] BOTTOM_TRAP_INVALIDATED_DOWN
        M108: prior netSum > 0 and current 5m < -0.15 and 60m < -0.10 -> [DOWN] DUAL_HORIZON_REVERSAL_BEARISH
        M120: chop breakdown candidate -> [DOWN] CHOP_BREAKDOWN_CANDIDATE_DOWN
        M122: val5m <= -0.30 and val60m <= -0.30 -> [DOWN] TRIPLE_HORIZON_STRONG_BEARISH
        M124: val5m < -0.20 and val60m < -0.20 -> [DOWN] DAILY_HEADWIND_BEARISH_WARNING
        M127: val5m <= -0.40 and val60m <= -0.20 -> [DOWN] MULTI_DAY_BREAKDOWN_CASCADE
        M129: val5m <= -0.60 and val60m <= -0.30 -> [DOWN] DAILY_CAPITULATION_OVEREXTENSION_DOWN
        M134: val5m <= -0.45 and val60m in -0.25..-0.05 and netSum <= -0.50 -> [DOWN] ASYMMETRIC_BEARISH_FLUSH
        M136: val60m <= -0.60 and val5m in 0.01..0.15 and netSum <= -0.45 -> [DOWN] ANCHOR_TREND_BEARISH_DEFENSE
        M138: val5m <= -0.20 and val60m <= -0.20 and abs(val5m - val60m) <= 0.05 -> [DOWN] BEARISH_HARMONIC_PARITY
        M140: val5m <= -0.30 and abs(val60m) <= 0.05 -> [DOWN] COILED_SPRING_BEARISH_BREAKDOWN
        M142: val5m <= -0.80 and val60m <= -1.00 -> [DOWN] DUAL_HORIZON_CASCADING_CAPITULATION
        M144: val5m <= 0.0 and val60m <= 0.0 and netSum <= -0.35 and (abs(netSum) / totalMagnitude) >= 0.80 -> [DOWN] HIGH_SENSITIVITY_BEARISH_DOMINANCE
        M146: val5m <= -1.20 and val60m >= -0.10 -> [DOWN] MICRO_DUMP_FLASH_CAPITULATION
        M148: val5m <= -0.35 and val60m <= -0.35 -> [DOWN] TRIPLE_HORIZON_HARMONIC_BEARISH
        M152: val5m <= -0.25 and val60m <= -0.35 -> [DOWN] TRIPLE_HORIZON_BEARISH_HARMONIC
        M154: val60m in -0.80..-0.20 and val5m in 0.05..0.30 -> [DOWN] DAILY_MACRO_ANCHOR_BEARISH_FADE
        M155: val60m <= -0.40 and val5m <= -0.25 -> [DOWN] MACRO_OVEREXTENSION_EXHAUSTION_SHORT
        M159: val60m in -0.90..-0.40 and val5m <= -0.55 -> [DOWN] CROSS_HORIZON_BEARISH_VELOCITY
        M161: abs(val60m) <= 0.08 and val5m <= -0.38 -> [DOWN] HOURLY_CONSOLIDATION_5M_BREAKDOWN_DOWN
        M163: val60m in -0.05..0.15 and val5m <= -0.30 -> [DOWN] MACRO_STRUCTURAL_SUPPLY_DISTRIBUTION
        M165: val5m <= -0.50 and val60m <= -0.50 -> [DOWN] TOTAL_HORIZON_RESONANCE_BEARISH

        ### MANDATORY RESPONSE FORMAT (STRICT BENGALI OUTPUT, NO INTRO, NO OUTRO):
        গাণিতিক হিসাব:
        ৫ মিনিটের পরিবর্তন: [Extracted 5m]%
        ৬০ মিনিটের পরিবর্তন: [Extracted 60m]%

        ম্যাট্রিক্স স্ট্যাটাস:
        ম্যাচড ম্যাট্রিক্স ID: [Match ID e.g., M025 or NONE]
        আউটপুট কোড: [Matching Code e.g., ALIGNED_BULLISH_EARLY_BREAKOUT or NO_MATCH]

        ফলাফল:
        দিক: [UP / DOWN / HOLD]
    """.trimIndent()

    suspend fun analyzeFrame(bitmap: Bitmap, apiKey: String): TradingAnalysis = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val trimmedKey = apiKey.replace(Regex("[\\s\\t\\r\\n]+"), "").trim()

        Log.d(TAG, "=== Starting Frame Analysis ===")
        Log.d(TAG, "Bitmap size: ${bitmap.width}x${bitmap.height}, config: ${bitmap.config}")

        if (trimmedKey.isBlank() || com.example.MainViewModel.isPlaceholderApiKey(trimmedKey)) {
            val err = "API key is not set or placeholder key provided (Please set a valid Gemini API Key)"
            Log.e(TAG, "Error: $err")
            return@withContext TradingAnalysis(
                isSuccess = false,
                errorMessage = err,
                latencyMs = 0,
                engineSource = "Gemini Cloud"
            )
        }

        try {
            // Convert bitmap to Base64 JPEG bytes
            val base64Image = bitmapToBase64(bitmap)
            Log.d(TAG, "Base64 image length: ${base64Image.length} characters")

            val requestPayload = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(text = "Analyze this chart image. Extract '5 min change' and '60 min change' values and calculate Net Sum, Strength Level, and Direction (UP/DOWN) with Final percentage as specified."),
                            GeminiPart(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                systemInstruction = GeminiContent(
                    parts = listOf(GeminiPart(text = systemInstructionText))
                ),
                generationConfig = GenerationConfig(
                    temperature = 0.1f,
                    topP = 0.95f,
                    maxOutputTokens = 512
                )
            )

            val jsonBody = requestAdapter.toJson(requestPayload)
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
            val maskedKey = if (trimmedKey.length > 8) "${trimmedKey.take(4)}...${trimmedKey.takeLast(4)}" else "***"

            var lastStatusCode = 0
            var lastErrorMessage = ""
            var lastResponseBody = ""
            var lastRetrySeconds: Int? = null
            var networkExceptionMessage: String? = null

            // Try candidate models in order if one experiences high demand (503), not found (404), or server error (500)
            for ((index, modelName) in MODEL_CANDIDATES.withIndex()) {
                val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$trimmedKey"
                Log.d(TAG, "Dispatching HTTP POST to $modelName (attempt ${index + 1}/${MODEL_CANDIDATES.size}) with key [$maskedKey]")

                val httpRequest = Request.Builder()
                    .url(endpointUrl)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("x-goog-api-key", trimmedKey)
                    .post(requestBody)
                    .build()

                val rawResponse = try {
                    okHttpClient.newCall(httpRequest).execute()
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    val netMsg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
                    networkExceptionMessage = netMsg
                    Log.w(TAG, "Network call failed for $modelName: $netMsg")
                    continue
                }

                val latencyMs = System.currentTimeMillis() - startTime

                val analysisResult: TradingAnalysis? = rawResponse.use { response ->
                    val statusCode = response.code
                    val responseString = response.body?.string().orEmpty()
                    lastStatusCode = statusCode
                    lastResponseBody = responseString

                    Log.d(TAG, "HTTP Status for $modelName: $statusCode ${response.message} (latency: ${latencyMs}ms)")

                    if (!response.isSuccessful) {
                        val errorParsed = try {
                            responseAdapter.fromJson(responseString)?.error?.message
                        } catch (e: Exception) {
                            null
                        }
                        val errorMessage = errorParsed ?: "HTTP $statusCode (${response.message}): $responseString"
                        lastErrorMessage = errorMessage
                        Log.w(TAG, "API Request non-successful for $modelName: $errorMessage")

                        val is429 = statusCode == 429 || 
                            errorMessage.contains("Quota", ignoreCase = true) || 
                            errorMessage.contains("rate-limit", ignoreCase = true) ||
                            errorMessage.contains("rate limit", ignoreCase = true) ||
                            errorMessage.contains("RESOURCE_EXHAUSTED", ignoreCase = true)

                        if (is429) {
                            lastRetrySeconds = extractRetrySeconds(errorMessage, response.header("Retry-After")) ?: 20
                        }

                        val isModelUnavailableOrHighDemand = statusCode == 503 || 
                            statusCode == 500 || 
                            statusCode == 404 ||
                            errorMessage.contains("high demand", ignoreCase = true) || 
                            errorMessage.contains("overloaded", ignoreCase = true) ||
                            errorMessage.contains("not found", ignoreCase = true) ||
                            errorMessage.contains("is not supported", ignoreCase = true)

                        // If it's a transient server spike, model-not-found, or high demand on a specific model, try next candidate
                        if (isModelUnavailableOrHighDemand && index < MODEL_CANDIDATES.size - 1) {
                            Log.w(TAG, "Model $modelName unavailable (status=$statusCode). Trying next fallback candidate...")
                            return@use null
                        }

                        // Otherwise return failure (if 429 rate limit, stop hammering other endpoints and return quota)
                        val finalErrorMsg = if (is429) {
                            "API Quota Exceeded [429 Quota Exceeded]: Retrying in $lastRetrySeconds seconds"
                        } else {
                            "API Error [$statusCode]: $errorMessage"
                        }
                        return@withContext TradingAnalysis(
                            isSuccess = false,
                            errorMessage = finalErrorMsg,
                            rawResponse = responseString,
                            latencyMs = latencyMs,
                            isQuotaExceeded = is429,
                            retryAfterSeconds = lastRetrySeconds
                        )
                    }

                    val parsedResponse = try {
                        responseAdapter.fromJson(responseString)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse Gemini response JSON from $modelName", e)
                        null
                    }

                    val generatedText = parsedResponse?.candidates?.firstOrNull()
                        ?.content?.parts?.firstOrNull()?.text.orEmpty().trim()

                    Log.d(TAG, "Extracted generated text from $modelName:\n$generatedText")

                    if (generatedText.isBlank()) {
                        val emptyErr = "No text received from model (Empty response from $modelName)"
                        Log.e(TAG, emptyErr)
                        if (index < MODEL_CANDIDATES.size - 1) return@use null
                        return@withContext TradingAnalysis(
                            isSuccess = false,
                            errorMessage = emptyErr,
                            rawResponse = responseString,
                            latencyMs = latencyMs
                        )
                    }

                    val parsed = TradingOutputParser.parse(generatedText, latencyMs)
                    Log.d(TAG, "Analysis parsed result: success=${parsed.isSuccess}, dir=${parsed.direction}, 5m=${parsed.change5m}, 60m=${parsed.change60m}, net=${parsed.netSum}")
                    return@withContext parsed
                }

                if (analysisResult != null) {
                    return@withContext analysisResult
                }
            }

            // If loop exhausted
            val latencyMs = System.currentTimeMillis() - startTime
            val finalErrorMessage = when {
                lastStatusCode == 429 -> "API Quota Exceeded [429 Quota Exceeded]"
                lastStatusCode > 0 -> "API Error [$lastStatusCode]: $lastErrorMessage"
                networkExceptionMessage != null -> "Unable to connect to internet or Gemini server. Please try again later ($networkExceptionMessage)"
                else -> "Unable to connect to internet or Gemini server. Please try again later."
            }

            return@withContext TradingAnalysis(
                isSuccess = false,
                errorMessage = finalErrorMessage,
                rawResponse = lastResponseBody,
                latencyMs = latencyMs,
                isQuotaExceeded = lastStatusCode == 429,
                retryAfterSeconds = lastRetrySeconds ?: 20
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "analyzeFrame coroutine cancelled normally")
                throw e
            }
            val latencyMs = System.currentTimeMillis() - startTime
            val exceptionMessage = e.localizedMessage ?: e.message ?: e.javaClass.simpleName
            Log.e(TAG, "Exception during analyzeFrame: $exceptionMessage", e)
            val isExceeded = exceptionMessage.contains("rate limit", ignoreCase = true) ||
                    exceptionMessage.contains("rate-limit", ignoreCase = true) ||
                    exceptionMessage.contains("Quota", ignoreCase = true) ||
                    exceptionMessage.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                    exceptionMessage.contains("429")
            val retrySec = if (isExceeded) extractRetrySeconds(exceptionMessage, null) ?: 20 else null
            return@withContext TradingAnalysis(
                isSuccess = false,
                errorMessage = if (isExceeded) "API Quota Exceeded [429 Quota Exceeded]: Retrying in $retrySec seconds" else "Error: $exceptionMessage",
                latencyMs = latencyMs,
                isQuotaExceeded = isExceeded,
                retryAfterSeconds = retrySec
            )
        }
    }

    private fun extractRetrySeconds(errorMsg: String, retryAfterHeader: String?): Int? {
        if (!retryAfterHeader.isNullOrBlank()) {
            retryAfterHeader.toIntOrNull()?.let { return (it + 1).coerceIn(15, 75) }
        }
        val patterns = listOf(
            Regex("""retry in\s+([\d\.]+)\s*s""", RegexOption.IGNORE_CASE),
            Regex("""retry after\s+([\d\.]+)\s*s""", RegexOption.IGNORE_CASE),
            Regex("""reset in\s+([\d\.]+)\s*s""", RegexOption.IGNORE_CASE),
            Regex("""wait\s+([\d\.]+)\s*s""", RegexOption.IGNORE_CASE)
        )
        for (pattern in patterns) {
            val match = pattern.find(errorMsg)
            if (match != null) {
                val secondsFloat = match.groupValues.getOrNull(1)?.toFloatOrNull()
                if (secondsFloat != null) {
                    return (secondsFloat.toInt() + 1).coerceIn(15, 75)
                }
            }
        }
        return 20
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDim = 1024
        val isScaled = bitmap.width > maxDim || bitmap.height > maxDim
        val scaledBitmap = if (isScaled) {
            val ratio = maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
            val newWidth = (bitmap.width * ratio).toInt()
            val newHeight = (bitmap.height * ratio).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        return try {
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } finally {
            if (isScaled && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
        }
    }
}
