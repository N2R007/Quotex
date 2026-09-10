package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.RelayDeliveryStatus
import com.example.network.TradeExecutionDispatcher
import com.example.network.WebSocketTradeRelay
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BorderStrokeLight
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenLight
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedDim
import com.example.ui.theme.NeonRedLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Clean, High-Contrast, Minimalist Auto-Trade Control Panel.
 * Clutter-free design with unified Auto-Seek action, streamlined manual dispatch, and collapsible logs.
 */
@Composable
fun WebSocketAutoTradeTab(
    isAutoTradeEnabled: Boolean = false,
    onToggleAutoTrade: () -> Unit = {},
    onResetTradeLock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isConnected by WebSocketTradeRelay.connectionState.collectAsState()
    val autoConnectState by WebSocketTradeRelay.autoConnectState.collectAsState()
    val logs by WebSocketTradeRelay.logs.collectAsState()
    val manualStatus by TradeExecutionDispatcher.lastManualStatus.collectAsState()
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var activeClickedButton by remember { mutableStateOf<String?>(null) }
    var showServerSettings by remember { mutableStateOf(false) }
    var showDiagnosticsLogs by remember { mutableStateOf(false) }

    var inputUrl by remember {
        mutableStateOf(
            if (WebSocketTradeRelay.serverUrl.isNotBlank()) WebSocketTradeRelay.serverUrl
            else WebSocketTradeRelay.DEFAULT_SERVER_URL
        )
    }
    var inputWebhookUrl by remember {
        mutableStateOf(
            if (com.example.network.HttpTradeRelay.webhookUrl.isNotBlank()) com.example.network.HttpTradeRelay.webhookUrl
            else com.example.network.HttpTradeRelay.DEFAULT_HTTP_URL
        )
    }

    val isScanningOrConnected = autoConnectState != WebSocketTradeRelay.AutoConnectState.PAUSED

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("websocket_autotrade_tab"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section 1: Hero Auto-Trade & Relay Controller (Clean, No-Switch, One-Tap Action)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tab_master_auto_trade_card"),
                shape = RoundedCornerShape(16.dp),
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isScanningOrConnected) NeonGreen.copy(alpha = 0.5f) else BorderStrokeLight
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row: Status Indicator & Quick Settings Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isConnected -> NeonGreen
                                            isScanningOrConnected -> AccentCyan
                                            else -> NeonRedLight
                                        }
                                    )
                            )
                            Text(
                                text = when {
                                    isConnected -> "AUTO-TRADE RELAY • LIVE"
                                    isScanningOrConnected -> "AUTO-SEEKING PC DESKTOP..."
                                    else -> "AUTO-TRADE PAUSED"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    isConnected -> NeonGreenLight
                                    isScanningOrConnected -> AccentCyan
                                    else -> TextMuted
                                },
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Right header icons: Reset lock & server configuration toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onResetTradeLock,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Signal Lock",
                                    tint = AccentCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { showServerSettings = !showServerSettings },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Server Endpoint Settings",
                                    tint = if (showServerSettings) AccentCyan else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Main Action: Push Auto-Seek / Pause Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                WebSocketTradeRelay.serverUrl = inputUrl
                                WebSocketTradeRelay.toggleConnection()
                                if (!isAutoTradeEnabled && !isScanningOrConnected) {
                                    onToggleAutoTrade()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("ws_toggle_connect_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanningOrConnected) NeonRedDim else NeonGreen
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isScanningOrConnected) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (isScanningOrConnected) Color.White else Color.Black
                                )
                                Text(
                                    text = if (isScanningOrConnected) "PAUSE AUTO-SEEK" else "PUSH AUTO SEEK (START)",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isScanningOrConnected) Color.White else Color.Black,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Instant Manual Reconnect
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                WebSocketTradeRelay.serverUrl = inputUrl
                                WebSocketTradeRelay.connectByUser()
                            },
                            modifier = Modifier
                                .size(46.dp)
                                .background(DarkSurfaceVariant, RoundedCornerShape(12.dp))
                                .border(1.dp, BorderStrokeLight, RoundedCornerShape(12.dp))
                                .testTag("ws_reconnect_icon_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Force Reconnect",
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Collapsible Server Endpoint Settings (Hidden by default to avoid clutter)
                    AnimatedVisibility(
                        visible = showServerSettings,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            OutlinedTextField(
                                value = inputUrl,
                                onValueChange = {
                                    inputUrl = it
                                    WebSocketTradeRelay.serverUrl = it
                                },
                                label = { Text("WebSocket URL", fontSize = 10.sp) },
                                placeholder = { Text("ws://192.168.0.102:8765", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ws_server_url_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = BorderStrokeLight,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextSecondary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = inputWebhookUrl,
                                onValueChange = {
                                    inputWebhookUrl = it
                                    com.example.network.HttpTradeRelay.webhookUrl = it
                                },
                                label = { Text("HTTP Webhook URL", fontSize = 10.sp) },
                                placeholder = { Text("http://192.168.0.102:5000/trade", fontSize = 10.sp) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("http_webhook_url_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = BorderStrokeLight,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextSecondary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Quick Manual Trigger Buttons (Clean & Sleek)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_triggers_card"),
                shape = RoundedCornerShape(16.dp),
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeLight)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "INSTANT MANUAL DISPATCH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 0.5.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // BUY / CALL Button
                        val isBuyActive = activeClickedButton == "BUY"
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activeClickedButton = "BUY"
                                coroutineScope.launch {
                                    delay(500)
                                    if (activeClickedButton == "BUY") activeClickedButton = null
                                }
                                val currentWebhook = if (inputWebhookUrl.isNotBlank()) inputWebhookUrl.trim() else com.example.network.HttpTradeRelay.DEFAULT_HTTP_URL
                                val currentWs = if (inputUrl.isNotBlank()) inputUrl.trim() else WebSocketTradeRelay.DEFAULT_SERVER_URL
                                com.example.network.HttpTradeRelay.webhookUrl = currentWebhook
                                WebSocketTradeRelay.serverUrl = currentWs
                                TradeExecutionDispatcher.dispatchManualTrade("CLICK_BUY", 100.0)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("manual_buy_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBuyActive) NeonGreenLight else NeonGreen
                            )
                        ) {
                            Text(
                                text = if (isBuyActive) "SENT BUY ✔" else "BUY / CALL (UP)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }

                        // SELL / PUT Button
                        val isSellActive = activeClickedButton == "SELL"
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                activeClickedButton = "SELL"
                                coroutineScope.launch {
                                    delay(500)
                                    if (activeClickedButton == "SELL") activeClickedButton = null
                                }
                                val currentWebhook = if (inputWebhookUrl.isNotBlank()) inputWebhookUrl.trim() else com.example.network.HttpTradeRelay.DEFAULT_HTTP_URL
                                val currentWs = if (inputUrl.isNotBlank()) inputUrl.trim() else WebSocketTradeRelay.DEFAULT_SERVER_URL
                                com.example.network.HttpTradeRelay.webhookUrl = currentWebhook
                                WebSocketTradeRelay.serverUrl = currentWs
                                TradeExecutionDispatcher.dispatchManualTrade("CLICK_SELL", 100.0)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("manual_sell_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSellActive) NeonRedDim else NeonRed
                            )
                        ) {
                            Text(
                                text = if (isSellActive) "SENT SELL ✔" else "SELL / PUT (DOWN)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    // Compact Delivery Status
                    if (manualStatus != null) {
                        val status = manualStatus!!
                        val isBuy = status.direction == com.example.data.models.TradeDirection.UP
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isBuy) "BUY (${status.command})" else "SELL (${status.command})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBuy) NeonGreenLight else NeonRedLight
                            )
                            Text(
                                text = when (status.status) {
                                    RelayDeliveryStatus.DELIVERED -> "Delivered (${status.latencyMs}ms) ✔"
                                    RelayDeliveryStatus.SENDING -> "Dispatching..."
                                    RelayDeliveryStatus.FAILED -> "Failed"
                                    else -> status.status.name
                                },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = when (status.status) {
                                    RelayDeliveryStatus.DELIVERED -> NeonGreenLight
                                    RelayDeliveryStatus.FAILED -> NeonRedLight
                                    else -> AccentCyan
                                }
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Collapsible Diagnostic Logs (Clean & Hidden by default)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ws_diagnostics_card"),
                shape = RoundedCornerShape(16.dp),
                color = DarkCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStrokeLight)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDiagnosticsLogs = !showDiagnosticsLogs },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "LOGS & DIAGNOSTICS (${logs.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (showDiagnosticsLogs && logs.isNotEmpty()) {
                                Text(
                                    text = "Clear",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentCyan,
                                    modifier = Modifier
                                        .testTag("ws_clear_logs_button")
                                        .padding(horizontal = 4.dp)
                                        .clickable { WebSocketTradeRelay.clearLogs() }
                                )
                            }
                            Icon(
                                imageVector = if (showDiagnosticsLogs) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Toggle Logs",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showDiagnosticsLogs,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBackground)
                                .border(1.dp, BorderStrokeLight, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            if (logs.isEmpty()) {
                                Text(
                                    text = "No logs recorded yet.",
                                    fontSize = 10.5.sp,
                                    color = TextMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    items(logs) { logLine ->
                                        Text(
                                            text = logLine,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = when {
                                                logLine.contains("Sent Signal") || logLine.contains("Delivered") -> NeonGreenLight
                                                logLine.contains("error", ignoreCase = true) || logLine.contains("failed", ignoreCase = true) -> NeonRedLight
                                                else -> TextSecondary
                                            },
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
