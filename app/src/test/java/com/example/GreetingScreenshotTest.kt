package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.models.AnalyzerUiState
import com.example.data.models.StrengthLevel
import com.example.data.models.TradeDirection
import com.example.data.models.TradingAnalysis
import com.example.ui.components.TradingDashboardSection
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleState = AnalyzerUiState(
      currentAnalysis = TradingAnalysis(
        change5m = "+1.25%",
        change5mValue = 1.25,
        change60m = "+0.75%",
        change60mValue = 0.75,
        netSum = "+2.00%",
        netSumValue = 2.0,
        strengthLevel = StrengthLevel.HIGH,
        direction = TradeDirection.UP,
        calculatedPercentage = 100.0,
        latencyMs = 180
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        TradingDashboardSection(uiState = sampleState)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

