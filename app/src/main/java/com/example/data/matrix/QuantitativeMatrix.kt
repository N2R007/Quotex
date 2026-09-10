package com.example.data.matrix

import com.example.data.models.MetricSnapshot
import com.example.data.models.TradeDirection

/**
 * Required input dimensions for quantitative matrix decision rules.
 */
enum class InputType {
    CHANGE_5M,
    CHANGE_60M,
    CHANGE_1D,
    SNAPSHOT_HISTORY,
    DATA_QUALITY
}

/**
 * Deterministic quantitative risk classification.
 */
enum class RiskLevel(val bengaliLabel: String) {
    LOW("স্বল্প ঝুঁকি (Low Risk)"),
    MODERATE("মাঝারি ঝুঁকি (Moderate Risk)"),
    HIGH("উচ্চ ঝুঁকি (High Risk)"),
    EXTREME("চরম ঝুঁকি / নো-ট্রেড (Extreme Risk)"),
    NO_TRADE("ট্রেড নিষিদ্ধ (No-Trade Zone)")
}

/**
 * Deterministic multi-timeframe alignment regime.
 */
enum class MatrixAlignment {
    ALIGNED_BULLISH,
    ALIGNED_BEARISH,
    COUNTER_TREND_PULLBACK_UP,
    COUNTER_TREND_PULLBACK_DOWN,
    CONFLICTING,
    NEUTRAL_UNRESOLVED
}

/**
 * Historical pattern classification.
 */
enum class HistoricalPattern {
    FIRST_OBSERVATION,
    STABLE_TREND,
    ACCELERATING,
    DECELERATING,
    MOMENTUM_LOSS,
    PULLBACK,
    POSSIBLE_REVERSAL,
    CONFIRMED_REVERSAL,
    REPEATED_ALTERNATION,
    SUDDEN_SPIKE,
    INSUFFICIENT_HISTORY
}

/**
 * Test vector for unit test and matrix verification.
 */
data class MatrixTestVector(
    val val5m: Double,
    val val60m: Double,
    val val1d: Double? = null,
    val dataQuality: String = "VERIFIED",
    val history: List<MetricSnapshot> = emptyList(),
    val expectedMatched: Boolean = true,
    val expectedOutputCode: String = "",
    val expectedDirection: TradeDirection = TradeDirection.NEUTRAL
)

/**
 * Runtime context passed into each matrix evaluator.
 */
data class MatrixEvaluationContext(
    val val5m: Double,
    val val60m: Double,
    val val1d: Double? = null,
    val rawNetSum: Double = val5m + val60m,
    val netSum: Double = rawNetSum,
    val normalizedMovement: Double = 0.0,
    val totalMagnitude: Double = kotlin.math.abs(val5m) + kotlin.math.abs(val60m),
    val dataQuality: String = "VERIFIED",
    val isNoTradeZone: Boolean = false,
    val history: List<MetricSnapshot> = emptyList(),
    val is5mPresent: Boolean = true,
    val is60mPresent: Boolean = true,
    val is1dPresent: Boolean = val1d != null
)

/**
 * Deterministic User & System State for each Quantitative Matrix.
 */
enum class MatrixUserState {
    CHECKED,
    CANCELLED,
    UNCHECKED
}

/**
 * Resolves standard category name for a Matrix by its sequential ID.
 */
fun resolveCategoryForMatrix(id: String): String {
    val num = id.removePrefix("M").toIntOrNull() ?: return "Multi-timeframe alignment"
    return when (num) {
        in 1..14 -> "Data quality"
        in 15..24 -> "Dead-zone/no-trade"
        in 25..44 -> "Multi-timeframe alignment"
        in 45..64 -> "Multi-timeframe alignment"
        in 65..80 -> "Pullback/reversal"
        in 81..94 -> "Momentum"
        in 95..108 -> "Safety/conflict"
        in 109..120 -> "Volatility"
        in 121..132 -> "Macro/1D context"
        in 133..150 -> "Micro pressure"
        in 151..165 -> "Multi-timeframe confluence"
        else -> "Multi-timeframe alignment"
    }
}

/**
 * Declarative specification for a Deterministic Quantitative Decision Matrix.
 */
data class QuantitativeMatrix(
    val id: String,
    val title: String,
    val description: String,
    /**
     * Numerical priority: Higher value indicates higher evaluation precedence and conflict resolution dominance.
     * Scale: 900+ (Safety Interlocks & Kill-Switches), 800+ (Structural Traps & High-Risk Reversals),
     * 700+ (Momentum & Dynamic Velocity), 500-600 (Standard Trend Alignment), <500 (Baseline/Low-Sensitivity).
     */
    val priority: Int,
    val requiredInputs: Set<InputType>,
    val conditionDescription: String,
    val outputCode: String,
    val direction: TradeDirection,
    val riskLevel: RiskLevel,
    val warningOnly: Boolean = false,
    val requiresHistory: Boolean = false,
    val testVector: MatrixTestVector,
    val evaluator: (MatrixEvaluationContext) -> Boolean,
    val category: String = resolveCategoryForMatrix(id)
)

/**
 * Evaluated runtime record of an individual Matrix for explainability, soft scoring,
 * and strict user state enforcement.
 */
