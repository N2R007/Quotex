package com.example.data.analyzer

import kotlin.math.abs

/**
 * Gate validation result for canonical trade execution.
 */
sealed class GateValidationResult {
    object Eligible : GateValidationResult()
    data class Ineligible(val reason: String) : GateValidationResult()
}

/**
 * Thread-safe cooldown and duplicate detection gate for quantitative signals.
 * Default cooldownMs = 0L to allow instant auto-trade dispatch per project guidelines,
 * but allows parameterized cooldown windows for testing or conservative configurations.
 */
class SignalCooldownGate(
    val cooldownMs: Long = 0L,
    val epsilon: Double = 0.005
) {
    @Volatile
    private var lastEmittedTimeMs: Long = 0L

    @Volatile
    private var lastP5m: Double? = null

    @Volatile
    private var lastP60m: Double? = null

    @Volatile
    private var lastP1d: Double? = null

    @Volatile
    private var lastDirection: PressureDirection? = null

    @Volatile
    private var lastPressureBand: PressureBand? = null

    fun canEmit(nowElapsedMs: Long): Boolean {
        if (cooldownMs <= 0L) return true
        return (nowElapsedMs - lastEmittedTimeMs) >= cooldownMs
    }

    fun remainingMs(nowElapsedMs: Long): Long {
        if (cooldownMs <= 0L) return 0L
        val elapsed = nowElapsedMs - lastEmittedTimeMs
        return (cooldownMs - elapsed).coerceAtLeast(0L)
    }

    fun markEmitted(
        nowElapsedMs: Long,
        p5m: Double,
        p60m: Double,
        p1d: Double,
        direction: PressureDirection,
        pressureBand: PressureBand
    ) {
        lastEmittedTimeMs = nowElapsedMs
        lastP5m = p5m
        lastP60m = p60m
        lastP1d = p1d
        lastDirection = direction
        lastPressureBand = pressureBand
    }

    fun isSnapshotFresh(snapshotCapturedElapsedMs: Long, nowElapsedMs: Long): Boolean {
        if (!canEmit(nowElapsedMs)) return false
        return snapshotCapturedElapsedMs >= lastEmittedTimeMs + cooldownMs
    }

    fun isDuplicateFingerprint(
        p5m: Double,
        p60m: Double,
        p1d: Double,
        direction: PressureDirection,
        pressureBand: PressureBand
    ): Boolean {
        val prevP5 = lastP5m ?: return false
        val prevP60 = lastP60m ?: return false
        val prevP1d = lastP1d ?: return false
        val prevDir = lastDirection ?: return false
        val prevBand = lastPressureBand ?: return false

        if (direction != prevDir || pressureBand != prevBand) return false

        val diff5 = abs(p5m - prevP5)
        val diff60 = abs(p60m - prevP60)
        val diff1d = abs(p1d - prevP1d)

        return diff5 <= epsilon && diff60 <= epsilon && diff1d <= epsilon
    }
}

/**
 * Gate validator validating pre-execution prerequisites before trade dispatch.
 */
object CanonicalExecutionGate {
    fun validate(
        pressureResult: ThreeTimeframePressureResult,
        cooldownGate: SignalCooldownGate,
        nowElapsedMs: Long,
        snapshotCapturedElapsedMs: Long,
        isKillSwitchActive: Boolean,
        isDestinationConfigured: Boolean,
        isConnectionAvailable: Boolean
    ): GateValidationResult {
        if (isKillSwitchActive) return GateValidationResult.Ineligible("Kill switch active")
        if (!isDestinationConfigured) return GateValidationResult.Ineligible("Destination not configured")
        if (!isConnectionAvailable) return GateValidationResult.Ineligible("Connection unavailable")
        if (pressureResult.isDataIncomplete || pressureResult.direction == PressureDirection.NO_SIGNAL) {
            return GateValidationResult.Ineligible("Data incomplete or no signal")
        }
        if (!cooldownGate.canEmit(nowElapsedMs)) {
            return GateValidationResult.Ineligible("Cooldown active")
        }
        val p5 = pressureResult.p5m ?: return GateValidationResult.Ineligible("Missing p5m")
        val p60 = pressureResult.p60m ?: return GateValidationResult.Ineligible("Missing p60m")
        val p1d = pressureResult.p1d ?: return GateValidationResult.Ineligible("Missing p1d")
        if (cooldownGate.isDuplicateFingerprint(p5, p60, p1d, pressureResult.direction, pressureResult.pressureBand)) {
            return GateValidationResult.Ineligible("Duplicate fingerprint")
        }
        return GateValidationResult.Eligible
    }
}
