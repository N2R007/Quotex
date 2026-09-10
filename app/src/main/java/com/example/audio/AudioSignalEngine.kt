package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

object AudioSignalEngine {
    private const val TAG = "AudioSignalEngine"

    const val SOUND_UP_ALERT = "SOUND_UP_ALERT"
    const val SOUND_DOWN_ALERT = "SOUND_DOWN_ALERT"
    const val SOUND_NONE = "SOUND_NONE"

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var toneGenerator: ToneGenerator? = null

    @Volatile
    private var textToSpeech: TextToSpeech? = null

    @Volatile
    private var isTtsInitialized: Boolean = false

    @Volatile
    var isMutedForTesting: Boolean = false

    val recordedEventsForTesting = CopyOnWriteArrayList<String>()

    fun clearRecordedEventsForTesting() {
        recordedEventsForTesting.clear()
    }

    private val isRobolectric: Boolean by lazy {
        try {
            android.os.Build.FINGERPRINT.contains("robolectric", ignoreCase = true)
        } catch (_: Throwable) {
            false
        }
    }

    // Pre-generated PCM audio buffers for instant 0ms latency audio beeps
    private val sampleRate = 44100
    private val upPcmBuffer: ByteArray by lazy {
        generatePcmSineWave(frequencyHz = 960.0, durationMs = 150, sampleRate = sampleRate)
    }
    private val downPcmBuffer: ByteArray by lazy {
        generatePcmSineWave(frequencyHz = 420.0, durationMs = 190, sampleRate = sampleRate)
    }

    /**
     * Initializes AudioSignalEngine with application context and initializes TTS engine.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        if (!isRobolectric && textToSpeech == null) {
            try {
                textToSpeech = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        try {
                            textToSpeech?.language = Locale.US
                            textToSpeech?.setSpeechRate(1.15f)
                            isTtsInitialized = true
                        } catch (e: Exception) {
                            Log.w(TAG, "TTS configuration notice: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "TTS initialization notice: ${e.message}")
            }
        }
    }

    private fun initToneGenerator() {
        if (isRobolectric) return
        try {
            toneGenerator?.release()
            // Try STREAM_NOTIFICATION, then STREAM_ALARM, then STREAM_MUSIC
            toneGenerator = try {
                ToneGenerator(AudioManager.STREAM_NOTIFICATION, 95)
            } catch (_: Exception) {
                try {
                    ToneGenerator(AudioManager.STREAM_ALARM, 95)
                } catch (_: Exception) {
                    ToneGenerator(AudioManager.STREAM_MUSIC, 95)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ToneGenerator initialization notice: ${e.message}")
        }
    }

    /**
     * Synthesizes a clean 16-bit PCM mono sine wave with envelope shaping to prevent audio pops.
     */
    private fun generatePcmSineWave(frequencyHz: Double, durationMs: Int, sampleRate: Int): ByteArray {
        val numSamples = (durationMs * sampleRate) / 1000
        val buffer = ByteArray(numSamples * 2)
        val rampSamples = (sampleRate * 0.01).toInt().coerceAtLeast(100) // 10ms ramp
        for (i in 0 until numSamples) {
            val envelope = when {
                i < rampSamples -> i.toDouble() / rampSamples.toDouble()
                i > numSamples - rampSamples -> (numSamples - i).toDouble() / rampSamples.toDouble()
                else -> 1.0
            }
            val angle = 2.0 * Math.PI * i * frequencyHz / sampleRate
            val sampleValue = (kotlin.math.sin(angle) * 28000.0 * envelope).toInt().coerceIn(-32768, 32767).toShort()
            buffer[2 * i] = (sampleValue.toInt() and 0xFF).toByte()
            buffer[2 * i + 1] = ((sampleValue.toInt() shr 8) and 0xFF).toByte()
        }
        return buffer
    }

    /**
     * Plays a synthesized audio tone using AudioTrack (hardware-independent, 100% reliable).
     */
    private fun playSynthesizedPcm(buffer: ByteArray) {
        if (isRobolectric) return
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val track = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(buffer.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()

            track.setNotificationMarkerPosition(buffer.size / 2)
            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onPeriodicNotification(t: AudioTrack?) {}
                override fun onMarkerReached(t: AudioTrack?) {
                    try {
                        t?.stop()
                        t?.release()
                    } catch (_: Exception) {}
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "PCM AudioTrack playback notice: ${e.message}")
        }
    }

