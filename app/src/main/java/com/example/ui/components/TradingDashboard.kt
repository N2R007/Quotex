package com.example.ui.components

import android.app.DatePickerDialog
import com.example.data.analyzer.EntryState
import com.example.data.analyzer.ShortEntryDecision
import com.example.data.analyzer.ShortEntryDecisionEngine
import com.example.data.analyzer.TradingOutputParser
import com.example.data.matrix.InputType
import com.example.data.matrix.MatrixCatalog
import com.example.data.matrix.QuantitativeMatrix
import com.example.data.matrix.RiskLevel
import com.example.data.analyzer.PressureBand
import com.example.data.analyzer.PressureDirection
import com.example.data.analyzer.SourceQuality
import com.example.data.analyzer.ThreeTimeframePressureCalculator
import com.example.data.analyzer.ThreeTimeframePressureResult
import com.example.data.models.DataQualityState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.matrix.Authorized106MatrixEngine
import com.example.data.matrix.UserRuleRegistry
import com.example.data.models.AnalyzerUiState
import com.example.data.models.MetricSnapshot
import com.example.data.models.StrengthLevel
import com.example.data.models.TradeDirection
import com.example.data.models.TradeOutcome
import com.example.data.models.TradingAnalysis
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentCyanDim
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.BorderStrokeLight
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenDim
import com.example.ui.theme.NeonGreenLight
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedDim
import com.example.ui.theme.NeonRedLight
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun TradingDashboardSection(
    uiState: AnalyzerUiState,
    onOpenSettings: () -> Unit = {},
    onSetInvestmentAmount: (Double) -> Unit = {},
    onSetTradeOutcome: (TradeOutcome) -> Unit = {},
    onSetHistoryItemOutcome: (Long, TradeOutcome) -> Unit = { _, _ -> },
    onResetSessionPnl: () -> Unit = {},
    onToggleAutoTrade: () -> Unit = {},
    onResetTradeLock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val isWsConnected by com.example.network.WebSocketTradeRelay.connectionState.collectAsState()
    val tabs = listOf("Analysis", "Auto-Trade", "History")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("trading_dashboard_container")
    ) {
        // High Density Navigation / Tab bar
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkBackground,
            contentColor = NeonGreenLight,
            edgePadding = 12.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = NeonGreen,
                    height = 2.dp
                )
            },
            divider = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BorderStrokeLight)
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            if (index == 1) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(
                                            if (isWsConnected) NeonGreen else NeonRed,
                                            androidx.compose.foundation.shape.CircleShape
                                        )
                                )
                            }
                            Text(
                                text = title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == index) NeonGreenLight else TextMuted
                            )
                        }
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            when (selectedTab) {
                0 -> MainAnalysisTab(
                    uiState = uiState,
                    onOpenSettings = onOpenSettings,
                    onSetInvestmentAmount = onSetInvestmentAmount,
                    onSetTradeOutcome = onSetTradeOutcome,
                    onResetSessionPnl = onResetSessionPnl
                )
                1 -> WebSocketAutoTradeTab(
                    isAutoTradeEnabled = uiState.isAutoTradeEnabled,
                    onToggleAutoTrade = onToggleAutoTrade,
                    onResetTradeLock = onResetTradeLock
                )
                2 -> HistoryTab(
                    history = uiState.history,
                    onSetHistoryItemOutcome = onSetHistoryItemOutcome
                )
            }
        }
    }
}

