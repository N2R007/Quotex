package com.example

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.analyzer.LocalQuantVisionEngine
import com.example.data.api.GeminiVisionClient
import com.example.data.models.AnalyzerUiState
import com.example.data.models.EngineMode
import com.example.data.models.TradeDirection
import com.example.data.models.TradingAnalysis
import com.example.data.sample.SampleChartGenerator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MainViewModel"

        // 1. Stall Detection Watchdog thresholds (Auto Watchdog Circuit Breaker: 30s)
        private const val STALL_DETECTION_TIMEOUT_MS = 30000L // 30 seconds without frame progress triggers auto-restart

        // 2. Optional conservative pre-OCR downscale (Code-level toggle, default disabled as requested)
        const val ENABLE_PRE_OCR_DOWNSCALE = false
        const val MAX_PRE_OCR_DIMENSION = 1280

        private val PLACEHOLDER_API_KEYS = setOf(
            "",
            "my_gemini_api_key",
            "default_key",
            "your_api_key_here",
            "your_gemini_api_key",
            "your-api-key-here",
            "your-gemini-api-key",
            "placeholder"
        )

        fun isPlaceholderApiKey(key: String?): Boolean {
            if (key.isNullOrBlank()) return true
            val trimmedLower = key.trim().lowercase(java.util.Locale.ROOT)
            return trimmedLower.isEmpty() ||
                   PLACEHOLDER_API_KEYS.contains(trimmedLower) ||
                   trimmedLower == "your_api_key_here" ||
                   trimmedLower == "your_gemini_api_key" ||
                   trimmedLower == "your-api-key-here" ||
                   trimmedLower == "my_gemini_api_key" ||
                   trimmedLower == "default_key" ||
                   trimmedLower.startsWith("your_api_key") ||
                   trimmedLower.startsWith("your-api-key") ||
                   trimmedLower.startsWith("my_gemini_api_key")
        }
    }

    private val apiMutex = Mutex()
    private var scanLoopJob: Job? = null
    private var cooldownJob: Job? = null
    private var activeProcessingJob: Job? = null

    // API key loaded strictly from BuildConfig (Secrets Gradle Plugin), rejecting any placeholder from .env.example
    private val defaultApiKey = if (BuildConfig.GEMINI_API_KEY.isNotBlank() && !isPlaceholderApiKey(BuildConfig.GEMINI_API_KEY)) {
        BuildConfig.GEMINI_API_KEY.trim()
    } else {
        ""
    }

    private val _uiState = MutableStateFlow(
        AnalyzerUiState(
            apiKey = defaultApiKey,
            scanIntervalMs = 10L,
            selectedTestImageId = null,
            engineMode = EngineMode.LOCAL,
            statusMessage = "LIVE OCR SCANNING"
        )
    )
    val uiState: StateFlow<AnalyzerUiState> = _uiState.asStateFlow()

    // Delta-Confluence Hybrid Matrix Engine (DCHM-Engine)
    val dchmEngine = com.example.data.analyzer.DCHMEngine()
    val dchmInstantPrediction: StateFlow<com.example.data.analyzer.TradeSignal?> = dchmEngine.latestInstantSignal
    val dchmConfirmedSignal: StateFlow<com.example.data.analyzer.TradeSignal?> = dchmEngine.latestConfirmedSignal
    val dchmMetrics: StateFlow<com.example.data.analyzer.DCHMEngine.EngineMetrics> = dchmEngine.engineMetrics

    init {
        com.example.audio.AudioSignalEngine.init(application)
        com.example.network.WebSocketTradeRelay.init(application)
        com.example.network.WebSocketTradeRelay.start()
    }

    override fun onCleared() {
        super.onCleared()
        stopScanning()
        dchmEngine.shutdown()
        com.example.audio.AudioSignalEngine.shutdown()
        com.example.network.WebSocketTradeRelay.stop()
    }

    // Holds the latest frame supplier callback from CameraX
    private var currentFrameProvider: (() -> Bitmap?)? = null

    fun registerFrameProvider(provider: (() -> Bitmap?)?) {
        Log.d(TAG, "registerFrameProvider updated: ${provider != null}")
        currentFrameProvider = provider
    }

    private var processingStartTime = 0L
    private var lastCloudCallTimestamp = 0L

    data class StableMetrics(
        val val5m: Double?,
        val val60m: Double?,
        val val1d: Double?
    )

    private var lastStableMetrics: StableMetrics? = null
    private var pendingCandidateMetrics: StableMetrics? = null
    private var pendingCandidateCount = 0
    private var consecutiveUnchangedFrames = 0
    private var consecutiveMissingCoreFrames = 0

    // Stateful tracking for Momentum Loss & Top/Bottom Fakeout zero-crossing detection
    private var previousConfirmed5m: Double? = null
    private var previousConfirmed60m: Double? = null
    private var previousConfirmed1d: Double? = null
    private var lastEmittedSignalType: com.example.data.models.SignalType = com.example.data.models.SignalType.NONE
    private var lastEmittedDirection: TradeDirection = TradeDirection.NEUTRAL
    private var lastWebSocketCommand: String? = null
    private var lastAutoTradeDispatchedAtMs: Long = 0L
    private var lastAutoTradeMatrixId: String? = null
    private var lastAutoTradeDirection: TradeDirection? = null
    private var lastDispatchedFingerprint: String? = null
    private val rollingMetricHistory = mutableListOf<com.example.data.models.MetricSnapshot>()

    // Auto-restart guard (Freeze / Stall detection) - completely internal
    private var lastSuccessfulFrameTimestamp = System.currentTimeMillis()
    private var lastMonitoredFrameCount = 0L
    private var consecutiveStallRestarts = 0
    private var lastRestartAttemptTimestamp = 0L

    // In-memory low-memory event logging (last 50 events, strictly internal / Logcat only)
    data class LowMemoryEvent(
        val timestamp: Long = System.currentTimeMillis(),
        val level: Int,
        val levelName: String
    )

    private val _lowMemoryEvents = java.util.Collections.synchronizedList(mutableListOf<LowMemoryEvent>())
    val lowMemoryEvents: List<LowMemoryEvent>
        get() = synchronized(_lowMemoryEvents) { _lowMemoryEvents.toList() }

    fun onTrimMemory(level: Int) {
        val levelName = when (level) {
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE -> "RUNNING_MODERATE (5)"
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW -> "RUNNING_LOW (10)"
            android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL -> "RUNNING_CRITICAL (15)"
            android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> "UI_HIDDEN (20)"
            android.content.ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> "BACKGROUND (40)"
            android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE -> "MODERATE (60)"
            android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> "COMPLETE (80)"
            else -> "LEVEL_$level"
        }
        val event = LowMemoryEvent(System.currentTimeMillis(), level, levelName)
        synchronized(_lowMemoryEvents) {
            _lowMemoryEvents.add(event)
            if (_lowMemoryEvents.size > 50) {
                _lowMemoryEvents.removeAt(0)
            }
        }
        Log.w(TAG, "onTrimMemory triggered: level=$level ($levelName). Total recorded events: ${_lowMemoryEvents.size}. Applying memory reduction.")

        // Safely trim rollingMetricHistory: preserve the last 4 snapshots so velocity,
        // kinetic momentum, and direction confirmation logic remain 100% active and unperturbed.
        synchronized(rollingMetricHistory) {
            if (rollingMetricHistory.size > 4) {
                val preserved = rollingMetricHistory.takeLast(4)
                rollingMetricHistory.clear()
                rollingMetricHistory.addAll(preserved)
            }
        }

        // Trim UI history list under system memory pressure: keep at most 3 items
        _uiState.update { current ->
            if (current.history.size > 3) {
                current.copy(history = current.history.take(3))
            } else {
                current
            }
        }

        // Trim WebSocket relay logs to free RAM
        com.example.network.WebSocketTradeRelay.trimLogs()

        // Explicitly trigger Garbage Collector on memory pressure to reclaim memory immediately
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            System.gc()
        }
    }

    fun startScanning() {
        Log.d(TAG, "startScanning invoked")
        lastSuccessfulFrameTimestamp = System.currentTimeMillis()
        lastMonitoredFrameCount = _uiState.value.frameCount
        _uiState.update { 
            it.copy(
                isScanning = true, 
                statusMessage = if (it.cooldownRemainingSeconds > 0 && it.engineMode == EngineMode.CLOUD) "COOLDOWN (${it.cooldownRemainingSeconds}s)" else "SCANNING ACTIVE"
            ) 
        }
        scanLoopJob?.cancel()
        scanLoopJob = viewModelScope.launch {
            while (isActive) {
                val cycleStart = android.os.SystemClock.elapsedRealtime()
                var isCloudCooldownActive = false
                try {
                    val currentState = _uiState.value
                    // Watchdog Circuit Breaker: If isProcessing is stuck for more than 2.5 seconds, safely cancel the active processing job
                    if (currentState.isProcessing && processingStartTime > 0 && (System.currentTimeMillis() - processingStartTime) > 2500L) {
                        Log.w(TAG, "Watchdog Circuit Breaker: isProcessing timed out (>2.5s). Auto-recovering scanning loop.")
                        activeProcessingJob?.cancel()
                        _uiState.update { it.copy(isProcessing = false) }
                        processingStartTime = 0L
                    }

                    // Auto-restart guard (Freeze / Stall detection):
                    // If scanning is active on live camera (not test scenario) and frameCount has previously advanced (> 0) but stopped for >= 15-20s:
                    if (currentState.isScanning && currentState.selectedTestImageId == null && currentFrameProvider != null && currentState.frameCount > 0L) {
                        val currentFrameCount = currentState.frameCount
                        val nowTime = System.currentTimeMillis()
                        if (currentFrameCount != lastMonitoredFrameCount) {
                            lastMonitoredFrameCount = currentFrameCount
                            lastSuccessfulFrameTimestamp = nowTime
                            if (consecutiveStallRestarts > 0) {
                                consecutiveStallRestarts = 0
                            }
                        } else {
                            val stallDuration = nowTime - lastSuccessfulFrameTimestamp
                            val backoffCooldownMs = when {
                                consecutiveStallRestarts == 0 -> STALL_DETECTION_TIMEOUT_MS
                                consecutiveStallRestarts <= 2 -> 30000L
                                consecutiveStallRestarts <= 5 -> 60000L
                                else -> 120000L
                            }
                            val timeSinceLastRestart = nowTime - lastRestartAttemptTimestamp

                            if (stallDuration >= STALL_DETECTION_TIMEOUT_MS && timeSinceLastRestart >= backoffCooldownMs) {
                                consecutiveStallRestarts++
                                lastRestartAttemptTimestamp = nowTime
                                lastSuccessfulFrameTimestamp = nowTime // reset stall timer baseline
                                Log.w(
                                    TAG,
                                    "Camera pipeline stall detected (no frame progress for ${stallDuration / 1000}s). " +
                                    "Triggering background CameraX restart #$consecutiveStallRestarts (backoff: ${backoffCooldownMs}ms)."
                                )
                                activeProcessingJob?.cancel()
                                _uiState.update {
                                    it.copy(
                                        isProcessing = false,
                                        cameraRestartTrigger = System.currentTimeMillis()
                                    )
                                }
                            }
                        }
                    }

                    val updatedState = _uiState.value
                    val hasFrameSource = currentFrameProvider != null || updatedState.selectedTestImageId != null
                    // In LOCAL or AUTO mode, scanning never stops even if Cloud API is cooling down
                    isCloudCooldownActive = updatedState.cooldownRemainingSeconds > 0 && updatedState.engineMode == EngineMode.CLOUD
                    val isProcessingActive = updatedState.isProcessing || (activeProcessingJob?.isActive == true)
                    if (updatedState.isScanning && hasFrameSource && !isProcessingActive && !isCloudCooldownActive) {
                        processingStartTime = System.currentTimeMillis()
                        activeProcessingJob = launch {
                            processNextFrame()
                        }
                        activeProcessingJob?.join()
                    }
                } catch (t: Throwable) {
                    if (t is kotlinx.coroutines.CancellationException) throw t
                    Log.e(TAG, "Error in scan loop iteration: ${t.message}", t)
                    _uiState.update { it.copy(isProcessing = false) }
                }
                val elapsed = android.os.SystemClock.elapsedRealtime() - cycleStart
                val isCloudMode = _uiState.value.engineMode == EngineMode.CLOUD
                val timeSinceLastCloud = System.currentTimeMillis() - lastCloudCallTimestamp
                val isAdaptiveCpuActive = _uiState.value.isAdaptiveCpuProtectionEnabled &&
                        !isCloudMode &&
                        _uiState.value.isScanning &&
                        consecutiveUnchangedFrames >= 3 &&
                        pendingCandidateMetrics == null

                val minDelay = if (currentFrameProvider == null && _uiState.value.selectedTestImageId == null) {
                    1000L
                } else if (!_uiState.value.isScanning) {
                    500L
                } else if (isCloudCooldownActive) {
                    1000L
                } else if (isCloudMode) {
                    // Enforce Cloud request pacing (<= 13 RPM, minimum 4500ms between calls)
                    (4500L - timeSinceLastCloud).coerceIn(100L, 4500L)
                } else if (isAdaptiveCpuActive && _uiState.value.scanIntervalMs <= 25L) {
                    25L
                } else {
                    if (_uiState.value.scanIntervalMs <= 0L) 0L else if (_uiState.value.scanIntervalMs <= 25L) 0L else 5L
                }
                val targetInterval = if (isAdaptiveCpuActive && _uiState.value.scanIntervalMs <= 25L) {
                    25L
                } else {
                    _uiState.value.scanIntervalMs
                }
                val hasFrameSource = currentFrameProvider != null || _uiState.value.selectedTestImageId != null
                val remainingDelay = if (!hasFrameSource || !_uiState.value.isScanning || isCloudCooldownActive || isCloudMode) {
                    minDelay
                } else if (targetInterval <= 0L && !isAdaptiveCpuActive) {
                    0L
                } else {
                    (targetInterval - elapsed).coerceAtLeast(minDelay)
                }
                if (remainingDelay > 0L) {
                    delay(remainingDelay)
                } else {
                    kotlinx.coroutines.yield()
                }
            }
        }
    }

    fun stopScanning() {
        Log.d(TAG, "stopScanning invoked")
        activeProcessingJob?.cancel()
        activeProcessingJob = null
        dchmEngine.reset()
        lastStableMetrics = null
        pendingCandidateMetrics = null
        pendingCandidateCount = 0
        consecutiveUnchangedFrames = 0
        consecutiveMissingCoreFrames = 0
        previousConfirmed5m = null
        previousConfirmed60m = null
        previousConfirmed1d = null
        lastEmittedSignalType = com.example.data.models.SignalType.NONE
        lastEmittedDirection = TradeDirection.NEUTRAL
        _uiState.update { 
            it.copy(
                isScanning = false, 
                statusMessage = "PAUSED"
            ) 
        }
        scanLoopJob?.cancel()
        scanLoopJob = null
        cooldownJob?.cancel()
        cooldownJob = null
    }

    private fun startCooldown(seconds: Int) {
        val currentRemaining = _uiState.value.cooldownRemainingSeconds
        val safeSeconds = seconds.coerceIn(15, 75)
        if (currentRemaining > 0 && safeSeconds <= currentRemaining) {
            Log.d(TAG, "Cooldown already in progress with $currentRemaining seconds remaining. Not restarting timer.")
            return
        }
        cooldownJob?.cancel()
        Log.d(TAG, "Starting rate-limit cooldown for $safeSeconds seconds")
        _uiState.update { 
            // Seamless automatic fallback: If in CLOUD mode, switch to AUTO to continue scanning on-device
            val targetEngine = if (it.engineMode == EngineMode.CLOUD) EngineMode.AUTO else it.engineMode
            it.copy(
                cooldownRemainingSeconds = safeSeconds,
                cooldownTotalSeconds = safeSeconds,
                isProcessing = false,
                engineMode = targetEngine,
                statusMessage = "COOLDOWN (${safeSeconds}s)",
                error = null
            ) 
        }
        cooldownJob = viewModelScope.launch {
            for (sec in safeSeconds downTo 1) {
                if (!isActive) break
                _uiState.update { 
                    it.copy(
                        cooldownRemainingSeconds = sec,
                        statusMessage = "COOLDOWN (${sec}s)"
                    ) 
                }
                delay(1000L)
            }
            _uiState.update { 
                it.copy(
                    cooldownRemainingSeconds = 0,
                    statusMessage = if (it.isScanning) "SCANNING ACTIVE" else "PAUSED",
                    error = if (it.error?.contains("429") == true || 
                                it.error?.contains("কোটা") == true || 
                                it.error?.contains("rate limit", ignoreCase = true) == true ||
                                it.error?.contains("rate-limit", ignoreCase = true) == true ||
                                it.error?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true) null else it.error
                ) 
            }
            Log.d(TAG, "Cooldown finished. Resuming normal scanning.")
        }
    }

    fun toggleScanning() {
        if (_uiState.value.isScanning) {
            stopScanning()
        } else {
            startScanning()
        }
    }

    fun setScanInterval(intervalMs: Long) {
        Log.d(TAG, "setScanInterval: $intervalMs ms")
        _uiState.update { current ->
            // If user sets ultra-fast interval (<2000ms) while on Cloud mode, switch to AUTO to avoid 429 quota exhaustion
            val newEngineMode = if (intervalMs < 2000L && current.engineMode == EngineMode.CLOUD) {
                EngineMode.AUTO
            } else {
                current.engineMode
            }
            current.copy(
                scanIntervalMs = intervalMs,
                engineMode = newEngineMode
            )
        }
        if (_uiState.value.isScanning) {
            startScanning() // restart loop with new interval
        }
    }

    fun setApiKey(newKey: String) {
        val rawSanitized = newKey.replace(Regex("[\\s\\t\\r\\n]+"), "").trim()
        val sanitized = if (isPlaceholderApiKey(rawSanitized)) "" else rawSanitized
        Log.d(TAG, "setApiKey: length=${sanitized.length}")
        _uiState.update { it.copy(apiKey = sanitized, error = null, cooldownRemainingSeconds = 0) }
        if (sanitized.isNotEmpty() && _uiState.value.isScanning) {
            triggerManualAnalysis()
        }
    }

    fun toggleAudioAlert() {
        _uiState.update { current ->
            current.copy(isAudioAlertEnabled = !current.isAudioAlertEnabled)
        }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(isTorchOn = !it.isTorchOn) }
    }

    fun switchCamera() {
        _uiState.update { it.copy(selectedCameraLens = if (it.selectedCameraLens == 0) 1 else 0) }
    }

    fun selectSampleScenario(scenarioIndex: Int?) {
        Log.d(TAG, "selectSampleScenario: $scenarioIndex")
        dchmEngine.reset()
        lastStableMetrics = null
        pendingCandidateMetrics = null
        pendingCandidateCount = 0
        rollingMetricHistory.clear()
        previousConfirmed5m = null
        previousConfirmed60m = null
        previousConfirmed1d = null
        lastEmittedDirection = TradeDirection.NEUTRAL
        lastEmittedSignalType = com.example.data.models.SignalType.NONE
        _uiState.update { it.copy(selectedTestImageId = scenarioIndex, error = null) }
        triggerManualAnalysis()
    }

    fun setEngineMode(mode: EngineMode) {
        Log.d(TAG, "setEngineMode: ${mode.name}")
        _uiState.update { current ->
            // If switching to Cloud mode with a very fast interval (<2000ms), adjust to safe 6.0s default to prevent 429 quota exhaustion
            val safeInterval = if (mode == EngineMode.CLOUD && current.scanIntervalMs < 2000L) {
                6000L
            } else {
                current.scanIntervalMs
            }
            current.copy(
                engineMode = mode,
                scanIntervalMs = safeInterval,
                error = null,
                cooldownRemainingSeconds = 0
            )
        }
        if (_uiState.value.isScanning) {
            triggerManualAnalysis()
        }
    }

    fun setDecisionMode(mode: com.example.data.models.DecisionMode) {
        Log.d(TAG, "setDecisionMode: ${mode.name}")
        com.example.data.analyzer.CanonicalDecisionEngine.currentDecisionMode = mode
        _uiState.update { it.copy(decisionMode = mode) }
        if (_uiState.value.isScanning) {
            triggerManualAnalysis()
        }
    }

    fun setInvestmentAmount(amount: Double) {
        val safeAmount = if (amount <= 0.0) 1.0 else amount
        Log.d(TAG, "setInvestmentAmount: $$safeAmount")
        _uiState.update { it.copy(investmentAmount = safeAmount) }
    }

    fun incrementProfitCount() {
        _uiState.update { it.copy(profitCount = it.profitCount + 1) }
    }

    fun decrementProfitCount() {
        _uiState.update { it.copy(profitCount = (it.profitCount - 1).coerceAtLeast(0)) }
    }

    fun incrementLossCount() {
        _uiState.update { it.copy(lossCount = it.lossCount + 1) }
    }

    fun decrementLossCount() {
        _uiState.update { it.copy(lossCount = (it.lossCount - 1).coerceAtLeast(0)) }
    }

    fun resetSessionPnl() {
        _uiState.update { it.copy(profitCount = 0, lossCount = 0) }
    }

    fun setTradeOutcome(outcome: com.example.data.models.TradeOutcome) {
        _uiState.update { currentState ->
            val current = currentState.currentAnalysis
            if (current == null) {
                val firstHistory = currentState.history.firstOrNull()
                if (firstHistory != null) {
                    val toggled = if (firstHistory.outcome == outcome) null else outcome
                    val updatedHistory = currentState.history.mapIndexed { idx, itm ->
                        if (idx == 0) itm.copy(outcome = toggled) else itm
                    }
                    val totalProfit = updatedHistory.count { it.outcome == com.example.data.models.TradeOutcome.PROFIT }
                    val totalLoss = updatedHistory.count { it.outcome == com.example.data.models.TradeOutcome.LOSS }
                    return@update currentState.copy(
                        history = updatedHistory,
                        profitCount = totalProfit,
                        lossCount = totalLoss
                    )
                }
                val newProfit = if (outcome == com.example.data.models.TradeOutcome.PROFIT) currentState.profitCount + 1 else currentState.profitCount
                val newLoss = if (outcome == com.example.data.models.TradeOutcome.LOSS) currentState.lossCount + 1 else currentState.lossCount
                return@update currentState.copy(
                    profitCount = newProfit,
                    lossCount = newLoss
                )
            }
            val prevOutcome = current.outcome
            // Toggle outcome if tapped again, or set new outcome
            val newOutcome = if (prevOutcome == outcome) null else outcome

            // Ensure primary matrix id is linked (e.g. D061, U030)
            val resolvedMatrixId = current.primaryMatrixId
                ?: com.example.data.matrix.Directional206MatrixEngine.evaluate(
                    current.change5mValue, current.change60mValue, null, emptyList()
                )?.id
                ?: current.canonicalDecision?.primaryMatrixId

            val updatedCurrent = current.copy(
                primaryMatrixId = resolvedMatrixId ?: current.primaryMatrixId,
                outcome = newOutcome
            )

            // Ensure history is synced and current item exists in history
            val existsInHistory = currentState.history.any { it.timestamp == current.timestamp }
            val updatedHistory = if (existsInHistory) {
                currentState.history.map { item ->
                    if (item.timestamp == current.timestamp) {
                        item.copy(
                            outcome = newOutcome,
                            primaryMatrixId = resolvedMatrixId ?: item.primaryMatrixId
                        )
                    } else {
                        item
                    }
                }
            } else if (currentState.history.isNotEmpty()) {
                // If timestamp in history slightly differs from currentAnalysis, update latest history item
                currentState.history.mapIndexed { idx, item ->
                    if (idx == 0) {
                        item.copy(
                            outcome = newOutcome,
                            primaryMatrixId = resolvedMatrixId ?: item.primaryMatrixId
                        )
                    } else {
                        item
                    }
                }
            } else {
                (listOf(updatedCurrent) + currentState.history).take(100)
            }

            val totalProfit = updatedHistory.count { it.outcome == com.example.data.models.TradeOutcome.PROFIT }
            val totalLoss = updatedHistory.count { it.outcome == com.example.data.models.TradeOutcome.LOSS }

            currentState.copy(
                currentAnalysis = updatedCurrent,
                history = updatedHistory,
                profitCount = totalProfit,
                lossCount = totalLoss
            )
        }
    }

    fun setHistoryItemOutcome(timestamp: Long, outcome: com.example.data.models.TradeOutcome) {
        _uiState.update { currentState ->
            val updatedHistory = currentState.history.map { item ->
                if (item.timestamp == timestamp) {
                    val toggled = if (item.outcome == outcome) null else outcome
                    item.copy(outcome = toggled)
                } else {
                    item
                }
            }
            val updatedCurrent = if (currentState.currentAnalysis?.timestamp == timestamp) {
                val toggled = if (currentState.currentAnalysis.outcome == outcome) null else outcome
                currentState.currentAnalysis.copy(outcome = toggled)
            } else {
                currentState.currentAnalysis
            }
            val totalProfit = updatedHistory.count { it.outcome == com.example.data.models.TradeOutcome.PROFIT }
            val totalLoss = updatedHistory.count { it.outcome == com.example.data.models.TradeOutcome.LOSS }
            currentState.copy(
                currentAnalysis = updatedCurrent,
                history = updatedHistory,
                profitCount = totalProfit,
                lossCount = totalLoss
            )
        }
    }

    fun toggleAutoTrade(enabled: Boolean? = null) {
        var isNowEnabled = false
        _uiState.update { current ->
            val newState = enabled ?: !current.isAutoTradeEnabled
            isNowEnabled = newState
            Log.i(TAG, "Master Auto-Trade toggled: $newState")
            current.copy(isAutoTradeEnabled = newState)
        }
        resetAutoTradeLock()

        if (isNowEnabled) {
            val currentAnalysis = _uiState.value.currentAnalysis
            val v5 = currentAnalysis?.change5mValue
            val v60 = currentAnalysis?.change60mValue
            val v1d = currentAnalysis?.change1dValue
            if (currentAnalysis != null && v5 != null && v60 != null) {
                evaluateAndDispatchAutoTrade(
                    analysis = currentAnalysis,
                    v5 = v5,
                    v60 = v60,
                    v1d = v1d,
                    investmentAmount = _uiState.value.investmentAmount
                )
            }
        }
    }

    fun resetAutoTradeLock() {
        lastDispatchedFingerprint = null
        lastAutoTradeDirection = null
        lastAutoTradeMatrixId = null
        lastWebSocketCommand = null
        com.example.network.TradeExecutionDispatcher.resetAutoTradeState()
        Log.i(TAG, "Auto-Trade memory lock reset")
    }

    fun toggleAntiGlitchConfirmation(enabled: Boolean? = null) {
        _uiState.update { current ->
            val newState = enabled ?: !current.isAntiGlitchConfirmationEnabled
            Log.i(TAG, "Anti-Glitch Confirmation toggled: $newState")
            current.copy(isAntiGlitchConfirmationEnabled = newState)
        }
    }

    fun toggleAdaptiveCpuProtection(enabled: Boolean? = null) {
        _uiState.update { current ->
            val newState = enabled ?: !current.isAdaptiveCpuProtectionEnabled
            Log.i(TAG, "Adaptive CPU Protection toggled: $newState")
            current.copy(isAdaptiveCpuProtectionEnabled = newState)
        }
    }

    fun triggerManualAnalysis() {
        Log.d(TAG, "triggerManualAnalysis triggered")
        activeProcessingJob?.cancel()
        activeProcessingJob = viewModelScope.launch {
            processNextFrame()
        }
    }

    private suspend fun processNextFrame() {
        if (!apiMutex.tryLock()) {
            return
        }

        try {
            val state = _uiState.value

            // 1. If Test Scenario mode is active, handle with instant algorithmic calculation or Gemini API
            if (state.selectedTestImageId != null) {
                val scenario = SampleChartGenerator.scenarios.getOrNull(state.selectedTestImageId)
                if (scenario != null) {
                    _uiState.update {
                        it.copy(
                            isProcessing = true,
                            statusMessage = "PROCESSING...",
                            frameCount = it.frameCount + 1
                        )
                    }

                    // Synthetic / Test Scenarios use instant accurate offline simulation engine
                    val result = SampleChartGenerator.generateAnalysisForScenario(scenario)

                    _uiState.update { currentState ->
                        val prev = currentState.currentAnalysis
                        val isIdentical = prev != null && isValuesIdentical(prev, result)
                        val isRealSignal = (result.direction == TradeDirection.UP || result.direction == TradeDirection.DOWN) &&
                                !result.isDeadMarket &&
                                !result.isNoTradeZone &&
                                !result.isApproximate
                        // Authoritative audio trigger is handled exclusively by ThreeTimeframePressureDashboardCard
                        val newHistory = if (isIdentical) {
                            currentState.history
                        } else {
                            (listOf(result) + currentState.history).take(100)
                        }
                        currentState.copy(
                            isProcessing = false,
                            statusMessage = if (currentState.isScanning) {
                                if (isIdentical) "SCANNING (মান অপরিবর্তিত)" else "SCANNING ACTIVE"
                            } else "PAUSED",
                            currentAnalysis = if (isIdentical) prev else result,
                            history = newHistory,
                            error = null,
                            lastChangedTimestamp = if (!isIdentical) System.currentTimeMillis() else currentState.lastChangedTimestamp
                        )
                    }
                    return
                }
            }

            // 2. Live Camera Mode
            val rawBitmap = currentFrameProvider?.invoke()
            if (rawBitmap == null) {
                _uiState.update { it.copy(isProcessing = false) }
                delay(150L)
                return
            }

            // Optional conservative downscaling (disabled by default via ENABLE_PRE_OCR_DOWNSCALE)
            val bitmapToAnalyze = if (ENABLE_PRE_OCR_DOWNSCALE && (rawBitmap.width > MAX_PRE_OCR_DIMENSION || rawBitmap.height > MAX_PRE_OCR_DIMENSION)) {
                val maxDim = MAX_PRE_OCR_DIMENSION.toFloat()
                val scale = maxDim / kotlin.math.max(rawBitmap.width, rawBitmap.height)
                val targetW = (rawBitmap.width * scale).toInt().coerceAtLeast(1)
                val targetH = (rawBitmap.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(rawBitmap, targetW, targetH, true)
            } else {
                rawBitmap
            }

            try {
                lastSuccessfulFrameTimestamp = System.currentTimeMillis()
                if (consecutiveStallRestarts > 0) {
                    consecutiveStallRestarts = 0
                }

                _uiState.update {
                    it.copy(
                        isProcessing = true,
                        frameCount = it.frameCount + 1
                    )
                }

                // 100% On-Device Deterministic Quantitative Execution (Offline-First, Zero Cloud Network Calls)
                if (_uiState.value.frameCount % 20L == 0L) {
                    Log.d(TAG, "Captured frame: ${bitmapToAnalyze.width}x${bitmapToAnalyze.height}. Running On-Device LocalQuantVisionEngine...")
                }
                val result: TradingAnalysis = LocalQuantVisionEngine.analyzeBitmap(bitmapToAnalyze, rollingMetricHistory)

                val isRateLimit = result.isQuotaExceeded ||
                        result.errorMessage?.contains("rate limit", ignoreCase = true) == true ||
                        result.errorMessage?.contains("rate-limit", ignoreCase = true) == true ||
                        result.errorMessage?.contains("429") == true ||
                        result.errorMessage?.contains("কোটা", ignoreCase = true) == true ||
                        result.errorMessage?.contains("RESOURCE_EXHAUSTED", ignoreCase = true) == true

                if (isRateLimit) {
                    startCooldown(result.retryAfterSeconds ?: 20)
                }

                _uiState.update { currentState ->
                    val prev = currentState.currentAnalysis

                    if (!result.isSuccess) {
                        consecutiveMissingCoreFrames++
                        val currentStatus = if (currentState.cooldownRemainingSeconds > 0 && currentState.engineMode == EngineMode.CLOUD) {
                            "COOLDOWN (${currentState.cooldownRemainingSeconds}s)"
                        } else if (currentState.isScanning) {
                            if (consecutiveMissingCoreFrames < 2 && prev != null && prev.isSuccess) "LIVE OCR • মান অপরিবর্তিত" else "LIVE OCR • চার্ট খুঁজছে (5m ও 60m আবশ্যক)"
                        } else {
                            "PAUSED"
                        }

                        // If 2 or more consecutive frames fail to detect (or no prev), immediately clear analysis to NEUTRAL and NO SIGNAL
                        val analysisToSet = if (consecutiveMissingCoreFrames >= 2 || prev == null) {
                            lastStableMetrics = null
                            pendingCandidateMetrics = null
                            pendingCandidateCount = 0
                            result.copy(
                                isSuccess = false,
                                isValid = false,
                                direction = TradeDirection.NEUTRAL,
                                change5m = "--",
                                change5mValue = null,
                                change60m = "--",
                                change60mValue = null,
                                change1d = "--",
                                change1dValue = null,
                                isNoTradeZone = true,
                                audioEvent = com.example.audio.AudioSignalEngine.SOUND_NONE
                            )
                        } else {
                            prev
                        }

                        return@update currentState.copy(
                            isProcessing = false,
                            statusMessage = currentStatus,
                            currentAnalysis = analysisToSet,
                            error = if (result.isQuotaExceeded) null else (if (prev == null) result.errorMessage else null)
                        )
                    }

                    // Rule 1: Extract core values: 5-min, 60-min, and 1-day
                    val v5 = parseVal(result.change5mValue, result.change5m)
                    val v60 = parseVal(result.change60mValue, result.change60m)
                    val v1dRaw = parseVal(result.change1dValue, result.change1d)

                    // Mandatory Dual-Timeframe Guard: Both 5m AND 60m must be present for a valid trading screen analysis.
                    // If either 5m or 60m is missing, treat as an incomplete/non-trading frame to prevent false entries from arbitrary scenes.
                    if (v5 == null || v60 == null) {
                        consecutiveMissingCoreFrames++
                        val currentStatus = if (currentState.cooldownRemainingSeconds > 0 && currentState.engineMode == EngineMode.CLOUD) {
                            "COOLDOWN (${currentState.cooldownRemainingSeconds}s)"
                        } else if (currentState.isScanning) {
                            if (consecutiveMissingCoreFrames < 2 && prev != null && prev.isSuccess) "LIVE OCR • মান অপরিবর্তিত" else "LIVE OCR • চার্ট খুঁজছে (5m ও 60m আবশ্যক)"
                        } else {
                            "PAUSED"
                        }
                        val invalidResult = TradingAnalysis(
                            isSuccess = false,
                            isValid = false,
                            errorMessage = "স্ক্রিনে 5m ও 60m পার্সেন্টেজ ডিটেক্ট হয়নি। ক্যামেরা চার্টে ফোকাস করুন।",
                            rawResponse = "No 5m & 60m detected on screen",
                            direction = TradeDirection.NEUTRAL,
                            upPercentage = 50.0,
                            downPercentage = 50.0,
                            calculatedPercentage = 50.0,
                            change5m = "--",
                            change5mValue = null,
                            change60m = "--",
                            change60mValue = null,
                            change1d = "--",
                            change1dValue = null,
                            dataQuality = com.example.data.models.DataQualityState.UNAVAILABLE,
                            isNoTradeZone = true,
                            engineSource = result.engineSource,
                            audioEvent = com.example.audio.AudioSignalEngine.SOUND_NONE
                        )

                        val analysisToSet = if (consecutiveMissingCoreFrames >= 2 || prev == null) {
                            lastStableMetrics = null
                            pendingCandidateMetrics = null
                            pendingCandidateCount = 0
                            invalidResult
                        } else {
                            prev
                        }

                        return@update currentState.copy(
                            isProcessing = false,
                            statusMessage = currentStatus,
                            currentAnalysis = analysisToSet,
                            error = if (prev == null) invalidResult.errorMessage else null
                        )
                    }

                    // Reset consecutive missing counter since valid 5m and 60m are actively detected on screen
                    consecutiveMissingCoreFrames = 0

                    // Preserve previous valid 1-day value ONLY if current OCR frame genuinely missed it (missing 1D),
                    // NOT when current frame contained an approximate (~ / ≈) or explicitly rejected 1-day value.
                    val v1d = if (result.is1dExplicitlyRejected) {
                        null
                    } else {
                        v1dRaw ?: parseVal(prev?.change1dValue, prev?.change1d)
                    }
                    val currentMetrics = StableMetrics(val5m = v5, val60m = v60, val1d = v1d)

                    // DCHM-Engine Hook: Real-Time 5m & 60m Instant Delta & Matrix Validation
                    if (v5 != null && v60 != null) {
                        dchmEngine.processInstantDelta(current5mPercent = v5, current60mPercent = v60)
                    }

                    // Rule 2 & 3: Compare each new scan with the previous preserved stable values (Exact Snapshot Comparison)
                    val isCoreUnchanged = lastStableMetrics != null &&
                            isExactSameNumber(lastStableMetrics?.val5m, v5) &&
                            isExactSameNumber(lastStableMetrics?.val60m, v60)
                    val is1dChanged = !isExactSameNumber(lastStableMetrics?.val1d, v1d)

                    if (isCoreUnchanged) {
                        consecutiveUnchangedFrames++
                        if (is1dChanged && prev != null) {
                            // 1-day metric and context text update with audio alert on change
                            val str1d = v1d?.let { com.example.data.analyzer.TradingOutputParser.formatWithSign(it) } ?: "--"
                            val updatedBehavior = com.example.data.analyzer.TradingOutputParser.classifyMovementBehavior(
                                val5m = v5 ?: 0.0,
                                val60m = v60 ?: 0.0,
                                val1d = v1d,
                                history = rollingMetricHistory,
                                isDeadMarket = prev.isDeadMarket,
                                isNoTradeZone = prev.isNoTradeZone
                            )
                            // Authoritative audio trigger is handled exclusively by TradingDashboard
                            val updatedAnalysis = prev.copy(
                                change1d = str1d,
                                change1dValue = v1d,
                                audioEvent = com.example.audio.AudioSignalEngine.SOUND_NONE,
                                behaviorCode = updatedBehavior.code,
                                behaviorTitle = updatedBehavior.title,
                                behaviorDescription = updatedBehavior.subtitle,
                                behaviorTags = updatedBehavior.tags,
                                isWarningOnly = updatedBehavior.isWarningOnly,
                                dailyContext = updatedBehavior.dailyContext.name,
                                is1dExplicitlyRejected = result.is1dExplicitlyRejected,
                                confirmationStage = updatedBehavior.confirmationStage,
                                nextMovementBias = updatedBehavior.nextMovementBias
                            )
                            lastStableMetrics = currentMetrics
                            val currentStatus = if (currentState.cooldownRemainingSeconds > 0 && currentState.engineMode == EngineMode.CLOUD) {
                                "COOLDOWN (${currentState.cooldownRemainingSeconds}s)"
                            } else if (currentState.isScanning) {
                                "LIVE OCR • ১ দিনের প্রেক্ষাপট আপডেট"
                            } else {
                                "PAUSED"
                            }
                            return@update currentState.copy(
                                isProcessing = false,
                                statusMessage = currentStatus,
                                currentAnalysis = updatedAnalysis,
                                error = null,
                                lastChangedTimestamp = System.currentTimeMillis()
                            )
                        }

                        // Rule 8: If old value returns or matches, cancel any pending new value and keep dashboard completely unchanged
                        pendingCandidateMetrics = null
                        pendingCandidateCount = 0

                        // Immediate Auto-Trade evaluation: Ensures trades are dispatched with 0ms latency even when numbers are stable
                        evaluateAndDispatchAutoTrade(
                            analysis = prev ?: result,
                            v5 = v5,
                            v60 = v60,
                            v1d = v1d,
                            investmentAmount = currentState.investmentAmount
                        )

                        // If returning from an approximate frame to confirmed stable values with a verified frame:
                        // restore the verified state on currentAnalysis without triggering duplicate audio
                        if (!result.isApproximate && prev?.isApproximate == true) {
                            val restoredVerified = result.copy(
                                audioEvent = com.example.audio.AudioSignalEngine.SOUND_NONE, // Debounce contract: no duplicate audio!
                                hasValueChanged = false,
                                dataQuality = com.example.data.models.DataQualityState.VERIFIED,
                                isApproximate = false
                            )

                            return@update currentState.copy(
                                isProcessing = false,
                                statusMessage = if (currentState.isScanning) "LIVE OCR • নিশ্চিত মান সক্রিয়" else "PAUSED",
                                currentAnalysis = restoredVerified,
                                error = null
                            )
                        }

                        val currentStatus = if (currentState.cooldownRemainingSeconds > 0 && currentState.engineMode == EngineMode.CLOUD) {
                            "COOLDOWN (${currentState.cooldownRemainingSeconds}s)"
                        } else if (currentState.isScanning) {
                            "LIVE OCR • মান অপরিবর্তিত"
                        } else {
                            "PAUSED"
                        }

                        // Zero Ghost Recalculation: No new calculations, no history duplicates, no alert sounds, no timestamp changes
                        return@update currentState.copy(
                            isProcessing = false,
                            statusMessage = currentStatus,
                            currentAnalysis = prev,
                            error = null
                        )
                    }

                    consecutiveUnchangedFrames = 0
                    val isCandidateValidAndVerified = result.isValid && result.isSuccess
                    val isInitialScan = (lastStableMetrics == null || prev == null)
                    val isLiveScanMode = currentState.isScanning && currentState.selectedTestImageId == null && currentState.engineMode == EngineMode.LOCAL

                    // Check if current candidate metrics match an explicitly verified (✓) rule (e.g. D031)
                    val candidateRuleId = com.example.data.matrix.Authorized106MatrixEngine.evaluate(v5, v60, v1d, rollingMetricHistory)?.id
                        ?: com.example.data.matrix.Directional206MatrixEngine.evaluate(v5, v60, null, emptyList())?.id
                        ?: result.primaryMatrixId
                        ?: result.canonicalDecision?.primaryMatrixId
                    val candidateCleanId = candidateRuleId?.replace("[", "")?.replace("]", "")?.trim()
                    val isCandidateVerifiedRule = candidateCleanId?.let { com.example.data.matrix.UserRuleRegistry.isRuleVerified(it) } ?: false

                    // Verified rules (✓) are mandatory instant commands: bypass multi-frame staging for 0ms zero-delay execution!
                    val shouldCheckAntiGlitch = currentState.isAntiGlitchConfirmationEnabled &&
                            isLiveScanMode &&
                            !isInitialScan &&
                            isCandidateValidAndVerified &&
                            !isCandidateVerifiedRule

                    if (shouldCheckAntiGlitch) {
                        val matchesPendingCandidate = pendingCandidateMetrics != null &&
                                isExactSameNumber(pendingCandidateMetrics?.val5m, v5) &&
                                isExactSameNumber(pendingCandidateMetrics?.val60m, v60) &&
                                isExactSameNumber(pendingCandidateMetrics?.val1d, v1d)

                        if (!matchesPendingCandidate) {
                            // Frame 1 of a new metric value: stage as candidate and await consecutive 2nd frame confirmation
                            pendingCandidateMetrics = currentMetrics
                            pendingCandidateCount = 1
                            Log.d(TAG, "Anti-Glitch: Frame 1 candidate staged (5m=$v5, 60m=$v60, 1d=$v1d). Awaiting frame 2 confirmation.")
                            return@update currentState.copy(
                                isProcessing = false,
                                statusMessage = "LIVE OCR • মান যাচাই হচ্ছে (Frame 1/2)...",
                                currentAnalysis = prev,
                                error = null
                            )
                        } else {
                            // Frame 2 confirmed! Consecutive agreement verified across consecutive frames
                            Log.d(TAG, "Anti-Glitch: Frame 2 confirmed! Consecutive agreement verified: (5m=$v5, 60m=$v60, 1d=$v1d).")
                            pendingCandidateMetrics = null
                            pendingCandidateCount = 0
                        }
                    } else {
                        pendingCandidateMetrics = null
                        pendingCandidateCount = 0
                    }

                    if (isCandidateValidAndVerified) {
                        lastStableMetrics = currentMetrics
                        previousConfirmed5m = v5
                        previousConfirmed60m = v60
                        previousConfirmed1d = v1d
                        pendingCandidateMetrics = null
                        pendingCandidateCount = 0
                        // Append confirmed snapshot to rolling history in standard chronological order (history[0] = oldest, history.last() = newest)
                        val snapshotNet = result.netSumValue ?: ((v5 ?: 0.0) + (v60 ?: 0.0))
                        val snapshot = com.example.data.models.MetricSnapshot(
                            val5m = v5 ?: 0.0,
                            val60m = v60 ?: 0.0,
                            val1d = v1d,
                            netSum = snapshotNet,
                            timestamp = System.currentTimeMillis()
                        )
                        rollingMetricHistory.add(snapshot)
                        if (rollingMetricHistory.size > 8) {
                            rollingMetricHistory.removeAt(0)
                        }
                    }

                    val determinedSignalType = result.signalType

                    val has5mChanged = !isExactSameNumber(prev?.change5mValue, v5)
                    val has60mChanged = !isExactSameNumber(prev?.change60mValue, v60)
                    val has1dChanged = !isExactSameNumber(prev?.change1dValue, v1d)
                    val isAnyPercentageChanged = has5mChanged || has60mChanged || has1dChanged

                    val isRealSignal = (result.direction == TradeDirection.UP || result.direction == TradeDirection.DOWN) &&
                            !result.isNoTradeZone &&
                            !result.isWarningOnly

                    // 1. Authoritative audio trigger is handled exclusively by TradingDashboard UI
                    if (result.direction == TradeDirection.NEUTRAL || result.isNoTradeZone || result.isWarningOnly) {
                        lastEmittedDirection = TradeDirection.NEUTRAL
                        lastEmittedSignalType = com.example.data.models.SignalType.NONE
                    }

                    // 2. Canonical Auto-Trade Dispatch (User Mandate: Only dispatch if verified checkmark (✓) is present)
                    evaluateAndDispatchAutoTrade(
                        analysis = result,
                        v5 = v5,
                        v60 = v60,
                        v1d = v1d,
                        investmentAmount = currentState.investmentAmount
                    )

                    val str5m = result.change5m.ifEmpty { v5?.let { com.example.data.analyzer.TradingOutputParser.formatWithSign(it) } ?: "--" }
                    val str60m = result.change60m.ifEmpty { v60?.let { com.example.data.analyzer.TradingOutputParser.formatWithSign(it) } ?: "--" }
                    val str1d = v1d?.let { com.example.data.analyzer.TradingOutputParser.formatWithSign(it) } ?: "--"
                    val strNet = result.netSum.ifEmpty { com.example.data.analyzer.TradingOutputParser.formatWithSign(result.netSumValue) }

                    val finalResult = result.copy(
                        change5m = str5m,
                        change5mValue = v5,
                        change60m = str60m,
                        change60mValue = v60,
                        change1d = str1d,
                        change1dValue = v1d,
                        netSum = strNet,
                        audioEvent = com.example.audio.AudioSignalEngine.SOUND_NONE,
                        hasValueChanged = !isInitialScan && isAnyPercentageChanged
                    )

                    val newHistory = if (isCandidateValidAndVerified) {
                        (listOf(finalResult) + currentState.history).take(100)
                    } else {
                        currentState.history
                    }

                    val currentStatus = if (currentState.cooldownRemainingSeconds > 0 && currentState.engineMode == EngineMode.CLOUD) {
                        "COOLDOWN (${currentState.cooldownRemainingSeconds}s)"
                    } else if (currentState.isScanning) {
                        "LIVE OCR • নতুন মান সক্রিয়"
                    } else {
                        "PAUSED"
                    }

                    currentState.copy(
                        isProcessing = false,
                        statusMessage = currentStatus,
                        currentAnalysis = finalResult,
                        history = newHistory,
                        error = null,
                        lastChangedTimestamp = if (isAnyPercentageChanged) System.currentTimeMillis() else currentState.lastChangedTimestamp
                    )
                }

                if (!result.isSuccess || result.change5mValue == null || result.change60mValue == null) {
                    // Frame guard & backoff to prevent spin-loop when camera is unready, blank, or no chart is detected
                    delay(200L)
                }
            } finally {
                // Recycle downscaled temporary bitmap if created
                if (bitmapToAnalyze != rawBitmap) {
                    try {
                        if (!bitmapToAnalyze.isRecycled) {
                            bitmapToAnalyze.recycle()
                        }
                    } catch (ignored: Exception) {}
                }
                // Recycle temporary downscaled bitmap if one was created
                try {
                    if (bitmapToAnalyze != rawBitmap && !bitmapToAnalyze.isRecycled) {
                        bitmapToAnalyze.recycle()
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Error recycling temporary bitmap: ${e.message}")
                }
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) {
                Log.d(TAG, "processNextFrame coroutine cancelled normally")
                throw t
            }
            val errStr = "Error: ${t.localizedMessage ?: t.message ?: "Unknown Exception"}"
            Log.e(TAG, "Exception during processNextFrame: $errStr", t)
            val isRateLimit = errStr.contains("rate limit", ignoreCase = true) ||
                    errStr.contains("rate-limit", ignoreCase = true) ||
                    errStr.contains("429") ||
                    errStr.contains("RESOURCE_EXHAUSTED", ignoreCase = true)
            if (isRateLimit) {
                startCooldown(20)
            }
            _uiState.update {
                it.copy(
                    isProcessing = false,
                    statusMessage = if (isRateLimit) "COOLDOWN (20s)" else (if (it.isScanning) "LIVE OCR • মান অপরিবর্তিত" else "PAUSED"),
                    error = if (isRateLimit) null else (if (it.currentAnalysis == null) errStr else it.error)
                )
            }
        } finally {
            apiMutex.unlock()
        }
    }

    private fun parseVal(value: Double?, text: String?): Double? {
        if (value != null && com.example.data.analyzer.TradingOutputParser.isValidMetricValue(value)) return value
        if (text == null || text == "--" || text.isBlank() || text.contains("NaN", ignoreCase = true) || text.contains("Infinity", ignoreCase = true)) return null
        val clean = com.example.data.analyzer.TradingOutputParser.normalizeBengaliAndDashes(text)
            .replace("%", "").replace("+", "").replace(" ", "").trim()
        val parsed = clean.toDoubleOrNull()
        return if (parsed != null && com.example.data.analyzer.TradingOutputParser.isValidMetricValue(parsed)) parsed else null
    }

    private fun isExactSameNumber(v1: Double?, v2: Double?): Boolean {
        if (v1 == null && v2 == null) return true
        if (v1 == null || v2 == null) return false
        // Compare with 2-decimal precision (e.g. +0.05% vs +0.05%) to eliminate floating point/OCR micro-jitter
        return kotlin.math.abs(v1 - v2) < 0.005
    }

    private fun isSameMetrics(m1: StableMetrics?, m2: StableMetrics?): Boolean {
        if (m1 == null || m2 == null) return false
        val same5 = isExactSameNumber(m1.val5m, m2.val5m)
        val same60 = isExactSameNumber(m1.val60m, m2.val60m)
        val same1d = isExactSameNumber(m1.val1d, m2.val1d)
        return same5 && same60 && same1d
    }

    private fun isValuesIdentical(prev: TradingAnalysis, next: TradingAnalysis): Boolean {
        if (!prev.isSuccess || !next.isSuccess) return false

        val v5Prev = parseVal(prev.change5mValue, prev.change5m)
        val v5Next = parseVal(next.change5mValue, next.change5m)

        val v60Prev = parseVal(prev.change60mValue, prev.change60m)
        val v60Next = parseVal(next.change60mValue, next.change60m)

        if (v5Prev == null || v60Prev == null || v5Next == null || v60Next == null) return false

        val m1 = StableMetrics(v5Prev, v60Prev, parseVal(prev.change1dValue, prev.change1d))
        val m2 = StableMetrics(v5Next, v60Next, parseVal(next.change1dValue, next.change1d))
        return isSameMetrics(m1, m2)
    }

    /**
     * Authoritative Instant Auto-Trade Dispatcher.
     * Guaranteed to dispatch in 0ms without latency or artificial cooldowns when the Verified checkmark (✓)
     * is present (either via automated deterministic data quality verification OR explicit user rule registry verification).
     * Synchronized 100% with UI verification tick mark logic in TradingDashboard.
     */
    private fun evaluateAndDispatchAutoTrade(
        analysis: TradingAnalysis,
        v5: Double?,
        v60: Double?,
        v1d: Double?,
        investmentAmount: Double
    ) {
        // 0. Master Auto-Trade Switch Guard:
        // Must be explicitly enabled by user via dashboard switch (default is OFF)
        if (!_uiState.value.isAutoTradeEnabled) {
            return
        }

        // 1. Strict Screen Detection Guard:
        // Frame must have been successfully recognized by OCR with optical clarity (no missing metrics or failed frames)
        if (!analysis.isSuccess || !analysis.isValid || v5 == null || v60 == null) {
            return
        }

        val mathSignal = com.example.data.matrix.Authorized106MatrixEngine.evaluate(
            val5m = v5,
            val60m = v60,
            val1d = v1d,
            history = rollingMetricHistory
        ) ?: com.example.data.matrix.Directional206MatrixEngine.evaluate(
            val5m = v5,
            val60m = v60,
            val1d = null,
            history = emptyList()
        )?.let { match ->
            com.example.data.matrix.Authorized106MatrixEngine.Matrix106Match(
                id = match.id,
                direction = match.direction,
                outputCode = match.outputCode,
                title = match.title,
                conditionDescription = match.conditionDescription,
                priority = match.priority
            )
        }

        // Unify rule identification with UI dashboard: check mathSignal, analysis.primaryMatrixId, canonicalDecision
        val candidateRuleId = mathSignal?.id
            ?: analysis.primaryMatrixId
            ?: analysis.canonicalDecision?.primaryMatrixId

        if (candidateRuleId.isNullOrBlank()) {
            return
        }

        val cleanRuleId = candidateRuleId.replace("[", "").replace("]", "").uppercase().trim()

        // 100% MANDATORY STRICT USER MANDATE (মেট্রিক সেকশনে ভেরিফাইকৃত ও টিক চিহ্নযুক্ত রুলস):
        // Only verified and ticked (✓) metric numbers in UserRuleRegistry will take auto entry as soon as detected on screen!
        // Outside of verified rules, NO entry will be taken! This permanent policy cannot be changed without explicit user permission.
        val isRuleUserVerified = com.example.data.matrix.UserRuleRegistry.isRuleVerified(cleanRuleId)
        if (!isRuleUserVerified) {
            // Rule is not verified or user unticked it via the dashboard Verify button: strictly block auto entry!
            if (lastAutoTradeMatrixId != null && lastAutoTradeMatrixId != cleanRuleId) {
                lastAutoTradeMatrixId = null
                lastDispatchedFingerprint = null
            }
            return
        }

        val isCancelled = analysis.canonicalDecision?.cancelledMatrixIds?.contains(cleanRuleId) == true
        if (isCancelled) {
            return
        }

        // Resolve effective trade direction, honoring any user direction override in UserRuleRegistry
        val overrideDir = com.example.data.matrix.UserRuleRegistry.getRuleOverride(cleanRuleId)
        val effectiveDir = overrideDir
            ?: mathSignal?.direction
            ?: if (cleanRuleId.startsWith("U")) TradeDirection.UP
            else if (cleanRuleId.startsWith("D")) TradeDirection.DOWN
            else analysis.direction

        if (effectiveDir != TradeDirection.UP && effectiveDir != TradeDirection.DOWN) {
            return
        }

        val tradeSide = if (effectiveDir == TradeDirection.UP) "BUY" else "SELL"
        val ruleTitle = mathSignal?.title ?: analysis.primaryMatrixTitle ?: "Rule $cleanRuleId"
        val fp = "SIG:${cleanRuleId}:${tradeSide}"
        val now = System.currentTimeMillis()

        // STRICT SINGLE-ENTRY BURST CHECK:
        // Prevents multi-firing (e.g. 100 orders/sec) during continuous 10ms camera OCR frames on the exact same static display.
        // Once 2000ms have elapsed OR if the rule/direction changes, verified rules dispatch instantly.
        val isBurstDuplicate = (cleanRuleId == lastAutoTradeMatrixId) &&
                (effectiveDir == lastAutoTradeDirection) &&
                ((now - lastAutoTradeDispatchedAtMs) < 2000L)

        if (!isBurstDuplicate) {
            val decisionToDispatch = com.example.data.models.CanonicalDecision(
                decisionId = "AUTO_${cleanRuleId}_${now}",
                direction = effectiveDir,
                side = tradeSide,
                upPercentage = if (effectiveDir == TradeDirection.UP) 80.0 else 20.0,
                downPercentage = if (effectiveDir == TradeDirection.DOWN) 80.0 else 20.0,
                strength = 85.0,
                dataQuality = com.example.data.models.DataQualityState.VERIFIED,
                fingerprint = fp,
                explanation = "Verified Math Rule [$cleanRuleId] ✔ Mandatory Command",
                executionEligibility = true,
                primaryMatrixId = cleanRuleId,
                primaryMatrixTitle = ruleTitle
            )

            val availableTfs = mutableSetOf("5m", "60m")
            if (v1d != null) availableTfs.add("1d")

            val dispatched = com.example.network.TradeExecutionDispatcher.dispatchDecision(
                decision = decisionToDispatch,
                investmentAmount = investmentAmount,
                availableTimeframes = availableTfs
            )

            if (dispatched) {
                lastDispatchedFingerprint = fp
                lastAutoTradeDispatchedAtMs = now
                lastAutoTradeDirection = effectiveDir
                lastAutoTradeMatrixId = cleanRuleId
                lastWebSocketCommand = if (effectiveDir == TradeDirection.UP) "CLICK_BUY" else "CLICK_SELL"
                Log.i(TAG, "MANDATORY AUTO-TRADE EXECUTED: [$cleanRuleId] ✔ -> $tradeSide via WebSocket/Webhook | Burst single-entry locked for 2s")
            }
        }

        // SMART MEMORY SHIELD:
        // Do NOT wipe the trade lock on temporary camera blur, jitter, or neutral frames.
        // The lock stays firmly in place so that when the camera refocused on the same chart,
        // it remembers this trade has already been taken and does NOT send duplicate orders.
    }
}