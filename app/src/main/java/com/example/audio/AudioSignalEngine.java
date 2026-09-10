package com.example.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import com.example.data.analyzer.CanonicalDecisionEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import kotlin.Lazy;
import kotlin.LazyKt;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import kotlin.text.Regex;
import kotlin.text.StringsKt;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.Dispatchers;

/* compiled from: AudioSignalEngine.kt */
@Metadata(d1 = {"\u0000\\\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0012\n\u0002\b\u000b\n\u0002\u0010\u0006\n\u0002\b\u0005\n\u0002\u0010\t\n\u0002\b\u000b\bÇ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u0006\u0010\u0019\u001a\u00020\u001aJ\u000e\u0010(\u001a\u00020\u001a2\u0006\u0010)\u001a\u00020\nJ\b\u0010*\u001a\u00020\u001aH\u0002J \u0010+\u001a\u00020!2\u0006\u0010,\u001a\u00020-2\u0006\u0010.\u001a\u00020\u001f2\u0006\u0010\u001e\u001a\u00020\u001fH\u0002J\u0010\u0010/\u001a\u00020\u001a2\u0006\u00100\u001a\u00020!H\u0002J\u0010\u00104\u001a\u00020\u001a2\u0006\u00105\u001a\u00020\u0005H\u0002J\u000e\u00106\u001a\u00020\u00052\u0006\u00107\u001a\u00020\u0005J\b\u00108\u001a\u00020\u001aH\u0002J\u0006\u00109\u001a\u00020\u001aJ\"\u0010:\u001a\u00020\u001a2\u0006\u0010;\u001a\u00020\u00052\n\b\u0002\u0010<\u001a\u0004\u0018\u00010\u0005H\u0086@¢\u0006\u0002\u0010=R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000R\u0010\u0010\t\u001a\u0004\u0018\u00010\nX\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\u000b\u001a\u0004\u0018\u00010\fX\u0082\u000e¢\u0006\u0002\n\u0000R\u0010\u0010\r\u001a\u0004\u0018\u00010\u000eX\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u000f\u001a\u00020\u0010X\u0082\u000e¢\u0006\u0002\n\u0000R\u001a\u0010\u0011\u001a\u00020\u0010X\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\u0011\u0010\u0012\"\u0004\b\u0013\u0010\u0014R\u0017\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u00050\u0016¢\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u001b\u0010\u001b\u001a\u00020\u00108BX\u0082\u0084\u0002¢\u0006\f\n\u0004\b\u001c\u0010\u001d\u001a\u0004\b\u001b\u0010\u0012R\u000e\u0010\u001e\u001a\u00020\u001fX\u0082D¢\u0006\u0002\n\u0000R\u001b\u0010 \u001a\u00020!8BX\u0082\u0084\u0002¢\u0006\f\n\u0004\b$\u0010\u001d\u001a\u0004\b\"\u0010#R\u001b\u0010%\u001a\u00020!8BX\u0082\u0084\u0002¢\u0006\f\n\u0004\b'\u0010\u001d\u001a\u0004\b&\u0010#R\u0010\u00101\u001a\u0004\u0018\u00010\u0005X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u00102\u001a\u000203X\u0082\u000e¢\u0006\u0002\n\u0000¨\u0006>"}, d2 = {"Lcom/example/audio/AudioSignalEngine;", "", "<init>", "()V", "TAG", "", AudioSignalEngine.SOUND_UP_ALERT, AudioSignalEngine.SOUND_DOWN_ALERT, AudioSignalEngine.SOUND_NONE, "appContext", "Landroid/content/Context;", "toneGenerator", "Landroid/media/ToneGenerator;", "textToSpeech", "Landroid/speech/tts/TextToSpeech;", "isTtsInitialized", "", "isMutedForTesting", "()Z", "setMutedForTesting", "(Z)V", "recordedEventsForTesting", "Ljava/util/concurrent/CopyOnWriteArrayList;", "getRecordedEventsForTesting", "()Ljava/util/concurrent/CopyOnWriteArrayList;", "clearRecordedEventsForTesting", "", "isRobolectric", "isRobolectric$delegate", "Lkotlin/Lazy;", "sampleRate", "", "upPcmBuffer", "", "getUpPcmBuffer", "()[B", "upPcmBuffer$delegate", "downPcmBuffer", "getDownPcmBuffer", "downPcmBuffer$delegate", "init", "context", "initToneGenerator", "generatePcmSineWave", "frequencyHz", "", "durationMs", "playSynthesizedPcm", "buffer", "lastSpokenText", "lastSpokenTime", "", "speakCallout", "text", "sanitizeCalloutText", "rawText", "playSystemNotificationFallback", "shutdown", "playSoundEvent", "event", "calloutText", "(Ljava/lang/String;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app"}, k = CanonicalDecisionEngine.DEFAULT_REQUIRED_CONFIRMATIONS, mv = {2, 2, 0}, xi = 48)
/* loaded from: /tmp/apk_extracted/classes9.dex */
public final class AudioSignalEngine {
    public static final String SOUND_DOWN_ALERT = "SOUND_DOWN_ALERT";
    public static final String SOUND_NONE = "SOUND_NONE";
    public static final String SOUND_UP_ALERT = "SOUND_UP_ALERT";
    private static final String TAG = "AudioSignalEngine";
    private static volatile Context appContext;
    private static volatile boolean isMutedForTesting;
    private static volatile boolean isTtsInitialized;
    private static String lastSpokenText;
    private static long lastSpokenTime;
    private static volatile TextToSpeech textToSpeech;
    private static volatile ToneGenerator toneGenerator;
    public static final AudioSignalEngine INSTANCE = new AudioSignalEngine();
    private static final CopyOnWriteArrayList<String> recordedEventsForTesting = new CopyOnWriteArrayList<>();