@Composable
fun MainAnalysisTab(
    uiState: AnalyzerUiState,
    onOpenSettings: () -> Unit = {},
    onSetInvestmentAmount: (Double) -> Unit = {},
    onSetTradeOutcome: (TradeOutcome) -> Unit = {},
    onResetSessionPnl: () -> Unit = {}
) {
    val analysis = uiState.currentAnalysis
    val rawError = uiState.error ?: (if (analysis != null && !analysis.isSuccess && !analysis.isQuotaExceeded) analysis.errorMessage else null)
    val isRateLimitError = rawError?.let {
        it.contains("429") ||
        it.contains("rate limit", ignoreCase = true) ||
        it.contains("rate-limit", ignoreCase = true) ||
        it.contains("quota", ignoreCase = true) ||
        it.contains("RESOURCE_EXHAUSTED", ignoreCase = true)
    } ?: false
    val displayedError = if (isRateLimitError) null else rawError

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Cooldown Banner if rate-limited
        if (uiState.cooldownRemainingSeconds > 0) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rate_limit_cooldown_banner"),
                    shape = RoundedCornerShape(16.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = {
                                if (uiState.cooldownTotalSeconds > 0) {
                                    (uiState.cooldownRemainingSeconds.toFloat() / uiState.cooldownTotalSeconds.toFloat())
                                } else 0f
                            },
                            modifier = Modifier.size(28.dp),
                            color = AccentCyan,
                            strokeWidth = 3.dp,
                            trackColor = BorderStrokeLight
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "API RATE LIMIT COOLDOWN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentCyan,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Auto-scan will resume in ${uiState.cooldownRemainingSeconds}s.",
                                fontSize = 11.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        if (displayedError != null && uiState.cooldownRemainingSeconds <= 0) {
            // Explicit Error Banner when API or Frame parsing fails
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_error_banner"),
                    shape = RoundedCornerShape(16.dp),
                    color = NeonRedDim,
                    border = BorderStroke(1.dp, NeonRedLight.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error Alert",
                                tint = NeonRedLight,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ERROR / ALERT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonRedLight,
                                letterSpacing = 1.sp
                            )
                        }
                        Text(
                            text = displayedError,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = onOpenSettings,
                                colors = ButtonDefaults.textButtonColors(contentColor = NeonGreenLight)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Settings & Engine Config", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // High Density Section Header + Side-by-Side Grid (গাণিতিক হিসাব ANALYSIS + ১০৬ ম্যাট্রিক্স সিগন্যাল)
        item {
            QuantitativeMetricsGrid(
                analysis = analysis,
                history = uiState.history,
                isAudioAlertEnabled = uiState.isAudioAlertEnabled
            )
        }

        // Three-Timeframe Pressure Engine Card (ফলাফল সেকশন)
        // Hidden per user requirement: "ফলাফল সেকশনটা সম্পূর্ণভাবে হাইড করে দেন আমি আপনাকে একটি স্ক্রিনশট দিলাম দেখতে এমন হবে উদাহরণস্বরূপ"

        // High Impact Hero Prediction & Net Strength Card (Includes Dua display and Profit/Loss buttons)
        item {
            HighDensityPredictionCard(
                analysis = analysis,
                isProcessing = uiState.isProcessing,
                onSetTradeOutcome = onSetTradeOutcome,
                history = uiState.history
            )
        }

        // Hidden per user screenshot instruction ("ড্যাশবোর্ডের সবার নিচে এই লেখাগুলো দিবেন। আমি একটি স্ক্রিনশট দিলাম। এই পর্যন্ত সবকিছু হাইড করে দেন।")
        // InvestmentTargetCard and HighDensityFooterBar are hidden so that the prediction card with Duas is cleanly at the bottom.

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

internal data class QuickPredictionState(
    val label: String,
    val powerPercentageStr: String,
    val isUp: Boolean,
    val isDown: Boolean,
    val isValid: Boolean,
    val patternSubtitle: String = "",
    val shortDecision: ShortEntryDecision? = null
)

internal fun calculateQuickPrediction(
    analysis: TradingAnalysis?,
    history: List<TradingAnalysis> = emptyList(),
    currentTimeMs: Long = System.currentTimeMillis()
): QuickPredictionState {
    val isNoTrade = analysis != null && (analysis.isNoTradeZone || analysis.isDeadMarket || analysis.direction == TradeDirection.NEUTRAL)
    val isValid = analysis?.isValid == true && analysis.isSuccess && (isNoTrade || (analysis.change5mValue != null && analysis.change60mValue != null))
    if (!isValid || analysis == null) {
        return QuickPredictionState(
            label = "সিগন্যাল",
            powerPercentageStr = "--",
            isUp = false,
            isDown = false,
            isValid = false,
            patternSubtitle = "",
            shortDecision = null
        )
    }

    val bCode = analysis.behaviorCode ?: ""
    val v5m = analysis.change5mValue ?: 0.0
    val v60m = analysis.change60mValue ?: 0.0
    val v1d = analysis.change1dValue ?: 0.0
    val direction = analysis.direction

    // Evaluate deterministic Short Entry Decision if down direction or down candidates
    val shortDecision = if (direction == TradeDirection.DOWN || v60m < 0.0) {
        ShortEntryDecisionEngine.evaluateShortEntry(analysis, history, currentTimeMs)
    } else {
        null
    }

    val cd = analysis.canonicalDecision
    if (cd != null) {
        val isUp = cd.direction == TradeDirection.UP
        val isDown = cd.direction == TradeDirection.DOWN
        val label = when {
            isUp -> "UP ↗"
            isDown -> "DOWN ↘"
            else -> "HOLD"
        }
        val powerStr = String.format(
            Locale.US, "%.1f%%",
            if (isUp) cd.upPct else if (isDown) cd.downPct else 50.0
        )
        val sub = cd.primaryMatrixId ?: (if (cd.noTrade) "HOLD" else "CAN")
        return QuickPredictionState(
            label = label,
            powerPercentageStr = powerStr,
            isUp = isUp,
            isDown = isDown,
            isValid = !cd.noTrade && !cd.stale,
            patternSubtitle = sub,
            shortDecision = shortDecision
        )
    }

    // 1. Weak Pullback in Uptrend (UPTREND_PULLBACK_DOWN / পুলব্যাক শেষ)
    // Short-time: Dip is terminating, imminent 1-3m bounce UP
    if (bCode == "UPTREND_PULLBACK_DOWN" || (v60m > 0.10 && v5m < 0.0 && abs(v5m) < abs(v60m) * 0.45)) {
        val retracementPct = if (abs(v60m) > 0.001) abs(v5m) / abs(v60m) * 100.0 else 0.0
        val baseBuyerPower = maxOf(50.0, minOf(99.9, 100.0 - retracementPct))
        val dailyBoost = if (v1d > 0.0) minOf(5.0, v1d * 2.0) else maxOf(-4.0, v1d * 2.0)
        val finalPower = maxOf(52.0, minOf(99.9, baseBuyerPower + dailyBoost * 0.4))
        return QuickPredictionState(
            label = "UP ↗",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", finalPower),
            isUp = true,
            isDown = false,
            isValid = true,
            patternSubtitle = "পুলব্যাক",
            shortDecision = null
        )
    }

    // 2. Weak Bounce / Bearish Pullback in Downtrend (DOWNTREND_PULLBACK_UP / বিয়ারিশ পুলব্যাক)
    // Short-time: Exhaustion rally into resistance, imminent 1-3m continuation DOWN
    if (bCode == "DOWNTREND_PULLBACK_UP" || (v60m < -0.10 && v5m > 0.0 && abs(v5m) < abs(v60m) * 0.45)) {
        val bouncePct = if (abs(v60m) > 0.001) abs(v5m) / abs(v60m) * 100.0 else 0.0
        val baseSellerPower = maxOf(50.0, minOf(99.9, 100.0 - bouncePct))
        val dailyBoost = if (v1d < 0.0) minOf(5.0, abs(v1d) * 2.0) else maxOf(-4.0, -v1d * 2.0)
        val finalPower = maxOf(52.0, minOf(99.9, baseSellerPower + dailyBoost * 0.4))
        return QuickPredictionState(
            label = "DOWN ↘",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", finalPower),
            isUp = false,
            isDown = true,
            isValid = true,
            patternSubtitle = "পুলব্যাক",
            shortDecision = shortDecision
        )
    }

    // 3. Sudden Spike Down (SUDDEN_SPIKE_DOWN / তীব্র পতন)
    if (bCode == "SUDDEN_SPIKE_DOWN" || (bCode != "STEADY_ALIGNED_DOWN" && v5m < -0.40 && (v60m >= -0.15 || abs(v5m) > abs(v60m) * 1.5))) {
        val quantPower = if (analysis.downPercentage.isFinite() && analysis.downPercentage > 50.0) {
            analysis.downPercentage
        } else {
            maxOf(75.0, minOf(98.5, 65.0 + abs(v5m) * 25.0))
        }
        return QuickPredictionState(
            label = "DOWN ↘",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", quantPower),
            isUp = false,
            isDown = true,
            isValid = true,
            patternSubtitle = "তীব্র পতন",
            shortDecision = shortDecision
        )
    }

    // 4. Sudden Spike Up (SUDDEN_SPIKE_UP / হঠাৎ স্পাইক)
    if (bCode == "SUDDEN_SPIKE_UP" || (bCode != "STEADY_ALIGNED_UP" && v5m > 0.40 && (v60m <= 0.15 || abs(v5m) > abs(v60m) * 1.5))) {
        val quantPower = if (analysis.upPercentage.isFinite() && analysis.upPercentage > 50.0) {
            analysis.upPercentage
        } else {
            maxOf(75.0, minOf(98.5, 65.0 + abs(v5m) * 25.0))
        }
        return QuickPredictionState(
            label = "UP ↗",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", quantPower),
            isUp = true,
            isDown = false,
            isValid = true,
            patternSubtitle = "হঠাৎ স্পাইক",
            shortDecision = null
        )
    }

    // 5. Bull Trap / Top Fakeout (TOP_FAKEOUT_RISK / বুল ট্র্যাপ -> তীব্র পতন ঝুঁকি)
    if (bCode == "TOP_FAKEOUT_RISK" || bCode == "BULL_TRAP") {
        val quantPower = if (analysis.downPercentage.isFinite() && analysis.downPercentage > 50.0) {
            analysis.downPercentage
        } else {
            val mag = abs(v5m) + abs(v60m)
            maxOf(60.0, minOf(95.0, 55.0 + mag * 25.0))
        }
        val finalPower = maxOf(60.0, minOf(99.0, quantPower))
        return QuickPredictionState(
            label = "DOWN ↘",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", finalPower),
            isUp = false,
            isDown = true,
            isValid = true,
            patternSubtitle = "বুল ট্র্যাপ",
            shortDecision = shortDecision
        )
    }

    // 6. Bear Trap / Bottom Fakeout (BOTTOM_FAKEOUT_RISK / বিয়ার ট্র্যাপ -> তীব্র বাউন্স)
    if (bCode == "BOTTOM_FAKEOUT_RISK" || bCode == "BEAR_TRAP") {
        val quantPower = if (analysis.upPercentage.isFinite() && analysis.upPercentage > 50.0) {
            analysis.upPercentage
        } else {
            val mag = abs(v5m) + abs(v60m)
            maxOf(60.0, minOf(95.0, 55.0 + mag * 25.0))
        }
        val finalPower = maxOf(60.0, minOf(99.0, quantPower))
        return QuickPredictionState(
            label = "UP ↗",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", finalPower),
            isUp = true,
            isDown = false,
            isValid = true,
            patternSubtitle = "বিয়ার ট্র্যাপ",
            shortDecision = null
        )
    }

    // 7. Confirmed Bullish Reversal (CONFIRMED_BULLISH_REVERSAL / নিশ্চিত UP রিভার্সাল)
    if (bCode == "CONFIRMED_BULLISH_REVERSAL") {
        val quantPower = if (analysis.upPercentage.isFinite() && analysis.upPercentage > 50.0) {
            analysis.upPercentage
        } else {
            val mag = abs(v5m) + abs(v60m)
            maxOf(60.0, minOf(95.0, 55.0 + mag * 20.0))
        }
        return QuickPredictionState(
            label = "UP ↗",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", quantPower),
            isUp = true,
            isDown = false,
            isValid = true,
            patternSubtitle = "নিশ্চিত UP রিভার্সাল",
            shortDecision = null
        )
    }

    // 8. Confirmed Bearish Reversal (CONFIRMED_BEARISH_REVERSAL / নিশ্চিত DOWN রিভার্সাল)
    if (bCode == "CONFIRMED_BEARISH_REVERSAL") {
        val quantPower = if (analysis.downPercentage.isFinite() && analysis.downPercentage > 50.0) {
            analysis.downPercentage
        } else {
            val mag = abs(v5m) + abs(v60m)
            maxOf(60.0, minOf(95.0, 55.0 + mag * 20.0))
        }
        return QuickPredictionState(
            label = "DOWN ↘",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", quantPower),
            isUp = false,
            isDown = true,
            isValid = true,
            patternSubtitle = "নিশ্চিত DOWN রিভার্সাল",
            shortDecision = shortDecision
        )
    }

    // 9. Momentum Loss Down (MOMENTUM_LOSS_DOWN / পতন শেষ -> Imminent Bounce UP)
    if (bCode == "MOMENTUM_LOSS_DOWN") {
        val quantPower = if (analysis.upPercentage.isFinite() && analysis.upPercentage > 50.0) {
            analysis.upPercentage
        } else if (analysis.calculatedPercentage.isFinite() && analysis.calculatedPercentage > 50.0) {
            analysis.calculatedPercentage
        } else {
            val ratio = if (abs(v60m) > 0.001) (1.0 - abs(v5m) / abs(v60m)) else 0.65
            maxOf(50.0, minOf(95.0, 50.0 + ratio * 40.0))
        }
        val finalPower = maxOf(53.0, minOf(99.0, quantPower))
        return QuickPredictionState(
            label = "UP ↗",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", finalPower),
            isUp = true,
            isDown = false,
            isValid = true,
            patternSubtitle = "পতন সমাপ্তি",
            shortDecision = null
        )
    }

    // 10. Momentum Loss Up (MOMENTUM_LOSS_UP / বৃদ্ধি শেষ -> Imminent Drop DOWN)
    if (bCode == "MOMENTUM_LOSS_UP") {
        val quantPower = if (analysis.downPercentage.isFinite() && analysis.downPercentage > 50.0) {
            analysis.downPercentage
        } else if (analysis.calculatedPercentage.isFinite() && analysis.calculatedPercentage > 50.0) {
            analysis.calculatedPercentage
        } else {
            val ratio = if (abs(v60m) > 0.001) (1.0 - abs(v5m) / abs(v60m)) else 0.65
            maxOf(50.0, minOf(95.0, 50.0 + ratio * 40.0))
        }
        val finalPower = maxOf(53.0, minOf(99.0, quantPower))
        return QuickPredictionState(
            label = "DOWN ↘",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", finalPower),
            isUp = false,
            isDown = true,
            isValid = true,
            patternSubtitle = "উত্থান সমাপ্তি",
            shortDecision = shortDecision
        )
    }

    // 11. Strong Momentum Breakout Up (STRONG_MOMENTUM_BREAK_UP / শক্তিশালী ব্রেকআউট UP)
    if (bCode == "STRONG_MOMENTUM_BREAK_UP") {
        val quantPower = if (analysis.upPercentage.isFinite() && analysis.upPercentage > 60.0) {
            analysis.upPercentage
        } else {
            val mag = abs(v5m) + abs(v60m)
            maxOf(65.0, minOf(96.0, 60.0 + mag * 25.0))
        }
        return QuickPredictionState(
            label = "UP ↗",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", quantPower),
            isUp = true,
            isDown = false,
            isValid = true,
            patternSubtitle = "ব্রেকআউট",
            shortDecision = null
        )
    }

    // 12. Strong Momentum Breakdown Down (STRONG_MOMENTUM_BREAK_DOWN / শক্তিশালী ব্রেকডাউন DOWN)
    if (bCode == "STRONG_MOMENTUM_BREAK_DOWN") {
        val quantPower = if (analysis.downPercentage.isFinite() && analysis.downPercentage > 60.0) {
            analysis.downPercentage
        } else {
            val mag = abs(v5m) + abs(v60m)
            maxOf(65.0, minOf(96.0, 60.0 + mag * 25.0))
        }
        return QuickPredictionState(
            label = "DOWN ↘",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", quantPower),
            isUp = false,
            isDown = true,
            isValid = true,
            patternSubtitle = "ব্রেকডাউন",
            shortDecision = shortDecision
        )
    }

    // 13. Squeeze Breakout Imminent (SQUEEZE_BREAKOUT_IMMINENT / স্কুইজ)
    if (bCode == "SQUEEZE_BREAKOUT_IMMINENT") {
        val targetDir = if (direction == TradeDirection.DOWN) "DOWN ↘" else "UP ↗"
        val isUpSig = direction != TradeDirection.DOWN
        val squeezeScore = maxOf(52.0, minOf(80.0, 50.0 + (abs(v5m) + abs(v60m)) * 20.0))
        return QuickPredictionState(
            label = targetDir,
            powerPercentageStr = String.format(Locale.US, "%.1f%%", squeezeScore),
            isUp = isUpSig,
            isDown = !isUpSig,
            isValid = true,
            patternSubtitle = "স্কুইজ ব্রেক",
            shortDecision = shortDecision
        )
    }

    // 14. No-Trade Zone / Dead Market / Neutral
    if (isNoTrade) {
        return QuickPredictionState(
            label = "HOLD",
            powerPercentageStr = "50.0%",
            isUp = false,
            isDown = false,
            isValid = true,
            patternSubtitle = "নো-ট্রেড",
            shortDecision = shortDecision
        )
    }

    // 15. Standard UP Direction with Multi-Timeframe Confluence Calculation
    if (direction == TradeDirection.UP) {
        val primaryProb = if (analysis.upPercentage.isFinite() && analysis.upPercentage > 0.0) {
            analysis.upPercentage
        } else if (analysis.calculatedPercentage.isFinite() && analysis.calculatedPercentage > 0.0) {
            analysis.calculatedPercentage
        } else {
            50.0
        }
        // Micro + macro confirmation
        val v5Boost = if (v5m > 0.0) minOf(4.0, v5m * 4.0) else maxOf(-6.0, v5m * 4.0)
        val v60Boost = if (v60m > 0.0) minOf(3.0, v60m * 2.0) else maxOf(-5.0, v60m * 2.0)
        val weightedPower = maxOf(50.0, minOf(99.9, primaryProb + (v5Boost * 0.6 + v60Boost * 0.4)))
        return QuickPredictionState(
            label = "UP ↗",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", weightedPower),
            isUp = true,
            isDown = false,
            isValid = true,
            patternSubtitle = "",
            shortDecision = null
        )
    }

    // 16. Standard DOWN Direction with Multi-Timeframe Confluence Calculation
    if (direction == TradeDirection.DOWN) {
        val primaryProb = if (analysis.downPercentage.isFinite() && analysis.downPercentage > 0.0) {
            analysis.downPercentage
        } else if (analysis.calculatedPercentage.isFinite() && analysis.calculatedPercentage > 0.0) {
            analysis.calculatedPercentage
        } else {
            50.0
        }
        // Micro + macro confirmation
        val v5Boost = if (v5m < 0.0) minOf(4.0, abs(v5m) * 4.0) else maxOf(-6.0, -v5m * 4.0)
        val v60Boost = if (v60m < 0.0) minOf(3.0, abs(v60m) * 2.0) else maxOf(-5.0, -v60m * 2.0)
        val weightedPower = maxOf(50.0, minOf(99.9, primaryProb + (v5Boost * 0.6 + v60Boost * 0.4)))

        val sub = when (shortDecision?.state) {
            EntryState.SHORT_ENTRY_READY -> "READY"
            EntryState.SHORT_SETUP -> "SETUP"
            EntryState.WAIT_CONFIRMATION -> "WAIT"
            else -> ""
        }

        return QuickPredictionState(
            label = "DOWN ↘",
            powerPercentageStr = String.format(Locale.US, "%.1f%%", weightedPower),
            isUp = false,
            isDown = true,
            isValid = true,
            patternSubtitle = sub,
            shortDecision = shortDecision
        )
    }

    return QuickPredictionState(
        label = "HOLD",
        powerPercentageStr = "50.0%",
        isUp = false,
        isDown = false,
        isValid = true,
        patternSubtitle = "",
        shortDecision = shortDecision
    )
}

@Composable
fun QuantitativeMetricsGrid(
    analysis: TradingAnalysis?,
    history: List<TradingAnalysis> = emptyList(),
    isAudioAlertEnabled: Boolean = true
) {
    // 106 Matrix evaluation logic
    val val5m = analysis?.change5mValue
    val val60m = analysis?.change60mValue
    val val1d = analysis?.change1dValue

    val metricSnapshots = remember(history) {
        history.mapNotNull { h ->
            val m5 = h.change5mValue
            val m60 = h.change60mValue
            if (m5 != null && m60 != null) {
                MetricSnapshot(val5m = m5, val60m = m60, val1d = h.change1dValue)
            } else null
        }
    }

    var refreshVerifiedState by remember { mutableIntStateOf(0) }

    val isCoreDetected = analysis?.isValid == true && analysis.isSuccess && val5m != null && val60m != null

    val evaluatedMatch = remember(val5m, val60m, val1d, metricSnapshots, refreshVerifiedState, isCoreDetected) {
        if (!isCoreDetected) {
            null
        } else {
            Authorized106MatrixEngine.evaluate(
                val5m = val5m,
                val60m = val60m,
                val1d = val1d,
                history = metricSnapshots
            )
        }
    }

    // Active signal state
    var activeSignal by remember { mutableStateOf<Authorized106MatrixEngine.Matrix106Match?>(null) }
    var signalTimestamp by remember { mutableStateOf(0L) }
    var isSignalActive by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableIntStateOf(0) }

    // Authoritative audio alert & rule de-duplication tracking
    var lastAlertedRuleId by remember { mutableStateOf<String?>(null) }
    var lastAlertedDirection by remember { mutableStateOf<TradeDirection?>(null) }
    var lastAlertedTimestamp by remember { mutableStateOf(0L) }

    // When 5m or 60m are not detected on screen, immediately deactivate and clear any UP/DOWN signal
    LaunchedEffect(isCoreDetected) {
        if (!isCoreDetected) {
            activeSignal = null
            isSignalActive = false
            remainingSeconds = 0
            signalTimestamp = 0L
        }
    }

    // Update signal when a valid matrix match is actively detected on screen
    LaunchedEffect(evaluatedMatch?.id, evaluatedMatch?.direction, isCoreDetected) {
        if (isCoreDetected && evaluatedMatch != null) {
            val now = System.currentTimeMillis()
            val isSameRule = (lastAlertedRuleId == evaluatedMatch.id && lastAlertedDirection == evaluatedMatch.direction)
            // Strict user command: do NOT repeatedly pronounce the same number.
            // Only announce if it is a genuinely new rule number/direction or if at least 60 seconds have elapsed.
            val isDuplicate = isSameRule && (now - lastAlertedTimestamp < 60_000L)

            activeSignal = evaluatedMatch
            signalTimestamp = now
            isSignalActive = true
            remainingSeconds = 30

            if (!isDuplicate) {
                lastAlertedRuleId = evaluatedMatch.id
                lastAlertedDirection = evaluatedMatch.direction
                lastAlertedTimestamp = now

                if (isAudioAlertEnabled) {
                    val sound = if (evaluatedMatch.direction == TradeDirection.UP) {
                        com.example.audio.AudioSignalEngine.SOUND_UP_ALERT
                    } else {
                        com.example.audio.AudioSignalEngine.SOUND_DOWN_ALERT
                    }
                    // Strip letters (U, D, M) and leading zeros so TTS speaks only digit number, e.g. "UP 45" or "DOWN 7"
                    val numericOnly = evaluatedMatch.id.filter { it.isDigit() }.toIntOrNull()?.toString()
                        ?: evaluatedMatch.id.filter { it.isDigit() }
                    val dirText = if (evaluatedMatch.direction == TradeDirection.UP) "UP" else "DOWN"
                    val callout = if (numericOnly.isNotEmpty()) "$dirText $numericOnly" else dirText
                    com.example.audio.AudioSignalEngine.playSoundEvent(sound, callout)
                }
            }
        }
    }

    // 30-second countdown timer: Once expired, becomes inactive (greyed out) and stays grey until a genuinely new match occurs
    LaunchedEffect(signalTimestamp) {
        if (signalTimestamp > 0L) {
            isSignalActive = true
            while (true) {
                val elapsed = System.currentTimeMillis() - signalTimestamp
                val remainingMs = 30_000L - elapsed
                if (remainingMs <= 0L) {
                    isSignalActive = false
                    remainingSeconds = 0
                    break
                }
                remainingSeconds = ((remainingMs + 999L) / 1000L).toInt()
                delay(250L)
            }
        }
    }

    val isUp = activeSignal?.direction == TradeDirection.UP
    val isDown = activeSignal?.direction == TradeDirection.DOWN

    // Color assignment: User explicitly commanded UP = Green, DOWN = Red.
    val activeColor = when {
        !isSignalActive -> TextMuted
        isUp -> NeonGreen
        isDown -> NeonRed
        else -> TextMuted
    }

    val activeColorLight = when {
        !isSignalActive -> TextMuted
        isUp -> NeonGreenLight
        isDown -> NeonRedLight
        else -> TextMuted
    }

    val buttonBrush = if (isSignalActive && activeSignal != null) {
        if (isUp) {
            Brush.verticalGradient(listOf(Color(0xFF22C55E), Color(0xFF16A34A)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
        }
    } else {
        SolidColor(Color(0xFF27272A))
    }

    val buttonBorderColor = if (isSignalActive) activeColorLight else Color(0xFF3F3F46)
    val cardBorderColor = if (isSignalActive) activeColor.copy(alpha = 0.85f) else BorderStrokeLight

    var showRuleManagerDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // LEFT COLUMN: "গাণিতিক হিসাব" + 3 Timeframe Cards (5m, 60m, 1D)
        Column(
            modifier = Modifier.weight(1.58f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CANONICAL",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { showRuleManagerDialog = true },
                        shape = RoundedCornerShape(4.dp),
                        color = DarkSurfaceVariant,
                        border = BorderStroke(0.5.dp, AccentCyan.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Edit Rules",
                                tint = AccentCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Rule Edit",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }
                    }

                    // Verify Button: Marks current matched rule as verified/unverified with tick mark (✓)
                    val currentSignalId = (activeSignal?.id ?: analysis?.primaryMatrixId ?: analysis?.canonicalDecision?.primaryMatrixId)
                        ?.replace("[", "")?.replace("]", "")?.trim()
                    val isCurrentRuleVerified = if (!currentSignalId.isNullOrBlank()) {
                        // Read state to ensure recomposition occurs when state changes
                        refreshVerifiedState.let { }
                        UserRuleRegistry.isRuleVerified(currentSignalId)
                    } else false

                    Surface(
                        onClick = {
                            if (!currentSignalId.isNullOrBlank()) {
                                UserRuleRegistry.toggleVerifiedRule(currentSignalId)
                                refreshVerifiedState++
                            }
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = if (isCurrentRuleVerified) NeonGreenDim else DarkSurfaceVariant,
                        border = BorderStroke(
                            0.5.dp,
                            if (isCurrentRuleVerified) NeonGreen.copy(alpha = 0.8f) else BorderStrokeLight
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Verify",
                                tint = if (isCurrentRuleVerified) NeonGreenLight else TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = if (isCurrentRuleVerified) "✓ Verified" else "Verify",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCurrentRuleVerified) NeonGreenLight else TextSecondary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // 1. 5 min card
                val v5 = analysis?.change5mValue ?: 0.0
                val p5mStr = analysis?.change5m ?: "--"
                val font5m = when {
                    p5mStr.length <= 5 -> 18.sp
                    p5mStr.length <= 6 -> 16.5.sp
                    else -> 14.5.sp
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_5m_card"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, BorderStrokeLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "5 MIN",
                            fontSize = 8.5.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = p5mStr,
                            fontSize = font5m,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (p5mStr == "--") TextPrimary else if (v5 >= 0) NeonGreenLight else NeonRedLight,
                            letterSpacing = (-0.8).sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 2. 60 min card
                val v60 = analysis?.change60mValue ?: 0.0
                val p60mStr = analysis?.change60m ?: "--"
                val font60m = when {
                    p60mStr.length <= 5 -> 18.sp
                    p60mStr.length <= 6 -> 16.5.sp
                    else -> 14.5.sp
                }
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("metric_60m_card"),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, BorderStrokeLight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "60 MIN",
                            fontSize = 8.5.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = p60mStr,
                            fontSize = font60m,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (p60mStr == "--") TextPrimary else if (v60 >= 0) NeonGreenLight else NeonRedLight,
                            letterSpacing = (-0.8).sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 3. 1 Day card (1D) hidden per user request
            }
        }

        // RIGHT COLUMN: 106 Matrix Signal Card
        Card(
            modifier = Modifier
                .weight(1f)
                .testTag("matrix_106_signal_card"),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.2.dp, cardBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Middle Button: "UP" / "DOWN" (Solid colored when active, greyed out after 30s)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(buttonBrush)
                        .border(1.dp, buttonBorderColor, RoundedCornerShape(7.dp))
                        .testTag("signal_action_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSignalActive && activeSignal != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (activeSignal?.direction == TradeDirection.DOWN) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "DOWN",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "DOWN",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "UP",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "UP",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            if (remainingSeconds > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${remainingSeconds}s",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "-Wait",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // Bottom row: Matched Matrix ID flanked by 5m (left) and 60m (right) real strength arrows
                val v5 = analysis?.change5mValue ?: 0.0
                val v60 = analysis?.change60mValue ?: 0.0
                val str5 = kotlin.math.abs(v5)
                val str60 = kotlin.math.abs(v60)

                val is5mStronger = str5 > str60 && str5 > 0.0001
                val is60mStronger = str60 > str5 && str60 > 0.0001

                // Verification checkmark logic:
                // Signal is verified (✓) ONLY when data quality is VERIFIED, non-approximate, valid 5m/60m, and uncancelled.
                val isVerifiedSignal = isSignalActive &&
                        activeSignal != null &&
                        (activeSignal?.direction == TradeDirection.UP || activeSignal?.direction == TradeDirection.DOWN) &&
                        analysis != null &&
                        analysis.isValid &&
                        analysis.isSuccess &&
                        analysis.dataQuality == DataQualityState.VERIFIED &&
                        !analysis.isApproximate &&
                        !analysis.isProvisional &&
                        !analysis.isNoTradeZone &&
                        !analysis.isWarningOnly &&
                        analysis.change5mValue != null &&
                        analysis.change60mValue != null &&
                        analysis.canonicalDecision?.cancelledMatrixIds?.contains(activeSignal?.id) != true

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable { showRuleManagerDialog = true }
                ) {
                    // Left Slot: 5 min real strength indicator (only shown if 5m is stronger)
                    Box(
                        modifier = Modifier.width(13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (is5mStronger) {
                            Text(
                                text = if (v5 >= 0) "▲" else "▼",
                                color = if (v5 >= 0) NeonGreenLight else NeonRedLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    // Center: Matched Matrix ID (e.g. [D061] or [M152])
                    val matrixLabel = if (activeSignal != null) "[${activeSignal?.id}]" else "--"
                    Text(
                        text = matrixLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSignalActive) activeColorLight else TextMuted,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width(2.dp))

                    // Right Slot: 60 min real strength indicator (only shown if 60m is stronger)
                    Box(
                        modifier = Modifier.width(13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (is60mStronger) {
                            Text(
                                text = if (v60 >= 0) "▲" else "▼",
                                color = if (v60 >= 0) NeonGreenLight else NeonRedLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Verified Tick Mark (✓) displayed when signal is verified in UserRuleRegistry
                    val isRuleUserVerified = (activeSignal?.id ?: analysis?.primaryMatrixId ?: analysis?.canonicalDecision?.primaryMatrixId)?.let {
                        val cleanId = it.replace("[", "").replace("]", "").trim()
                        refreshVerifiedState.let { }
                        UserRuleRegistry.isRuleVerified(cleanId)
                    } ?: false

                    if (isRuleUserVerified) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "✓",
                            color = if (isSignalActive) NeonGreenLight else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("signal_verified_tick")
                        )
                    }
                }

                // Power / Confidence Tier (HIGH / MEDIUM / LOW) displayed under the matrix number
                val matchedRuleId = activeSignal?.id ?: analysis?.primaryMatrixId ?: analysis?.canonicalDecision?.primaryMatrixId
                val tierText = if (isSignalActive && !matchedRuleId.isNullOrBlank()) {
                    UserRuleRegistry.getRuleTierSimple(matchedRuleId)
                } else ""

                if (tierText.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = when (tierText) {
                            "HIGH" -> NeonGreenDim
                            "MEDIUM" -> AccentCyan.copy(alpha = 0.15f)
                            else -> DarkSurfaceVariant
                        },
                        border = BorderStroke(
                            0.5.dp,
                            when (tierText) {
                                "HIGH" -> NeonGreen.copy(alpha = 0.7f)
                                "MEDIUM" -> AccentCyan.copy(alpha = 0.7f)
                                else -> BorderStrokeLight
                            }
                        ),
                        modifier = Modifier
                            .testTag("canonical_signal_tier_badge")
                            .clickable { showRuleManagerDialog = true }
                    ) {
                        Text(
                            text = tierText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = when (tierText) {
                                "HIGH" -> NeonGreenLight
                                "MEDIUM" -> AccentCyan
                                else -> TextMuted
                            },
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 0.5.dp)
                        )
                    }
                }
            }
        }
    }

    if (showRuleManagerDialog) {
        RuleManagerDialog(
            initialMatrixId = activeSignal?.id,
            live5m = analysis?.change5mValue,
            live60m = analysis?.change60mValue,
            onDismiss = { showRuleManagerDialog = false }
        )
    }
}

@Composable
fun ThreeTimeframePressureDashboardCard(
    analysis: TradingAnalysis? = null,
    cooldownRemainingSeconds: Int = 0,
    pressureResult: ThreeTimeframePressureResult? = null,
    isAudioAlertEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 1. Compute deterministic ThreeTimeframePressureResult from analysis or use provided
    val currentComputedResult = remember(
        pressureResult,
        analysis?.change5mValue,
        analysis?.change60mValue,
        analysis?.change1dValue,
        analysis?.isApproximate,
        analysis?.dataQuality
    ) {
        if (pressureResult != null) {
            pressureResult
        } else {
            val p5 = analysis?.change5mValue
            val p60 = analysis?.change60mValue
            val p1d = analysis?.change1dValue
            val isApprox = analysis?.isApproximate == true
            val isAmbiguous = analysis?.dataQuality == DataQualityState.AMBIGUOUS
            val sourceQ = when {
                isApprox -> SourceQuality.APPROXIMATE
                isAmbiguous -> SourceQuality.AMBIGUOUS
                else -> SourceQuality.VERIFIED
            }
            ThreeTimeframePressureCalculator.calculate(
                p5m = p5,
                p60m = p60,
                p1d = p1d,
                timestamp = analysis?.timestamp ?: System.currentTimeMillis(),
                sourceQuality = sourceQ
            )
        }
    }

    // 2. Real-time update logic:
    // Strictly requires active 5m and 60m detection on screen, or an explicitly provided valid pressureResult
    val isCoreDetected = (pressureResult != null && !pressureResult.isDataIncomplete) ||
        (analysis?.isValid == true && analysis.isSuccess && analysis.change5mValue != null && analysis.change60mValue != null)

    var activeValidResult by remember {
        mutableStateOf<ThreeTimeframePressureResult?>(
            if (isCoreDetected && !currentComputedResult.isDataIncomplete) currentComputedResult else null
        )
    }

    LaunchedEffect(currentComputedResult.signature, currentComputedResult.isDataIncomplete, isCoreDetected) {
        if (isCoreDetected && !currentComputedResult.isDataIncomplete) {
            val prevSig = activeValidResult?.signature
            if (prevSig == null || prevSig != currentComputedResult.signature) {
                activeValidResult = currentComputedResult
            }
        } else if (!isCoreDetected) {
            activeValidResult = null
        }
    }

    val displayResult = if (isCoreDetected && !currentComputedResult.isDataIncomplete) {
        if (activeValidResult != null && activeValidResult?.signature == currentComputedResult.signature) {
            activeValidResult!!
        } else {
            currentComputedResult
        }
    } else if (isCoreDetected) {
        activeValidResult ?: currentComputedResult
    } else {
        currentComputedResult.copy(
            direction = PressureDirection.NO_SIGNAL,
            isDataIncomplete = true
        )
    }

    val direction = if (isCoreDetected) displayResult.direction else PressureDirection.NO_SIGNAL
    val directionColor = when (direction) {
        PressureDirection.UP -> NeonGreen
        PressureDirection.DOWN -> NeonRed
        PressureDirection.NO_SIGNAL -> TextMuted
    }
    val directionText = when (direction) {
        PressureDirection.UP -> "UP  🔺"
        PressureDirection.DOWN -> "DOWN  🔻"
        PressureDirection.NO_SIGNAL -> "NO SIGNAL"
    }

    val netFormatted = if (displayResult.netPressurePercent >= 0.0) {
        String.format(Locale.US, "+%.2f%%", displayResult.netPressurePercent)
    } else {
        String.format(Locale.US, "%.2f%%", displayResult.netPressurePercent)
    }

    // Primary Triggered Matrix & Direction resolution (matching "নির্বাচিত প্রাইমারি ম্যাট্রিক্স" section)
    val primaryMatrixId = if (isCoreDetected) analysis?.primaryMatrixId else null
    val pMat = primaryMatrixId?.let { MatrixCatalog.getById(it) }
    val pBadge = getMatrixSafetySymbol(pMat?.riskLevel)

    val effectiveMatrixDirection: TradeDirection = if (!isCoreDetected) {
        TradeDirection.NEUTRAL
    } else {
        when {
            analysis?.direction == TradeDirection.UP -> TradeDirection.UP
            analysis?.direction == TradeDirection.DOWN -> TradeDirection.DOWN
            else -> {
                val v5m = analysis?.change5mValue ?: displayResult.p5m ?: 0.0
                val v60m = analysis?.change60mValue ?: displayResult.p60m ?: 0.0
                val net = analysis?.netSumValue ?: (v5m + v60m)
                val v1d = analysis?.change1dValue ?: displayResult.p1d ?: 0.0
                when {
                    v5m > 0.001 && v60m > 0.001 -> TradeDirection.UP
                    v5m < -0.001 && v60m < -0.001 -> TradeDirection.DOWN
                    net > 0.005 -> TradeDirection.UP
                    net < -0.005 -> TradeDirection.DOWN
                    v60m > 0.005 -> TradeDirection.UP
                    v60m < -0.005 -> TradeDirection.DOWN
                    v5m > 0.005 -> TradeDirection.UP
                    v5m < -0.005 -> TradeDirection.DOWN
                    v1d > 0.005 -> TradeDirection.UP
                    v1d < -0.005 -> TradeDirection.DOWN
                    (analysis?.upPercentage ?: 0.0) > (analysis?.downPercentage ?: 0.0) + 5.0 -> TradeDirection.UP
                    (analysis?.downPercentage ?: 0.0) > (analysis?.upPercentage ?: 0.0) + 5.0 -> TradeDirection.DOWN
                    direction == PressureDirection.UP -> TradeDirection.UP
                    direction == PressureDirection.DOWN -> TradeDirection.DOWN
                    else -> TradeDirection.NEUTRAL
                }
            }
        }
    }

    val primaryDirLabel = when (effectiveMatrixDirection) {
        TradeDirection.UP -> "UP ↗"
        TradeDirection.DOWN -> "DOWN ↘"
        TradeDirection.NEUTRAL -> if (direction == PressureDirection.UP) "UP ↗" else if (direction == PressureDirection.DOWN) "DOWN ↘" else "WAIT"
    }
    val primaryDirColor = when (effectiveMatrixDirection) {
        TradeDirection.UP -> NeonGreenLight
        TradeDirection.DOWN -> NeonRedLight
        TradeDirection.NEUTRAL -> if (direction == PressureDirection.UP) NeonGreenLight else if (direction == PressureDirection.DOWN) NeonRedLight else TextMuted
    }
    val primaryBoxBg = when (effectiveMatrixDirection) {
        TradeDirection.UP -> NeonGreen.copy(alpha = 0.15f)
        TradeDirection.DOWN -> NeonRed.copy(alpha = 0.15f)
        TradeDirection.NEUTRAL -> if (direction == PressureDirection.UP) NeonGreen.copy(alpha = 0.15f) else if (direction == PressureDirection.DOWN) NeonRed.copy(alpha = 0.15f) else DarkBackground
    }
    val primaryBoxBorder = when (effectiveMatrixDirection) {
        TradeDirection.UP -> NeonGreen.copy(alpha = 0.6f)
        TradeDirection.DOWN -> NeonRed.copy(alpha = 0.6f)
        TradeDirection.NEUTRAL -> if (direction == PressureDirection.UP) NeonGreen.copy(alpha = 0.6f) else if (direction == PressureDirection.DOWN) NeonRed.copy(alpha = 0.6f) else BorderStrokeLight
    }

    // Authoritative Single-Point Audio Trigger:
    // Plays sound ONLY when dashboard direction display is UP or DOWN, and changed from previous direction.
    // NEUTRAL / HOLD / NO_SIGNAL plays zero sound.
    val currentDisplayedDirection = remember(primaryMatrixId, effectiveMatrixDirection, direction, isCoreDetected) {
        if (!isCoreDetected) {
            "HOLD"
        } else {
            when {
                primaryMatrixId != null -> when (effectiveMatrixDirection) {
                    TradeDirection.UP -> "UP"
                    TradeDirection.DOWN -> "DOWN"
                    TradeDirection.NEUTRAL -> when (direction) {
                        PressureDirection.UP -> "UP"
                        PressureDirection.DOWN -> "DOWN"
                        else -> "HOLD"
                    }
                }
                direction == PressureDirection.UP -> "UP"
                direction == PressureDirection.DOWN -> "DOWN"
                else -> "HOLD"
            }
        }
    }

    var lastSignaledDirection by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(currentDisplayedDirection, isAudioAlertEnabled, isCoreDetected) {
        if (!isAudioAlertEnabled || !isCoreDetected) {
            lastSignaledDirection = null
            return@LaunchedEffect
        }

        // QuantitativeMetricsGrid already handles authoritative matrix alert with speech and beep.
        // Only trigger generic sound if primaryMatrixId is null to avoid double sound events.
        if (primaryMatrixId == null && (currentDisplayedDirection == "UP" || currentDisplayedDirection == "DOWN") &&
            currentDisplayedDirection != lastSignaledDirection
        ) {
            lastSignaledDirection = currentDisplayedDirection
            val sound = if (currentDisplayedDirection == "UP") {
                com.example.audio.AudioSignalEngine.SOUND_UP_ALERT
            } else {
                com.example.audio.AudioSignalEngine.SOUND_DOWN_ALERT
            }
            com.example.audio.AudioSignalEngine.playSoundEvent(sound)
        } else if (currentDisplayedDirection != "UP" && currentDisplayedDirection != "DOWN") {
            lastSignaledDirection = null
        }
    }

    val upShareFormatted = String.format(Locale.US, "%.2f%%", displayResult.upSharePercent)
    val downShareFormatted = String.format(Locale.US, "%.2f%%", displayResult.downSharePercent)
    val upEnergyFormatted = String.format(Locale.US, "%.6f", displayResult.upEnergy)
    val downEnergyFormatted = String.format(Locale.US, "%.6f", displayResult.downEnergy)

    val cleanMeaning = when (displayResult.pressureBand) {
        PressureBand.VERY_STRONG_UP -> "Very Strong"
        PressureBand.STRONG_UP -> "Strong"
        PressureBand.MODERATE_UP -> "Moderate"
        PressureBand.MIXED_OR_WEAK -> "Mixed/Weak"
        PressureBand.MODERATE_DOWN -> "Moderate"
        PressureBand.STRONG_DOWN -> "Strong"
        PressureBand.VERY_STRONG_DOWN -> "Very Strong"
    }
    val pressureBandFormatted = "${displayResult.pressureBand.name}\n($cleanMeaning)"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("three_timeframe_pressure_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, BorderStrokeLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Section Header (top-left corner of the box, with detected signed inputs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pressure_card_header"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Result:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                val p5Str = displayResult.p5m?.let { (if (it >= 0) "+" else "") + String.format(Locale.US, "%.2f%%", it) } ?: "--"
                val p60Str = displayResult.p60m?.let { (if (it >= 0) "+" else "") + String.format(Locale.US, "%.2f%%", it) } ?: "--"
                val p1dStr = displayResult.p1d?.let { (if (it >= 0) "+" else "") + String.format(Locale.US, "%.2f%%", it) } ?: "--"
                Text(
                    text = "5m: $p5Str | 60m: $p60Str | 1D: $p1dStr",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextMuted
                )
            }

            // Side-by-side row: LEFT (Direction + Primary Matrix) & RIGHT (5-line metrics)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT SIDE: Big Direction Indicator / Primary Matrix Box
                Column(
                    modifier = Modifier
                        .weight(0.38f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (primaryMatrixId != null) primaryBoxBg else when (direction) {
                                PressureDirection.UP -> NeonGreen.copy(alpha = 0.12f)
                                PressureDirection.DOWN -> NeonRed.copy(alpha = 0.12f)
                                PressureDirection.NO_SIGNAL -> DarkBackground
                            }
                        )
                        .border(
                            1.dp,
                            if (primaryMatrixId != null) primaryBoxBorder else when (direction) {
                                PressureDirection.UP -> NeonGreen.copy(alpha = 0.45f)
                                PressureDirection.DOWN -> NeonRed.copy(alpha = 0.45f)
                                PressureDirection.NO_SIGNAL -> BorderStrokeLight
                            },
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 10.dp)
                        .testTag("pressure_left_direction_box"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (primaryMatrixId != null) {
                        val cleanPrimaryId = primaryMatrixId.replace("[", "").replace("]", "").trim()
                        val isPrimaryVerified = UserRuleRegistry.isRuleVerified(cleanPrimaryId)
                        val fullMatrixText = buildString {
                            append(primaryMatrixId)
                            if (pBadge.isNotEmpty()) {
                                append(" ")
                                append(pBadge)
                            }
                            if (isPrimaryVerified) {
                                append(" ✓")
                            }
                        }
                        Text(
                            text = fullMatrixText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = primaryDirColor,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = primaryDirLabel,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = primaryDirColor,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = directionText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = directionColor,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Net/P: $netFormatted",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = directionColor,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // RIGHT SIDE: Exactly 5 lines
                Column(
                    modifier = Modifier
                        .weight(0.62f)
                        .testTag("pressure_right_metrics_column"),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    PressureMetricRow(label = "UP Share", value = upShareFormatted)
                    PressureMetricRow(label = "DOWN Share", value = downShareFormatted)
                    PressureMetricRow(label = "UP Energy", value = upEnergyFormatted)
                    PressureMetricRow(label = "DOWN Energy", value = downEnergyFormatted)
                    PressureMetricRow(label = "Pressure Band", value = pressureBandFormatted, isMultiLine = true)
                }
            }
        }
    }
}

@Composable
private fun PressureMetricRow(
    label: String,
    value: String,
    isMultiLine: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = if (isMultiLine) Alignment.Top else Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(
            text = value,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = TextPrimary,
            textAlign = TextAlign.End,
            maxLines = if (isMultiLine) 2 else 1,
            softWrap = isMultiLine,
            modifier = if (isMultiLine) Modifier.weight(1f) else Modifier
        )
    }
}

private fun buildHighlightedForecastText(
    text: String,
    defaultColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val regex = Regex("(?<=[^A-Za-z0-9\u0980-\u09FF]|^)(Bull Trap|Bear Trap|বুল ট্র্যাপ|বিয়ার ট্র্যাপ|রিভার্সাল ডাউন|নো-ট্রেড|UP ↗|DOWN ↘|UP|DOWN|BUY|SELL|Pullback|Retracement|পুলব্যাক|রিট্রেসমেন্ট|পরামর্শ|উপরে|নিচে|আপ|ডাউন|বাই|সেল|বৃদ্ধি|পতন|বাউন্সের|বাউন্স|ঝুঁকি|ফেকআউট|শঙ্কা|সতর্কতা|রিভার্সাল|নিষেধ|স্থির|অপেক্ষা|শান্ত|গতিহীন|অস্থির|টানাপোড়েন|চপি|ঊর্ধ্বমুখী|নিম্নমুখী|বুলিশ|বিয়ারিশ|সম্ভাবনা|উত্থান)(?=[^A-Za-z0-9\u0980-\u09FF]|$)")
        val matches = regex.findAll(text)
        for (match in matches) {
            val start = match.range.first
            val end = match.range.last + 1
            if (start > cursor) {
                withStyle(SpanStyle(color = defaultColor, fontWeight = FontWeight.Normal)) {
                    append(text.substring(cursor, start))
                }
            }
            val word = match.value
            when (word) {
                "UP", "UP ↗", "উপরে", "আপ", "বৃদ্ধি", "বাই", "BUY", "ঊর্ধ্বমুখী", "বুলিশ", "উত্থান" -> {
                    withStyle(SpanStyle(color = Color(0xFF22C55E), fontWeight = FontWeight.Black)) {
                        append(word)
                    }
                }
                "DOWN", "DOWN ↘", "নিচে", "ডাউন", "সেল", "SELL", "পতন", "রিভার্সাল ডাউন", "নিম্নমুখী", "বিয়ারিশ" -> {
                    withStyle(SpanStyle(color = Color(0xFFEF4444), fontWeight = FontWeight.Black)) {
                        append(word)
                    }
                }
                "ঝুঁকি", "ফেকআউট", "বুল ট্র্যাপ", "বিয়ার ট্র্যাপ", "Bull Trap", "Bear Trap", "শঙ্কা", "সতর্কতা", "রিভার্সাল", "নিষেধ", "পুলব্যাক", "রিট্রেসমেন্ট", "Pullback", "Retracement", "পরামর্শ", "বাউন্স", "বাউন্সের", "সম্ভাবনা" -> {
                    withStyle(SpanStyle(color = Color(0xFFFBBF24), fontWeight = FontWeight.Black)) {
                        append(word)
                    }
                }
                "অস্থির", "টানাপোড়েন", "চপি" -> {
                    withStyle(SpanStyle(color = Color(0xFFF59E0B), fontWeight = FontWeight.Black)) {
                        append(word)
                    }
                }
                "স্থির", "নো-ট্রেড", "অপেক্ষা", "শান্ত", "গতিহীন" -> {
                    withStyle(SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)) {
                        append(word)
                    }
                }
                else -> {
                    withStyle(SpanStyle(color = defaultColor, fontWeight = FontWeight.Normal)) {
                        append(word)
                    }
                }
            }
            cursor = end
        }
        if (cursor < text.length) {
            withStyle(SpanStyle(color = defaultColor, fontWeight = FontWeight.Normal)) {
                append(text.substring(cursor))
            }
        }
    }
}

/**
 * Data structure representing a confirmed history point for duration calculation.
 */
internal data class PullbackHistoryPoint(
    val val5m: Double,
    val val60m: Double,
    val timestamp: Long,
    val isValid: Boolean,
    val isApproximate: Boolean,
    val behaviorCode: String = ""
)

internal fun com.example.data.models.MetricSnapshot.toHistoryPoint(): PullbackHistoryPoint = PullbackHistoryPoint(
    val5m = this.val5m,
    val60m = this.val60m,
    timestamp = this.timestamp,
    isValid = this.isValid,
    isApproximate = this.isApproximate
)

internal fun TradingAnalysis.toHistoryPoint(): PullbackHistoryPoint? {
    val v5 = this.change5mValue ?: return null
    val v60 = this.change60mValue ?: return null
    return PullbackHistoryPoint(
        val5m = v5,
        val60m = v60,
        timestamp = this.timestamp,
        isValid = this.isValid,
        isApproximate = this.isApproximate,
        behaviorCode = this.behaviorCode
    )
}

internal const val INSUFFICIENT_HISTORY_MSG = "Insufficient history to estimate time"
internal const val HEURISTIC_WINDOW_LABEL = "Approximate pullback window"

internal fun matchesPullbackPhase(
    pt: PullbackHistoryPoint,
    current5m: Double,
    current60m: Double,
    currentBehaviorCode: String?
): Boolean {
    val bCode = currentBehaviorCode ?: ""
    return when {
        bCode == "DOWNTREND_PULLBACK_UP" || (current60m < -0.10 && current5m > 0.0) -> {
            pt.val5m > 0.0 && (pt.val60m < -0.10 || pt.behaviorCode == "DOWNTREND_PULLBACK_UP")
        }
        bCode == "UPTREND_PULLBACK_DOWN" || (current60m > 0.10 && current5m < 0.0) -> {
            pt.val5m < 0.0 && (pt.val60m > 0.10 || pt.behaviorCode == "UPTREND_PULLBACK_DOWN")
        }
        bCode == "MOMENTUM_LOSS_DOWN" -> {
            pt.val5m < 0.0
        }
        bCode == "MOMENTUM_LOSS_UP" -> {
            pt.val5m > 0.0
        }
        bCode == "SUDDEN_SPIKE_UP" -> {
            pt.val5m > 0.0
        }
        bCode == "SUDDEN_SPIKE_DOWN" -> {
            pt.val5m < 0.0
        }
        current5m < 0.0 && current60m < 0.0 -> {
            pt.val5m < 0.0
        }
        current5m > 0.0 && current60m > 0.0 -> {
            pt.val5m > 0.0
        }
        else -> false
    }
}

internal fun evaluateCanHaveDuration(
    isValid: Boolean,
    isNoTrade: Boolean,
    isApprox: Boolean,
    hasValidMetrics: Boolean,
    historyPoints: List<PullbackHistoryPoint>,
    v5m: Double?,
    v60m: Double?,
    behaviorCode: String?
): Boolean {
    return if (isValid && !isNoTrade && !isApprox && hasValidMetrics && historyPoints.isNotEmpty()) {
        val current5 = v5m
        val current60 = v60m

        if (current5 != null && current60 != null) {
            historyPoints.any {
                matchesPullbackPhase(
                    it,
                    current5,
                    current60,
                    behaviorCode
                )
            }
        } else {
            false
        }
    } else {
        false
    }
}

internal fun calculateConfirmedHistoryDurationSec(
    current5m: Double?,
    current60m: Double?,
    currentBehaviorCode: String?,
    currentTimestamp: Long,
    history: List<PullbackHistoryPoint>
): Long? {
    if (current5m == null || current60m == null ||
        current5m.isNaN() || current60m.isNaN() ||
        current5m.isInfinite() || current60m.isInfinite() ||
        currentTimestamp <= 0
    ) {
        return null
    }

    val confirmed = history.filter { pt ->
        pt.isValid && !pt.isApproximate &&
            !pt.val5m.isNaN() && !pt.val5m.isInfinite() &&
            !pt.val60m.isNaN() && !pt.val60m.isInfinite() &&
            pt.timestamp > 0
    }

    if (confirmed.isEmpty()) return null

    val sortedConfirmed = confirmed.sortedByDescending { it.timestamp }
    val matchingRun = sortedConfirmed.takeWhile {
        matchesPullbackPhase(it, current5m, current60m, currentBehaviorCode)
    }

    if (matchingRun.isEmpty()) return null

    val earliest = matchingRun.last()
    val elapsedMs = currentTimestamp - earliest.timestamp
    if (elapsedMs < 1000L) return null

    return elapsedMs / 1000L
}

/**
 * Calculates pullback duration label.
 *
 * Requirements:
 * 1. Derives duration exclusively from actual timestamped confirmed history.
 * 2. Does NOT use max(abs(v5m), abs(v60m)) as a direct proxy for seconds.
 * 3. Never defaults missing, null, NaN, or infinite values to ~15s.
 * 4. If timestamped history is insufficient, returns "সময় অনুমান করার মতো পর্যাপ্ত history নেই".
 */
internal fun calculatePullbackDurationLabel(
    v5m: Double?,
    v60m: Double?,
    history: List<com.example.data.models.MetricSnapshot> = emptyList(),
    currentTimestamp: Long = System.currentTimeMillis(),
    behaviorCode: String? = null
): String {
    if (v5m == null || v60m == null || v5m.isNaN() || v60m.isNaN() || v5m.isInfinite() || v60m.isInfinite()) {
        return INSUFFICIENT_HISTORY_MSG
    }
    val points = history.map { it.toHistoryPoint() }
    val durationSec = calculateConfirmedHistoryDurationSec(
        current5m = v5m,
        current60m = v60m,
        currentBehaviorCode = behaviorCode,
        currentTimestamp = currentTimestamp,
        history = points
    )
    return if (durationSec != null && durationSec > 0) {
        "${durationSec}s"
    } else {
        INSUFFICIENT_HISTORY_MSG
    }
}

@JvmName("calculatePullbackDurationLabelFromAnalysis")
internal fun calculatePullbackDurationLabel(
    v5m: Double?,
    v60m: Double?,
    history: List<TradingAnalysis>,
    currentTimestamp: Long = System.currentTimeMillis(),
    behaviorCode: String? = null
): String {
    if (v5m == null || v60m == null || v5m.isNaN() || v60m.isNaN() || v5m.isInfinite() || v60m.isInfinite()) {
        return INSUFFICIENT_HISTORY_MSG
    }
    val points = history.mapNotNull { it.toHistoryPoint() }
    val durationSec = calculateConfirmedHistoryDurationSec(
        current5m = v5m,
        current60m = v60m,
        currentBehaviorCode = behaviorCode,
        currentTimestamp = currentTimestamp,
        history = points
    )
    return if (durationSec != null && durationSec > 0) {
        "${durationSec}s"
    } else {
        INSUFFICIENT_HISTORY_MSG
    }
}

/**
 * If a heuristic estimate is requested without history, explicitly labeled as
 * "আনুমানিক pullback window" and NOT presented as actual speed or exact seconds.
 */
internal fun calculateHeuristicPullbackWindowLabel(v5m: Double?, v60m: Double?): String {
    if (v5m == null || v60m == null || v5m.isNaN() || v60m.isNaN() || v5m.isInfinite() || v60m.isInfinite()) {
        return INSUFFICIENT_HISTORY_MSG
    }
    return HEURISTIC_WINDOW_LABEL
}

/**
 * Comprehensive dashboard text translator ensuring that all dynamic text,
 * forensic diagnosis, matrix descriptions, and action card notes are 100% in English.
 */
fun translateDashboardText(input: String): String {
    if (input.isBlank()) return input
    var result = input
    val translations = listOf(
        "স্বাভাবিক গতিশীল ভারসাম্য (Dynamic Equilibrium Flow)" to "Dynamic Equilibrium Flow",
        "স্বাভাবিক গতিশীল ভারসাম্য" to "Dynamic Equilibrium Flow",
        "কাইনেটিক ডাটা অপূর্ণ" to "Kinetic Data Incomplete",
        "মাল্টি-টাইমফ্রেম গতিবেগ ও ত্বরণ ভেক্টরের লাইভ বিশ্লেষণ চলমান।" to "Live multi-timeframe velocity and acceleration vector analysis active.",
        "ক্যামেরা ফ্রেমের স্বচ্ছতা নিশ্চিত করুন।" to "Ensure camera frame clarity.",
        "স্থিতিশীল গতিতে উপরে ওঠার ট্রেন্ড" to "Steady Uptrend Continuation",
        "স্থিতিশীল গতিতে নিচে নামার ট্রেন্ড" to "Steady Downtrend Continuation",
        "স্থিতিশীল UP ট্রেন্ড — ঊর্ধ্বমুখী গতি" to "Steady UP Trend — Upward Momentum",
        "স্থিতিশীল DOWN ট্রেন্ড — নিম্নমুখী পতন" to "Steady DOWN Trend — Downward Momentum",
        "স্থিতিশীল UP ট্রেন্ড" to "Steady UP Trend",
        "স্থিতিশীল DOWN ট্রেন্ড" to "Steady DOWN Trend",
        "শক্তিশালী UP মোমেন্টাম ব্রেক — তীব্র চাপ" to "Strong UP Momentum Break — High Pressure",
        "শক্তিশালী DOWN মোমেন্টাম ব্রেক — তীব্র পতন" to "Strong DOWN Momentum Break — Sharp Drop",
        "শক্তিশালী UP মোমেন্টাম ব্রেক" to "Strong UP Momentum Break",
        "শক্তিশালী DOWN মোমেন্টাম ব্রেক" to "Strong DOWN Momentum Break",
        "শক্তিশালী UP মোমেন্টাম" to "Strong UP Momentum",
        "শক্তিশালী DOWN মোমেন্টাম" to "Strong DOWN Momentum",
        "হঠাৎ উপরের ধাক্কা — দ্রুত অস্থিরতার ঝুঁকি" to "Sudden Upward Spike — High Volatility Risk",
        "হঠাৎ নিচের ধাক্কা — দ্রুত পতনের ঝুঁকি" to "Sudden Downward Drop — Sharp Plunge Risk",
        "হঠাৎ UP ধাক্কা" to "Sudden UP Surge",
        "হঠাৎ DOWN ধাক্কা" to "Sudden DOWN Surge",
        "হঠাৎ স্পাইক" to "Sudden Spike",
        "সামান্য উপরে গিয়ে দ্রুত পতনের ঝুঁকি (Bull Trap)" to "Bull Trap — Risk of Sharp Plunge",
        "সামান্য নিচে নেমে দ্রুত বাউন্সের সম্ভাবনা (Bear Trap)" to "Bear Trap — Potential Sharp Bounce",
        "সামান্য উপরে গিয়ে" to "Slight Upside before Drop",
        "সামান্য নিচে নেমে" to "Slight Downside before Bounce",
        "সামান্য UP গিয়ে" to "Slight UP Move",
        "সামান্য DOWN নেমে" to "Slight DOWN Move",
        "বুল ট্র্যাপ -> তীব্র পতন ঝুঁকি" to "Bull Trap -> Sharp Drop Risk",
        "বিয়ার ট্র্যাপ -> তীব্র বাউন্স" to "Bear Trap -> Sharp Bounce Potential",
        "বুল ট্র্যাপ" to "Bull Trap",
        "বিয়ার ট্র্যাপ" to "Bear Trap",
        "বুলিশ" to "Bullish",
        "বিয়ারিশ" to "Bearish",
        "নিশ্চিত ঊর্ধ্বমুখী রিভার্সাল — ট্রেন্ড বদল" to "Confirmed Bullish Reversal — Trend Change",
        "নিশ্চিত নিম্নমুখী রিভার্সাল — ট্রেন্ড বদল" to "Confirmed Bearish Reversal — Trend Change",
        "নিশ্চিত ঊর্ধ্বমুখী রিভার্সাল" to "Confirmed Bullish Reversal",
        "নিশ্চিত নিম্নমুখী রিভার্সাল" to "Confirmed Bearish Reversal",
        "নিশ্চিত UP রিভার্সাল" to "Confirmed UP Reversal",
        "নিশ্চিত DOWN রিভার্সাল" to "Confirmed DOWN Reversal",
        "ঊর্ধ্বমুখী রিভার্সাল প্রচেষ্টা — যাচাই চলছে" to "Bullish Reversal Attempt — Verifying",
        "নিম্নমুখী রিভার্সাল প্রচেষ্টা — যাচাই চলছে" to "Bearish Reversal Attempt — Verifying",
        "ঊর্ধ্বমুখী রিভার্সাল" to "Bullish Reversal",
        "নিম্নমুখী রিভার্সাল" to "Bearish Reversal",
        "দুর্বল রিভার্সাল ডাউন" to "Weak Reversal Down",
        "দুর্বল রিভার্সাল আপ" to "Weak Reversal Up",
        "দুর্বল রিভার্সাল" to "Weak Reversal",
        "রিভার্সাল ডাউন" to "Reversal Down",
        "রিভার্সাল আপ" to "Reversal Up",
        "রিভার্সাল" to "Reversal",
        "উপরে ওঠার গতি কমছে — সাময়িক বিরতি বা পতনের সম্ভাবনা" to "Momentum Decelerating Upward — Temporary Pause or Drop Potential",
        "নিচে নামার গতি কমছে — সাময়িক বিরতি বা বাউন্সের সম্ভাবনা" to "Momentum Decelerating Downward — Temporary Pause or Bounce Potential",
        "উপরে ওঠার গতি কমছে" to "Upward Momentum Decelerating",
        "নিচে নামার গতি কমছে" to "Downward Momentum Decelerating",
        "UP যাওয়ার গতি কমছে" to "UP Momentum Decelerating",
        "DOWN নামার গতি কমছে" to "DOWN Momentum Decelerating",
        "মোমেন্টাম পতন" to "Momentum Drop",
        "মোমেন্টাম বৃদ্ধি" to "Momentum Expansion",
        "মোমেন্টাম লস" to "Momentum Loss",
        "মোমেন্টাম শেষ" to "Momentum Exhaustion",
        "মোমেন্টাম" to "Momentum",
        "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন" to "Bearish Pullback — Upward Retracement",
        "আপট্রেন্ডে নিম্নমুখী পুলব্যাক — সংশোধন" to "Bullish Pullback — Downward Retracement",
        "ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক" to "Bearish Pullback",
        "আপট্রেন্ডে নিম্নমুখী পুলব্যাক" to "Bullish Pullback",
        "পুলব্যাক শেষ" to "Pullback Complete",
        "সাময়িক UP পুলব্যাক" to "Temporary UP Pullback",
        "সাময়িক DOWN পুলব্যাক" to "Temporary DOWN Pullback",
        "দুর্বল UP বাউন্স" to "Weak UP Bounce",
        "দুর্বল DOWN পুলব্যাক" to "Weak DOWN Pullback",
        "পুলব্যাক" to "Pullback",
        "ব্রেকআউট" to "Breakout",
        "ব্রেকডাউন" to "Breakdown",
        "ফেকআউট" to "Fakeout",
        "স্কুইজ ব্রেক" to "Squeeze Break",
        "স্কুইজ" to "Squeeze",
        "রেঞ্জ বাউন্ড" to "Range Bound",
        "অস্থির ওঠানামা" to "Choppy Volatility",
        "নিউট্রাল ফ্লো" to "Neutral Flow",
        "নো-ট্রেড" to "No-Trade",
        "ডেড জোন" to "Dead Zone",
        "ডেড মার্কেট" to "Dead Market",
        "চাপ ও গতিবেগ ভারসাম্যপূর্ণ" to "Pressure & Velocity Balanced",
        "বায়ার্সদের চাপ" to "Buyer Pressure",
        "বায়ারদের চাপ" to "Buyer Pressure",
        "বায়ার্সদের" to "Buyers",
        "বায়ারদের" to "Buyers",
        "সেলার্সদের চাপ" to "Seller Pressure",
        "সেলারদের চাপ" to "Seller Pressure",
        "সেলার্সদের" to "Sellers",
        "সেলারদের" to "Sellers",
        "বাউন্স সতর্কতা" to "Bounce Warning",
        "বাউন্সের" to "Bounce",
        "বাউন্স" to "Bounce",
        "তীব্র পতন" to "Sharp Plunge",
        "পতন শেষ" to "Drop Terminating",
        "পতন" to "Drop",
        "উত্থান" to "Rally",
        "বৃদ্ধি শেষ" to "Rally Terminating",
        "বৃদ্ধি" to "Rally",
        "অনির্ধারিত" to "Undetermined",
        "নিরপেক্ষ" to "Neutral",
        "প্রফিট" to "Profit",
        "লস" to "Loss",
        "শঙ্কা" to "Risk",
        "সতর্কতা" to "Warning",
        "সম্ভাবনা" to "Potential",
        "পরামর্শ" to "Guidance",
        "শান্ত" to "Calm",
        "স্থির" to "Stable",
        "মাঝারি" to "Medium",
        "শক্তিশালী" to "Strong",
        "মিশ্র/দুর্বল" to "Mixed/Weak",
        "অস্পষ্ট" to "Ambiguous",
        "কোন উত্তর এখনো পাওয়া যায়নি।" to "No response received yet.",
        "কোন উত্তর এখনো পাওয়া যায়নি (No response yet)." to "No response received yet."
    )
    for ((b, e) in translations) {
        if (result.contains(b)) {
            result = result.replace(b, e)
        }
    }
    return result
}

/**
 * Formats action card title and subtitle:
 * 1. 100% Pure English formatting for title and subtitle.
 * 2. Incorporates momentum exhaustion & pullback warnings in the subtitle for safe short-time trading entries.
 * 3. Incorporates measured duration only when confirmed by timestamped history, avoiding fabricated seconds.
 */
internal fun formatActionCardContent(
    rawTitle: String,
    rawSubtitle: String,
    analysis: TradingAnalysis?,
    isValid: Boolean,
    isNoTrade: Boolean,
    isApprox: Boolean,
    v5m: Double?,
    v60m: Double?,
    history: List<TradingAnalysis> = emptyList(),
    metricHistory: List<com.example.data.models.MetricSnapshot> = emptyList(),
    wallClockTimestamp: Long = analysis?.timestamp ?: System.currentTimeMillis()
): Pair<String, String> {
    val hasValidMetrics = v5m != null && v60m != null &&
        !v5m.isNaN() && !v60m.isNaN() &&
        !v5m.isInfinite() && !v60m.isInfinite()

    val historyPoints = when {
        history.isNotEmpty() -> history.mapNotNull { it.toHistoryPoint() }
        metricHistory.isNotEmpty() -> metricHistory.map { it.toHistoryPoint() }
        else -> emptyList()
    }
    val bCode = analysis?.behaviorCode ?: ""
    val currentTs = wallClockTimestamp

    val measuredDurationSec = if (isValid && !isNoTrade && !isApprox && hasValidMetrics) {
        calculateConfirmedHistoryDurationSec(
            current5m = v5m,
            current60m = v60m,
            currentBehaviorCode = bCode,
            currentTimestamp = currentTs,
            history = historyPoints
        )
    } else null

    val durationLabel = if (measuredDurationSec != null && measuredDurationSec > 0) {
        "${measuredDurationSec}s"
    } else null

    var formattedTitle = rawTitle
        .replace("ডাউনট্রেন্ডে ঊর্ধ্বমুখী পুলব্যাক — সংশোধন", "DOWN ট্রেন্ডে সাময়িক UP পুলব্যাক")
        .replace("আপট্রেন্ডে নিম্নমুখী পুলব্যাক — সংশোধন", "UP ট্রেন্ডে সাময়িক DOWN পুলব্যাক")
        .replace("নিশ্চিত ঊর্ধ্বমুখী রিভার্সাল — ট্রেন্ড বদল", "নিশ্চিত UP রিভার্সাল")
        .replace("নিশ্চিত নিম্নমুখী রিভার্সাল — ট্রেন্ড বদল", "নিশ্চিত DOWN রিভার্সাল")
        .replace("উপরে ওঠার গতি কমছে — সাময়িক বিরতি বা পতনের সম্ভাবনা", "UP যাওয়ার গতি কমছে")
        .replace("নিচে নামার গতি কমছে — সাময়িক বিরতি বা বাউন্সের সম্ভাবনা", "DOWN নামার গতি কমছে")
        .replace("সামান্য উপরে গিয়ে দ্রুত পতনের ঝুঁকি (Bull Trap)", "UP ট্র্যাপ / Bull Trap")
        .replace("সামান্য নিচে নেমে দ্রুত বাউন্সের সম্ভাবনা (Bear Trap)", "DOWN ট্র্যাপ / Bear Trap")
        .replace("সামান্য উপরে গিয়ে দ্রুত পতনের ঝুঁকি", "UP ট্র্যাপ / Bull Trap")
        .replace("সামান্য নিচে নেমে দ্রুত বাউন্সের সম্ভাবনা", "DOWN ট্র্যাপ / Bear Trap")

    // Priority ordering for topAlertTag:
    // 1. Invalid/no-trade/approximate protection
    if (isValid && !isNoTrade && !isApprox && hasValidMetrics) {
        val topAlertTag = when {
            // 2. Fakeout and trap-specific text
            bCode == "TOP_FAKEOUT_RISK" || formattedTitle.contains("Bull Trap") || formattedTitle.contains("UP ট্র্যাপ") -> {
                " • [হঠাৎ তীব্র DOWN পতনের ঝুঁকি]"
            }
            bCode == "BOTTOM_FAKEOUT_RISK" || formattedTitle.contains("Bear Trap") || formattedTitle.contains("DOWN ট্র্যাপ") -> {
                " • [হঠাৎ তীব্র UP বাউন্সের ঝুঁকি]"
            }
            // 3. Confirmed reversal-specific text
            bCode == "CONFIRMED_BULLISH_REVERSAL" || formattedTitle.contains("নিশ্চিত UP রিভার্সাল") -> {
                if (v5m != null && v60m != null && Math.abs(v5m) < Math.abs(v60m)) {
                    " • [BUY সিগন্যাল • সাময়িক DOWN পুলব্যাক ঝুঁকি]"
                } else {
                    " • [BUY সিগন্যাল]"
                }
            }
            bCode == "CONFIRMED_BEARISH_REVERSAL" || formattedTitle.contains("নিশ্চিত DOWN রিভার্সাল") -> {
                if (v5m != null && v60m != null && Math.abs(v5m) < Math.abs(v60m)) {
                    " • [SELL সিগন্যাল • সাময়িক UP বাউন্স ঝুঁকি]"
                } else {
                    " • [SELL সিগন্যাল]"
                }
            }
            // 4. Momentum loss up/down-specific text
            bCode == "MOMENTUM_LOSS_DOWN" || formattedTitle.contains("DOWN নামার গতি কমছে") -> {
                if (durationLabel != null) " • [$durationLabel UP বাউন্স সম্ভাবনা]" else " • [UP বাউন্স সম্ভাবনা]"
            }
            bCode == "MOMENTUM_LOSS_UP" || formattedTitle.contains("UP যাওয়ার গতি কমছে") -> {
                if (durationLabel != null) " • [$durationLabel DOWN রিট্রেসমেন্ট সম্ভাবনা]" else " • [DOWN রিট্রেসমেন্ট সম্ভাবনা]"
            }
            // 5. Pullback-specific text
            bCode == "DOWNTREND_PULLBACK_UP" || formattedTitle.contains("DOWN ট্রেন্ডে সাময়িক UP পুলব্যাক") -> {
                if (durationLabel != null) " • [${durationLabel} পুনরায় DOWN পতন শঙ্কা]" else " • [পুনরায় DOWN পতন শঙ্কা]"
            }
            bCode == "UPTREND_PULLBACK_DOWN" || formattedTitle.contains("UP ট্রেন্ডে সাময়িক DOWN পুলব্যাক") -> {
                if (durationLabel != null) " • [${durationLabel} পুনরায় UP উত্থান শঙ্কা]" else " • [পুনরায় UP উত্থান শঙ্কা]"
            }
            // 6. Sudden spike-specific text
            bCode == "SUDDEN_SPIKE_UP" || formattedTitle.contains("হঠাৎ তীব্র বৃদ্ধি") -> {
                if (durationLabel != null) " • [$durationLabel DOWN পুলব্যাক সতর্কতা]" else " • [DOWN পুলব্যাক সতর্কতা]"
            }
            bCode == "SUDDEN_SPIKE_DOWN" || formattedTitle.contains("হঠাৎ তীব্র পতন") -> {
                if (durationLabel != null) " • [$durationLabel UP বাউন্স সতর্কতা]" else " • [UP বাউন্স সতর্কতা]"
            }
            // 7. Generic aligned up/down trend text
            bCode == "DOWNWARD_MOMENTUM_BREAK" || bCode == "STEADY_ALIGNED_DOWN" ||
                formattedTitle.contains("স্থির ডাউনট্রেন্ড") || formattedTitle.contains("শক্তিশালী ডাউনওয়ার্ড মোমেন্টাম") ||
                (v5m < 0 && v60m < 0) -> {
                if (durationLabel != null) " • [$durationLabel UP বাউন্স সতর্কতা]" else " • [UP বাউন্স সতর্কতা]"
            }
            bCode == "UPWARD_MOMENTUM_BREAK" || bCode == "STEADY_ALIGNED_UP" ||
                formattedTitle.contains("স্থির আপট্রেন্ড") || formattedTitle.contains("শক্তিশালী আপওয়ার্ড মোমেন্টাম") ||
                (v5m > 0 && v60m > 0) -> {
                if (durationLabel != null) " • [$durationLabel DOWN পুলব্যাক সতর্কতা]" else " • [DOWN পুলব্যাক সতর্কতা]"
            }
            else -> ""
        }

        if (topAlertTag.isNotEmpty() && !formattedTitle.contains(" • [") && !formattedTitle.contains("সিগন্যাল]")) {
            formattedTitle += topAlertTag
        }
    }

    var formattedSubtitle = rawSubtitle
        .replace("পরবর্তী সম্ভাব্য দিক: ঊর্ধ্বমুখী", "পরবর্তী সম্ভাব্য দিক: UP")
        .replace("পরবর্তী সম্ভাব্য দিক: নিম্নমুখী", "পরবর্তী সম্ভাব্য দিক: DOWN")
        .replace("দৈনিক ট্রেন্ড: ঊর্ধ্বমুখী", "দৈনিক ট্রেন্ড: UP")
        .replace("দৈনিক ট্রেন্ড: নিম্নমুখী", "দৈনিক ট্রেন্ড: DOWN")

    // Priority ordering for pullbackNote:
    // 1. Invalid/no-trade/approximate protection
    if (isValid && !isNoTrade && !isApprox && hasValidMetrics) {
        val bCode = analysis?.behaviorCode ?: ""
        val pullbackNote = when {
            // 2. Fakeout and trap-specific text (preserve existing text without appending generic trend warning)
            bCode == "TOP_FAKEOUT_RISK" || bCode == "BOTTOM_FAKEOUT_RISK" ||
                formattedTitle.contains("Bull Trap") || formattedTitle.contains("Bear Trap") ||
                formattedTitle.contains("UP ট্র্যাপ") || formattedTitle.contains("DOWN ট্র্যাপ") -> ""

            // 3. Confirmed reversal-specific text (alert if momentum decelerates)
            bCode == "CONFIRMED_BULLISH_REVERSAL" || formattedTitle.contains("নিশ্চিত UP রিভার্সাল") -> {
                if (v5m != null && v60m != null && Math.abs(v5m) < Math.abs(v60m)) {
                    val durClause = if (durationLabel != null) "($durationLabel চলমান) " else ""
                    " • ⚠️ মোমেন্টাম সতর্কতা: UP যাওয়ার গতি কমছে; UP চাপ কমে যেকোনো সময় সাময়িক DOWN পুলব্যাক বা রিভার্সাল ঝুঁকি রয়েছে।"
                } else ""
            }
            bCode == "CONFIRMED_BEARISH_REVERSAL" || formattedTitle.contains("নিশ্চিত DOWN রিভার্সাল") -> {
                if (v5m != null && v60m != null && Math.abs(v5m) < Math.abs(v60m)) {
                    val durClause = if (durationLabel != null) "($durationLabel চলমান) " else ""
                    " • ⚠️ বাউন্স সতর্কতা: DOWN নামার গতি কমছে; DOWN চাপ কমে গাড়ির ব্রেকের মতো সাময়িক UP বাউন্স বা রিভার্সাল ঝুঁকি রয়েছে।"
                } else ""
            }

            // 4. Momentum loss up/down-specific text
            bCode == "MOMENTUM_LOSS_DOWN" || formattedTitle.contains("DOWN নামার গতি কমছে") -> {
                val durClause = if (durationLabel != null) "($durationLabel চলমান) " else ""
                " • ⚠️ বাউন্স সতর্কতা: DOWN নামার গতি কমছে; DOWN চাপ কমে গাড়ির ব্রেকের মতো যেকোনো মুহূর্তে সাময়িক UP বাউন্স বা রিভার্সাল ঝুঁকি রয়েছে${if (durClause.isNotEmpty()) " $durClause" else "।"}"
            }
            bCode == "MOMENTUM_LOSS_UP" || formattedTitle.contains("UP যাওয়ার গতি কমছে") -> {
                val durClause = if (durationLabel != null) "($durationLabel চলমান) " else ""
                " • ⚠️ পুলব্যাক সতর্কতা: UP যাওয়ার গতি কমছে; UP চাপ কমে যেকোনো সময় সাময়িক DOWN পুলব্যাক বা রিভার্সাল ঝুঁকি রয়েছে${if (durClause.isNotEmpty()) " $durClause" else "।"}"
            }

            // 5. Pullback-specific text
            bCode == "DOWNTREND_PULLBACK_UP" || formattedTitle.contains("DOWN ট্রেন্ডে সাময়িক UP পুলব্যাক") -> {
                val durClause = if (durationLabel != null) " ($durationLabel চলমান)" else ""
                " • 💡 পুলব্যাক পরামর্শ: এটি মূল পতনের মাঝে সাময়িক UP বাউন্স$durClause; বাউন্স শেষ হওয়া পর্যন্ত অপেক্ষা করে পুনরায় ডাউন এন্ট্রি নিন।"
            }
            bCode == "UPTREND_PULLBACK_DOWN" || formattedTitle.contains("UP ট্রেন্ডে সাময়িক DOWN পুলব্যাক") -> {
                val durClause = if (durationLabel != null) " ($durationLabel চলমান)" else ""
                " • 💡 পুলব্যাক পরামর্শ: এটি মূল ঊর্ধ্বগতির মাঝে সাময়িক DOWN সংশোধন$durClause; সংশোধন শেষ হওয়া পর্যন্ত অপেক্ষা করে পুনরায় আপ এন্ট্রি নিন।"
            }

            // 6. Sudden spike-specific text
            bCode == "SUDDEN_SPIKE_UP" || formattedTitle.contains("হঠাৎ তীব্র বৃদ্ধি") -> {
                val durClause = if (durationLabel != null) "($durationLabel চলমান) " else ""
                " • ⚠️ পুলব্যাক সতর্কতা: তীব্র বৃদ্ধির কারণে যেকোনো মুহূর্তে গাড়ির ব্রেকের মতো${if (durClause.isNotEmpty()) " $durClause" else " "}সাময়িক DOWN পুলব্যাক হতে পারে; চূড়ায় তাড়াহুড়ো না করে সংশোধনের জন্য অপেক্ষা করুন।"
            }
            bCode == "SUDDEN_SPIKE_DOWN" || formattedTitle.contains("হঠাৎ তীব্র পতন") -> {
                val durClause = if (durationLabel != null) "($durationLabel চলমান) " else ""
                " • ⚠️ বাউন্স সতর্কতা: তীব্র পতনের কারণে যেকোনো মুহূর্তে গাড়ির ব্রেকের মতো${if (durClause.isNotEmpty()) " $durClause" else " "}সাময়িক UP বাউন্স হতে পারে; তলানিতে তাড়াহুড়ো না করে সংশোধনের জন্য অপেক্ষা করুন।"
            }

            // 7. Generic aligned up/down trend text
            bCode == "DOWNWARD_MOMENTUM_BREAK" || bCode == "STEADY_ALIGNED_DOWN" || (v5m < 0 && v60m < 0) -> {
                val durClause = if (durationLabel != null) "($durationLabel চলমান) " else ""
                " • ⚠️ পুলব্যাক সতর্কতা: তীব্র পতনের কারণে যেকোনো মুহূর্তে গাড়ির ব্রেকের মতো${if (durClause.isNotEmpty()) " $durClause" else " "}সাময়িক UP বাউন্স হতে পারে; তলানিতে তাড়াহুড়ো না করে সংশোধনের জন্য অপেক্ষা করুন।"
            }
            bCode == "UPWARD_MOMENTUM_BREAK" || bCode == "STEADY_ALIGNED_UP" || (v5m > 0 && v60m > 0) -> {
                val durClause = if (durationLabel != null) "($durationLabel চলমান) " else ""
                " • ⚠️ পুলব্যাক সতর্কতা: তীব্র বৃদ্ধির কারণে যেকোনো মুহূর্তে গাড়ির ব্রেকের মতো${if (durClause.isNotEmpty()) " $durClause" else " "}সাময়িক DOWN পুলব্যাক হতে পারে; চূড়ায় তাড়াহুড়ো না করে সংশোধনের জন্য অপেক্ষা করুন।"
            }
            else -> ""
        }
        if (pullbackNote.isNotEmpty() && !formattedSubtitle.contains("পুলব্যাক পরামর্শ") && !formattedSubtitle.contains("বাউন্স সতর্কতা") && !formattedSubtitle.contains("মোমেন্টাম সতর্কতা") && !formattedSubtitle.contains("পুলব্যাক সতর্কতা:")) {
            formattedSubtitle += pullbackNote
        }
    }

    return Pair(formattedTitle, formattedSubtitle)
}

@Composable
fun HighDensityPredictionCard(
    analysis: TradingAnalysis?,
    isProcessing: Boolean,
    onSetTradeOutcome: (TradeOutcome) -> Unit = {},
    history: List<TradingAnalysis> = emptyList()
) {
    val isValid = analysis?.isValid == true
    val isApprox = analysis?.isApproximate == true
    val direction = if (isValid) (analysis?.direction ?: TradeDirection.NEUTRAL) else TradeDirection.NEUTRAL
    val calculatedPct = if (isValid) (analysis?.calculatedPercentage ?: 50.0) else 50.0
    val netSumStr = if (isValid) (analysis?.netSum ?: "--") else "--"
    val netSumVal = if (isValid) (analysis?.netSumValue ?: 0.0) else 0.0
    val strength = if (isValid) (analysis?.strengthLevel ?: StrengthLevel.NORMAL) else StrengthLevel.NORMAL

    val dirColor = when (direction) {
        TradeDirection.UP -> NeonGreen
        TradeDirection.DOWN -> NeonRed
        TradeDirection.NEUTRAL -> AccentCyan
    }

    val dirLightColor = when (direction) {
        TradeDirection.UP -> NeonGreenLight
        TradeDirection.DOWN -> NeonRedLight
        TradeDirection.NEUTRAL -> AccentCyan
    }

    val strengthColor = when (strength) {
        StrengthLevel.HIGH -> NeonRedLight
        StrengthLevel.MEDIUM -> AccentAmber
        StrengthLevel.NORMAL -> NeonGreenLight
    }

    val strengthBg = when (strength) {
        StrengthLevel.HIGH -> NeonRedDim
        StrengthLevel.MEDIUM -> Color(0x1FF59E0B)
        StrengthLevel.NORMAL -> NeonGreenDim
    }

    // Instant Action Signal & Trap Trigger Logic (Zero Hesitation Direct Action)
    val v5m = if (isValid) analysis?.change5mValue else null
    val v60m = if (isValid) analysis?.change60mValue else null
    val isDeadMarket = isValid && (analysis?.isDeadMarket == true || (v5m != null && v60m != null && abs(v5m) <= 0.10 && abs(v60m) <= 0.10 && direction == TradeDirection.NEUTRAL))
    val isAligned = if (v5m != null && v60m != null) {
        (v5m > 0 && v60m > 0) || (v5m < 0 && v60m < 0)
    } else false

    val isNoTrade = isDeadMarket || analysis?.isNoTradeZone == true
    val signalType = analysis?.signalType ?: com.example.data.models.SignalType.NONE

    val cardText = com.example.data.analyzer.TradingOutputParser.resolveActionCardTexts(
        direction = direction,
        signalType = signalType,
        isNoTrade = isNoTrade,
        isApproximate = isApprox,
        isValid = isValid,
        errorMessage = analysis?.errorMessage,
        strength = strength,
        isAligned = isAligned,
        v5m = v5m,
        v60m = v60m,
        behaviorTitle = analysis?.behaviorTitle?.let { translateDashboardText(it) },
        behaviorDescription = analysis?.behaviorDescription?.let { translateDashboardText(it) },
        behaviorWarningOnly = analysis?.isWarningOnly
    )

    // Lightweight live ticker: active only while a valid confirmed duration phase is ongoing
    val hasValidMetrics = v5m != null && v60m != null &&
        !v5m.isNaN() && !v60m.isNaN() &&
        !v5m.isInfinite() && !v60m.isInfinite()

    val historyPoints = remember(history) {
        history.mapNotNull { it.toHistoryPoint() }
    }

    var liveWallClockTime by remember { mutableStateOf(System.currentTimeMillis()) }

    val canHaveDuration =
        if (isValid && !isNoTrade && !isApprox && hasValidMetrics && historyPoints.isNotEmpty()) {
            val current5 = v5m
            val current60 = v60m

            if (current5 != null && current60 != null) {
                historyPoints.any {
                    matchesPullbackPhase(
                        it,
                        current5,
                        current60,
                        analysis?.behaviorCode
                    )
                }
            } else {
                false
            }
        } else {
            false
        }

    LaunchedEffect(canHaveDuration, analysis?.behaviorCode, historyPoints.size, historyPoints.firstOrNull()?.timestamp) {
        if (!canHaveDuration) return@LaunchedEffect
        liveWallClockTime = System.currentTimeMillis()
        while (isActive) {
            delay(1000L)
            liveWallClockTime = System.currentTimeMillis()
        }
    }

    val (actionTitle, actionSubtitle) = formatActionCardContent(
        rawTitle = cardText.title,
        rawSubtitle = cardText.subtitle,
        analysis = analysis,
        isValid = isValid,
        isNoTrade = isNoTrade,
        isApprox = isApprox,
        v5m = v5m,
        v60m = v60m,
        history = history,
        wallClockTimestamp = liveWallClockTime
    )
    val actionBg: Color
    val actionBorder: Color
    val actionTextColor: Color

    val isWeakPullbackCase = (analysis?.behaviorCode == "UPTREND_PULLBACK_DOWN" && direction == TradeDirection.UP) ||
        (analysis?.behaviorCode == "DOWNTREND_PULLBACK_UP" && direction == TradeDirection.DOWN)

    when {
        !isValid || isApprox -> {
            actionBg = Color(0x18F59E0B)
            actionBorder = AccentAmber.copy(alpha = 0.5f)
            actionTextColor = AccentAmber
        }
        isNoTrade -> {
            actionBg = Color(0x30EF4444)
            actionBorder = Color(0xFFEF4444)
            actionTextColor = Color(0xFFFCA5A5)
        }
        cardText.isWarningOnly && !isWeakPullbackCase -> {
            actionBg = Color(0x18F59E0B)
            actionBorder = AccentAmber.copy(alpha = 0.5f)
            actionTextColor = AccentAmber
        }
        direction == TradeDirection.UP -> {
            val isHigh = strength == StrengthLevel.HIGH
            actionBg = if (isHigh) NeonGreenDim else NeonGreenDim.copy(alpha = 0.5f)
            actionBorder = if (isHigh) NeonGreen else NeonGreen.copy(alpha = 0.4f)
            actionTextColor = NeonGreenLight
        }
        direction == TradeDirection.DOWN -> {
            val isHigh = strength == StrengthLevel.HIGH
            actionBg = if (isHigh) NeonRedDim else NeonRedDim.copy(alpha = 0.5f)
            actionBorder = if (isHigh) NeonRed else NeonRed.copy(alpha = 0.4f)
            actionTextColor = NeonRedLight
        }
        else -> {
            actionBg = Color(0x18F59E0B)
            actionBorder = AccentAmber.copy(alpha = 0.35f)
            actionTextColor = AccentAmber
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("result_signal_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, BorderStroke)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val isProfitSelected = analysis?.outcome == TradeOutcome.PROFIT
            val isLossSelected = analysis?.outcome == TradeOutcome.LOSS
            val activeMatrixId = analysis?.primaryMatrixId ?: analysis?.canonicalDecision?.primaryMatrixId
            if (activeMatrixId != null && activeMatrixId.isNotBlank() && activeMatrixId != "NONE") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Signal Matrix:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = DarkBackground,
                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.6f))
                        ) {
                            Text(
                                text = activeMatrixId,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = if (isProfitSelected) "✓ Profit Added" else if (isLossSelected) "✗ Loss Added" else "Set Outcome",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isProfitSelected) NeonGreenLight else if (isLossSelected) NeonRedLight else TextMuted
                    )
                }
            }

            // Bottom Row: User-Requested Big Bold Profit and Loss Action Buttons on Two Sides
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Profit Button (Left Side - Big & Bold)
                Surface(
                    onClick = { onSetTradeOutcome(TradeOutcome.PROFIT) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isProfitSelected) NeonGreen else Color(0x2416A34A),
                    border = BorderStroke(
                        1.5.dp,
                        if (isProfitSelected) Color(0xFF86EFAC) else NeonGreen.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_mark_profit")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Profit",
                            tint = if (isProfitSelected) TextDark else NeonGreenLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Profit",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isProfitSelected) TextDark else NeonGreenLight,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // 2. Loss Button (Right Side - Big & Bold)
                Surface(
                    onClick = { onSetTradeOutcome(TradeOutcome.LOSS) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isLossSelected) NeonRed else Color(0x24DC2626),
                    border = BorderStroke(
                        1.5.dp,
                        if (isLossSelected) Color(0xFFFCA5A5) else NeonRed.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("btn_mark_loss")
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Loss",
                            tint = if (isLossSelected) Color.White else NeonRedLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Loss",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = if (isLossSelected) Color.White else NeonRedLight,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // User-requested Dua Banner when Profit or Loss is clicked
            val duaText = when {
                isProfitSelected -> "Alhamdulillah (الْحَمْدُ لِلَّهِ)"
                isLossSelected -> "Alhamdulillah ala kulli hal (الْحَمْدُ لِلَّهِ عَلَى كُلِّ حَالٍ)"
                else -> null
            }

            if (duaText != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isProfitSelected) Color(0x2416A34A) else Color(0x24DC2626),
                    border = BorderStroke(
                        1.dp,
                        if (isProfitSelected) NeonGreen.copy(alpha = 0.6f) else NeonRed.copy(alpha = 0.6f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = duaText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isProfitSelected) NeonGreenLight else NeonRedLight,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InvestmentTargetCard(
    uiState: AnalyzerUiState,
    onSetInvestmentAmount: (Double) -> Unit,
    onResetSessionPnl: () -> Unit = {}
) {
    val investment = uiState.investmentAmount
    val profitCount = uiState.profitCount
    val lossCount = uiState.lossCount
    val payoutPercentage = uiState.payoutPercentage
    val totalTrades = profitCount + lossCount

    val pnlData = com.example.data.analyzer.TradingOutputParser.calculatePnl(
        investmentAmount = investment,
        payoutPercentage = payoutPercentage,
        profitCount = profitCount,
        lossCount = lossCount
    )

    val totalProfitAmount = pnlData.grossProfit
    val totalLossAmount = pnlData.grossLoss
    val netPnl = pnlData.netPnl
    val winRate = pnlData.winRate

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("investment_target_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, BorderStrokeLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Title & Quick Reset Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Investment Icon",
                        tint = NeonGreenLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Investment & Target Profit (TARGET PNL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }

                // Reset Session Counter Button
                Surface(
                    onClick = onResetSessionPnl,
                    shape = RoundedCornerShape(8.dp),
                    color = DarkBackground,
                    border = BorderStroke(0.5.dp, BorderStrokeLight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset PnL",
                            tint = TextMuted,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Reset",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                    }
                }
            }

            // Custom Editable Input Field for Trade Amount (১ ডলার থেকে ১০০০+ ডলার যেকোনো পরিমাণ)
            var amountInput by remember(investment) {
                mutableStateOf(if (investment % 1.0 == 0.0) investment.toInt().toString() else investment.toString())
            }

            // Top Row: 3 balanced metric cards with identical height and layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Box 1: প্রফিট সংখ্যা (Automated Wins Counter)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (profitCount > 0) Color(0x1F00E676) else DarkBackground,
                    border = BorderStroke(1.dp, if (profitCount > 0) NeonGreen.copy(alpha = 0.8f) else BorderStrokeLight),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("box_profit_count")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Profit Count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreenLight,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "$profitCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = NeonGreenLight
                        )
                    }
                }

                // Box 2: লস সংখ্যা (Automated Loss Counter)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (lossCount > 0) Color(0x1FFF1744) else DarkBackground,
                    border = BorderStroke(1.dp, if (lossCount > 0) NeonRed.copy(alpha = 0.8f) else BorderStrokeLight),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("box_loss_count")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NeonRed)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Loss Count",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonRedLight,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "$lossCount",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = NeonRedLight
                        )
                    }
                }

                // Box 3: Custom Investment Amount ($) - পরিচ্ছন্ন এডিট ঘর
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkBackground,
                    border = BorderStroke(1.2.dp, AccentCyan.copy(alpha = 0.85f)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("box_per_trade_base")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = AccentCyan
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            BasicTextField(
                                value = amountInput,
                                onValueChange = { newText ->
                                    val filtered = newText.filter { it.isDigit() || it == '.' }
                                    if (filtered.count { it == '.' } <= 1 && filtered.length <= 7) {
                                        amountInput = filtered
                                        val parsed = filtered.toDoubleOrNull()
                                        if (parsed != null && parsed > 0.0) {
                                            onSetInvestmentAmount(parsed)
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .widthIn(min = 28.dp, max = 70.dp)
                                    .testTag("input_custom_investment"),
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = AccentCyan,
                                    textAlign = TextAlign.Start
                                ),
                                cursorBrush = SolidColor(AccentCyan),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Done
                                )
                            )
                        }
                    }
                }
            }

            // Financial Output Grid: 2 balanced cards for Total Profit & Total Loss
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val formattedUnit = if (investment % 1.0 == 0.0) investment.toInt().toString() else String.format(Locale.US, "%.1f", investment)

                // Box 4: টোটাল প্রফিট ($ TOTAL PROFIT)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkBackground,
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("box_total_profit")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Profit",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreenLight
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DarkCard,
                                border = BorderStroke(0.5.dp, BorderStrokeLight)
                            ) {
                                Text(
                                    text = "${profitCount} × $$formattedUnit (${payoutPercentage.toInt()}%)",
                                    fontSize = 8.5.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "+$${String.format(Locale.US, "%.2f", totalProfitAmount)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = NeonGreenLight
                        )
                    }
                }

                // Box 5: টোটাল লস ($ TOTAL LOSS)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = DarkBackground,
                    border = BorderStroke(1.dp, NeonRed.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .testTag("box_total_loss")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 9.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Loss",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonRedLight
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = DarkCard,
                                border = BorderStroke(0.5.dp, BorderStrokeLight)
                            ) {
                                Text(
                                    text = "${lossCount} × $$formattedUnit",
                                    fontSize = 8.5.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "-$${String.format(Locale.US, "%.2f", totalLossAmount)}",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = NeonRedLight
                        )
                    }
                }
            }

            // Bottom Session Summary Banner: NET PNL & WIN RATE
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (netPnl > 0) Color(0x1F00E676) else if (netPnl < 0) Color(0x1FFF1744) else DarkBackground,
                border = BorderStroke(
                    1.dp,
                    if (netPnl > 0) NeonGreen.copy(alpha = 0.6f) else if (netPnl < 0) NeonRed.copy(alpha = 0.6f) else BorderStrokeLight
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pnl_net_summary_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Net PNL:",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val pnlSign = if (netPnl > 0) "+$" else if (netPnl < 0) "-$" else "$"
                        val pnlColor = if (netPnl > 0) NeonGreenLight else if (netPnl < 0) NeonRedLight else TextMuted
                        Text(
                            text = "$pnlSign${String.format(Locale.US, "%.2f", abs(netPnl))}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = pnlColor
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkCard,
                        border = BorderStroke(0.5.dp, BorderStrokeLight)
                    ) {
                        Text(
                            text = "Win Rate: ${String.format(Locale.US, "%.1f", winRate)}% ($profitCount W - $lossCount L)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (winRate >= 50.0) NeonGreenLight else AccentAmber,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BengaliFormattedOutputCard(analysis: TradingAnalysis?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bengali_output_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, BorderStrokeLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MATHEMATICAL BREAKDOWN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Analysis",
                    tint = NeonGreenLight,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Formatted Bengali Bullet points
            val isValid = analysis?.isValid == true
            val isApprox = analysis?.isApproximate == true
            val p5 = analysis?.change5m ?: "--"
            val p60 = analysis?.change60m ?: "--"
            val p1d = analysis?.change1d ?: "--"
            val pNet = if (isValid) (analysis?.netSum ?: "--") else "--"
            val strengthLabel = if (isValid) (analysis?.strengthLevel?.strictName ?: "Normal") else "--"
            val dirStr = if (isValid) (analysis?.direction?.name ?: "NEUTRAL") else "--"
            val audioEvent = if (isValid) (analysis?.audioEvent ?: "SOUND_NONE") else "SOUND_NONE"
            val upPct = if (isValid) (analysis?.upPercentage ?: 50.0) else null
            val downPct = if (isValid) (analysis?.downPercentage ?: 50.0) else null

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBackground)
                    .border(1.dp, BorderStrokeLight, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "• 5m Change: $p5",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                Text(
                    text = "• 60m Change: $p60",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                if (p1d != "--") {
                    Text(
                        text = "• 1D Change: $p1d",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
                if (!isValid) {
                    Text(
                        text = "• Status: ⚠️ Ambiguous / Invalid Data (Not No-Trade Zone)",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber
                    )
                } else if (analysis?.isDeadMarket == true) {
                    Text(
                        text = "• Trade Zone: 🚫 NO TRADE ZONE (0.00% - 0.10% Dead Market / Fake Spike Warning)",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = NeonRedLight
                    )
                }
                if (isApprox) {
                    Text(
                        text = "• Approx Value Detected: Identified (~ / ≈) • Audio warning silenced",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber
                    )
                }
                Text(
                    text = "• Strength Level: $strengthLabel (Net: $pNet)",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )

                Text(
                    text = if (isValid && upPct != null && downPct != null) "• UP: ${String.format(Locale.US, "%.1f", upPct)}% | DOWN: ${String.format(Locale.US, "%.1f", downPct)}%" else "• UP: -- | DOWN: --",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isValid && upPct != null && downPct != null) {
                        if (upPct >= downPct) NeonGreenLight else NeonRedLight
                    } else TextMuted
                )

                Text(
                    text = "• Audio Event: $audioEvent",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (audioEvent == "SOUND_UP_ALERT") NeonGreenLight else if (audioEvent == "SOUND_DOWN_ALERT") NeonRedLight else TextMuted
                )

                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(BorderStrokeLight)
                )
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isValid && upPct != null && downPct != null) "Result: $dirStr (UP: ${String.format(Locale.US, "%.1f", upPct)}% / DOWN: ${String.format(Locale.US, "%.1f", downPct)}%)" else "Result: $dirStr",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = if (dirStr == "UP") NeonGreenLight else if (dirStr == "DOWN") NeonRedLight else TextPrimary
                )
            }
        }
    }
}

