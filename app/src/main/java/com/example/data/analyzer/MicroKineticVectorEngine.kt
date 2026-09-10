package com.example.data.analyzer

import com.example.data.models.MetricSnapshot
import com.example.data.models.TradeDirection
import java.util.Locale
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Multi-Timeframe Micro-Kinetic Pressure & Velocity Vector Analysis Engine.
 *
 * Implements rigorous classical kinematic and market-microstructure physics:
 * 1. Velocity Vectors (v = dx/dt) across 5m, 60m, and 1D horizons.
 * 2. Acceleration Vectors (a = dv/dt) measuring instantaneous momentum shift.
 * 3. Inertial Kinematic Mass: m_5m = 1.0 (agile), m_60m = 3.5 (structural), m_1d = 6.0 (macro gravity).
 * 4. Micro-Kinetic Energy: E_k = 0.5 * m * v^2.
 * 5. Kinetic Impulse Force: F = m * a + gamma * m * v.
 * 6. Logistic Pressure Derivation: P_up = 100 / (1 + e^(-k * F_net)), P_down = 100 - P_up.
 * 7. Forensic Footprint Classification: Identifies smart money footprints (absorption, brake inertia,
 *    cascading velocity, bull/bear traps, energy dissipation).
 */
object MicroKineticVectorEngine {

    // Inertial mass constants for each timeframe horizon
    const val MASS_5M = 1.0
    const val MASS_60M = 3.5
    const val MASS_1D = 6.0

    // Time horizon scaling factors in seconds
    private const val HORIZON_5M_SEC = 300.0
    private const val HORIZON_60M_SEC = 3600.0
    private const val HORIZON_1D_SEC = 86400.0

    // Damping and sigmoid sensitivity constants
    private const val DAMPING_COEFFICIENT = 0.15
    private const val SIGMOID_K = 1.85
    private const val EPSILON = 1e-6

    data class TimeframeVector(
        val timeframe: String,
        val value: Double,
        val velocity: Double,            // % per second
        val acceleration: Double,        // % per second squared
        val inertiaMass: Double,
        val kineticEnergy: Double,       // 0.5 * m * v^2
        val kineticImpulse: Double       // m * a + gamma * m * v
    )

    data class ForensicMarketFootprint(
        val code: String,
        val title: String,
        val diagnosis: String,
        val rootCause: String,
        val confidenceScore: Double,
        val isBrakeInertiaPullback: Boolean = false,
        val suggestedDirection: TradeDirection = TradeDirection.NEUTRAL
    )

    data class KineticVectorResult(
        val microVelocity: Double,            // Resultant system micro-velocity (%/s)
        val kineticBaseEnergy: Double,        // Total system kinetic energy (E_k)
        val netKineticForce: Double,          // Resultant net directional force
        val kineticUpWeight: Double,          // Derived upward pressure percentage
        val kineticDownWeight: Double,        // Derived downward pressure percentage
        val vectors: Map<String, TimeframeVector>,
        val footprint: ForensicMarketFootprint,
        val mathematicalProof: String,
        val primaryDriverTimeframe: String = "5M",
        val primaryDriverExplanation: String = "",
        val isStrategicReversal: Boolean = false,
        val strategicReversalReason: String = ""
    )

