package com.example.data.models

import com.example.data.analyzer.ConfirmationStage
import com.example.data.analyzer.NextMovementBias

enum class TradeDirection {
    UP,
    DOWN,
    NEUTRAL
}

enum class StrengthLevel(val bengaliLabel: String, val englishLabel: String, val strictName: String) {
    NORMAL("Normal", "Normal (< 0.50%)", "Normal"),
    MEDIUM("Medium", "Medium (0.50% - 0.99%)", "Medium"),
    HIGH("High", "High (>= 1.00%)", "High")
}

enum class TradeOutcome(val label: String, val colorHex: Long) {
    PROFIT("PROFIT", 0xFF00E676),
    LOSS("LOSS", 0xFFFF1744)
}

enum class SignalType(val bengaliTitle: String) {
    NONE("No Active Signal"),
    STANDARD_SIGNAL("Standard Momentum Signal"),
    MOMENTUM_LOSS("⚠️ Momentum Loss (MOMENTUM LOSS)"),
    TOP_FAKEOUT_SELL("🚨 Top Fakeout Sell Signal (TOP FAKEOUT SELL)"),
    BOTTOM_FAKEOUT_BUY("🚀 Bottom Fakeout Buy Signal (BOTTOM FAKEOUT BUY)")
}

enum class EngineMode(val bengaliTitle: String, val subtitle: String) {
    AUTO("Auto (Smart Fallback)", "Cloud if API available, otherwise on-device engine"),
    LOCAL("On-Device (Local Vision - No API)", "100% Offline Detection, Zero Quota Limit, Ultra Fast"),
    CLOUD("Cloud AI (Gemini Flash)", "Google Cloud Vision Model")
}

data class TradingAnalysis(
    val id: String = java.util.UUID.randomUUID().toString(),
    val change5m: String = "--",
    val change5mValue: Double? = null,
    val change60m: String = "--",
    val change60mValue: Double? = null,
    val change1d: String = "--",
    val change1dValue: Double? = null,
    /**
     * Bounded weighted reactive movement score from ReactiveMarketPressureEngine (v58+).
     * Retained as `netSum` and `netSumValue` for backward compatibility across UI and matrix components.
     */
    val netSum: String = "--",
    val netSumValue: Double? = null,
    val totalMagnitude: Double = 0.0,
    val sensitivityRatio: Double = 0.0,
    val alignmentScore: Double = 0.0,
    val magnitudeScore: Double = 0.0,
    val evidenceScore: Double = 0.0,
    val strengthLevel: StrengthLevel = StrengthLevel.NORMAL,
    val direction: TradeDirection = TradeDirection.NEUTRAL,
    val calculatedPercentage: Double = 50.0,
    val upPercentage: Double = 50.0,
    val downPercentage: Double = 50.0,
    val audioEvent: String = "SOUND_NONE",
    val rawResponse: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean = true,
    val isValid: Boolean = true,
    val errorMessage: String? = null,
    val latencyMs: Long = 0,
    val isQuotaExceeded: Boolean = false,
    val retryAfterSeconds: Int? = null,
    val engineSource: String = "On-Device OCR (100% Local)",
    val hasValueChanged: Boolean = false,
    val isNoTradeZone: Boolean = false,
    val outcome: TradeOutcome? = null,
    val signalType: SignalType = SignalType.NONE,
    val dataQuality: String = DataQualityState.VERIFIED,
    val isApproximate: Boolean = false,
    val isProvisional: Boolean = false,
    val behaviorCode: String = "",
    val behaviorTitle: String = "",
    val behaviorDescription: String = "",
    val behaviorTags: List<String> = emptyList(),
    val isWarningOnly: Boolean = false,
    val dailyContext: String = "NEUTRAL",
    val is1dExplicitlyRejected: Boolean = false,
    val confirmationStage: ConfirmationStage = ConfirmationStage.UNKNOWN,
    val nextMovementBias: NextMovementBias = NextMovementBias.UNKNOWN,
    val mtfCalculatedPercentage: Double = 50.0,
    val mtfUpPercentage: Double = 50.0,
    val mtfDownPercentage: Double = 50.0,
    val totalMatricesCount: Int = 165,
    val evaluatedMatricesCount: Int = 165,
    val skippedMatricesCount: Int = 0,
    val failedMatricesCount: Int = 0,
    val failureTraces: List<String> = emptyList(),
    val matchedMatrixIds: List<String> = emptyList(),
    val primaryMatrixId: String? = null,
    val primaryMatrixTitle: String? = null,
    val primaryMatrixDescription: String? = null,
    val primaryMatrixRiskLevel: String? = null,
    val pressureFingerprint: String? = null,
    val decisionTrace: List<String> = emptyList(),
    val rejectedMatrixNotes: List<String> = emptyList(),
    val kineticBaseEnergy: Double = 0.0,
    val microVelocity: Double = 0.0,
    val netKineticForce: Double = 0.0,
    val kineticUpWeight: Double = 50.0,
    val kineticDownWeight: Double = 50.0,
    val forensicPatternCode: String = "",
    val forensicPatternTitle: String = "",
    val forensicDiagnosis: String = "",
    val isBrakeInertiaPullback: Boolean = false,
    val canonicalDecision: CanonicalDecision? = null,
    val matrixRecords: List<com.example.data.matrix.EvaluatedMatrixRecord> = emptyList()
) {
    val isDeadMarket: Boolean
        get() = isValid && isSuccess && (isNoTradeZone || (change5mValue != null && change60mValue != null && !change5mValue.isNaN() && !change60mValue.isNaN() && !change5mValue.isInfinite() && !change60mValue.isInfinite() && kotlin.math.abs(change5mValue) <= 0.10 && kotlin.math.abs(change60mValue) <= 0.10))
}

