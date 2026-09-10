package com.example.data.sample

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.audio.AudioSignalEngine
import com.example.data.analyzer.TradingOutputParser
import com.example.data.models.StrengthLevel
import com.example.data.models.TradeDirection
import com.example.data.models.TradingAnalysis
import java.util.Locale
import kotlin.math.abs

object SampleChartGenerator {

    data class SampleScenario(
        val name: String,
        val change5m: String,
        val change60m: String,
        val pair: String = "BTC/USDT"
    )

    val scenarios = listOf(
        SampleScenario("Bullish Surge", "+1.25%", "+0.75%", "BTC/USDT"),
        SampleScenario("Bearish Reversal", "-0.85%", "-0.45%", "ETH/USDT"),
        SampleScenario("Dead Market (No Trade)", "+0.03%", "-0.05%", "DOGE/USDT"),
        SampleScenario("High Volatility Up", "+2.40%", "-0.60%", "SOL/USDT"),
        SampleScenario("Normal Range Down", "-0.30%", "-0.15%", "BNB/USDT")
    )

    fun createChartBitmap(scenario: SampleScenario): Bitmap {
        val width = 800
        val height = 600
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background
        canvas.drawColor(Color.rgb(18, 22, 30))

        val bgPaint = Paint().apply {
            color = Color.rgb(26, 32, 44)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val cardRect = RectF(40f, 40f, width - 40f, height - 40f)
        canvas.drawRoundRect(cardRect, 20f, 20f, bgPaint)

        // Title / Pair
        val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("${scenario.pair} - Quantitative Metrics", 70f, 100f, textPaint)

        // Subtitle
        val subTextPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 22f
            isAntiAlias = true
        }
        canvas.drawText("Live Order Flow & Momentum Changes", 70f, 135f, subTextPaint)

        // Draw Headers & Values
        // Box 1: 5 min change
        val boxPaint1 = Paint().apply {
            color = Color.rgb(34, 43, 60)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val box1 = RectF(70f, 180f, 360f, 320f)
        canvas.drawRoundRect(box1, 16f, 16f, boxPaint1)

        val headerPaint = Paint().apply {
            color = Color.rgb(148, 163, 184)
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("5 min change", 90f, 225f, headerPaint)

        val is5mPos = scenario.change5m.startsWith("+")
        val valPaint1 = Paint().apply {
            color = if (is5mPos) Color.rgb(0, 230, 118) else Color.rgb(255, 82, 82)
            textSize = 46f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(scenario.change5m, 90f, 285f, valPaint1)

        // Box 2: 60 min change
        val box2 = RectF(400f, 180f, 690f, 320f)
        canvas.drawRoundRect(box2, 16f, 16f, boxPaint1)

        canvas.drawText("60 min change", 420f, 225f, headerPaint)

        val is60mPos = scenario.change60m.startsWith("+")
        val valPaint2 = Paint().apply {
            color = if (is60mPos) Color.rgb(0, 230, 118) else Color.rgb(255, 82, 82)
            textSize = 46f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(scenario.change60m, 420f, 285f, valPaint2)

        // Draw Candlesticks decor
        val candlePaint = Paint().apply {
            strokeWidth = 4f
            isAntiAlias = true
        }
        var candleX = 80f
        val candleYBase = 460f
        val heights = intArrayOf(30, -25, 40, -15, 60, 45, -20, 50, 70, -35, 65, 80)

        for (h in heights) {
            val isGreen = h > 0
            candlePaint.color = if (isGreen) Color.rgb(0, 230, 118) else Color.rgb(255, 82, 82)
            // Wick
            canvas.drawLine(candleX + 15f, candleYBase - h - 20f, candleX + 15f, candleYBase + 20f, candlePaint)
            // Body
            val top = if (isGreen) candleYBase - h else candleYBase
            val bot = if (isGreen) candleYBase else candleYBase - h
            canvas.drawRect(candleX, top, candleX + 30f, bot, candlePaint)
            candleX += 50f
        }

        return bitmap
    }

    fun generateAnalysisForScenario(scenario: SampleScenario, latencyMs: Long = 120L): TradingAnalysis {
        val val5m = scenario.change5m.replace("%", "").trim().toDoubleOrNull()
        val val60m = scenario.change60m.replace("%", "").trim().toDoubleOrNull()

        if (val5m == null || val60m == null) {
            return TradingAnalysis(
                isSuccess = false,
                errorMessage = "ইনপুট মান সঠিক নয় বা পার্স করা যায়নি (5m=$val5m, 60m=$val60m)",
                rawResponse = "Scenario Error: ${scenario.name}",
                latencyMs = latencyMs,
                audioEvent = AudioSignalEngine.SOUND_NONE
            )
        }

        // Unified Canonical Calculation Engine
        val quantResult = TradingOutputParser.calculateStrengthsAudited(val5m, val60m)
        val netSumVal = quantResult.netSum
        val upPercent = quantResult.upPercentage
        val downPercent = quantResult.downPercentage
        val direction = quantResult.direction
        val strength = quantResult.strengthLevel
        val audioEvent = quantResult.audioEvent
        val isNoTrade = quantResult.isNoTradeZone

        val formatted5m = TradingOutputParser.formatWithSign(val5m)
        val formatted60m = TradingOutputParser.formatWithSign(val60m)
        val formattedNet = TradingOutputParser.formatWithSign(netSumVal)

        // Mandatory Output Format (Strictly no intro, no outro)
        val rawOutput = buildString {
            appendLine("**গাণিতিক হিসাব:**")
            appendLine("* **৫ মিনিটের পরিবর্তন:** $formatted5m")
            appendLine("* **৬০ মিনিটের পরিবর্তন:** $formatted60m")
            if (isNoTrade) {
                appendLine("* **ট্রেড জোন:** 🚫 NO TRADE ZONE (০.০০% - ০.১০% ডেড মার্কেট / ফেক স্পাইক ঝুঁকি)")
            }
            appendLine()
            appendLine("**শক্তি লেভেল:** ${strength.strictName}")
            appendLine()
            appendLine("**ফলাফল:**")
            appendLine("* **UP:** ${String.format(Locale.US, "%.1f", upPercent)}%")
            appendLine("* **DOWN:** ${String.format(Locale.US, "%.1f", downPercent)}%")
            append("* **শব্দ সংকেত (Audio Event):** $audioEvent")
        }

        val behavior = TradingOutputParser.classifyMovementBehavior(
            val5m = val5m,
            val60m = val60m,
            val1d = null,
            history = emptyList(),
            isValid = true,
            isDeadMarket = isNoTrade,
            isNoTradeZone = isNoTrade
        )

        // 120+ Deterministic Quantitative Decision Matrices Evaluation
        val matrixContext = com.example.data.matrix.MatrixEvaluationContext(
            val5m = val5m,
            val60m = val60m,
            val1d = null,
            netSum = netSumVal,
            totalMagnitude = quantResult.totalMagnitude,
            dataQuality = "VERIFIED",
            isNoTradeZone = isNoTrade,
            history = emptyList()
        )
        val matrixResult = com.example.data.matrix.MatrixEvaluationEngine.evaluate(matrixContext)

        val determinedSignalType = when (behavior.code) {
            "TOP_FAKEOUT_RISK", "TOP_FAKEOUT_SELL" -> com.example.data.models.SignalType.TOP_FAKEOUT_SELL
            "BOTTOM_FAKEOUT_RISK", "BOTTOM_FAKEOUT_BUY" -> com.example.data.models.SignalType.BOTTOM_FAKEOUT_BUY
            "MOMENTUM_LOSS_UP", "MOMENTUM_LOSS_DOWN", "MOMENTUM_LOSS_SELL", "MOMENTUM_LOSS_BUY" -> com.example.data.models.SignalType.MOMENTUM_LOSS
            else -> if (!isNoTrade && (direction == TradeDirection.UP || direction == TradeDirection.DOWN)) {
                com.example.data.models.SignalType.STANDARD_SIGNAL
            } else {
                com.example.data.models.SignalType.NONE
            }
        }

        return TradingAnalysis(
            change5m = formatted5m,
            change5mValue = val5m,
            change60m = formatted60m,
            change60mValue = val60m,
            netSum = formattedNet,
            netSumValue = netSumVal,
            totalMagnitude = quantResult.totalMagnitude,
            sensitivityRatio = quantResult.sensitivityRatio,
            alignmentScore = quantResult.sensitivityRatio,
            magnitudeScore = quantResult.magnitudeScore,
            evidenceScore = quantResult.evidenceScore,
            strengthLevel = strength,
            direction = direction,
            calculatedPercentage = if (direction == TradeDirection.DOWN) downPercent else upPercent,
            upPercentage = upPercent,
            downPercentage = downPercent,
            audioEvent = audioEvent,
            rawResponse = rawOutput,
            isSuccess = true,
            latencyMs = latencyMs,
            engineSource = "Local Scenario Engine",
            isNoTradeZone = isNoTrade,
            dataQuality = "VERIFIED",
            signalType = determinedSignalType,
            behaviorCode = behavior.code,
            behaviorTitle = behavior.title,
            behaviorDescription = behavior.subtitle,
            behaviorTags = behavior.tags,
            isWarningOnly = behavior.isWarningOnly,
            dailyContext = behavior.dailyContext.name,
            confirmationStage = behavior.confirmationStage,
            nextMovementBias = behavior.nextMovementBias,
            totalMatricesCount = matrixResult.totalMatricesCount,
            evaluatedMatricesCount = matrixResult.evaluatedMatricesCount,
            matchedMatrixIds = matrixResult.matchedMatrices.map { it.id },
            primaryMatrixId = matrixResult.primaryMatrix?.id,
            primaryMatrixTitle = matrixResult.primaryMatrix?.title,
            primaryMatrixDescription = matrixResult.primaryMatrix?.description,
            primaryMatrixRiskLevel = matrixResult.primaryMatrix?.riskLevel?.name,
            decisionTrace = matrixResult.decisionTrace,
            rejectedMatrixNotes = matrixResult.rejectedMatrixNotes
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