data class EvaluatedMatrixRecord(
    val matrixId: String,
    val title: String,
    val category: String,
    val userState: MatrixUserState,
    val direction: TradeDirection,
    val rawScore: Double,
    val normalizedScore: Double,
    val reliabilityWeight: Double,
    val matched: Boolean,
    val cancelledReason: String? = null,
    val contributesToTrade: Boolean,
    val explanation: String
) {
    val isContributedToTrade: Boolean get() = contributesToTrade
    val softEvidenceScore: Double get() = normalizedScore
}

/**
 * Shared registry managing user & system state for all 165 Matrices.
 * Strictly preserves current UI mapping (Tick = CHECKED, Cancel = CANCELLED).
 * Persists user overrides across app restarts using SharedPreferences.
 */
object MatrixUserStateRegistry {
    private const val PREFS_NAME = "quant_matrix_user_states"
    private var preferences: android.content.SharedPreferences? = null
    private val userOverrides = java.util.concurrent.ConcurrentHashMap<String, MatrixUserState>()

    /**
     * Initializes the registry with an Android Context to restore persisted user states.
     * Enforces user mandate: Exactly 106 directional matrices active (M065-M080 excluded/cancelled).
     */
    fun init(context: android.content.Context) {
        try {
            val appCtx = context.applicationContext ?: context
            preferences = appCtx.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val isMigrated = preferences?.getBoolean("mandate_106_directional_matrices_v2", false) == true
            if (!isMigrated) {
                // Clear old legacy states to enforce user command: M065-M080 excluded, 106 directional matrices active
                preferences?.edit()?.clear()?.putBoolean("mandate_106_directional_matrices_v2", true)?.apply()
                userOverrides.clear()
            } else {
                preferences?.all?.forEach { (key, value) ->
                    if (key != "mandate_106_directional_matrices_v2" && value is String) {
                        try {
                            userOverrides[key] = MatrixUserState.valueOf(value)
                        } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Returns the inherent default state of a matrix without user overrides.
     * Newly added authorized matrices (M065, M066, M067, M068, M070, M071, M072, M073, M074, M075, M076, M077)
     * are CHECKED. Non-directional/warning matrices (M069, M078, M079, M080) are CANCELLED.
     */
    fun getDefaultState(matrix: QuantitativeMatrix): MatrixUserState {
        val idNum = matrix.id.removePrefix("M").toIntOrNull() ?: 0
        if (idNum in setOf(69, 78, 79, 80)) {
            return MatrixUserState.CANCELLED
        }
        return MatrixUserState.CHECKED
    }

    /**
     * Checks if a matrix has an explicit user override.
     */
    fun hasUserOverride(matrixId: String): Boolean = userOverrides.containsKey(matrixId)

    /**
     * Resolves the active MatrixUserState.
     */
    fun getUserState(matrix: QuantitativeMatrix): MatrixUserState {
        userOverrides[matrix.id]?.let { return it }
        return getDefaultState(matrix)
    }

    fun getState(matrixId: String): MatrixUserState {
        userOverrides[matrixId]?.let { return it }
        val m = MatrixCatalog.getById(matrixId)
        return if (m != null) getUserState(m) else {
            val idNum = matrixId.removePrefix("M").toIntOrNull() ?: 0
            if (idNum in setOf(69, 78, 79, 80)) MatrixUserState.CANCELLED else MatrixUserState.CHECKED
        }
    }

    fun setUserState(matrixId: String, state: MatrixUserState) {
        userOverrides[matrixId] = state
        try {
            preferences?.edit()?.putString(matrixId, state.name)?.apply()
        } catch (_: Exception) {}
    }

    fun setState(matrixId: String, state: MatrixUserState) {
        setUserState(matrixId, state)
    }

    fun getAllStates(allMatrices: List<QuantitativeMatrix>): Map<String, MatrixUserState> {
        return allMatrices.associate { it.id to getUserState(it) }
    }

    /**
     * Resets matrices to user mandate (106 directional active, M065-M080 excluded).
     */
    fun enableAll165() {
        userOverrides.clear()
        try {
            preferences?.edit()?.clear()?.putBoolean("mandate_106_directional_matrices_v2", true)?.apply()
        } catch (_: Exception) {}
    }

    fun reset() {
        enableAll165()
    }
}

/**
 * Evaluation trace record for full transparency and explainability.
 */
data class MatrixTraceItem(
    val matrixId: String,
    val title: String,
    val matched: Boolean,
    val priority: Int,
    val reason: String
)

/**
 * Output of the multi-matrix evaluation engine.
 */
data class MatrixEvaluationResult(
    val totalMatricesCount: Int,
    val evaluatedMatricesCount: Int,
    val matchedMatrices: List<QuantitativeMatrix>,
    val primaryMatrix: QuantitativeMatrix?,
    val isConflicting: Boolean,
    val conflictReason: String? = null,
    val decisionTrace: List<String>,
    val rejectedMatrixNotes: List<String>,
    val skippedMatricesCount: Int = 0,
    val failedMatricesCount: Int = 0,
    val failureTraces: List<String> = emptyList(),
    val matrixRecords: List<EvaluatedMatrixRecord> = emptyList(),
    val categoryScores: Map<String, Double> = emptyMap(),
    val matrixEvidence: Double = 0.0,
    val eligibleMatrixIds: List<String> = emptyList(),
    val cancelledMatrixIds: List<String> = emptyList(),
    val uncheckedMatrixIds: List<String> = emptyList(),
    val activeCheckedMatchesCount: Int = 0
)
