package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BorderStroke
import com.example.data.models.EngineMode
import com.example.data.models.DecisionMode
import com.example.ui.theme.BorderStrokeLight
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenDim
import com.example.ui.theme.NeonGreenLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ApiKeyDialog(
    currentApiKey: String,
    currentIntervalMs: Long,
    currentEngineMode: EngineMode = EngineMode.AUTO,
    currentDecisionMode: DecisionMode = DecisionMode.LEGACY_MULTILAYER,
    currentAntiGlitch: Boolean = true,
    currentAdaptiveCpu: Boolean = true,
    onSave: (apiKey: String, intervalMs: Long, engineMode: EngineMode, decisionMode: DecisionMode, antiGlitch: Boolean, adaptiveCpu: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKeyText by remember { mutableStateOf(currentApiKey) }
    var selectedInterval by remember { mutableLongStateOf(currentIntervalMs) }
    var selectedEngineMode by remember { mutableStateOf(currentEngineMode) }
    var selectedDecisionMode by remember { mutableStateOf(currentDecisionMode) }
    var selectedAntiGlitch by remember { mutableStateOf(currentAntiGlitch) }
    var selectedAdaptiveCpu by remember { mutableStateOf(currentAdaptiveCpu) }

    val intervals = listOf(
        0L to "0ms (Continuous Real-time Stream)",
        10L to "10ms (Zero-Delay Instant Mode / Default)",
        25L to "25ms (Turbo Stream)",
        50L to "50ms (Ultra Stream)",
        100L to "100ms (Hyper Speed)",
        150L to "150ms (Instant Speed)",
        200L to "200ms (Ultra Fast)",
        250L to "250ms (Very Fast)",
        500L to "500ms (Fast)",
        1000L to "1.0s (OCR Standard)",
        3000L to "3.0s (Moderate)",
        6000L to "6.0s (Cloud Safe)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(20.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = "API Settings",
                    tint = NeonGreenLight,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "API Key & Engine Configuration",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Engine Mode Selector
                Column {
                    Text(
                        text = "ANALYSIS ENGINE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EngineMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedEngineMode == mode,
                                onClick = { selectedEngineMode = mode },
                                label = {
                                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                                        Text(
                                            text = mode.bengaliTitle,
                                            fontSize = 11.sp,
                                            fontWeight = if (selectedEngineMode == mode) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = mode.subtitle,
                                            fontSize = 9.sp,
                                            color = if (selectedEngineMode == mode) NeonGreenLight else TextMuted
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGreenDim,
                                    selectedLabelColor = NeonGreenLight,
                                    containerColor = DarkBackground,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedEngineMode == mode,
                                    borderColor = BorderStrokeLight,
                                    selectedBorderColor = NeonGreen
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Decision Mode Selector
                Column {
                    Text(
                        text = "AUTO-ENTRY DECISION MODE:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        DecisionMode.values().forEach { mode ->
                            FilterChip(
                                selected = selectedDecisionMode == mode,
                                onClick = { selectedDecisionMode = mode },
                                label = {
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(
                                            text = mode.titleBengali,
                                            fontSize = 11.sp,
                                            fontWeight = if (selectedDecisionMode == mode) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = when (mode) {
                                                DecisionMode.THREE_TIMEFRAME_PRESSURE ->
                                                    "Pure Mathematical Pressure Engine: Direct auto-entry from 5m, 60m, 1D energy square and directional pressure."
                                                DecisionMode.SHORT_TERM_STRENGTH ->
                                                    "ReactiveMarketPressureEngine (up/down pressure) as primary source, MicroKineticVectorEngine for confirmation."
                                                else ->
                                                    "13-layer hierarchy, 165 matrices, and reversal guard"
                                            },
                                            fontSize = 9.sp,
                                            color = if (selectedDecisionMode == mode) NeonGreenLight else TextMuted
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonGreenDim,
                                    selectedLabelColor = NeonGreenLight,
                                    containerColor = DarkBackground,
                                    labelColor = TextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedDecisionMode == mode,
                                    borderColor = BorderStrokeLight,
                                    selectedBorderColor = NeonGreen
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 100% Offline Engine Status Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBackground)
                        .border(1.dp, BorderStrokeLight, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "100% Offline On-Device Detection Engine",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonGreenLight
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• 165 Deterministic Quantitative Decision Matrices (165 Active)\n" +
                               "• Screen reading and matrix analysis 100% offline on-device\n" +
                               "• Auto-trade execution dispatched via local network / webhook\n" +
                               "• Zero cloud latency, no external API key required",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        lineHeight = 15.sp
                    )
                }

                // Local Automation Webhook URL (Flask / FastAPI / ngrok)
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LAPTOP AUTOMATION SERVER URL (WEBHOOK):",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    var webhookUrlText by remember { mutableStateOf(com.example.network.HttpTradeRelay.webhookUrl) }
                    OutlinedTextField(
                        value = webhookUrlText,
                        onValueChange = {
                            webhookUrlText = it
                            com.example.network.HttpTradeRelay.webhookUrl = it
                        },
                        label = { Text("Webhook URL (e.g. http://192.168.0.102:5000/trade)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("webhook_url_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentCyan,
                            unfocusedBorderColor = BorderStrokeLight,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextSecondary,
                            focusedContainerColor = DarkBackground,
                            unfocusedContainerColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Dispatches {\"signal\":\"UP\"} or {\"signal\":\"DOWN\"} immediately upon signal.\n• Supports local Wi-Fi or ngrok URLs.",
                        fontSize = 9.sp,
                        color = TextMuted,
                        lineHeight = 13.sp
                    )
                }

                // Scan Interval Selector
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Interval",
                            tint = NeonGreenLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FRAME INTERVAL:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        intervals.chunked(2).forEach { rowGroup ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowGroup.forEach { (ms, label) ->
                                    FilterChip(
                                        selected = selectedInterval == ms,
                                        onClick = { selectedInterval = ms },
                                        label = {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = if (selectedInterval == ms) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NeonGreenDim,
                                            selectedLabelColor = NeonGreenLight,
                                            containerColor = DarkBackground,
                                            labelColor = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selectedInterval == ms,
                                            borderColor = BorderStrokeLight,
                                            selectedBorderColor = NeonGreen
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowGroup.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Stability & Glitch Protection
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Protection",
                            tint = NeonGreenLight,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "STABILITY & GLITCH PROTECTION:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))

                    // 2-Frame Anti-Glitch Confirmation
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .border(1.dp, if (selectedAntiGlitch) BorderStrokeLight else BorderStroke, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "2-Frame Anti-Glitch Confirmation",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedAntiGlitch) NeonGreenLight else TextPrimary
                            )
                            Text(
                                text = "পরপর ২টি ফ্রেমে মান নিশ্চিতকরণ (ক্যামেরা গ্লেয়ার ও ফ্লিকার রোধ)",
                                fontSize = 9.sp,
                                color = TextMuted,
                                lineHeight = 12.sp
                            )
                        }
                        Switch(
                            checked = selectedAntiGlitch,
                            onCheckedChange = { selectedAntiGlitch = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonGreen,
                                checkedTrackColor = DarkCard,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBackground
                            ),
                            modifier = Modifier.testTag("toggle_anti_glitch")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Adaptive CPU Protection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .border(1.dp, if (selectedAdaptiveCpu) BorderStrokeLight else BorderStroke, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Adaptive CPU Protection",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedAdaptiveCpu) NeonGreenLight else TextPrimary
                            )
                            Text(
                                text = "স্মার্ট সিপিইউ ও ব্যাটারি সুরক্ষা (স্থির স্ক্রিনে ২৫ms রেস্ট, পরিবর্তনে ০ms)",
                                fontSize = 9.sp,
                                color = TextMuted,
                                lineHeight = 12.sp
                            )
                        }
                        Switch(
                            checked = selectedAdaptiveCpu,
                            onCheckedChange = { selectedAdaptiveCpu = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonGreen,
                                checkedTrackColor = DarkCard,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkBackground
                            ),
                            modifier = Modifier.testTag("toggle_adaptive_cpu")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(apiKeyText, selectedInterval, selectedEngineMode, selectedDecisionMode, selectedAntiGlitch, selectedAdaptiveCpu)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Save",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted, fontSize = 12.sp)
            }
        }
    )
}