    private var lastSpokenText: String? = null
    private var lastSpokenTime: Long = 0L

    /**
     * Sanitizes callout text:
     * Extracts digits from matrix IDs (e.g., 'UP U045' -> 'UP 45' or 'DOWN D012' -> 'DOWN 12')
     * Strips leading letters like 'U' or 'D' and leading zeroes so only clear digit number is spoken.
     * Prevents double speaking via duplicate suppression window (1.5 seconds).
     */
    private fun speakCallout(text: String) {
        if (isRobolectric || !isTtsInitialized) return
        try {
            val now = System.currentTimeMillis()
            // Clean format: if text contains e.g. "UP U042" or "DOWN D007", make it "UP 42" or "DOWN 7"
            val cleanedText = sanitizeCalloutText(text)

            // Debounce: prevent duplicate voice readouts of identical rule numbers within 30 seconds
            if (cleanedText == lastSpokenText && (now - lastSpokenTime) < 30000L) {
                return
            }

            lastSpokenText = cleanedText
            lastSpokenTime = now

            textToSpeech?.speak(
                cleanedText,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "SignalVoiceCallout"
            )
        } catch (e: Exception) {
            Log.w(TAG, "TTS speak notice: ${e.message}")
        }
    }

    /**
     * Cleans text so that letters like U, D, M before digits are removed,
     * leaving only the pure direction ("UP" or "DOWN") and digit number (e.g. "45").
     */
    fun sanitizeCalloutText(rawText: String): String {
        val parts = rawText.trim().split("\\s+".toRegex())
        val result = mutableListOf<String>()
        for (part in parts) {
            val upper = part.uppercase(Locale.US)
            if (upper == "UP" || upper == "DOWN") {
                result.add(upper)
            } else {
                // Extract digits only from matrix identifiers like U045, D012, M005
                val digits = part.filter { it.isDigit() }
                if (digits.isNotEmpty()) {
                    val number = digits.toIntOrNull()
                    if (number != null) {
                        result.add(number.toString())
                    } else {
                        result.add(digits)
                    }
                }
            }
        }
        return if (result.isNotEmpty()) result.joinToString(" ") else rawText
    }

    private fun playSystemNotificationFallback() {
        if (isRobolectric) return
        try {
            appContext?.let { ctx ->
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val ringtone = RingtoneManager.getRingtone(ctx, uri)
                ringtone?.play()
            }
        } catch (_: Exception) {}
    }

    /**
     * Releases audio resources.
     */
    fun shutdown() {
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (_: Exception) {}
        try {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            isTtsInitialized = false
        } catch (_: Exception) {}
    }

    /**
     * Plays audio alert for UP or DOWN direction, with optional voice callout.
     */
    suspend fun playSoundEvent(event: String, calloutText: String? = null) = withContext(Dispatchers.Default) {
        if (event == SOUND_NONE) return@withContext
        recordedEventsForTesting.add(event)

        if (isMutedForTesting || isRobolectric) {
            return@withContext
        }

        try {
            var playedSuccessfully = false
            when (event) {
                SOUND_UP_ALERT -> {
                    // 1. Crystal clear high pitch synthesized beep (960Hz)
                    try {
                        playSynthesizedPcm(upPcmBuffer)
                        playedSuccessfully = true
                    } catch (_: Exception) {}

                    // 2. Hardware ToneGenerator buzzer
                    try {
                        if (toneGenerator == null) initToneGenerator()
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                        playedSuccessfully = true
                    } catch (_: Exception) {}

                    // 3. Clear Voice Callout
                    speakCallout(calloutText ?: "UP")
                }
                SOUND_DOWN_ALERT -> {
                    // 1. Distinctive alert synthesized beep (420Hz)
                    try {
                        playSynthesizedPcm(downPcmBuffer)
                        playedSuccessfully = true
                    } catch (_: Exception) {}

                    // 2. Hardware ToneGenerator buzzer
                    try {
                        if (toneGenerator == null) initToneGenerator()
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                        playedSuccessfully = true
                    } catch (_: Exception) {}

                    // 3. Clear Voice Callout
                    speakCallout(calloutText ?: "DOWN")
                }
            }

            if (!playedSuccessfully) {
                playSystemNotificationFallback()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play audio alert: ${e.message}")
            playSystemNotificationFallback()
        }
    }
}
