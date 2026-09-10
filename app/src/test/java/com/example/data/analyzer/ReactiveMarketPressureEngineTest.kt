package com.example.data.analyzer

import com.example.data.models.TradeDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReactiveMarketPressureEngineTest {

    @Test
    fun testOneAvailableTimeframe_5mOnly() {
        val input = ReactiveMarketPressureEngine.PressureInput(
            current5m = 0.85,
            previous5m = 0.50,
            current60m = null,
            previous60m = null,
            current1d = null,
            previous1d = null,
            elapsedSeconds = 1.0,
            approximate = false
        )
        val decision = ReactiveMarketPressureEngine.calculate(input)

        assertEquals(TradeDirection.UP, decision.direction)
        assertEquals(setOf(ReactiveMarketPressureEngine.Timeframe.FIVE_MIN), decision.availableTimeframes)
        assertTrue(decision.upPercentage > 50.0)
        assertTrue(decision.downPercentage < 50.0)
        assertEquals(100.0, decision.upPercentage + decision.downPercentage, 0.0001)
    }

    @Test
    fun testOneAvailableTimeframe_60mOnly() {
        val input = ReactiveMarketPressureEngine.PressureInput(
            current5m = null,
            previous5m = null,
            current60m = -1.20,
            previous60m = -0.80,
            current1d = null,
            previous1d = null,
            elapsedSeconds = 2.0,
            approximate = false
        )
        val decision = ReactiveMarketPressureEngine.calculate(input)

        assertEquals(TradeDirection.DOWN, decision.direction)
        assertEquals(setOf(ReactiveMarketPressureEngine.Timeframe.SIXTY_MIN), decision.availableTimeframes)
        assertTrue(decision.downPercentage > 50.0)
        assertEquals(100.0, decision.upPercentage + decision.downPercentage, 0.0001)
    }

    @Test
    fun testOneAvailableTimeframe_1dOnly() {
        val input = ReactiveMarketPressureEngine.PressureInput(
            current5m = null,
            previous5m = null,
            current60m = null,
            previous60m = null,
            current1d = 2.50,
            previous1d = 1.00,
            elapsedSeconds = 5.0,
            approximate = false
        )
        val decision = ReactiveMarketPressureEngine.calculate(input)

        assertEquals(TradeDirection.UP, decision.direction)
        assertEquals(setOf(ReactiveMarketPressureEngine.Timeframe.ONE_DAY), decision.availableTimeframes)
        assertTrue(decision.upPercentage > 50.0)
        assertEquals(100.0, decision.upPercentage + decision.downPercentage, 0.0001)
    }

    @Test
    fun testPairsOfAvailableTimeframes_5mAnd60m() {
        val input = ReactiveMarketPressureEngine.PressureInput(
            current5m = 0.50,
            previous5m = 0.40,
            current60m = 0.80,
            previous60m = 0.60,
            current1d = null,
            previous1d = null,
            elapsedSeconds = 1.0,
            approximate = false
        )
        val decision = ReactiveMarketPressureEngine.calculate(input)

        assertEquals(TradeDirection.UP, decision.direction)
        assertEquals(
            setOf(ReactiveMarketPressureEngine.Timeframe.FIVE_MIN, ReactiveMarketPressureEngine.Timeframe.SIXTY_MIN),
            decision.availableTimeframes
        )
        assertEquals(100.0, decision.upPercentage + decision.downPercentage, 0.0001)
    }

    @Test
    fun testPairsOfAvailableTimeframes_5mAnd1d() {
        val input = ReactiveMarketPressureEngine.PressureInput(
            current5m = -0.60,
            previous5m = -0.30,
            current60m = null,
            previous60m = null,
            current1d = -1.50,
            previous1d = -1.20,
            elapsedSeconds = 2.0,
            approximate = false
        )
        val decision = ReactiveMarketPressureEngine.calculate(input)

        assertEquals(TradeDirection.DOWN, decision.direction)
        assertEquals(
            setOf(ReactiveMarketPressureEngine.Timeframe.FIVE_MIN, ReactiveMarketPressureEngine.Timeframe.ONE_DAY),
            decision.availableTimeframes
        )
        assertEquals(100.0, decision.upPercentage + decision.downPercentage, 0.0001)
    }

    @Test
    fun testPairsOfAvailableTimeframes_60mAnd1d() {
        val input = ReactiveMarketPressureEngine.PressureInput(
            current5m = null,
            previous5m = null,
            current60m = 1.10,
            previous60m = 0.90,
            current1d = 0.50,
            previous1d = 0.20,
            elapsedSeconds = 3.0,
            approximate = false
        )
        val decision = ReactiveMarketPressureEngine.calculate(input)

        assertEquals(TradeDirection.UP, decision.direction)
        assertEquals(
            setOf(ReactiveMarketPressureEngine.Timeframe.SIXTY_MIN, ReactiveMarketPressureEngine.Timeframe.ONE_DAY),
            decision.availableTimeframes
        )
        assertEquals(100.0, decision.upPercentage + decision.downPercentage, 0.0001)
    }

    @Test
    fun testAllThreeTimeframesContributing() {
        val input = ReactiveMarketPressureEngine.PressureInput(
            current5m = 0.70,
            previous5m = 0.50,
            current60m = 0.60,
            previous60m = 0.40,
            current1d = 0.90,
            previous1d = 0.80,
            elapsedSeconds = 1.0,
            approximate = false
        )
        val decision = ReactiveMarketPressureEngine.calculate(input)

        assertEquals(TradeDirection.UP, decision.direction)
        assertEquals(
            setOf(
                ReactiveMarketPressureEngine.Timeframe.FIVE_MIN,
                ReactiveMarketPressureEngine.Timeframe.SIXTY_MIN,
                ReactiveMarketPressureEngine.Timeframe.ONE_DAY
            ),
            decision.availableTimeframes
        )
        assertTrue(decision.upPressure > 0.4)
        assertEquals(0.0, decision.downPressure, 0.0001)
        assertEquals(100.0, decision.upPercentage + decision.downPercentage, 0.0001)
    }

    @Test
    fun testOppositeValues_StrongerForceWins() {
        // 5m is very strong UP (+2.0), 60m is weak DOWN (-0.20), 1D is flat (0.0)
        val input = ReactiveMarketPressureEngine.PressureInput(
            current5m = 2.0,
            previous5m = 1.5,
            current60m = -0.20,
            previous60m = -0.20,
            current1d = 0.0,
            previous1d = 0.0,
            elapsedSeconds = 1.0,
            approximate = false
        )
        val decision = ReactiveMarketPressureEngine.calculate(input)

        assertEquals(TradeDirection.UP, decision.direction)
        assertTrue(decision.upPressure > decision.downPressure)
        assertTrue(decision.upPercentage > decision.downPercentage)
        assertEquals(100.0, decision.upPercentage + decision.downPercentage, 0.0001)
    }

    @Test
    fun testEqualOpposingForces() {
        // Equal opposing forces where net dominance is 0
        val input = ReactiveMarketPressureEngine.PressureInput(
            current5m = 0.0,
            previous5m = 0.0,
            current60m = 0.0,
            previous60m = 0.0,
            current1d = 0.0,
            previous1d = 0.0,
            elapsedSeconds = 1.0,
            approximate = false
        )
        val decision = ReactiveMarketPressureEngine.calculate(input)

        assertEquals(TradeDirection.NEUTRAL, decision.direction)
        assertEquals(50.0, decision.upPercentage, 0.001)
        assertEquals(50.0, decision.downPercentage, 0.001)
    }

    @Test
    fun test1dChangingWhile5mAnd60mConstant() {
        val baseInput = ReactiveMarketPressureEngine.PressureInput(
            current5m = 0.50,
            previous5m = 0.50,
            current60m = 0.40,
            previous60m = 0.40,
            current1d = 0.00,
            previous1d = 0.00,
            elapsedSeconds = 1.0,
            approximate = false
        )
        val decisionBase = ReactiveMarketPressureEngine.calculate(baseInput)

        // Now 1D increases significantly in bullish direction
        val bullishInput = baseInput.copy(current1d = 3.00, previous1d = 1.00)
        val decisionBullish = ReactiveMarketPressureEngine.calculate(bullishInput)

        assertTrue(
            "Higher 1D should increase UP percentage or pressure",
            decisionBullish.upPressure > decisionBase.upPressure
        )
        assertTrue(decisionBullish.upPercentage >= decisionBase.upPercentage)

        // Now 1D drops strongly into negative (opposing) territory
        val opposingInput = baseInput.copy(current1d = -4.00, previous1d = -2.00)
        val decisionOpposing = ReactiveMarketPressureEngine.calculate(opposingInput)

        assertTrue(
            "Opposing 1D should increase down pressure and reduce up percentage",
            decisionOpposing.downPressure > decisionBase.downPressure
        )
        assertTrue(decisionOpposing.upPercentage < decisionBase.upPercentage)
    }

    @Test
    fun testCurrentLevelConstant_VelocityChanging() {
        // Same current level, but previous level was lower (positive velocity / rising)
        val risingInput = ReactiveMarketPressureEngine.PressureInput(
            current5m = 0.50,
            previous5m = 0.10,
            current60m = 0.50,
            previous60m = 0.10,
            current1d = 0.50,
            previous1d = 0.10,
            elapsedSeconds = 1.0,
            approximate = false
        )
        val decisionRising = ReactiveMarketPressureEngine.calculate(risingInput)

        // Same current level, but previous level was higher (negative velocity / falling)
        val fallingInput = ReactiveMarketPressureEngine.PressureInput(
            current5m = 0.50,
            previous5m = 0.90,
            current60m = 0.50,
            previous60m = 0.90,
            current1d = 0.50,
            previous1d = 0.90,
            elapsedSeconds = 1.0,
            approximate = false
        )
        val decisionFalling = ReactiveMarketPressureEngine.calculate(fallingInput)

        assertTrue(
            "Positive velocity should generate greater UP pressure than negative velocity",
            decisionRising.upPressure > decisionFalling.upPressure
        )
        assertTrue(decisionRising.upPercentage > decisionFalling.upPercentage)
    }

    @Test
    fun testApproximateValuesConfidencePenalty() {
        val inputVerified = ReactiveMarketPressureEngine.PressureInput(
            current5m = 0.50,
            previous5m = 0.50,
            current60m = 0.50,
            previous60m = 0.50,
            current1d = 0.50,
            previous1d = 0.50,
            elapsedSeconds = 1.0,
            approximate = false
        )
        val decisionVerified = ReactiveMarketPressureEngine.calculate(inputVerified)
        assertEquals(0.00, decisionVerified.confidencePenalty, 0.0001)

        val inputApprox = inputVerified.copy(approximate = true)
        val decisionApprox = ReactiveMarketPressureEngine.calculate(inputApprox)
        assertEquals(0.15, decisionApprox.confidencePenalty, 0.0001)
        assertTrue(
            "Approximate data must reduce pressureStrength",
            decisionApprox.pressureStrength < decisionVerified.pressureStrength
        )

        // Direction should remain unchanged by approximate penalty alone
        assertEquals(decisionVerified.direction, decisionApprox.direction)
        assertTrue(decisionApprox.explanation.contains("Penalty: -15%"))
    }

    @Test
    fun testInfinityAndNaNRejection() {
        val inputWithInf = ReactiveMarketPressureEngine.PressureInput(
            current5m = Double.POSITIVE_INFINITY,
            previous5m = null,
            current60m = Double.NaN,
            previous60m = null,
            current1d = null,
            previous1d = null,
            elapsedSeconds = 1.0,
            approximate = false
        )
        val decision = ReactiveMarketPressureEngine.calculate(inputWithInf)
        assertEquals(TradeDirection.NEUTRAL, decision.direction)
        assertTrue(decision.availableTimeframes.isEmpty())
        assertEquals(0.0, decision.pressureStrength, 0.0001)
    }

    @Test
    fun testMissingValuesHandling() {
        val inputAllNull = ReactiveMarketPressureEngine.PressureInput(
            current5m = null,
            previous5m = null,
            current60m = null,
            previous60m = null,
            current1d = null,
            previous1d = null,
            elapsedSeconds = 1.0,
            approximate = false
        )
        val decision = ReactiveMarketPressureEngine.calculate(inputAllNull)

        assertEquals(TradeDirection.NEUTRAL, decision.direction)
        assertEquals(0.0, decision.pressureStrength, 0.0001)
        assertEquals(50.0, decision.upPercentage, 0.0001)
        assertEquals(50.0, decision.downPercentage, 0.0001)
        assertTrue(decision.availableTimeframes.isEmpty())
    }

    @Test
    fun testPercentagesSumToExactly100AfterRounding() {
        val values = listOf(
            Triple(0.12345, 0.67891, -0.45678),
            Triple(-1.23456, 0.23456, 0.87654),
            Triple(0.0001, -0.0002, 0.0003),
            Triple(3.4567, 2.3456, 1.2345),
            Triple(-2.7182, -3.1415, -1.6180)
        )
        for ((v5, v60, v1d) in values) {
            val input = ReactiveMarketPressureEngine.PressureInput(
                current5m = v5,
                previous5m = null,
                current60m = v60,
                previous60m = null,
                current1d = v1d,
                previous1d = null,
                elapsedSeconds = 1.0,
                approximate = false
            )
            val decision = ReactiveMarketPressureEngine.calculate(input)
            assertEquals(100.0, decision.upPercentage + decision.downPercentage, 0.0)
        }
    }

    @Test
    fun testDeterministicFingerprintGeneration() {
        val input1 = ReactiveMarketPressureEngine.PressureInput(
            current5m = 0.50,
            previous5m = 0.40,
            current60m = 0.80,
            previous60m = 0.70,
            current1d = 0.30,
            previous1d = 0.20,
            elapsedSeconds = 1.0,
            approximate = false,
            matrixDirectionalSupport = 5.0,
            matrixDirectionalConflict = 0.0
        )
        val decision1 = ReactiveMarketPressureEngine.calculate(input1)
        val decision2 = ReactiveMarketPressureEngine.calculate(input1)

        assertEquals(decision1.fingerprint, decision2.fingerprint)

        val inputDifferent = input1.copy(current5m = 0.51)
        val decision3 = ReactiveMarketPressureEngine.calculate(inputDifferent)
        assertNotEquals(decision1.fingerprint, decision3.fingerprint)
    }
}
