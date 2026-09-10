package com.example

import android.Manifest
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.sample.SampleChartGenerator
import com.example.network.WebSocketTradeRelay
import com.example.ui.camera.CameraPreviewSection
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.TradingDashboardSection
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenDim
import com.example.ui.theme.NeonGreenLight
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep phone screen awake during active trading chart monitoring
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        com.example.audio.AudioSignalEngine.init(applicationContext)
        com.example.data.matrix.UserRuleRegistry.init(applicationContext)
        com.example.network.WebSocketTradeRelay.init(applicationContext)

        // Initialize local trading relay endpoints (192.168.0.102) and start Bluetooth-style auto-seeking
        com.example.network.HttpTradeRelay.webhookUrl = com.example.network.HttpTradeRelay.DEFAULT_HTTP_URL
        com.example.network.WebSocketTradeRelay.serverUrl = com.example.network.WebSocketTradeRelay.DEFAULT_SERVER_URL
        com.example.network.WebSocketTradeRelay.start()

        setContent {
            MyApplicationTheme {
                QuantVisionApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.startScanning()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopScanning()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        viewModel.onTrimMemory(level)
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.network.WebSocketTradeRelay.stop()
        com.example.audio.AudioSignalEngine.shutdown()
    }
}

/**
 * Top bar eye icon with a live center pupil status dot.
 * - Connected: glowing NeonGreen dot that pulses, blinks, and zooms in/out.
 * - Disconnected: solid NeonRed dot (static).
 */
