package com.example.data.models

/**
 * Single Authoritative Quantitative Decision Model.
 *
 * This immutable data class represents the sole authoritative decision for:
 * - Trade direction (UP, DOWN, NEUTRAL) and execution side ("BUY", "SELL", "NONE")
 * - Directional scores (upPercentage, downPercentage - strictly directional momentum scores, NOT win-rate or profit probabilities)
 * - Micro energy bias and multi-component evidence aggregation
 * - Execution eligibility for automated trade dispatch
 * - Multi-frame confirmation lifecycle and staleness detection
 *
 * ReactiveMarketPressureEngine, MatrixEvaluationEngine (165 Matrices),
 * MicroKineticVectorEngine, and MicroPressureDecisionEngine feed into
 * CanonicalDecisionEngine as feature extractors, but NONE may independently
 * overwrite the canonical decision.
 */
data class CanonicalDecision(
    val decisionId: String = java.util.UUID.randomUUID().toString(),
    val direction: TradeDirection,
    val side: String = when (direction) {
        TradeDirection.UP -> "BUY"
        TradeDirection.DOWN -> "SELL"
        else -> "NONE"
    },
    val upPercentage: Double,
    val downPercentage: Double,
    val strength: Double,
    val netEnergy: Double = 0.0,
    val matrixEvidence: Double = 0.0,
    val microPressure: Double = 0.0,
    val candleConfirmation: Double = 0.0,
    val conflictIndex: Double = 0.0,
    val dataQuality: String,
    val eligibleMatrixIds: List<String> = emptyList(),
    val cancelledMatrixIds: List<String> = emptyList(),
    val fingerprint: String,
    val explanation: String,
    val timestamp: Long = System.currentTimeMillis(),
    val executionEligibility: Boolean,

    // Legacy and auxiliary properties for backward compatibility
    val upPct: Double = upPercentage,
    val downPct: Double = downPercentage,
    val strengthScore: Double = strength,
    val strengthLevel: StrengthLevel = StrengthLevel.NORMAL,
    val noTrade: Boolean = (direction == TradeDirection.NEUTRAL || side == "NONE"),
    val warningOnly: Boolean = false,
    val confirmationCount: Int = 1,
    val requiredConfirmations: Int = 1,
    val confirmationStage: String = "CONFIRMED",
    val stale: Boolean = false,
    val approximate: Boolean = false,
    val conflict: Boolean = false,
    val reason: String = explanation,
    val authoritativeSource: String = "CanonicalQuantitativeAuthority",
    val primaryMatrixId: String? = null,
    val primaryMatrixTitle: String? = null,
    val kineticVelocity: Double = 0.0,
    val kineticEnergy: Double = 0.0,
    val netKineticForce: Double = 0.0,
    val kineticUpWeight: Double = upPercentage,
    val kineticDownWeight: Double = downPercentage,
    val forensicPatternCode: String = "",
    val formulaExplanation: String = "",

    // Evidence-weighted micro-pressure analytical details
    val rawScore: Double = 0.0,
    val adjustedScore: Double = 0.0,
    val adaptiveNoiseFloor: Double = 0.05,
    val upEnergy: Double = 0.0,
    val downEnergy: Double = 0.0,
    val energyDominance: Double = 0.0,
    val isCandleDataUnavailable: Boolean = true,
    val historyInsufficient: Boolean = false,
    val velocityConfirmation: Double = 0.0,
    val accelerationConfirmation: Double = 0.0,
    val persistenceConfirmation: Double = 0.0
) {
    // Convenient alias properties matching previous API conventions
    val isNoTrade: Boolean get() = noTrade
    val isWarningOnly: Boolean get() = warningOnly
    val isStale: Boolean get() = stale
    val isApproximate: Boolean get() = approximate
    val isConflicting: Boolean get() = conflict
    val isExecutionEligible: Boolean get() = executionEligibility
}