    /* renamed from: isRobolectric$delegate, reason: from kotlin metadata */
    private static final Lazy isRobolectric = LazyKt.lazy(new Function0() { // from class: com.example.audio.AudioSignalEngine$$ExternalSyntheticLambda0
        public final Object invoke() {
            return Boolean.valueOf(AudioSignalEngine.isRobolectric_delegate$lambda$0());
        }
    });
    private static final int sampleRate = 44100;

    /* renamed from: upPcmBuffer$delegate, reason: from kotlin metadata */
    private static final Lazy upPcmBuffer = LazyKt.lazy(new Function0() { // from class: com.example.audio.AudioSignalEngine$$ExternalSyntheticLambda1
        public final Object invoke() {
            byte[] generatePcmSineWave;
            generatePcmSineWave = AudioSignalEngine.INSTANCE.generatePcmSineWave(960.0d, 150, AudioSignalEngine.sampleRate);
            return generatePcmSineWave;
        }
    });

    /* renamed from: downPcmBuffer$delegate, reason: from kotlin metadata */
    private static final Lazy downPcmBuffer = LazyKt.lazy(new Function0() { // from class: com.example.audio.AudioSignalEngine$$ExternalSyntheticLambda2
        public final Object invoke() {
            byte[] generatePcmSineWave;
            generatePcmSineWave = AudioSignalEngine.INSTANCE.generatePcmSineWave(420.0d, 190, AudioSignalEngine.sampleRate);
            return generatePcmSineWave;
        }
    });
    public static final int $stable = 8;

    private AudioSignalEngine() {
    }

    public final boolean isMutedForTesting() {
        return isMutedForTesting;
    }

    public final void setMutedForTesting(boolean z) {
        isMutedForTesting = z;
    }

    public final CopyOnWriteArrayList<String> getRecordedEventsForTesting() {
        return recordedEventsForTesting;
    }

