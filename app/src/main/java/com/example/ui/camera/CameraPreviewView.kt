package com.example.ui.camera

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.InsertPhoto
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.models.AnalyzerUiState
import com.example.data.models.StrengthLevel
import com.example.data.models.TradeDirection
import com.example.data.sample.SampleChartGenerator
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.AccentCyanDim
import com.example.ui.theme.BorderColor
import com.example.ui.theme.BorderLight
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenDim
import com.example.ui.theme.NeonGreenLight
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonRedDim
import com.example.ui.theme.NeonRedLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun CameraPreviewSection(
    uiState: AnalyzerUiState,
    onRegisterFrameProvider: ((() -> Bitmap?)?) -> Unit,
    onToggleScanning: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleTorch: () -> Unit,
    onSelectScenario: (Int?) -> Unit,
    onOpenSettings: () -> Unit,
    onCaptureNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var cameraRef by remember { mutableStateOf<Camera?>(null) }

    // Guaranteed User Zoom State - Default 3.5x Zoom strictly locked on launch per user mandate
    var userZoomRatio by remember { mutableFloatStateOf(3.5f) }
    var hardwareMinZoom by remember { mutableFloatStateOf(1.0f) }
    var hardwareMaxZoom by remember { mutableFloatStateOf(1.0f) }
    var hardwareZoomApplied by remember { mutableFloatStateOf(1.0f) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var showFocusRing by remember { mutableStateOf(false) }

    var boundLensFacing by remember { mutableStateOf<Int?>(null) }

    // Helper to safely command hardware zoom without exceeding capabilities
    val applyHardwareZoom: (Float) -> Unit = remember(cameraRef, hardwareMinZoom, hardwareMaxZoom) {
        { targetZoom ->
            val camera = cameraRef
            if (camera != null && hardwareMaxZoom > 1.05f) {
                val clamped = targetZoom.coerceIn(hardwareMinZoom, hardwareMaxZoom)
                if (kotlin.math.abs(clamped - hardwareZoomApplied) >= 0.05f) {
                    hardwareZoomApplied = clamped
                    try {
                        camera.cameraControl.setZoomRatio(clamped)
                    } catch (e: Exception) {
                        Log.d("CameraPreviewView", "Hw zoom set failed: ${e.message}")
                    }
                }
            }
        }
    }

    // Auto-hide focus ring after 1.5 seconds
    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            showFocusRing = true
            delay(1500)
            showFocusRing = false
        }
    }

    // Observe CameraX hardware capabilities cleanly with observer removal on dispose
    DisposableEffect(cameraRef, lifecycleOwner) {
        val camera = cameraRef ?: return@DisposableEffect onDispose {}
        val observer = androidx.lifecycle.Observer<androidx.camera.core.ZoomState> { state ->
            if (state != null) {
                hardwareMinZoom = state.minZoomRatio
                hardwareMaxZoom = state.maxZoomRatio.coerceAtMost(10f)
                val target = userZoomRatio.coerceIn(state.minZoomRatio, state.maxZoomRatio)
                if (kotlin.math.abs(target - hardwareZoomApplied) >= 0.05f) {
                    try {
                        camera.cameraControl.setZoomRatio(target)
                        hardwareZoomApplied = target
                    } catch (_: Exception) {}
                }
            }
        }
        camera.cameraInfo.zoomState.observe(lifecycleOwner, observer)
        onDispose {
            camera.cameraInfo.zoomState.removeObserver(observer)
        }
    }

    // Register frame provider callback to ViewModel
    LaunchedEffect(previewViewRef, cameraRef, uiState.selectedTestImageId) {
        if (uiState.selectedTestImageId != null) {
            val scenario = SampleChartGenerator.scenarios.getOrNull(uiState.selectedTestImageId)
            onRegisterFrameProvider {
                scenario?.let { SampleChartGenerator.createChartBitmap(it) }
            }
        } else {
            onRegisterFrameProvider {
                if (cameraRef != null) {
                    previewViewRef?.bitmap
                } else {
                    null
                }
            }
        }
    }

    // Rebind hardware camera strictly when lens or lifecycle changes (guarded by boundLensFacing per AGENTS.md rule 3.2)
    LaunchedEffect(previewViewRef, uiState.selectedCameraLens, lifecycleOwner) {
        val previewView = previewViewRef ?: return@LaunchedEffect
        if (uiState.selectedTestImageId != null) {
            boundLensFacing = null
            return@LaunchedEffect
        }

        if (boundLensFacing == uiState.selectedCameraLens && cameraRef != null) {
            return@LaunchedEffect
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val requestedLensFacing = if (uiState.selectedCameraLens == 1) {
                    CameraSelector.LENS_FACING_FRONT
                } else {
                    CameraSelector.LENS_FACING_BACK
                }
                val requestedSelector = CameraSelector.Builder()
                    .requireLensFacing(requestedLensFacing)
                    .build()

                val (cameraSelector, actualLensIndex) = when {
                    cameraProvider.hasCamera(requestedSelector) -> requestedSelector to (if (requestedLensFacing == CameraSelector.LENS_FACING_FRONT) 1 else 0)
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA to 0
                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA to 1
                    else -> null to null
                }

                if (cameraSelector == null || actualLensIndex == null) {
                    Log.w("CameraPreviewView", "No camera device available on this hardware/emulator")
                    cameraRef = null
                    boundLensFacing = null
                    return@addListener
                }

                cameraProvider.unbindAll()
                val boundCamera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview
                )
                cameraRef = boundCamera
                boundLensFacing = actualLensIndex
                val actualLensName = if (actualLensIndex == 1) "FRONT (1)" else "BACK (0)"
                Log.d("CameraPreviewView", "Camera bound successfully with actual lens $actualLensName (requested: ${uiState.selectedCameraLens})")
                try {
                    val zState = boundCamera.cameraInfo.zoomState.value
                    if (zState != null && zState.maxZoomRatio > 1.05f) {
                        val hwTarget = userZoomRatio.coerceIn(zState.minZoomRatio, zState.maxZoomRatio)
                        boundCamera.cameraControl.setZoomRatio(hwTarget)
                        hardwareZoomApplied = hwTarget
                    }
                } catch (ignored: Exception) {}
            } catch (exc: Exception) {
                Log.e("CameraPreviewView", "Camera binding failed: ${exc.message}", exc)
                cameraRef = null
                boundLensFacing = null
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // Unbind camera provider cleanly when composable leaves composition
    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
                cameraRef = null
                boundLensFacing = null
                Log.d("CameraPreviewView", "Camera unbindAll called on dispose")
            } catch (e: Exception) {
                Log.d("CameraPreviewView", "Camera unbind on dispose ignored: ${e.message}")
            }
        }
    }

    // Toggle Torch on hardware camera
    LaunchedEffect(uiState.isTorchOn, cameraRef) {
        cameraRef?.let { cam ->
            if (cam.cameraInfo.hasFlashUnit()) {
                cam.cameraControl.enableTorch(uiState.isTorchOn)
            }
        }
    }

    Box(
        modifier = modifier
            .background(DarkBackground)
            .testTag("camera_preview_container")
            // Pinch-to-zoom gesture detector
            .pointerInput(cameraRef, uiState.selectedTestImageId, userZoomRatio, hardwareMinZoom, hardwareMaxZoom) {
                detectTransformGestures { _, _, zoom, _ ->
                    val newZoom = (userZoomRatio * zoom).coerceIn(1.0f, 6.0f)
                    userZoomRatio = newZoom
                    applyHardwareZoom(newZoom)
                }
            }
            // Tap-to-focus gesture detector
            .pointerInput(cameraRef, previewViewRef, uiState.selectedTestImageId) {
                detectTapGestures { tapOffset ->
                    if (uiState.selectedTestImageId == null) {
                        focusPoint = tapOffset
                        val pView = previewViewRef ?: return@detectTapGestures
                        val factory = pView.meteringPointFactory
                        val point = factory.createPoint(tapOffset.x, tapOffset.y)
                        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
                            .setAutoCancelDuration(3, TimeUnit.SECONDS)
                            .build()
                        cameraRef?.cameraControl?.startFocusAndMetering(action)
                    }
                }
            }
    ) {
        if (uiState.selectedTestImageId != null) {
            // Render Selected Synthetic / Test Trading Chart with zoom scale
            val scenario = SampleChartGenerator.scenarios.getOrNull(uiState.selectedTestImageId)
            if (scenario != null) {
                val chartBitmap = remember(uiState.selectedTestImageId) {
                    SampleChartGenerator.createChartBitmap(scenario)
                }
                DisposableEffect(chartBitmap) {
                    onDispose {
                        if (!chartBitmap.isRecycled) {
                            chartBitmap.recycle()
                        }
                    }
                }
                Image(
                    bitmap = chartBitmap.asImageBitmap(),
                    contentDescription = "Test Trading Chart",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = userZoomRatio,
                            scaleY = userZoomRatio
                        ),
                    contentScale = ContentScale.Crop
                )
            }
        } else {
            // Live Hardware Camera Stream using CameraX with guaranteed digital scale fallback
            val digitalScale = if (hardwareZoomApplied > 0.05f) {
                (userZoomRatio / hardwareZoomApplied).coerceAtLeast(1.0f)
            } else {
                userZoomRatio
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = digitalScale,
                            scaleY = digitalScale
                        )
                        .testTag("camerax_preview_view"),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewViewRef = this
                        }
                    },
                    update = {
                        // PreviewView reference is updated once in factory
                    }
                )
            }
        }

        // Reticle / Header Alignment Guidelines Overlay
        ScanReticleOverlay(isProcessing = uiState.isProcessing)

        // Focus Indicator Ring when user taps on screen
        if (showFocusRing && focusPoint != null) {
            val point = focusPoint ?: Offset.Zero
            Box(
                modifier = Modifier
                    .offset { IntOffset(point.x.roundToInt() - 30, point.y.roundToInt() - 30) }
                    .size(60.dp)
                    .border(1.5.dp, NeonGreenLight, CircleShape)
            )
        }

        // Vertical Frosted Glass Zoom In (+) and Zoom Out (-) Controls on Right Side (Vertical)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
                .testTag("camera_zoom_controls")
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x60FFFFFF),
                            Color(0x221E293B),
                            Color(0x400F172A)
                        )
                    )
                )
                .border(
                    BorderStroke(
                        1.2.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0x99FFFFFF),
                                Color(0x4400E5FF),
                                Color(0x77FFFFFF)
                            )
                        )
                    ),
                    RoundedCornerShape(22.dp)
                )
                .padding(horizontal = 4.dp, vertical = 6.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Frosted Glass Zoom In (+) Button (Top)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color(0x5000E5FF),
                                    Color(0x2522C55E)
                                )
                            )
                        )
                        .border(1.dp, Color(0x9000E5FF), CircleShape)
                        .clickable {
                            val newZoom = (userZoomRatio + 0.25f).coerceIn(1.0f, 6.0f)
                            userZoomRatio = newZoom
                            applyHardwareZoom(newZoom)
                        }
                        .testTag("zoom_in_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Glass Zoom Ratio Display Badge (Middle) - Click to reset to 3.5X default
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x2500E5FF))
                        .border(0.8.dp, Color(0x5500E5FF), RoundedCornerShape(8.dp))
                        .clickable {
                            userZoomRatio = 3.5f
                            applyHardwareZoom(3.5f)
                        }
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1fX", userZoomRatio),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Frosted Glass Zoom Out (-) Button (Bottom)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                        .border(1.dp, Color(0x55FFFFFF), CircleShape)
                        .clickable {
                            val newZoom = (userZoomRatio - 0.25f).coerceIn(1.0f, 6.0f)
                            userZoomRatio = newZoom
                            applyHardwareZoom(newZoom)
                        }
                        .testTag("zoom_out_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ScanReticleOverlay(
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val lineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanProgress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Target box in center
        val boxWidth = width * 0.85f
        val boxHeight = height * 0.70f
        val left = (width - boxWidth) / 2f
        val top = (height - boxHeight) / 2f

        // Subtle target container outline
        drawRoundRect(
            color = Color(0x33FFFFFF),
            topLeft = Offset(left, top),
            size = Size(boxWidth, boxHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(14.dp.toPx(), 14.dp.toPx()),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
        )

        val cornerLength = minOf(18.dp.toPx(), boxHeight * 0.25f)
        val strokeWidth = 2.dp.toPx()
        val cornerColor = if (isProcessing) AccentCyan else NeonGreenLight

        // Top-Left Corner
        drawLine(cornerColor, Offset(left, top + 10.dp.toPx()), Offset(left, top + cornerLength), strokeWidth)
        drawLine(cornerColor, Offset(left + 10.dp.toPx(), top), Offset(left + cornerLength, top), strokeWidth)

        // Top-Right Corner
        drawLine(cornerColor, Offset(left + boxWidth, top + 10.dp.toPx()), Offset(left + boxWidth, top + cornerLength), strokeWidth)
        drawLine(cornerColor, Offset(left + boxWidth - 10.dp.toPx(), top), Offset(left + boxWidth - cornerLength, top), strokeWidth)

        // Bottom-Left Corner
        drawLine(cornerColor, Offset(left, top + boxHeight - 10.dp.toPx()), Offset(left, top + boxHeight - cornerLength), strokeWidth)
        drawLine(cornerColor, Offset(left + 10.dp.toPx(), top + boxHeight), Offset(left + cornerLength, top + boxHeight), strokeWidth)

        // Bottom-Right Corner
        drawLine(cornerColor, Offset(left + boxWidth, top + boxHeight - 10.dp.toPx()), Offset(left + boxWidth, top + boxHeight - cornerLength), strokeWidth)
        drawLine(cornerColor, Offset(left + boxWidth - 10.dp.toPx(), top + boxHeight), Offset(left + boxWidth - cornerLength, top + boxHeight), strokeWidth)

        // Animated laser sweep line inside the box
        val scanY = top + (boxHeight * lineProgress)
        val laserBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                (if (isProcessing) AccentCyan else NeonGreenLight).copy(alpha = 0.5f),
                Color.Transparent
            ),
            startX = left,
            endX = left + boxWidth
        )
        drawLine(
            brush = laserBrush,
            start = Offset(left + 6f, scanY),
            end = Offset(left + boxWidth - 6f, scanY),
            strokeWidth = 1.5.dp.toPx()
        )
    }
}