data class MetricSnapshot(
    val val5m: Double,
    val val60m: Double,
    val val1d: Double? = null,
    val netSum: Double = val5m + val60m,
    val timestamp: Long = System.currentTimeMillis(),
    val isValid: Boolean = true,
    val isApproximate: Boolean = false,
    val open: Double? = null,
    val high: Double? = null,
    val low: Double? = null,
    val close: Double? = null
)

object DataQualityState {
    const val VERIFIED = "VERIFIED"
    const val PARTIALLY_VERIFIED = "PARTIALLY_VERIFIED"
    const val APPROXIMATE = "APPROXIMATE"
    const val AMBIGUOUS = "AMBIGUOUS"
    const val INCOMPLETE = "INCOMPLETE"
    const val UNAVAILABLE = "UNAVAILABLE"
    const val CONFLICTING_EVIDENCE = "CONFLICTING_EVIDENCE"
    const val NO_TRADE = "NO_TRADE"
}

data class AnalyzerUiState(
    val isScanning: Boolean = true,
    val isProcessing: Boolean = false,
    val currentAnalysis: TradingAnalysis? = null,
    val history: List<TradingAnalysis> = emptyList(),
    val apiKey: String = "",
    val scanIntervalMs: Long = 10L,
    val isAutoTradeEnabled: Boolean = false,
    val statusMessage: String = "LIVE OCR SCANNING",
    val selectedCameraLens: Int = 0, // 0 = BACK, 1 = FRONT
    val isTorchOn: Boolean = false,
    val isAudioAlertEnabled: Boolean = true,
    val selectedTestImageId: Int? = null,
    val error: String? = null,
    val frameCount: Long = 0L,
    val cooldownRemainingSeconds: Int = 0,
    val cooldownTotalSeconds: Int = 20,
    val investmentAmount: Double = 100.0,
    val payoutPercentage: Double = 85.0,
    val profitCount: Int = 0,
    val lossCount: Int = 0,
    val engineMode: EngineMode = EngineMode.LOCAL,
    val lastChangedTimestamp: Long = 0L,
    val analyzedFrameCount: Long = 0L,
    val droppedFrameCount: Long = 0L,
    val lastOcrLatencyMs: Long = 0L,
    val isConfirmedOnlyMode: Boolean = false,
    val isAntiGlitchConfirmationEnabled: Boolean = true,
    val isAdaptiveCpuProtectionEnabled: Boolean = true,
    val decisionMode: DecisionMode = DecisionMode.LEGACY_MULTILAYER,
    val cameraRestartTrigger: Long = 0L
)
