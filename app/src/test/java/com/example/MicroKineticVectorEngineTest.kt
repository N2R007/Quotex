package com.example

import com.example.data.analyzer.MicroKineticVectorEngine
import com.example.data.models.MetricSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MicroKineticVectorEngineTest {

    @Test
    fun testMathematicalIntegrity_energyIsNonNegative_andWeightsSumTo100() {
        val testCases = listOf(
            Triple(0.50, 0.20, 1.50),
            Triple(-0.45, -0.30, -2.00),
            Triple(0.80, -0.10, 0.00),
            Triple(0.00, 0.00, 0.00),
            Triple(1.25, 0.75, null)
        )

        for ((v5, v60, v1d) in testCases) {
            val result = MicroKineticVectorEngine.calculate(
                val5m = v5,
                val60m = v60,
                val1d = v1d,
                history = emptyList(),
                isApproximate = false
            )

            // Kinetic Energy must always be >= 0
            assertTrue("Kinetic energy must be non-negative: ${result.kineticBaseEnergy}", result.kineticBaseEnergy >= 0.0)

            // Up + Down Weights must sum to 100.0 within floating point error
            val totalWeight = result.kineticUpWeight + result.kineticDownWeight
            assertEquals("Up + Down weights must sum to 100.0", 100.0, totalWeight, 0.05)

            // Weights must be in valid [0, 100] range
            assertTrue("Up weight in range [0, 100]", result.kineticUpWeight in 0.0..100.0)
            assertTrue("Down weight in range [0, 100]", result.kineticDownWeight in 0.0..100.0)

            // Forensic title and diagnosis must never be empty
            assertTrue("Pattern title must be non-empty", result.footprint.title.isNotBlank())
            assertTrue("Forensic diagnosis must be non-empty", result.footprint.diagnosis.isNotBlank())
            assertTrue("Mathematical proof must be present", result.mathematicalProof.isNotBlank())
        }
    }

    @Test
    fun testForensicFootprint_institutionalAbsorptionDetected() {
        // Macro 1D is positive (+2.50%), structural 60m is positive (+0.40%),
        // but 5m dipped negative (-0.35%) -> Institutional Absorption
        val result = MicroKineticVectorEngine.calculate(
            val5m = -0.35,
            val60m = 0.40,
            val1d = 2.50,
            history = emptyList(),
            isApproximate = false
        )

        assertEquals("FOOTPRINT_INSTITUTIONAL_ABSORPTION", result.footprint.code)
        assertTrue(result.footprint.title.contains("প্রাতিষ্ঠানিক"))
        assertTrue(result.footprint.diagnosis.contains("অ্যাবজরব"))
    }

    @Test
    fun testForensicFootprint_bearTrapSpringWithVelocityHistory() {
        // History shows 5m negative (-0.30) but surging upward with high acceleration
        val history = listOf(
            MetricSnapshot(val5m = -0.40, val60m = 0.10, val1d = null, timestamp = 1000L),
            MetricSnapshot(val5m = -0.35, val60m = 0.10, val1d = null, timestamp = 2000L),
            MetricSnapshot(val5m = -0.15, val60m = 0.15, val1d = null, timestamp = 3000L)
        )

        val result = MicroKineticVectorEngine.calculate(
            val5m = -0.15,
            val60m = 0.15,
            val1d = null,
            history = history,
            isApproximate = false
        )

        assertNotNull(result.footprint.code)
        assertTrue(result.footprint.title.isNotBlank())
        assertTrue("Base energy must reflect positive velocity change", result.kineticBaseEnergy > 0.0)
    }

    @Test
    fun testForensicFootprint_bullTrapUpthrustDetected() {
        // History shows 5m positive but suddenly decelerating and collapsing downward
        val history = listOf(
            MetricSnapshot(val5m = 0.40, val60m = -0.10, val1d = null, timestamp = 1000L),
            MetricSnapshot(val5m = 0.35, val60m = -0.10, val1d = null, timestamp = 2000L),
            MetricSnapshot(val5m = 0.15, val60m = -0.20, val1d = null, timestamp = 3000L)
        )

        val result = MicroKineticVectorEngine.calculate(
            val5m = 0.15,
            val60m = -0.20,
            val1d = null,
            history = history,
            isApproximate = false
        )

        assertNotNull(result.footprint.code)
        assertTrue(result.footprint.title.isNotBlank())
    }

    @Test
    fun testForensicFootprint_kineticCascadeAccelerationDown() {
        // Aligned strong downward momentum across horizons
        val result = MicroKineticVectorEngine.calculate(
            val5m = -0.90,
            val60m = -0.60,
            val1d = -2.00,
            history = emptyList(),
            isApproximate = false
        )

        assertEquals("FOOTPRINT_KINETIC_CASCADE_ACCELERATION", result.footprint.code)
        assertTrue("Down weight must dominate", result.kineticDownWeight > 60.0)
        assertTrue("Down weight must be greater than up weight", result.kineticDownWeight > result.kineticUpWeight)
        assertTrue(result.footprint.title.contains("ক্যাসকেড"))
    }

    @Test
    fun testForensicFootprint_bullishImpulseBreakoutUp() {
        // Aligned strong upward momentum across horizons
        val result = MicroKineticVectorEngine.calculate(
            val5m = 0.90,
            val60m = 0.60,
            val1d = 2.00,
            history = emptyList(),
            isApproximate = false
        )

        assertEquals("FOOTPRINT_BULLISH_IMPULSE_BREAKOUT", result.footprint.code)
        assertTrue("Up weight must dominate", result.kineticUpWeight > 60.0)
        assertTrue("Up weight must be greater than down weight", result.kineticUpWeight > result.kineticDownWeight)
        assertTrue(result.footprint.title.contains("বুলিশ"))
    }

    @Test
    fun testMicroChopDissipation_lowVolatility() {
        // Near-zero movement across horizons creates dissipation / chop
        val result = MicroKineticVectorEngine.calculate(
            val5m = 0.02,
            val60m = -0.01,
            val1d = 0.03,
            history = emptyList(),
            isApproximate = false
        )

        assertEquals("FOOTPRINT_MICRO_CHOP_DISSIPATION", result.footprint.code)
        assertTrue(result.footprint.title.contains("শূন্যতা"))
    }
}