    public final void clearRecordedEventsForTesting() {
        recordedEventsForTesting.clear();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final boolean isRobolectric() {
        return ((Boolean) isRobolectric.getValue()).booleanValue();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static final boolean isRobolectric_delegate$lambda$0() {
        try {
            String str = Build.FINGERPRINT;
            Intrinsics.checkNotNullExpressionValue(str, "FINGERPRINT");
            return StringsKt.contains(str, "robolectric", true);
        } catch (Throwable th) {
            return false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final byte[] getUpPcmBuffer() {
        return (byte[]) upPcmBuffer.getValue();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final byte[] getDownPcmBuffer() {
        return (byte[]) downPcmBuffer.getValue();
    }

    public final void init(Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        appContext = context.getApplicationContext();
        if (!isRobolectric() && textToSpeech == null) {
            try {
                textToSpeech = new TextToSpeech(context.getApplicationContext(), new TextToSpeech.OnInitListener() { // from class: com.example.audio.AudioSignalEngine$$ExternalSyntheticLambda3
                    @Override // android.speech.tts.TextToSpeech.OnInitListener
                    public final void onInit(int i) {
                        AudioSignalEngine.init$lambda$3(i);
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "TTS initialization notice: " + e.getMessage());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static final void init$lambda$3(int status) {
        if (status == 0) {
            try {
                TextToSpeech textToSpeech2 = textToSpeech;
                if (textToSpeech2 != null) {
                    textToSpeech2.setLanguage(Locale.US);
                }
                TextToSpeech textToSpeech3 = textToSpeech;
                if (textToSpeech3 != null) {
                    textToSpeech3.setSpeechRate(1.15f);
                }
                isTtsInitialized = true;
            } catch (Exception e) {
                Log.w(TAG, "TTS configuration notice: " + e.getMessage());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void initToneGenerator() {
        ToneGenerator toneGenerator2;
        ToneGenerator toneGenerator3;
        if (isRobolectric()) {
            return;
        }
        try {
            ToneGenerator toneGenerator4 = toneGenerator;
            if (toneGenerator4 != null) {
                toneGenerator4.release();
            }
            try {
                toneGenerator3 = new ToneGenerator(5, 95);
            } catch (Exception e) {
                try {
                    toneGenerator2 = new ToneGenerator(4, 95);
                } catch (Exception e2) {
                    toneGenerator2 = new ToneGenerator(3, 95);
                }
                toneGenerator3 = toneGenerator2;
            }
            toneGenerator = toneGenerator3;
        } catch (Exception e3) {
            Log.w(TAG, "ToneGenerator initialization notice: " + e3.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final byte[] generatePcmSineWave(double frequencyHz, int durationMs, int sampleRate2) {
        double envelope;
        int numSamples = (durationMs * sampleRate2) / 1000;
        byte[] buffer = new byte[numSamples * 2];
        int rampSamples = RangesKt.coerceAtLeast((int) (sampleRate2 * 0.01d), 100);
        int i = 0;
        while (i < numSamples) {
            if (i < rampSamples) {
                envelope = i / rampSamples;
            } else {
                envelope = i > numSamples - rampSamples ? (numSamples - i) / rampSamples : 1.0d;
            }
            double angle = ((i * 6.283185307179586d) * frequencyHz) / sampleRate2;
            short sampleValue = (short) RangesKt.coerceIn((int) (Math.sin(angle) * 28000.0d * envelope), -32768, 32767);
            buffer[i * 2] = (byte) (sampleValue & 255);
            buffer[(i * 2) + 1] = (byte) ((sampleValue >> 8) & 255);
            i++;
        }
        return buffer;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void playSynthesizedPcm(byte[] buffer) {
        if (isRobolectric()) {
            return;
        }
        try {
            AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(13).setContentType(4).build();
            AudioFormat audioFormat = new AudioFormat.Builder().setEncoding(2).setSampleRate(sampleRate).setChannelMask(4).build();
            AudioTrack track = new AudioTrack.Builder().setAudioAttributes(audioAttributes).setAudioFormat(audioFormat).setBufferSizeInBytes(buffer.length).setTransferMode(0).build();
            Intrinsics.checkNotNullExpressionValue(track, "build(...)");
            track.write(buffer, 0, buffer.length);
            track.play();
            track.setNotificationMarkerPosition(buffer.length / 2);
            track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() { // from class: com.example.audio.AudioSignalEngine$playSynthesizedPcm$1
                @Override // android.media.AudioTrack.OnPlaybackPositionUpdateListener
                public void onPeriodicNotification(AudioTrack t) {
                }

                @Override // android.media.AudioTrack.OnPlaybackPositionUpdateListener
                public void onMarkerReached(AudioTrack t) {
                    if (t != null) {
                        try {
                            t.stop();
                        } catch (Exception e) {
                            return;
                        }
                    }
                    if (t != null) {
                        t.release();
                    }
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "PCM AudioTrack playback notice: " + e.getMessage());
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void speakCallout(String text) {
        if (isRobolectric() || !isTtsInitialized) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            String cleanedText = sanitizeCalloutText(text);
            if (Intrinsics.areEqual(cleanedText, lastSpokenText) && now - lastSpokenTime < 30000) {
                return;
            }
            lastSpokenText = cleanedText;
            lastSpokenTime = now;
            TextToSpeech textToSpeech2 = textToSpeech;
            if (textToSpeech2 != null) {
                textToSpeech2.speak(cleanedText, 0, null, "SignalVoiceCallout");
            }
        } catch (Exception e) {
            Log.w(TAG, "TTS speak notice: " + e.getMessage());
        }
    }

    public final String sanitizeCalloutText(String rawText) {
        Intrinsics.checkNotNullParameter(rawText, "rawText");
        List<String> parts = new Regex("\\s+").split(StringsKt.trim(rawText).toString(), 0);
        List result = new ArrayList();
        for (String part : parts) {
            Locale locale = Locale.US;
            Intrinsics.checkNotNullExpressionValue(locale, "US");
            String upper = part.toUpperCase(locale);
            Intrinsics.checkNotNullExpressionValue(upper, "toUpperCase(...)");
            if (Intrinsics.areEqual(upper, "UP") || Intrinsics.areEqual(upper, "DOWN")) {
                result.add(upper);
            } else {
                String str = part;
                Appendable sb = new StringBuilder();
                int length = str.length();
                for (int i = 0; i < length; i++) {
                    char charAt = str.charAt(i);
                    if (Character.isDigit(charAt)) {
                        sb.append(charAt);
                    }
                }
                String sb2 = ((StringBuilder) sb).toString();
                if (sb2.length() > 0) {
                    Integer number = StringsKt.toIntOrNull(sb2);
                    if (number != null) {
                        result.add(number.toString());
                    } else {
                        result.add(sb2);
                    }
                }
            }
        }
        return !result.isEmpty() ? CollectionsKt.joinToString$default(result, " ", (CharSequence) null, (CharSequence) null, 0, (CharSequence) null, (Function1) null, 62, (Object) null) : rawText;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void playSystemNotificationFallback() {
        Ringtone ringtone;
        if (isRobolectric()) {
            return;
        }
        try {
            Context context = appContext;
            if (context == null || (ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(2))) == null) {
                return;
            }
            ringtone.play();
        } catch (Exception e) {
        }
    }

    public final void shutdown() {
        try {
            ToneGenerator toneGenerator2 = toneGenerator;
            if (toneGenerator2 != null) {
                toneGenerator2.release();
            }
            toneGenerator = null;
        } catch (Exception e) {
        }
        try {
            TextToSpeech textToSpeech2 = textToSpeech;
            if (textToSpeech2 != null) {
                textToSpeech2.stop();
            }
            TextToSpeech textToSpeech3 = textToSpeech;
            if (textToSpeech3 != null) {
                textToSpeech3.shutdown();
            }
            textToSpeech = null;
            isTtsInitialized = false;
        } catch (Exception e2) {
        }
    }

    public static /* synthetic */ Object playSoundEvent$default(AudioSignalEngine audioSignalEngine, String str, String str2, Continuation continuation, int i, Object obj) {
        if ((i & 2) != 0) {
            str2 = null;
        }
        return audioSignalEngine.playSoundEvent(str, str2, continuation);
    }

    public final Object playSoundEvent(String event, String calloutText, Continuation<? super Unit> continuation) {
        Object withContext = BuildersKt.withContext(Dispatchers.getDefault(), new AudioSignalEngine$playSoundEvent$2(event, calloutText, null), continuation);
        return withContext == IntrinsicsKt.getCOROUTINE_SUSPENDED() ? withContext : Unit.INSTANCE;
    }
}