@Composable
fun HighDensityFooterBar(
    frameCount: Long,
    latencyMs: Long,
    intervalMs: Long,
    error: String?,
    engineSource: String = "Local Quant Vision",
    onOpenSettings: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (error != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NeonRedDim,
                border = BorderStroke(1.dp, NeonRedLight.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = NeonRedLight,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ENGINE: $engineSource".uppercase(),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
                val intLabel = if (intervalMs <= 0L) "0ms (Real-time)" else if (intervalMs < 1000L) "${intervalMs}ms" else "${intervalMs / 1000.0}s"
                Text(
                    text = "SYNC: ${latencyMs}ms • FRAME #$frameCount • $intLabel INT",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontStyle = FontStyle.Italic,
                    color = TextMuted
                )
            }

            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .height(30.dp)
                    .testTag("api_config_footer_button")
            ) {
                Text(
                    text = "SETTINGS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
fun DiagnosticsBar(
    frameCount: Long,
    latencyMs: Long,
    intervalMs: Long,
    error: String?
) {
    HighDensityFooterBar(
        frameCount = frameCount,
        latencyMs = latencyMs,
        intervalMs = intervalMs,
        error = error
    )
}


@Composable
fun FormulaAuditTab(analysis: TradingAnalysis?) {
    val isValid = analysis?.isValid == true
    val isApprox = analysis?.isApproximate == true
    val str5m = analysis?.change5m ?: "--"
    val str60m = analysis?.change60m ?: "--"
    val str1d = analysis?.change1d ?: "--"
    val val5m = if (isValid) analysis?.change5mValue else null
    val val60m = if (isValid) analysis?.change60mValue else null
    val netSumVal = if (isValid) (analysis?.netSumValue ?: 0.0) else 0.0
    val netSumStr = if (isValid) (analysis?.netSum ?: "--") else "--"
    val totalMagnitude = if (isValid) (analysis?.totalMagnitude ?: 0.0) else 0.0
    val sensitivityRatio = if (isValid) (analysis?.sensitivityRatio ?: 0.0) else 0.0
    val calculatedPct = if (isValid) (analysis?.calculatedPercentage ?: 50.0) else 50.0
    val upPct = if (isValid) (analysis?.upPercentage ?: 50.0) else 50.0
    val downPct = if (isValid) (analysis?.downPercentage ?: 50.0) else 50.0
    val direction = if (isValid) (analysis?.direction ?: TradeDirection.NEUTRAL) else TradeDirection.NEUTRAL
    val strength = if (isValid) (analysis?.strengthLevel ?: StrengthLevel.NORMAL) else StrengthLevel.NORMAL
    val isNoTrade = isValid && (analysis?.isNoTradeZone == true || analysis?.isDeadMarket == true)
    val dataQuality = analysis?.dataQuality ?: if (analysis == null) "UNAVAILABLE" else if (!isValid) "AMBIGUOUS" else "VERIFIED"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "📐 Canonical Formula & Calculation Rules",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Net Sum = A + B = (5 min change) + (60 min change)\n" +
                                "• Total Magnitude = |A| + |B| = |5 min change| + |60 min change|\n" +
                                "• Sensitivity Ratio = (|Net Sum| / Total Magnitude) * 100 (If Magnitude == 0, Ratio = 0)\n" +
                                "• Base Score = 50 + (Sensitivity Ratio / 2) -> Rounded to 1 decimal\n" +
                                "• Directional Bias: UP if Net Sum > 0, DOWN if Net Sum < 0, NEUTRAL if Net Sum == 0\n" +
                                "• NO TRADE ZONE: |A| <= 0.10% and |B| <= 0.10% triggers automatic No-Trade Zone (UP 50%, DOWN 50%, NEUTRAL, Silent).",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🔬 Live Canonical Computation Audit",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (analysis == null) {
                            Text("• Status: WAITING FOR VERIFIED DATA", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = AccentAmber)
                            Text("• Data: DATA UNAVAILABLE (--)", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                            Text("• 5m Input: --", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                            Text("• 60m Input: --", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                            Text("• Net Sum (Canonical): --", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                            Text("• Total Magnitude: --", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                            Text("• Sensitivity Ratio: --", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                            Text("• Canonical Directional Scores: UP -- | DOWN --", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                            Text("• Strength Level: --", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                        } else {
                            Text("• 5m Input: $str5m (${val5m ?: "--"})", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            Text("• 60m Input: $str60m (${val60m ?: "--"})", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            Text("• 1D Value: $str1d", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextSecondary)
                            Text("• Data Quality: $dataQuality", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = if (dataQuality == "VERIFIED") NeonGreenLight else AccentAmber)
                            if (isApprox) {
                                Text("• Approx Symbol (~ / ≈): DETECTED (Audio paused, calculation preserved)", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = AccentAmber)
                            }
                            Text("• Net Sum (Canonical): ${if (isValid) "$netSumStr (${String.format(Locale.US, "%.3f", netSumVal)})" else "--"}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = AccentCyan)
                            Text("• Total Magnitude: ${if (isValid) String.format(Locale.US, "%.3f", totalMagnitude) else "--"}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            Text("• Sensitivity Ratio: ${if (isValid) "${String.format(Locale.US, "%.2f", sensitivityRatio)}%" else "--"}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = AccentAmber)
                            if (!isValid) {
                                Text("• Trade Zone: ⚠️ DATA UNAVAILABLE / AMBIGUOUS (Values Incomplete / Ambiguous)", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = AccentAmber)
                                Text("• Directional Bias: AMBIGUOUS", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = TextMuted)
                            } else if (isNoTrade) {
                                Text("• Trade Zone: 🚫 NO TRADE ZONE (0.00% - 0.10% Dead Market • Neutral)", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = NeonRedLight)
                                Text("• Directional Bias: NEUTRAL (50/50)", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = AccentCyan)
                            } else if (isApprox) {
                                val biasStr = when (direction) {
                                    TradeDirection.UP -> "UPWARD BIAS (Approx • $calculatedPct%)"
                                    TradeDirection.DOWN -> "DOWNWARD BIAS (Approx • $calculatedPct%)"
                                    TradeDirection.NEUTRAL -> "NEUTRAL"
                                }
                                Text("• Trade Zone: ⚠️ APPROXIMATE DATA (Approx Input • Score Uncertain • Audio Inactive)", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = AccentAmber)
                                Text("• Directional Bias: $biasStr", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = AccentAmber)
                            } else {
                                val biasStr = when (direction) {
                                    TradeDirection.UP -> "UPWARD BIAS ($calculatedPct%)"
                                    TradeDirection.DOWN -> "DOWNWARD BIAS ($calculatedPct%)"
                                    TradeDirection.NEUTRAL -> "NEUTRAL"
                                }
                                Text("• Trade Zone: ✅ ACTIVE TRADE ZONE", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = NeonGreenLight)
                                Text("• Directional Bias: $biasStr", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = if (direction == TradeDirection.UP) NeonGreenLight else if (direction == TradeDirection.DOWN) NeonRedLight else AccentCyan)
                            }
                            Text("• Canonical Directional Scores: ${if (isValid) "UP ${String.format(Locale.US, "%.1f", upPct)}% | DOWN ${String.format(Locale.US, "%.1f", downPct)}%" else "UP -- | DOWN --"}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextPrimary)
                            Text("  (Formula-Based Score — Not Statistical Probability)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                            Text("• Strength Level: ${if (isValid) strength.strictName else "--"}", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = AccentAmber)
                        }
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "🏷️ Strength Classification (|Net Sum|)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentAmber
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Normal: < 0.50%\n" +
                                "• Medium: 0.50% to 0.99%\n" +
                                "• High: >= 1.00%",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

fun getMatrixSafetySymbol(riskLevel: RiskLevel?): String {
    return when (riskLevel) {
        RiskLevel.LOW -> "✔"
        RiskLevel.MODERATE -> "⚠"
        RiskLevel.HIGH, RiskLevel.EXTREME, RiskLevel.NO_TRADE -> "❌"
        null -> ""
    }
}

fun getMatrixSafetyColor(riskLevel: RiskLevel?): Color {
    return when (riskLevel) {
        RiskLevel.LOW -> NeonGreenLight
        RiskLevel.MODERATE -> AccentAmber
        RiskLevel.HIGH, RiskLevel.EXTREME, RiskLevel.NO_TRADE -> NeonRedLight
        null -> TextPrimary
    }
}



@Composable
fun MatrixAuditTab(
    analysis: TradingAnalysis?,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val allMatrices: List<QuantitativeMatrix> = remember { MatrixCatalog.allMatrices }

    val filteredMatrices = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allMatrices
        } else {
            allMatrices.filter { matrix ->
                matrix.id.contains(searchQuery, ignoreCase = true) ||
                matrix.title.contains(searchQuery, ignoreCase = true) ||
                matrix.description.contains(searchQuery, ignoreCase = true) ||
                matrix.conditionDescription.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("matrix_audit_tab"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, BorderStroke)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "165 Deterministic Quantitative Decision Matrices",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreenLight
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "165 TOTAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonGreen
                            )
                        }
                    }

                    Text(
                        text = "100% Deterministic Quantitative Evaluation: Zero quantum claims, zero external cloud latency. 100% on-device deterministic rule-based evaluation engine.",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )

                    // Safety Categorization Legend — All 165 Matrices Active Entry Mandate
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x2222C55E))
                                .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            Text("✔ 165 Active Matrices", fontSize = 10.sp, color = NeonGreenLight, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x2206B6D4))
                                .border(1.dp, AccentCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            Text("⚡ Zero Entry Lag", fontSize = 10.sp, color = AccentCyan, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x2210B981))
                                .border(1.dp, NeonGreenLight.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(vertical = 4.dp, horizontal = 6.dp)
                        ) {
                            Text("🎯 Signal Ready", fontSize = 10.sp, color = NeonGreenLight, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("Evaluated", fontSize = 9.sp, color = TextMuted)
                                Text(
                                    "${analysis?.evaluatedMatricesCount ?: allMatrices.size}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("Matched (Active)", fontSize = 9.sp, color = TextMuted)
                                Text(
                                    "${analysis?.matchedMatrixIds?.size ?: 0}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreenLight
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .padding(8.dp)
                        ) {
                            Column {
                                Text("Primary Decision", fontSize = 9.sp, color = TextMuted)
                                val pId = analysis?.primaryMatrixId ?: "--"
                                val pObj = if (pId != "--") MatrixCatalog.getById(pId) else null
                                val pBadge = getMatrixSafetySymbol(pObj?.riskLevel)
                                Text(
                                    text = if (pBadge.isNotEmpty()) "$pId $pBadge" else pId,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pObj?.riskLevel != null) getMatrixSafetyColor(pObj.riskLevel) else AccentCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Primary Matrix Card
        if (analysis != null && analysis.primaryMatrixId != null) {
            item {
                val effectiveDirection: TradeDirection = when {
                    analysis.direction == TradeDirection.UP -> TradeDirection.UP
                    analysis.direction == TradeDirection.DOWN -> TradeDirection.DOWN
                    else -> {
                        val v5m = analysis.change5mValue ?: 0.0
                        val v60m = analysis.change60mValue ?: 0.0
                        val net = analysis.netSumValue ?: (v5m + v60m)
                        val v1d = analysis.change1dValue ?: 0.0
                        when {
                            v5m > 0.001 && v60m > 0.001 -> TradeDirection.UP
                            v5m < -0.001 && v60m < -0.001 -> TradeDirection.DOWN
                            net > 0.005 -> TradeDirection.UP
                            net < -0.005 -> TradeDirection.DOWN
                            v60m > 0.005 -> TradeDirection.UP
                            v60m < -0.005 -> TradeDirection.DOWN
                            v5m > 0.005 -> TradeDirection.UP
                            v5m < -0.005 -> TradeDirection.DOWN
                            v1d > 0.005 -> TradeDirection.UP
                            v1d < -0.005 -> TradeDirection.DOWN
                            analysis.upPercentage > analysis.downPercentage + 5.0 -> TradeDirection.UP
                            analysis.downPercentage > analysis.upPercentage + 5.0 -> TradeDirection.DOWN
                            else -> TradeDirection.NEUTRAL
                        }
                    }
                }

                val dirLabel = when (effectiveDirection) {
                    TradeDirection.UP -> "UP ↗"
                    TradeDirection.DOWN -> "DOWN ↘"
                    TradeDirection.NEUTRAL -> "NEUTRAL"
                }
                val dirColor = when (effectiveDirection) {
                    TradeDirection.UP -> NeonGreenLight
                    TradeDirection.DOWN -> NeonRedLight
                    TradeDirection.NEUTRAL -> AccentCyan
                }
                val dirBg = when (effectiveDirection) {
                    TradeDirection.UP -> NeonGreen.copy(alpha = 0.15f)
                    TradeDirection.DOWN -> NeonRed.copy(alpha = 0.15f)
                    TradeDirection.NEUTRAL -> AccentCyan.copy(alpha = 0.15f)
                }
                val dirBorder = when (effectiveDirection) {
                    TradeDirection.UP -> NeonGreen.copy(alpha = 0.6f)
                    TradeDirection.DOWN -> NeonRed.copy(alpha = 0.6f)
                    TradeDirection.NEUTRAL -> AccentCyan.copy(alpha = 0.5f)
                }
                val cardBorderColor = when (effectiveDirection) {
                    TradeDirection.DOWN -> NeonRed.copy(alpha = 0.5f)
                    TradeDirection.UP -> NeonGreen.copy(alpha = 0.5f)
                    TradeDirection.NEUTRAL -> AccentCyan.copy(alpha = 0.4f)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    border = BorderStroke(1.dp, cardBorderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val pMat = MatrixCatalog.getById(analysis.primaryMatrixId ?: "")
                        val pBadge = getMatrixSafetySymbol(pMat?.riskLevel)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Primary Triggered Matrix",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = analysis.primaryMatrixId ?: "",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (effectiveDirection == TradeDirection.DOWN) NeonRedLight else NeonGreen
                                )
                                if (pBadge.isNotEmpty()) {
                                    Text(
                                        text = pBadge,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = getMatrixSafetyColor(pMat?.riskLevel)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = analysis.primaryMatrixTitle ?: "",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (effectiveDirection == TradeDirection.DOWN) NeonRedLight else NeonGreenLight,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            // UP / DOWN Badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(dirBg)
                                    .border(1.dp, dirBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = dirLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = dirColor
                                )
                            }
                        }

                        Text(
                            text = analysis.primaryMatrixDescription ?: "",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )

                        if (!analysis.matchedMatrixIds.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Matching Matrices List (${analysis.matchedMatrixIds.size} Active):",
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                analysis.matchedMatrixIds.take(8).forEach { id ->
                                    val mMat = MatrixCatalog.getById(id)
                                    val chipBadge = getMatrixSafetySymbol(mMat?.riskLevel)
                                    val chipColor = getMatrixSafetyColor(mMat?.riskLevel)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (id == analysis.primaryMatrixId) NeonGreen.copy(alpha = 0.3f) else DarkBackground)
                                            .border(1.dp, if (id == analysis.primaryMatrixId) NeonGreen else BorderStrokeLight, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (chipBadge.isNotEmpty()) "$id $chipBadge" else id,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = if (id == analysis.primaryMatrixId) FontWeight.Bold else FontWeight.Normal,
                                            color = if (id == analysis.primaryMatrixId) NeonGreenLight else chipColor
                                        )
                                    }
                                }
                            }
                        }

                        if (!analysis.decisionTrace.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Decision Audit Trace:",
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkBackground)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = analysis.decisionTrace.joinToString("\n"),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AccentCyan,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search & Filter Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search Matrix (e.g. M001, Bullish, Dead Zone)...", fontSize = 12.sp, color = TextMuted) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonGreen,
                        unfocusedBorderColor = BorderStrokeLight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
                    )
                )
            }
        }

        // All 165 Matrix Items List
        items(filteredMatrices) { matrix ->
            val isPrimary = analysis?.primaryMatrixId == matrix.id
            val isMatched = analysis?.matchedMatrixIds?.contains(matrix.id) == true
            val matrixRecord = analysis?.matrixRecords?.find { it.matrixId == matrix.id }
            var userState: com.example.data.matrix.MatrixUserState by remember(matrix.id) {
                mutableStateOf(com.example.data.matrix.MatrixUserStateRegistry.getState(matrix.id))
            }
            val isContributed = matrixRecord?.isContributedToTrade ?: (userState == com.example.data.matrix.MatrixUserState.CHECKED && isMatched)
            val softScore = matrixRecord?.softEvidenceScore ?: 0.0

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (userState) {
                        com.example.data.matrix.MatrixUserState.CANCELLED -> DarkCard.copy(alpha = 0.5f)
                        com.example.data.matrix.MatrixUserState.UNCHECKED -> DarkCard.copy(alpha = 0.75f)
                        else -> if (isPrimary) DarkCard.copy(alpha = 0.95f) else DarkCard
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    when {
                        userState == com.example.data.matrix.MatrixUserState.CANCELLED -> NeonRed.copy(alpha = 0.3f)
                        isPrimary -> NeonGreen
                        isMatched -> NeonGreenLight.copy(alpha = 0.5f)
                        else -> BorderStrokeLight
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val itemBadge = getMatrixSafetySymbol(matrix.riskLevel)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (matrix.riskLevel) {
                                            RiskLevel.EXTREME, RiskLevel.NO_TRADE, RiskLevel.HIGH -> Color(0x33EF4444)
                                            RiskLevel.MODERATE -> Color(0x33F59E0B)
                                            RiskLevel.LOW -> Color(0x3322C55E)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${matrix.id} $itemBadge",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = when (matrix.riskLevel) {
                                        RiskLevel.EXTREME, RiskLevel.NO_TRADE, RiskLevel.HIGH -> NeonRedLight
                                        RiskLevel.MODERATE -> AccentAmber
                                        RiskLevel.LOW -> NeonGreenLight
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = matrix.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (userState == com.example.data.matrix.MatrixUserState.CANCELLED) TextMuted else TextPrimary
                            )
                        }

                        // Interactive User State Badge (Checked ✔, Cancelled ❌, Unchecked ⚠)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (userState) {
                                com.example.data.matrix.MatrixUserState.CHECKED -> NeonGreen.copy(alpha = 0.2f)
                                com.example.data.matrix.MatrixUserState.CANCELLED -> NeonRed.copy(alpha = 0.2f)
                                else -> Color(0x339CA3AF)
                            },
                            border = BorderStroke(
                                1.dp,
                                when (userState) {
                                    com.example.data.matrix.MatrixUserState.CHECKED -> NeonGreen.copy(alpha = 0.7f)
                                    com.example.data.matrix.MatrixUserState.CANCELLED -> NeonRed.copy(alpha = 0.7f)
                                    else -> TextMuted.copy(alpha = 0.7f)
                                }
                            ),
                            onClick = {
                                val nextState = when (userState) {
                                    com.example.data.matrix.MatrixUserState.CHECKED -> com.example.data.matrix.MatrixUserState.CANCELLED
                                    com.example.data.matrix.MatrixUserState.CANCELLED -> com.example.data.matrix.MatrixUserState.UNCHECKED
                                    else -> com.example.data.matrix.MatrixUserState.CHECKED
                                }
                                com.example.data.matrix.MatrixUserStateRegistry.setState(matrix.id, nextState)
                                userState = nextState
                            }
                        ) {
                            Text(
                                text = when (userState) {
                                    com.example.data.matrix.MatrixUserState.CHECKED -> "✔ CHECKED"
                                    com.example.data.matrix.MatrixUserState.CANCELLED -> "❌ CANCELLED"
                                    else -> "⚠ UNCHECKED"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (userState) {
                                    com.example.data.matrix.MatrixUserState.CHECKED -> NeonGreenLight
                                    com.example.data.matrix.MatrixUserState.CANCELLED -> NeonRedLight
                                    else -> TextMuted
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Soft Evidence Score and Contribution Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Direction: ${matrix.direction.name} | Soft Evidence: ${String.format(Locale.US, "%+.2f", softScore)}",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = when (matrix.direction) {
                                TradeDirection.UP -> NeonGreenLight
                                TradeDirection.DOWN -> NeonRedLight
                                else -> AccentAmber
                            }
                        )
                        Text(
                            text = if (isContributed) "Trade Impact: Active" else "Trade Impact: Inactive (Zero Weight)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isContributed) NeonGreenLight else TextMuted
                        )
                    }

                    Text(
                        text = "Condition: ${translateDashboardText(matrix.conditionDescription)}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = AccentCyan
                    )

                    Text(
                        text = translateDashboardText(matrix.description),
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryTab(
    history: List<TradingAnalysis>,
    onSetHistoryItemOutcome: (Long, TradeOutcome) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var outcomeFilter by remember { mutableStateOf<TradeOutcome?>(null) }
    var matrixSearchQuery by remember { mutableStateOf("") }

    val availableMatrices = remember(history) {
        history.mapNotNull { item ->
            item.primaryMatrixId ?: item.matchedMatrixIds.firstOrNull()
        }.filter { it.isNotBlank() && it != "NONE" }.distinct().take(12)
    }

    val fullDateFormat = remember { SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault()) }
    val displayDateHeaderFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    fun exportHistoryToText(records: List<TradingAnalysis>, specificLossOnly: Boolean = false) {
        val targetList = if (specificLossOnly) records.filter { it.outcome == TradeOutcome.LOSS } else records
        if (targetList.isEmpty()) {
            android.widget.Toast.makeText(context, "No records to export", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        val sb = StringBuilder()
        sb.append("=".repeat(48)).append("\n")
        sb.append("   QUANT VISION AI — TRADE HISTORY AUDIT REPORT   \n")
        sb.append("=".repeat(48)).append("\n")
        sb.append("Export Date: ").append(fullDateFormat.format(Date())).append("\n")
        sb.append("Total Signals: ").append(records.size).append("\n")
        sb.append("Profits: ").append(records.count { it.outcome == TradeOutcome.PROFIT }).append("\n")
        sb.append("Losses: ").append(records.count { it.outcome == TradeOutcome.LOSS }).append("\n")
        if (specificLossOnly) {
            sb.append("FILTER: LOSS TRADES AUDIT & CALIBRATION NOTE\n")
        }
        sb.append("=".repeat(48)).append("\n\n")

        targetList.forEachIndexed { index, item ->
            val mId = item.primaryMatrixId ?: item.matchedMatrixIds.firstOrNull() ?: "NONE"
            val isVerified = com.example.data.matrix.UserRuleRegistry.isRuleVerified(mId)
            val outcomeStr = when (item.outcome) {
                TradeOutcome.PROFIT -> "PROFIT [WIN]"
                TradeOutcome.LOSS -> "LOSS [DISCREPANCY - NEEDS REVIEW]"
                null -> "PENDING / UNMARKED"
            }

            sb.append("#${index + 1} | Time: ${fullDateFormat.format(Date(item.timestamp))}\n")
            sb.append("  Matrix ID: $mId ${if (isVerified) "[VERIFIED HIGH-POWER]" else ""}\n")
            sb.append("  Predicted Signal: ${item.direction.name} (Confidence: ${String.format(Locale.US, "%.1f%%", item.calculatedPercentage)})\n")
            sb.append("  Behavior Code: ${item.behaviorCode}\n")
            sb.append("  5m Change: ${item.change5m} | 60m Change: ${item.change60m} | Net: ${item.netSum}\n")
            sb.append("  Matched Matrices: ${if (item.matchedMatrixIds.isNotEmpty()) item.matchedMatrixIds.joinToString(", ") else "None"}\n")
            sb.append("  Outcome Result: $outcomeStr\n")

            if (item.outcome == TradeOutcome.LOSS) {
                val reverseDir = if (item.direction == TradeDirection.UP) "DOWN" else "UP"
                sb.append("  --> LOSS DIAGNOSTIC: Signal was ${item.direction.name} but market moved $reverseDir causing Loss.\n")
                sb.append("  --> CALIBRATION NOTE: Matrix $mId may need rule threshold adjustment or opposite reversal protection.\n")
            }
            sb.append("-".repeat(45)).append("\n")
        }

        val textToExport = sb.toString()

        // 1. Copy formatted text note to Android Clipboard
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Trade History Note", textToExport)
        clipboard?.setPrimaryClip(clip)

        // 2. Open Android Share Sheet so user can save note or send to text editor
        try {
            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, textToExport)
                putExtra(android.content.Intent.EXTRA_SUBJECT, "Trade_History_Audit_${System.currentTimeMillis()}.txt")
                type = "text/plain"
            }
            val shareIntent = android.content.Intent.createChooser(sendIntent, "Export Trade History Note")
            shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
            android.widget.Toast.makeText(context, "Note copied to clipboard & share opened!", android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Export copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun isSameCalendarDay(t1: Long, t2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = t1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = t2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    // Filter by selected date first for daily statistics
    val dateFilteredList = remember(history, selectedDateMillis) {
        val dateMillis = selectedDateMillis
        if (dateMillis != null) {
            history.filter { isSameCalendarDay(it.timestamp, dateMillis) }
        } else {
            history
        }
    }

    // Filter by Matrix Search Query (e.g. D061, U030, M015)
    val matrixFilteredList = remember(dateFilteredList, matrixSearchQuery) {
        val q = matrixSearchQuery.trim().uppercase(Locale.US)
        if (q.isEmpty()) {
            dateFilteredList
        } else {
            dateFilteredList.filter { item ->
                val pId = (item.primaryMatrixId ?: "").uppercase(Locale.US)
                val matched = item.matchedMatrixIds.any { it.uppercase(Locale.US).contains(q) }
                val beh = item.behaviorCode.uppercase(Locale.US)
                val dir = item.direction.name.uppercase(Locale.US)
                pId.contains(q) || matched || beh.contains(q) || dir.contains(q)
            }
        }
    }

    // Calculate Summary Stats for the selected date and matrix filter
    val totalTrades = matrixFilteredList.size
    val totalProfit = matrixFilteredList.count { it.outcome == TradeOutcome.PROFIT }
    val totalLoss = matrixFilteredList.count { it.outcome == TradeOutcome.LOSS }
    val decidedTrades = totalProfit + totalLoss
    val winRate = if (decidedTrades > 0) (totalProfit * 100.0 / decidedTrades) else 0.0

    // Filter by outcome (Profit Only, Loss Only, or All)
    val finalDisplayList = remember(matrixFilteredList, outcomeFilter) {
        when (outcomeFilter) {
            TradeOutcome.PROFIT -> matrixFilteredList.filter { it.outcome == TradeOutcome.PROFIT }
            TradeOutcome.LOSS -> matrixFilteredList.filter { it.outcome == TradeOutcome.LOSS }
            null -> matrixFilteredList
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_tab_root"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Streamlined History Header: Filters, Search & Performance Stats ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("history_stats_summary_card"),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            border = BorderStroke(1.dp, BorderStrokeLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(11.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Calendar Date Picker & Matrix Search Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Calendar Trigger
                    Surface(
                        onClick = {
                            val now = Calendar.getInstance()
                            selectedDateMillis?.let { dateMillis ->
                                now.timeInMillis = dateMillis
                            }
                            val dialog = DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val pickedCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, year)
                                        set(Calendar.MONTH, month)
                                        set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    selectedDateMillis = pickedCal.timeInMillis
                                },
                                now.get(Calendar.YEAR),
                                now.get(Calendar.MONTH),
                                now.get(Calendar.DAY_OF_MONTH)
                            )
                            dialog.show()
                        },
                        shape = RoundedCornerShape(9.dp),
                        color = if (selectedDateMillis != null) AccentCyanDim else DarkBackground,
                        border = BorderStroke(1.dp, if (selectedDateMillis != null) AccentCyan else BorderStrokeLight),
                        modifier = Modifier.testTag("btn_calendar_picker")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Calendar",
                                tint = if (selectedDateMillis != null) AccentCyan else TextPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = selectedDateMillis?.let { displayDateHeaderFormat.format(Date(it)) } ?: "All Dates",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedDateMillis != null) AccentCyan else TextPrimary
                            )
                        }
                    }

                    if (selectedDateMillis != null) {
                        Surface(
                            onClick = { selectedDateMillis = null },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x20DC2626),
                            border = BorderStroke(0.5.dp, NeonRedLight.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("btn_clear_date_filter")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Date",
                                tint = NeonRedLight,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(14.dp)
                            )
                        }
                    } else {
                        Surface(
                            onClick = {
                                val todayCal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                selectedDateMillis = todayCal.timeInMillis
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = DarkBackground,
                            border = BorderStroke(0.5.dp, BorderStrokeLight),
                            modifier = Modifier.testTag("btn_today_filter")
                        ) {
                            Text(
                                text = "Today",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Matrix Search Input
                    OutlinedTextField(
                        value = matrixSearchQuery,
                        onValueChange = { matrixSearchQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("matrix_search_input"),
                        placeholder = {
                            Text(
                                text = "Search Matrix...",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Matrix",
                                tint = if (matrixSearchQuery.isNotBlank()) AccentCyan else TextMuted,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        trailingIcon = {
                            if (matrixSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { matrixSearchQuery = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Search",
                                        tint = TextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(9.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground,
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = BorderStrokeLight,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                // Row 3: Outcome Filter Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // All Button
                    Surface(
                        onClick = { outcomeFilter = null },
                        shape = RoundedCornerShape(8.dp),
                        color = if (outcomeFilter == null) Color.White else DarkBackground,
                        border = BorderStroke(1.dp, if (outcomeFilter == null) Color.White else BorderStrokeLight),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_all_trades")
                    ) {
                        Text(
                            text = "All ($totalTrades)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (outcomeFilter == null) TextDark else TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }

                    // Profit Button
                    val isProfitActive = outcomeFilter == TradeOutcome.PROFIT
                    Surface(
                        onClick = {
                            outcomeFilter = if (isProfitActive) null else TradeOutcome.PROFIT
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isProfitActive) NeonGreen else NeonGreenDim,
                        border = BorderStroke(
                            1.dp,
                            if (isProfitActive) NeonGreenLight else NeonGreen.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_profit_trades")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Profit",
                                tint = if (isProfitActive) TextDark else NeonGreenLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Profit ($totalProfit)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isProfitActive) TextDark else NeonGreenLight
                            )
                        }
                    }

                    // Loss Button
                    val isLossActive = outcomeFilter == TradeOutcome.LOSS
                    Surface(
                        onClick = {
                            outcomeFilter = if (isLossActive) null else TradeOutcome.LOSS
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isLossActive) NeonRed else NeonRedDim,
                        border = BorderStroke(
                            1.dp,
                            if (isLossActive) NeonRedLight else NeonRed.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("filter_loss_trades")
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Loss",
                                tint = if (isLossActive) Color.White else NeonRedLight,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Loss ($totalLoss)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isLossActive) Color.White else NeonRedLight
                            )
                        }
                    }

                    // Export Text Note Button
                    Surface(
                        onClick = {
                            exportHistoryToText(finalDisplayList, specificLossOnly = (outcomeFilter == TradeOutcome.LOSS))
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = DarkBackground,
                        border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.7f)),
                        modifier = Modifier
                            .testTag("btn_export_history_note")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Code,
                                contentDescription = "Export Text Note",
                                tint = AccentCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (outcomeFilter == TradeOutcome.LOSS) "Export Loss Note" else "Export Note",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }
                    }
                }

                // Quick Matrix Filter Chips
                if (availableMatrices.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableMatrices) { mId ->
                            val isSelected = matrixSearchQuery.equals(mId, ignoreCase = true)
                            Surface(
                                onClick = {
                                    matrixSearchQuery = if (isSelected) "" else mId
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSelected) AccentCyan else DarkBackground,
                                border = BorderStroke(0.5.dp, if (isSelected) AccentCyan else BorderStrokeLight)
                            ) {
                                Text(
                                    text = mId,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isSelected) TextDark else AccentCyan,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- History Records List ---
        if (finalDisplayList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
                    .border(1.dp, BorderStrokeLight, RoundedCornerShape(14.dp))
                    .padding(28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "No Record",
                        tint = TextMuted,
                        modifier = Modifier.size(38.dp)
                    )
                    Text(
                        text = if (selectedDateMillis != null || outcomeFilter != null || matrixSearchQuery.isNotBlank())
                            "No trade records found for selected filters"
                        else
                            "No previous scan history found",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Text(
                        text = "Try changing the date filter or matrix search query",
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("history_list_view"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(finalDisplayList) { item ->
                    val isProf = item.outcome == TradeOutcome.PROFIT
                    val isLoss = item.outcome == TradeOutcome.LOSS
                    val isUp = item.direction == TradeDirection.UP
                    val isDown = item.direction == TradeDirection.DOWN
                    val dirColor = if (isUp) NeonGreenLight else if (isDown) NeonRedLight else TextSecondary

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("history_card_${item.timestamp}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isProf -> Color(0xFF101F16)
                                isLoss -> Color(0xFF221114)
                                else -> DarkCard
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            when {
                                isProf -> NeonGreen.copy(alpha = 0.5f)
                                isLoss -> NeonRed.copy(alpha = 0.5f)
                                else -> BorderStroke
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(11.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // Primary Row: Matrix Pill + Direction + Confidence % + ONLY Selected Outcome Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val mId = item.primaryMatrixId ?: item.matchedMatrixIds.firstOrNull()
                                    if (!mId.isNullOrBlank() && mId != "NONE") {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = AccentCyan.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, AccentCyan.copy(alpha = 0.6f)),
                                            onClick = { matrixSearchQuery = mId }
                                        ) {
                                            Text(
                                                text = mId,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = AccentCyan,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Direction Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isUp) NeonGreenDim else if (isDown) NeonRedDim else DarkBackground,
                                        border = BorderStroke(0.5.dp, dirColor.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isUp) Icons.AutoMirrored.Filled.TrendingUp else if (isDown) Icons.AutoMirrored.Filled.TrendingDown else Icons.Default.Info,
                                                contentDescription = item.direction.name,
                                                tint = dirColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = item.direction.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = dirColor
                                            )
                                        }
                                    }

                                    // Confidence %
                                    Text(
                                        text = String.format(Locale.US, "%.1f%%", item.calculatedPercentage),
                                        fontSize = 12.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Black,
                                        color = TextPrimary
                                    )
                                }

                                // Precise Outcome Badge:
                                // Only appears if selected (Profit -> Profit only, Loss -> Loss only).
                                // Prior to selection, neither Profit nor Loss appears on the card.
                                when {
                                    isProf -> {
                                        Surface(
                                            onClick = {
                                                // Tap to toggle or clear
                                                onSetHistoryItemOutcome(item.timestamp, TradeOutcome.PROFIT)
                                            },
                                            shape = RoundedCornerShape(7.dp),
                                            color = NeonGreen,
                                            border = BorderStroke(1.dp, NeonGreenLight)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Profit",
                                                    tint = TextDark,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "PROFIT",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = TextDark
                                                )
                                            }
                                        }
                                    }
                                    isLoss -> {
                                        Surface(
                                            onClick = {
                                                // Tap to toggle or clear
                                                onSetHistoryItemOutcome(item.timestamp, TradeOutcome.LOSS)
                                            },
                                            shape = RoundedCornerShape(7.dp),
                                            color = NeonRed,
                                            border = BorderStroke(1.dp, NeonRedLight)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Loss",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "LOSS",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        // Initially unmarked: NO Profit or Loss badge shown!
                                        // Provide a subtle, understated "+ Outcome" pill if user wants to set outcome manually from card.
                                        var showQuickAssign by remember { mutableStateOf(false) }
                                        if (showQuickAssign) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    onClick = {
                                                        onSetHistoryItemOutcome(item.timestamp, TradeOutcome.PROFIT)
                                                        showQuickAssign = false
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = NeonGreenDim,
                                                    border = BorderStroke(1.dp, NeonGreenLight)
                                                ) {
                                                    Text(
                                                        text = "✓ Profit",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NeonGreenLight,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                                Surface(
                                                    onClick = {
                                                        onSetHistoryItemOutcome(item.timestamp, TradeOutcome.LOSS)
                                                        showQuickAssign = false
                                                    },
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = NeonRedDim,
                                                    border = BorderStroke(1.dp, NeonRedLight)
                                                ) {
                                                    Text(
                                                        text = "✕ Loss",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NeonRedLight,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Surface(
                                                onClick = { showQuickAssign = true },
                                                shape = RoundedCornerShape(6.dp),
                                                color = DarkBackground,
                                                border = BorderStroke(0.5.dp, BorderStrokeLight)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "Set Outcome",
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = "Outcome",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = TextMuted
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // Secondary Row: Numeric 5m, 60m, Net & Timestamp
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val v5 = item.change5mValue ?: 0.0
                                    val v60 = item.change60mValue ?: 0.0
                                    Text(
                                        text = "5m: ${item.change5m}",
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (v5 > 0) NeonGreenLight else if (v5 < 0) NeonRedLight else TextMuted
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "60m: ${item.change60m}",
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (v60 > 0) NeonGreenLight else if (v60 < 0) NeonRedLight else TextMuted
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = "Net: ${item.netSum}",
                                        fontSize = 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary
                                    )
                                }

                                 Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val mId = item.primaryMatrixId ?: item.matchedMatrixIds.firstOrNull()
                                    if (mId != null && com.example.data.matrix.UserRuleRegistry.isRuleVerified(mId)) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = NeonGreenDim,
                                            border = BorderStroke(0.5.dp, NeonGreen.copy(alpha = 0.5f))
                                        ) {
                                            Text(
                                                text = "✓ Verified",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonGreenLight,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = fullDateFormat.format(Date(item.timestamp)),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextSecondary
                                    )
                                }
                            }

                            // Loss Discrepancy & Reverse Diagnostic (Visible when marked as LOSS)
                            if (isLoss) {
                                val expectedDir = item.direction.name
                                val reversedDir = if (item.direction == TradeDirection.UP) "DOWN" else "UP"
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = NeonRedDim,
                                    border = BorderStroke(0.5.dp, NeonRedLight.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Discrepancy Warning",
                                            tint = NeonRedLight,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "Loss Case: Signal was $expectedDir, reversed to $reversedDir • Ready to export note for fix",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = NeonRedLight
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RawOutputTab(analysis: TradingAnalysis?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                border = BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Model Raw Response (Raw Gemini Response)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentCyan
                        )
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Raw",
                            tint = AccentCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkBackground)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = analysis?.rawResponse?.ifBlank { "No response received yet." }
                                ?: "No response received yet.",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MicroKineticVectorCard(
    analysis: TradingAnalysis?
) {
    val isValid = analysis?.isValid == true
    val v5m = analysis?.change5mValue ?: 0.0
    val v60m = analysis?.change60mValue ?: 0.0
    val v1d = analysis?.change1dValue

    val microVel = analysis?.microVelocity ?: 0.0
    val baseEnergy = analysis?.kineticBaseEnergy ?: 0.0
    val netForce = analysis?.netKineticForce ?: 0.0
    val upWeight = analysis?.kineticUpWeight ?: 50.0
    val downWeight = analysis?.kineticDownWeight ?: 50.0
    val footprintTitle = analysis?.forensicPatternTitle?.takeIf { it.isNotBlank() }?.let { translateDashboardText(it) }
        ?: if (isValid) "Dynamic Equilibrium Flow" else "Kinetic Data Incomplete"
    val footprintDiagnosis = analysis?.forensicDiagnosis?.takeIf { it.isNotBlank() }?.let { translateDashboardText(it) }
        ?: if (isValid) "Live multi-timeframe velocity and acceleration vector analysis active." else "Ensure camera frame clarity."
    val isBrake = analysis?.isBrakeInertiaPullback == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("micro_kinetic_vector_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(
            1.dp,
            if (isBrake) AccentAmber.copy(alpha = 0.8f) else BorderStrokeLight
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Micro-Pressure Vectors",
                        tint = AccentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "EVIDENCE-WEIGHTED MICRO-PRESSURE VECTOR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentCyan,
                        letterSpacing = 0.8.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isBrake) AccentAmber.copy(alpha = 0.2f) else DarkSurfaceVariant,
                    border = BorderStroke(1.dp, if (isBrake) AccentAmber.copy(alpha = 0.6f) else BorderStrokeLight)
                ) {
                    Text(
                        text = if (isBrake) "⚠️ INERTIA BRAKE" else "PRESSURE ACTIVE",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBrake) AccentAmber else NeonGreenLight,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Forensic Footprint Banner (মার্কেটের পায়ের ছাপ ফরেনসিক ডায়াগনোসিস)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = if (isBrake) AccentAmber.copy(alpha = 0.12f) else DarkSurface,
                border = BorderStroke(1.dp, if (isBrake) AccentAmber.copy(alpha = 0.4f) else BorderStrokeLight)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "🐾 Pressure Forensics:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentCyan
                        )
                        Text(
                            text = footprintTitle,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isBrake) AccentAmber else TextPrimary
                        )
                    }
                    Text(
                        text = footprintDiagnosis,
                        fontSize = 8.5.sp,
                        color = TextSecondary,
                        lineHeight = 12.sp
                    )
                }
            }

            // 3-Timeframe Velocity Vector Metrics (5M, 60M, 1D)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // 5M Vector
                KineticTimeframePill(
                    modifier = Modifier.weight(1f),
                    label = "5M Vector",
                    valueStr = analysis?.change5m ?: "--",
                    massLabel = "w=0.50",
                    isPositive = v5m >= 0.0
                )

                // 60M Vector
                KineticTimeframePill(
                    modifier = Modifier.weight(1f),
                    label = "60M Vector",
                    valueStr = analysis?.change60m ?: "--",
                    massLabel = "w=0.35",
                    isPositive = v60m >= 0.0
                )

                // 1D Vector
                KineticTimeframePill(
                    modifier = Modifier.weight(1f),
                    label = "1D Macro",
                    valueStr = analysis?.change1d ?: "--",
                    massLabel = "w=0.15",
                    isPositive = (v1d ?: 0.0) >= 0.0
                )
            }

            // Evidence & Micro-Pressure Metrics: Velocity Confirmation, Micro Energy Bias, Normalized Micro Pressure
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Velocity Confirmation", fontSize = 8.sp, color = TextMuted)
                    Text(
                        text = if (isValid) "${String.format(Locale.US, "%+.3f", microVel)}/s" else "--",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (microVel >= 0) NeonGreenLight else NeonRedLight
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Micro Energy Bias", fontSize = 8.sp, color = TextMuted)
                    val netEng = analysis?.canonicalDecision?.netEnergy ?: baseEnergy
                    Text(
                        text = if (isValid) String.format(Locale.US, "%+.3f", netEng) else "--",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = AccentCyan
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Normalized Micro Pressure", fontSize = 8.sp, color = TextMuted)
                    Text(
                        text = if (isValid) String.format(Locale.US, "%+.3f", netForce) else "--",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (netForce >= 0) NeonGreenLight else NeonRedLight
                    )
                }
            }

            // Quantitative Evidence & Confirmation Status
            val cDecision = analysis?.canonicalDecision
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "NET MICRO ENERGY", fontSize = 7.5.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (isValid) String.format(Locale.US, "%+.3f", cDecision?.netEnergy ?: netForce) else "--",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if ((cDecision?.netEnergy ?: netForce) >= 0) NeonGreenLight else NeonRedLight
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "CANDLE CONFIRMATION", fontSize = 7.5.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Text(
                        text = if (cDecision?.isCandleDataUnavailable == false) String.format(Locale.US, "%+.2f", cDecision.candleConfirmation) else "N/A (No OHLC)",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (cDecision?.isCandleDataUnavailable == false) AccentCyan else TextMuted
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "MATRIX SUPPORT", fontSize = 7.5.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    val activeCount = cDecision?.eligibleMatrixIds?.size ?: (if (analysis?.primaryMatrixId != null) 1 else 0)
                    Text(
                        text = "$activeCount Active",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = NeonGreenLight
                    )
                }
            }

            // Kinetic Logistic Pressure Distribution Bar
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "UP PRESSURE: ${String.format(Locale.US, "%.1f", upWeight)}%",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonGreenLight,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "DOWN PRESSURE: ${String.format(Locale.US, "%.1f", downWeight)}%",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonRedLight,
                        fontFamily = FontFamily.Monospace
                    )
                }

                val upRatio = (upWeight / 100.0).toFloat().coerceIn(0.01f, 0.99f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(DarkSurface)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(upRatio)
                            .fillMaxHeight()
                            .background(NeonGreen)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f - upRatio)
                            .fillMaxHeight()
                            .background(NeonRed)
                    )
                }
            }
        }
    }
}

@Composable
private fun KineticTimeframePill(
    modifier: Modifier = Modifier,
    label: String,
    valueStr: String,
    massLabel: String,
    isPositive: Boolean
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, BorderStrokeLight)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, fontSize = 7.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                Text(text = massLabel, fontSize = 7.sp, color = AccentCyan, fontFamily = FontFamily.Monospace)
            }
            Text(
                text = valueStr,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = if (valueStr == "--") TextPrimary else if (isPositive) NeonGreenLight else NeonRedLight
            )
        }
    }
}
