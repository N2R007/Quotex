package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.models.StrengthLevel
import com.example.data.models.TradeDirection
import com.example.data.sample.SampleChartGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Quant Vision", appName)
  }

  @Test
  fun `sample scenario analysis computes correct trading metrics`() {
    val scenario = SampleChartGenerator.scenarios[0] // Bullish Surge: +1.25%, +0.75%
    val analysis = SampleChartGenerator.generateAnalysisForScenario(scenario)

    assertTrue(analysis.isSuccess)
    assertEquals(TradeDirection.UP, analysis.direction)
    assertEquals(StrengthLevel.HIGH, analysis.strengthLevel)
    assertEquals("+2%", analysis.netSum)
    assertTrue("Calculated percentage must be between 50 and 100", analysis.calculatedPercentage in 50.0..100.0)
  }

  @Test
  fun `mainViewModel parseVal rejects invalid out of bound or non-finite values`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = MainViewModel(app)
    val parseValMethod = MainViewModel::class.java.getDeclaredMethod("parseVal", java.lang.Double::class.java, String::class.java)
    parseValMethod.isAccessible = true

    // Values outside [-500, 500] must return null
    val resultOutHigh = parseValMethod.invoke(vm, 999.0, null)
    org.junit.Assert.assertNull("999.0 must be rejected", resultOutHigh)

    val resultOutLow = parseValMethod.invoke(vm, -999.0, null)
    org.junit.Assert.assertNull("-999.0 must be rejected", resultOutLow)

    // Non-finite values must return null
    val resultNaN = parseValMethod.invoke(vm, Double.NaN, null)
    org.junit.Assert.assertNull("NaN must be rejected", resultNaN)

    val resultInf = parseValMethod.invoke(vm, Double.POSITIVE_INFINITY, null)
    org.junit.Assert.assertNull("Infinity must be rejected", resultInf)

    // Valid values must be preserved
    val resultValid = parseValMethod.invoke(vm, 1.5, null) as Double
    assertEquals(1.5, resultValid, 1e-6)

    val resultValidBoundary = parseValMethod.invoke(vm, 500.0, null) as Double
    assertEquals(500.0, resultValidBoundary, 1e-6)

    // String text with out of bounds value must return null
    val resultTextInvalid = parseValMethod.invoke(vm, null, "+999.0%")
    org.junit.Assert.assertNull("+999.0% text must be rejected", resultTextInvalid)

    val resultTextValid = parseValMethod.invoke(vm, null, "+1.50%") as Double
    assertEquals(1.50, resultTextValid, 1e-6)
  }

  @Test
  fun `mainViewModel preserves valid analysis on scenario selection`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = MainViewModel(app)
    org.junit.Assert.assertNull(vm.uiState.value.currentAnalysis)

    vm.selectSampleScenario(0)

    var analysis = vm.uiState.value.currentAnalysis
    val start = System.currentTimeMillis()
    while (analysis == null && System.currentTimeMillis() - start < 3000L) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      Thread.sleep(20)
      analysis = vm.uiState.value.currentAnalysis
    }

    org.junit.Assert.assertNotNull(analysis)
    assertTrue(analysis!!.isSuccess)
    assertEquals(TradeDirection.UP, analysis.direction)
  }

  @Test
  fun `mainViewModel handles invalid first frame by exposing error and invalid later frame by preserving state`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = MainViewModel(app)
    org.junit.Assert.assertNull(vm.uiState.value.currentAnalysis)
    org.junit.Assert.assertNull(vm.uiState.value.error)

    // 1. Initial invalid frame: blank bitmap with no OCR metrics
    val blankBitmap = android.graphics.Bitmap.createBitmap(10, 10, android.graphics.Bitmap.Config.ARGB_8888)
    vm.registerFrameProvider { blankBitmap }
    vm.triggerManualAnalysis()

    val start1 = System.currentTimeMillis()
    while ((vm.uiState.value.currentAnalysis == null || vm.uiState.value.error == null) && System.currentTimeMillis() - start1 < 3000L) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      Thread.sleep(20)
    }

    val stateAfterFirstInvalid = vm.uiState.value
    org.junit.Assert.assertNotNull("First invalid frame must set currentAnalysis", stateAfterFirstInvalid.currentAnalysis)
    org.junit.Assert.assertFalse("First invalid frame analysis must not be success", stateAfterFirstInvalid.currentAnalysis!!.isSuccess)
    org.junit.Assert.assertNotNull("First invalid frame must expose error message", stateAfterFirstInvalid.error)

    // 2. Set a valid scenario so previous analysis exists
    vm.selectSampleScenario(0)
    val start2 = System.currentTimeMillis()
    while ((vm.uiState.value.currentAnalysis?.isSuccess != true) && System.currentTimeMillis() - start2 < 3000L) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      Thread.sleep(20)
    }

    val validAnalysis = vm.uiState.value.currentAnalysis
    org.junit.Assert.assertNotNull(validAnalysis)
    assertTrue(validAnalysis!!.isSuccess)
    org.junit.Assert.assertNull("Error must be cleared on successful analysis", vm.uiState.value.error)

    // 3. Subsequent invalid frame: switch back to blank frame provider
    vm.selectSampleScenario(null) // clear test scenario, back to camera frame provider
    vm.triggerManualAnalysis()

    val start3 = System.currentTimeMillis()
    while (vm.uiState.value.isProcessing && System.currentTimeMillis() - start3 < 3000L) {
      org.robolectric.shadows.ShadowLooper.idleMainLooper()
      Thread.sleep(20)
    }

    val stateAfterLaterInvalid = vm.uiState.value
    org.junit.Assert.assertNotNull("Subsequent invalid frame must preserve previous valid analysis", stateAfterLaterInvalid.currentAnalysis)
    assertTrue("Preserved analysis must remain success", stateAfterLaterInvalid.currentAnalysis!!.isSuccess)
    assertEquals(validAnalysis.timestamp, stateAfterLaterInvalid.currentAnalysis!!.timestamp)
    org.junit.Assert.assertNull("Transient error must not overwrite valid dashboard state", stateAfterLaterInvalid.error)

    blankBitmap.recycle()
  }

  @Test
  fun `isExactSameNumber correctly handles 2-decimal rounding and micro-jitter`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = MainViewModel(app)
    val isExactSameNumberMethod = MainViewModel::class.java.getDeclaredMethod(
      "isExactSameNumber",
      java.lang.Double::class.java,
      java.lang.Double::class.java
    )
    isExactSameNumberMethod.isAccessible = true

    // Micro-jitter in floating point representation (e.g. 0.05000000000000004 vs 0.05) must be treated as exact same
    assertTrue(isExactSameNumberMethod.invoke(vm, 0.05, 0.05000000000000004) as Boolean)
    assertTrue(isExactSameNumberMethod.invoke(vm, -0.53, -0.5300000001) as Boolean)
    assertTrue(isExactSameNumberMethod.invoke(vm, 1.25, 1.25) as Boolean)
    assertTrue(isExactSameNumberMethod.invoke(vm, null, null) as Boolean)

    // Distinct percentage changes must be detected as different
    org.junit.Assert.assertFalse(isExactSameNumberMethod.invoke(vm, 0.05, 0.06) as Boolean)
    org.junit.Assert.assertFalse(isExactSameNumberMethod.invoke(vm, 0.05, -0.05) as Boolean)
    org.junit.Assert.assertFalse(isExactSameNumberMethod.invoke(vm, 0.05, null) as Boolean)
    org.junit.Assert.assertFalse(isExactSameNumberMethod.invoke(vm, null, 0.05) as Boolean)
  }

  @Test
  fun `antiGlitch and adaptiveCpu protection toggles operate correctly and default to true`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = MainViewModel(app)

    // Verify defaults are active
    assertTrue("Anti-Glitch confirmation must be enabled by default", vm.uiState.value.isAntiGlitchConfirmationEnabled)
    assertTrue("Adaptive CPU protection must be enabled by default", vm.uiState.value.isAdaptiveCpuProtectionEnabled)

    // Toggle off
    vm.toggleAntiGlitchConfirmation(false)
    org.junit.Assert.assertFalse(vm.uiState.value.isAntiGlitchConfirmationEnabled)

    vm.toggleAdaptiveCpuProtection(false)
    org.junit.Assert.assertFalse(vm.uiState.value.isAdaptiveCpuProtectionEnabled)

    // Toggle on
    vm.toggleAntiGlitchConfirmation(true)
    assertTrue(vm.uiState.value.isAntiGlitchConfirmationEnabled)

    vm.toggleAdaptiveCpuProtection(true)
    assertTrue(vm.uiState.value.isAdaptiveCpuProtectionEnabled)
  }

  @Test
  fun `audioSignalEngine records UP and DOWN alert events properly`() = kotlinx.coroutines.runBlocking {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    com.example.audio.AudioSignalEngine.init(app)
    com.example.audio.AudioSignalEngine.clearRecordedEventsForTesting()

    // Test UP event
    com.example.audio.AudioSignalEngine.playSoundEvent(
      com.example.audio.AudioSignalEngine.SOUND_UP_ALERT,
      "UP U032"
    )
    org.junit.Assert.assertEquals(1, com.example.audio.AudioSignalEngine.recordedEventsForTesting.size)
    org.junit.Assert.assertEquals(com.example.audio.AudioSignalEngine.SOUND_UP_ALERT, com.example.audio.AudioSignalEngine.recordedEventsForTesting[0])

    // Test DOWN event
    com.example.audio.AudioSignalEngine.playSoundEvent(
      com.example.audio.AudioSignalEngine.SOUND_DOWN_ALERT,
      "DOWN D061"
    )
    org.junit.Assert.assertEquals(2, com.example.audio.AudioSignalEngine.recordedEventsForTesting.size)
    org.junit.Assert.assertEquals(com.example.audio.AudioSignalEngine.SOUND_DOWN_ALERT, com.example.audio.AudioSignalEngine.recordedEventsForTesting[1])
  }

  @Test
  fun `userRuleRegistry pre-verifies top 3 power categories including U032`() {
    val app = ApplicationProvider.getApplicationContext<android.app.Application>()
    com.example.data.matrix.UserRuleRegistry.init(app)

    // Verify Category 2: U032 and D032
    assertTrue("U032 must be verified by default", com.example.data.matrix.UserRuleRegistry.isRuleVerified("U032"))
    assertTrue("D032 must be verified by default", com.example.data.matrix.UserRuleRegistry.isRuleVerified("D032"))

    // Verify Category 1: U036 and D036
    assertTrue("U036 must be verified by default", com.example.data.matrix.UserRuleRegistry.isRuleVerified("U036"))
    assertTrue("D036 must be verified by default", com.example.data.matrix.UserRuleRegistry.isRuleVerified("D036"))

    // Verify Category 3: U024 and D024
    assertTrue("U024 must be verified by default", com.example.data.matrix.UserRuleRegistry.isRuleVerified("U024"))
    assertTrue("D024 must be verified by default", com.example.data.matrix.UserRuleRegistry.isRuleVerified("D024"))

    // Verify Good/Medium rules do NOT have default checkmarks (user manual verify)
    org.junit.Assert.assertFalse("U011 (Strong) should not be verified by default", com.example.data.matrix.UserRuleRegistry.isRuleVerified("U011"))
    org.junit.Assert.assertFalse("U005 (Medium) should not be verified by default", com.example.data.matrix.UserRuleRegistry.isRuleVerified("U005"))

    // Verify user can manually toggle verify
    com.example.data.matrix.UserRuleRegistry.setRuleVerified("U011", true)
    assertTrue("U011 should now be verified after user marks it", com.example.data.matrix.UserRuleRegistry.isRuleVerified("U011"))
  }
}