    /**
     * Unified calculation entry point for Multi-Timeframe Micro-Kinetic Pressure
     * and Velocity Vector Analysis.
     */
    fun calculate(
        val5m: Double?,
        val60m: Double?,
        val1d: Double?,
        history: List<MetricSnapshot> = emptyList(),
        isApproximate: Boolean = false
    ): KineticVectorResult {
        val v5 = val5m?.takeIf { !it.isNaN() && !it.isInfinite() }
        val v60 = val60m?.takeIf { !it.isNaN() && !it.isInfinite() }
        val v1d = val1d?.takeIf { !it.isNaN() && !it.isInfinite() }

        // Chronologically order snapshots by timestamp
        val chronological = history.filter { it.isValid }.sortedBy { it.timestamp }

        // Disambiguate whether history already contains the current sample as its final element
        val (prevSnapshot, prevPrevSnapshot) = if (chronological.isNotEmpty() &&
            v5 != null && chronological.last().val5m == v5 &&
            (v60 == null || chronological.last().val60m == v60)
        ) {
            Pair(
                chronological.getOrNull(chronological.size - 2),
                chronological.getOrNull(chronological.size - 3)
            )
        } else {
            Pair(
                chronological.lastOrNull(),
                chronological.getOrNull(chronological.size - 2)
            )
        }

        // Determine elapsed time (dt) in seconds between current and previous snapshot
        val dt = if (prevSnapshot != null && prevSnapshot.timestamp > 0) {
            val baseTime = if (chronological.isNotEmpty() && chronological.last().val5m == v5 && (v60 == null || chronological.last().val60m == v60)) {
                chronological.last().timestamp
            } else {
                System.currentTimeMillis()
            }
            val elapsedSec = (baseTime - prevSnapshot.timestamp) / 1000.0
            elapsedSec.coerceIn(0.05, 300.0)
        } else {
            1.0 // Normalized initial delta-t (seconds)
        }

        // Determine prior elapsed time (dtPrev) between previous and previous-previous snapshot
        val dtPrev = if (prevSnapshot != null && prevPrevSnapshot != null && prevSnapshot.timestamp > prevPrevSnapshot.timestamp) {
            ((prevSnapshot.timestamp - prevPrevSnapshot.timestamp) / 1000.0).coerceIn(0.05, 300.0)
        } else {
            dt
        }

        // 1. Calculate Kinematic Vectors for each timeframe
        val vector5m = computeTimeframeVector(
            timeframe = "5M",
            currentVal = v5 ?: 0.0,
            prevVal = prevSnapshot?.val5m,
            prevPrevVal = prevPrevSnapshot?.val5m,
            dt = dt,
            dtPrev = dtPrev,
            mass = MASS_5M,
            horizonSec = HORIZON_5M_SEC
        )

        val vector60m = computeTimeframeVector(
            timeframe = "60M",
            currentVal = v60 ?: 0.0,
            prevVal = prevSnapshot?.val60m,
            prevPrevVal = prevPrevSnapshot?.val60m,
            dt = dt,
            dtPrev = dtPrev,
            mass = MASS_60M,
            horizonSec = HORIZON_60M_SEC
        )

        val vector1d = if (v1d != null) {
            computeTimeframeVector(
                timeframe = "1D",
                currentVal = v1d,
                prevVal = prevSnapshot?.val1d,
                prevPrevVal = prevPrevSnapshot?.val1d,
                dt = dt,
                dtPrev = dtPrev,
                mass = MASS_1D,
                horizonSec = HORIZON_1D_SEC
            )
        } else {
            null
        }

        val vectorsMap = mutableMapOf(
            "5M" to vector5m,
            "60M" to vector60m
        )
        if (vector1d != null) {
            vectorsMap["1D"] = vector1d
        }

        // 2. System Resultant Kinematics
        // Total Kinetic Energy: E_k = Sum(0.5 * m_i * v_i^2)
        val totalKineticEnergy = vector5m.kineticEnergy + vector60m.kineticEnergy + (vector1d?.kineticEnergy ?: 0.0)

        // Resultant Micro-Velocity Vector: Weighted sum of directional velocities
        val totalMass = MASS_5M + MASS_60M + (if (vector1d != null) MASS_1D else 0.0)
        val weightedVelocitySum = (vector5m.velocity * MASS_5M) +
                (vector60m.velocity * MASS_60M) +
                (vector1d?.let { it.velocity * MASS_1D } ?: 0.0)
        val systemVelocity = weightedVelocitySum / totalMass

        // Net Kinetic Force: Combining dynamic impulse and displacement positioning
        val dynamicForceSum = vector5m.kineticImpulse + vector60m.kineticImpulse + (vector1d?.kineticImpulse ?: 0.0)
        val staticDisplacementForce = (vector5m.value * 0.55) +
                (vector60m.value * 0.35) +
                (vector1d?.let { it.value * 0.10 } ?: 0.0)

        // Combined Net Force with tanh bounds
        val netRawForce = (dynamicForceSum * 0.60) + (staticDisplacementForce * 0.40)
        val netForceBounded = tanh(netRawForce)

        // Timeframe Kinetic Force Decomposition & Attribution
        val force5m = (vector5m.kineticImpulse * 0.60) + (vector5m.value * 0.55 * 0.40)
        val force60m = (vector60m.kineticImpulse * 0.60) + (vector60m.value * 0.35 * 0.40)
        val force1d = ((vector1d?.kineticImpulse ?: 0.0) * 0.60) + ((vector1d?.value ?: 0.0) * 0.10 * 0.40)

        val mag5m = abs(force5m)
        val mag60m = abs(force60m)
        val mag1d = abs(force1d)

        val (primaryDriverTf, primaryDriverExpl) = when {
            vector1d != null && mag1d > mag60m && mag1d > mag5m -> {
                Pair(
                    "1DAY (Macro Gravity)",
                    "1-Day long-term macro force (${String.format(Locale.US, "%+.2f", v1d)}%) is the primary driver behind the current candle."
                )
            }
            mag60m >= mag5m -> {
                Pair(
                    "60M (Structural Trend Momentum)",
                    "60-Min medium-term structural momentum (${String.format(Locale.US, "%+.2f", v60)}%, velocity ${String.format(Locale.US, "%+.3f", vector60m.velocity)}%/s) is the primary source of candle direction."
                )
            }
            else -> {
                Pair(
                    "5M (Instant Micro-Momentum)",
                    "5-Min rapid velocity (${String.format(Locale.US, "%+.2f", v5)}%, acceleration ${String.format(Locale.US, "%+.3f", vector5m.acceleration)}%/s²) is actively driving the candle."
                )
            }
        }

        // STRATEGIC TRAP & SUDDEN REVERSAL DETECTION (হঠাৎ উপরে গিয়ে নিচে নামা বা নিচে গিয়ে উপরে ওঠা):
        // 1. Sudden Bull Trap / Upthrust Exhaustion (হঠাৎ স্পাইক দিয়ে নিম্মমুখী কলাপ্স):
        val isBullTrap = (v5 != null && v5 > 0.05) &&
                (vector5m.acceleration < -0.05 || (vector5m.velocity < -0.02 && (v60 ?: 0.0) <= 0.0) || ((v60 ?: 0.0) < -0.25 && vector5m.velocity <= 0.02))

        // 2. Sudden Bear Trap / Spring Absorption (হঠাৎ ডিপ দিয়ে ঊর্ধ্বমুখী বাউন্স):
        val isBearTrap = (v5 != null && v5 < -0.05) &&
                (vector5m.acceleration > 0.05 || (vector5m.velocity > 0.02 && (v60 ?: 0.0) >= 0.0) || ((v60 ?: 0.0) > 0.25 && vector5m.velocity >= -0.02))

        var isStrategicReversal = false
        var strategicReversalReason = ""
        var adjustedNetForce = netForceBounded

        if (isBullTrap) {
            isStrategicReversal = true
            strategicReversalReason = "Strategic Bull Trap Detected: 5m temporary upward spike with negative acceleration (${String.format(Locale.US, "%+.3f", vector5m.acceleration)}%/s²) and strong downward momentum. Trade with the higher-probability direction (DOWN/SELL)."
            adjustedNetForce = min(-0.45, netForceBounded - 0.50)
        } else if (isBearTrap) {
            isStrategicReversal = true
            strategicReversalReason = "Strategic Bear Trap Detected: 5m false downward spike with upward acceleration (${String.format(Locale.US, "%+.3f", vector5m.acceleration)}%/s²) and active buyer support. Trade with the higher-probability direction (UP/BUY)."
            adjustedNetForce = max(0.45, netForceBounded + 0.50)
        }

        // 3. Mathematical Pressure Percentages via Logistic Sigmoid Function
        // P_up = 100 / (1 + e^(-k * F_net)), P_down = 100 - P_up
        val isDeadMarket = (v5 == null || abs(v5) <= 0.10) && (v60 == null || abs(v60) <= 0.10)
        val (rawUpPct, rawDownPct) = if (isDeadMarket && totalKineticEnergy < 0.01) {
            Pair(50.0, 50.0)
        } else {
            val logisticExponent = -SIGMOID_K * adjustedNetForce * (if (isApproximate) 0.85 else 1.0)
            val upProb = 1.0 / (1.0 + exp(logisticExponent.coerceIn(-10.0, 10.0)))
            val upPct = (upProb * 100.0).coerceIn(1.0, 99.0)
            val downPct = 100.0 - upPct
            Pair(upPct, downPct)
        }

        // Strict 2-decimal rounding with 100.00% sum invariant
        val finalUpPct = round(rawUpPct * 100.0) / 100.0
        val finalDownPct = round((100.0 - finalUpPct) * 100.0) / 100.0

        // 4. Forensic Market Footprint Analysis (মার্কেট পায়ের ছাপ ফরেনসিক বিশ্লেষণ)
        val footprint = detectForensicFootprint(
            v5 = v5 ?: 0.0,
            v60 = v60 ?: 0.0,
            v1d = v1d,
            vec5m = vector5m,
            vec60m = vector60m,
            vec1d = vector1d,
            netForce = adjustedNetForce,
            totalEnergy = totalKineticEnergy,
            isDeadMarket = isDeadMarket
        )

        // Integrate Pullback Exhaustion & Inertia Brake into Strategic Reversals
        var finalIsStrategicReversal = isStrategicReversal
        var finalStrategicReversalReason = strategicReversalReason
        if (!finalIsStrategicReversal && footprint.isBrakeInertiaPullback && footprint.suggestedDirection != TradeDirection.NEUTRAL) {
            finalIsStrategicReversal = true
            finalStrategicReversalReason = "${footprint.title}: ${footprint.diagnosis}"
        }

        val mathProof = buildString {
            appendLine("Multi-Timeframe Kinematic Power & Vector Formulation:")
            appendLine("• Primary Driver: $primaryDriverTf ($primaryDriverExpl)")
            if (finalIsStrategicReversal) {
                appendLine("• Strategic Momentum & Direction: $finalStrategicReversalReason")
            }
            appendLine("• dt = ${String.format(Locale.US, "%.3f", dt)}s (dt_prev = ${String.format(Locale.US, "%.3f", dtPrev)}s)")
            appendLine("• v_5m = ${String.format(Locale.US, "%+.4f", vector5m.velocity)}%/s, a_5m = ${String.format(Locale.US, "%+.4f", vector5m.acceleration)}%/s²")
            appendLine("• v_60m = ${String.format(Locale.US, "%+.4f", vector60m.velocity)}%/s, a_60m = ${String.format(Locale.US, "%+.4f", vector60m.acceleration)}%/s²")
            appendLine("• Total Kinetic Energy: ${String.format(Locale.US, "%.5f", totalKineticEnergy)} J")
            appendLine("• Resultant Net Force: ${String.format(Locale.US, "%+.4f", adjustedNetForce)}")
            append("• Mathematical Power: UP = $finalUpPct%, DOWN = $finalDownPct%")
        }

        return KineticVectorResult(
            microVelocity = systemVelocity,
            kineticBaseEnergy = totalKineticEnergy,
            netKineticForce = adjustedNetForce,
            kineticUpWeight = finalUpPct,
            kineticDownWeight = finalDownPct,
            vectors = vectorsMap,
            footprint = footprint,
            mathematicalProof = mathProof,
            primaryDriverTimeframe = primaryDriverTf,
            primaryDriverExplanation = primaryDriverExpl,
            isStrategicReversal = finalIsStrategicReversal,
            strategicReversalReason = finalStrategicReversalReason
        )
    }