@Composable
fun EyeConnectionStatusIcon(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eye_ws_pulse")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .size(20.dp)
            .testTag("top_bar_eye_connection_status"),
        contentAlignment = Alignment.Center
    ) {
        // Eye Outline Icon
        Icon(
            imageVector = Icons.Default.Visibility,
            contentDescription = if (isConnected) "Desktop WebSocket Connected" else "Desktop WebSocket Disconnected",
            tint = if (isConnected) NeonGreen.copy(alpha = 0.75f) else TextMuted.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp)
        )

        // Center Status Pupil Dot
        if (isConnected) {
            // Pulsing Glow Aura
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .graphicsLayer {
                        scaleX = glowScale
                        scaleY = glowScale
                        alpha = glowAlpha
                    }
                    .clip(CircleShape)
                    .background(NeonGreen)
            )

            // Zoom In / Zoom Out and Blinking Center Green Dot
            Box(
                modifier = Modifier
                    .size(5.5.dp)
                    .graphicsLayer {
                        scaleX = dotScale
                        scaleY = dotScale
                        alpha = dotAlpha
                    }
                    .clip(CircleShape)
                    .background(NeonGreenLight)
                    .border(0.6.dp, Color.White.copy(alpha = 0.85f), CircleShape)
            )
        } else {
            // Disconnected: Solid Red Dot (static)
            Box(
                modifier = Modifier
                    .size(5.5.dp)
                    .clip(CircleShape)
                    .background(NeonRed)
                    .border(0.6.dp, NeonRedLight, CircleShape)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QuantVisionApp(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isWsConnected by WebSocketTradeRelay.connectionState.collectAsStateWithLifecycle()
    var showSettingsDialog by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("quant_vision_main_screen"),
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        // 1. Flash / Torch Button (only active for live camera feed)
                        IconButton(
                            onClick = { viewModel.toggleTorch() },
                            enabled = uiState.selectedTestImageId == null,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("top_bar_torch_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Flash",
                                tint = if (uiState.isTorchOn) Color(0xFFFFD600) else if (uiState.selectedTestImageId != null) TextMuted.copy(alpha = 0.3f) else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 2. Rotate / Switch Camera Button
                        IconButton(
                            onClick = { viewModel.switchCamera() },
                            enabled = uiState.selectedTestImageId == null,
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("top_bar_rotate_camera_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FlipCameraAndroid,
                                contentDescription = "Rotate Camera",
                                tint = if (uiState.selectedTestImageId != null) TextMuted.copy(alpha = 0.3f) else TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        EyeConnectionStatusIcon(
                            isConnected = isWsConnected,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Quant Vision AI",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                letterSpacing = 0.4.sp,
                                maxLines = 1
                            )
                            Text(
                                text = "MD.NURAALAM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonGreenLight,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleAudioAlert() },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("top_bar_audio_alert_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isAudioAlertEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                contentDescription = if (uiState.isAudioAlertEnabled) "Audio Alert Enabled" else "Audio Alert Muted",
                                tint = if (uiState.isAudioAlertEnabled) NeonGreenLight else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("top_bar_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            if (cameraPermissionState.status.isGranted || uiState.selectedTestImageId != null) {
                // Split-Screen Architecture: Top 50% Camera, Bottom 50% Dashboard
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isLandscape = maxWidth > maxHeight

                    if (isLandscape) {
                        // Side-by-side for landscape/tablet
                        Row(modifier = Modifier.fillMaxSize()) {
                            CameraPreviewSection(
                                uiState = uiState,
                                onRegisterFrameProvider = { viewModel.registerFrameProvider(it) },
                                onToggleScanning = { viewModel.toggleScanning() },
                                onSwitchCamera = { viewModel.switchCamera() },
                                onToggleTorch = { viewModel.toggleTorch() },
                                onSelectScenario = { viewModel.selectSampleScenario(it) },
                                onOpenSettings = { showSettingsDialog = true },
                                onCaptureNow = { viewModel.triggerManualAnalysis() },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            TradingDashboardSection(
                                uiState = uiState,
                                onOpenSettings = { showSettingsDialog = true },
                                onSetInvestmentAmount = { viewModel.setInvestmentAmount(it) },
                                onSetTradeOutcome = { viewModel.setTradeOutcome(it) },
                                onSetHistoryItemOutcome = { ts, outcome -> viewModel.setHistoryItemOutcome(ts, outcome) },
                                onResetSessionPnl = { viewModel.resetSessionPnl() },
                                onToggleAutoTrade = { viewModel.toggleAutoTrade() },
                                onResetTradeLock = { viewModel.resetAutoTradeLock() },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    } else {
                        // Portrait: Camera ~18% height (halved for sleek compact scanner), Dashboard ~82% height for optimal visibility
                        Column(modifier = Modifier.fillMaxSize()) {
                            CameraPreviewSection(
                                uiState = uiState,
                                onRegisterFrameProvider = { viewModel.registerFrameProvider(it) },
                                onToggleScanning = { viewModel.toggleScanning() },
                                onSwitchCamera = { viewModel.switchCamera() },
                                onToggleTorch = { viewModel.toggleTorch() },
                                onSelectScenario = { viewModel.selectSampleScenario(it) },
                                onOpenSettings = { showSettingsDialog = true },
                                onCaptureNow = { viewModel.triggerManualAnalysis() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.36f)
                            )
                            TradingDashboardSection(
                                uiState = uiState,
                                onOpenSettings = { showSettingsDialog = true },
                                onSetInvestmentAmount = { viewModel.setInvestmentAmount(it) },
                                onSetTradeOutcome = { viewModel.setTradeOutcome(it) },
                                onSetHistoryItemOutcome = { ts, outcome -> viewModel.setHistoryItemOutcome(ts, outcome) },
                                onResetSessionPnl = { viewModel.resetSessionPnl() },
                                onToggleAutoTrade = { viewModel.toggleAutoTrade() },
                                onResetTradeLock = { viewModel.resetAutoTradeLock() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1.64f)
                            )
                        }
                    }
                }
            } else {
                // Camera Permission Request Screen
                CameraPermissionScreen(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                    onUseTestMode = { viewModel.selectSampleScenario(0) }
                )
            }
        }
    }

    if (showSettingsDialog) {
        ApiKeyDialog(
            currentApiKey = uiState.apiKey,
            currentIntervalMs = uiState.scanIntervalMs,
            currentEngineMode = uiState.engineMode,
            currentDecisionMode = uiState.decisionMode,
            currentAntiGlitch = uiState.isAntiGlitchConfirmationEnabled,
            currentAdaptiveCpu = uiState.isAdaptiveCpuProtectionEnabled,
            onSave = { key, interval, mode, decMode, antiGlitch, adaptiveCpu ->
                viewModel.setApiKey(key)
                viewModel.setScanInterval(interval)
                viewModel.setEngineMode(mode)
                viewModel.setDecisionMode(decMode)
                viewModel.toggleAntiGlitchConfirmation(antiGlitch)
                viewModel.toggleAdaptiveCpuProtection(adaptiveCpu)
            },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
fun CameraPermissionScreen(
    onRequestPermission: () -> Unit,
    onUseTestMode: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("camera_permission_screen"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = AccentCyan.copy(alpha = 0.15f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera Required",
                            tint = AccentCyan,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "ক্যামেরা অ্যাক্সেস প্রয়োজন",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "ট্রেডিং স্ক্রিনের '5 min change' এবং '60 min change' হেডার লাইভ স্ক্যান করতে ক্যামেরার অনুমতি দিন।",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = DarkBackground),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_camera_permission_button")
                ) {
                    Text(
                        text = "ক্যামেরার অনুমতি দিন (Grant Camera)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onUseTestMode,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = AccentCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("use_test_chart_button")
                ) {
                    Text(
                        text = "টেস্ট চার্ট মোড ব্যবহার করুন (Test Mode)",
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