    /**
     * Computes quantitative kinematic properties for an individual timeframe.
     * Velocity: v = (x_current - x_prev) / dt
     * Acceleration: a = (v_current - v_prev) / dt
     * Acceleration is strictly unavailable (0.0) if fewer than two previous valid snapshots exist.
     */
    private fun computeTimeframeVector(
        timeframe: String,
        currentVal: Double,
        prevVal: Double?,
        prevPrevVal: Double?,
        dt: Double,
        dtPrev: Double,
        mass: Double,
        horizonSec: Double
    ): TimeframeVector {
        val velocity = if (prevVal != null && !prevVal.isNaN() && !prevVal.isInfinite()) {
            (currentVal - prevVal) / dt
        } else {
            // Instantaneous normalized velocity from displacement horizon (%/s)
            currentVal / horizonSec
        }

        // Acceleration: dv / dt where v1 is previous velocity and v2 is current velocity
        // Strictly 0.0 when fewer than 2 previous snapshots exist
        val acceleration = if (prevVal != null && prevPrevVal != null &&
            !prevVal.isNaN() && !prevVal.isInfinite() &&
            !prevPrevVal.isNaN() && !prevPrevVal.isInfinite()
        ) {
            val v2 = (currentVal - prevVal) / dt
            val v1 = (prevVal - prevPrevVal) / dtPrev
            (v2 - v1) / dt
        } else {
            0.0
        }

        // Directional Energy Score: E = 0.5 * m * v^2
        val kineticEnergy = 0.5 * mass * velocity.pow(2)

        // Normalized Micro Pressure dynamic component: F = m * a + gamma * m * v
        val impulse = (mass * acceleration) + (DAMPING_COEFFICIENT * mass * velocity)

        return TimeframeVector(
            timeframe = timeframe,
            value = currentVal,
            velocity = velocity,
            acceleration = acceleration,
            inertiaMass = mass,
            kineticEnergy = kineticEnergy,
            kineticImpulse = impulse
        )
    }

    /**
     * Forensic Market Footprint Analyzer.
     * Deciphers structural absorption, retail traps, kinetic deceleration, and energy surges.
     */
    private fun detectForensicFootprint(
        v5: Double,
        v60: Double,
        v1d: Double?,
        vec5m: TimeframeVector,
        vec60m: TimeframeVector,
        vec1d: TimeframeVector?,
        netForce: Double,
        totalEnergy: Double,
        isDeadMarket: Boolean
    ): ForensicMarketFootprint {
        // Pattern 1: Dead Market / Energy Dissipation
        if (isDeadMarket || (totalEnergy < 0.001 && abs(v5) <= 0.10 && abs(v60) <= 0.10)) {
            return ForensicMarketFootprint(
                code = "FOOTPRINT_MICRO_CHOP_DISSIPATION",
                title = "মাইক্রো-কাইনেটিক শক্তির শূন্যতা (Chop / Low Liquidity)",
                diagnosis = "মার্কেটে গতিশক্তি ও কার্যকর মোমেন্টাম প্রায় শূন্য। লিকুইডিটি শুষ্কতা বা সংবাদের আগের স্থবিরতা।",
                rootCause = "স্বল্প ভলিউম ও ট্রেডারদের নিষ্ক্রিয়তায় ফেক স্পাইক বা উইকের ঝুঁকি চরম।",
                confidenceScore = 92.0,
                isBrakeInertiaPullback = false,
                suggestedDirection = TradeDirection.NEUTRAL
            )
        }

        // Pattern 2: Inertia Brake & Pullback Exhaustion (ব্রেক জড়তা ও পুলব্যাক সমাপ্তি)
        // 60m trend is strongly UP, but 5m dipped negative; however, 5m acceleration has turned positive (curving up)
        if (v60 > 0.15 && v5 < 0.0 && abs(v5) < abs(v60) * 0.50 && vec5m.acceleration >= -0.01) {
            return ForensicMarketFootprint(
                code = "FOOTPRINT_INERTIA_BRAKE_PULLBACK",
                title = "ব্রেক জড়তা ও পুলব্যাক সমাপ্তি (Brake Inertia Reversal)",
                diagnosis = "হায়ার টাইমফ্রেম আপট্রেন্ড বজায় থাকলেও ৫মি বিক্রি ব্রেক জড়তায় বাধা পেয়েছে; নিম্নমুখী বেগ সমাপ্ত।",
                rootCause = "বিক্রেতাদের মোমেন্টাম শেষ এবং মূল ট্রেন্ডের জড়তা বাউন্স টানছে।",
                confidenceScore = 88.5,
                isBrakeInertiaPullback = true,
                suggestedDirection = TradeDirection.UP
            )
        }

        // Pattern 3: Downward Pullback Exhaustion in Downtrend (বেয়ারিশ পুলব্যাক সমাপ্তি)
        if (v60 < -0.15 && v5 > 0.0 && abs(v5) < abs(v60) * 0.50 && vec5m.acceleration <= 0.01) {
            return ForensicMarketFootprint(
                code = "FOOTPRINT_INERTIA_BRAKE_BEAR_PULLBACK",
                title = "ডাউনট্রেন্ডে জড়তা ব্রেক (Bearish Pullback Exhaustion)",
                diagnosis = "ডাউনট্রেন্ডের বিপরীতে সাময়িক বায়ার পুলব্যাকের বেগ শেষ; রেজিস্ট্যান্সে জড়তা সক্রিয়।",
                rootCause = "কাউন্টার ট্রেন্ড বায়ার গতি ক্ষয়প্রাপ্ত; বিয়ারিশ গতি বজায় থাকার সম্ভাবনা বেশি।",
                confidenceScore = 87.0,
                isBrakeInertiaPullback = true,
                suggestedDirection = TradeDirection.DOWN
            )
        }

        // Pattern 4: High Density Absorption & Liquidity Sweep (উচ্চ ঘনত্বের অ্যাবজরপশন)
        if (v5 < -0.30 && v60 > 0.25 && (v1d == null || v1d > -0.10)) {
            return ForensicMarketFootprint(
                code = "FOOTPRINT_INSTITUTIONAL_ABSORPTION",
                title = "প্রাতিষ্ঠানিক লিকুইডিটি অ্যাবজরপশন (Institutional Absorption)",
                diagnosis = "বড় ট্রেডাররা রিটেইল স্টপলস ও প্যানিক সেল অর্ডার আগ্রাসীভাবে অ্যাবজরব করছে।",
                rootCause = "রিটেইল সেল হজম করে মার্কেট স্ট্রাকচার রক্ষা করা হয়েছে, যা আসন্ন ঊর্ধ্বমুখী বিস্ফোরণের ইঙ্গিত।",
                confidenceScore = 91.0,
                isBrakeInertiaPullback = true,
                suggestedDirection = TradeDirection.UP
            )
        }

        // Pattern 5: Bear Trap Spring (বিয়ার ট্র্যাপ স্প্রিং রিভার্সাল)
        if (v5 < -0.10 && vec5m.velocity > 0.05 && vec5m.acceleration > 0.10) {
            return ForensicMarketFootprint(
                code = "FOOTPRINT_BEAR_TRAP_SPRING",
                title = "বিয়ার ট্র্যাপ স্প্রিং রিভার্সাল (Spring Velocity Surge)",
                diagnosis = "ফলস ব্রেকডাউন সম্পূর্ণ ব্যর্থ হয়ে স্প্রিংয়ের মতো দ্রুত পজিটিভ ভেলোসিটি ভেক্টরে রূপ নিয়েছে।",
                rootCause = "বিক্রেতারা ফাঁদে পড়েছে এবং আকস্মিক বায়ার ইনজেকশন দাম দ্রুত উপরে টানছে।",
                confidenceScore = 89.0,
                isBrakeInertiaPullback = false,
                suggestedDirection = TradeDirection.UP
            )
        }

        // Pattern 6: Bull Trap Upthrust (বুল ট্র্যাপ আপথ্রাস্ট রিভার্সাল)
        if (v5 > 0.10 && vec5m.velocity < -0.05 && vec5m.acceleration < -0.10) {
            return ForensicMarketFootprint(
                code = "FOOTPRINT_BULL_TRAP_UPTHRUST",
                title = "বুল ট্র্যাপ আপথ্রাস্ট রিভার্সাল (Upthrust Velocity Collapse)",
                diagnosis = "হাইসে ফলস ব্রেকআউটে বায়ারদের আটকে দিয়ে ভেলোসিটি ভেক্টর হঠাৎ তীব্র নিচে ঘুরে গেছে।",
                rootCause = "হাইসে প্রাতিষ্ঠানিক ডিস্ট্রিবিউশন গতিকে দ্রুত নেগেটিভে রূপান্তর করেছে।",
                confidenceScore = 89.5,
                isBrakeInertiaPullback = false,
                suggestedDirection = TradeDirection.DOWN
            )
        }

        // Pattern 7: Bullish Vector Impulse Breakout (বুলিশ ইমপালস ভেক্টর ব্রেকআউট)
        if (v5 > 0.15 && v60 > 0.10 && netForce > 0.25) {
            return ForensicMarketFootprint(
                code = "FOOTPRINT_BULLISH_IMPULSE_BREAKOUT",
                title = "বুলিশ ইমপালস ভেক্টর ব্রেকআউট (Kinetic Expansion)",
                diagnosis = "সব টাইমফ্রেমের বেগ ও ত্বরণ একমুখী; বায়ার কাইনেটিক এনার্জি দ্রুত সম্প্রসারিত হচ্ছে।",
                rootCause = "৫মি ও ৬০মি একসাথে সর্বোচ্চ শক্তিতে বাধা ভেঙে নতুন উচ্চতায় পৌঁছাচ্ছে।",
                confidenceScore = 93.0,
                isBrakeInertiaPullback = false,
                suggestedDirection = TradeDirection.UP
            )
        }

        // Pattern 8: Kinetic Bearish Cascade (কাইনেটিক সেল ক্যাসকেড)
        if (v5 < -0.15 && v60 < -0.10 && netForce < -0.25) {
            return ForensicMarketFootprint(
                code = "FOOTPRINT_KINETIC_CASCADE_ACCELERATION",
                title = "কাইনেটিক সেল ক্যাসকেড (Cascade Acceleration)",
                diagnosis = "বহু-মাত্রিক নেগেটিভ বেগ ভেক্টর সিঙ্ক্রোনাইজড; লিকুইডেশন ও প্যানিক সেল ক্যাসকেড চলছে।",
                rootCause = "বিক্রেতাদের শক্তি ও মেজরের চাপ একসাথে সাপোর্ট দ্রুত ভেঙে ফেলছে।",
                confidenceScore = 93.5,
                isBrakeInertiaPullback = false,
                suggestedDirection = TradeDirection.DOWN
            )
        }

        // Default: Steady Dynamic Equilibrium
        return ForensicMarketFootprint(
            code = "FOOTPRINT_STEADY_FLOW",
            title = "ডাইনামিক ইকুইলিব্রিয়াম ফ্লো (Dynamic Equilibrium Flow)",
            diagnosis = "মার্কেট স্থিতিশীল গতি ভেক্টর বজায় রাখছে; কোনো চরম ভারসাম্যহীনতা ঘটেনি।",
            rootCause = "টাইমফ্রেম ভিত্তিক মসৃণ রূপান্তর এবং বায়ার-সেলারদের স্বাভাবিক ভারসাম্য।",
            confidenceScore = 75.0,
            isBrakeInertiaPullback = false,
            suggestedDirection = if (netForce > 0.10) TradeDirection.UP else if (netForce < -0.10) TradeDirection.DOWN else TradeDirection.NEUTRAL
        )
    }
}
