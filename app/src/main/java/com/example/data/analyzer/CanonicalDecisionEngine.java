package com.example.data.analyzer;

import com.example.data.analyzer.MicroKineticVectorEngine;
import com.example.data.analyzer.ReactiveMarketPressureEngine;
import com.example.data.models.CanonicalDecision;
import com.example.data.models.DataQualityState;
import com.example.data.models.DecisionMode;
import com.example.data.models.TradeDirection;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.StringCompanionObject;

/* compiled from: CanonicalDecisionEngine.kt */
@Metadata(d1 = {"\u0000n\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u000b\bÇ\u0002\u0018\u00002\u00020\u0001B\t\b\u0002¢\u0006\u0004\b\u0002\u0010\u0003J\u0006\u0010\u0014\u001a\u00020\u0015J\u0091\u0001\u0010\u0016\u001a\u00020\u00172\b\u0010\u0018\u001a\u0004\u0018\u00010\u00192\b\u0010\u001a\u001a\u0004\u0018\u00010\u00192\b\u0010\u001b\u001a\u0004\u0018\u00010\u00192\u000e\b\u0002\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001e0\u001d2\b\b\u0002\u0010\u001f\u001a\u00020 2\b\b\u0002\u0010!\u001a\u00020\u00072\n\b\u0002\u0010\"\u001a\u0004\u0018\u00010#2\n\b\u0002\u0010$\u001a\u0004\u0018\u00010%2\n\b\u0002\u0010&\u001a\u0004\u0018\u00010'2\n\b\u0002\u0010(\u001a\u0004\u0018\u00010)2\b\b\u0002\u0010*\u001a\u00020\u00052\b\b\u0002\u0010+\u001a\u00020\t¢\u0006\u0002\u0010,J_\u0010-\u001a\u00020\u00172\b\u0010.\u001a\u0004\u0018\u00010\u00192\b\u0010/\u001a\u0004\u0018\u00010\u00192\b\u00100\u001a\u0004\u0018\u00010\u00192\u000e\b\u0002\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001e0\u001d2\b\u0010\"\u001a\u0004\u0018\u00010#2\b\u0010&\u001a\u0004\u0018\u00010'2\u0006\u0010!\u001a\u00020\u00072\u0006\u0010\u001f\u001a\u00020 H\u0002¢\u0006\u0002\u00101J?\u00102\u001a\u00020\u00172\b\u0010.\u001a\u0004\u0018\u00010\u00192\b\u0010/\u001a\u0004\u0018\u00010\u00192\b\u00100\u001a\u0004\u0018\u00010\u00192\b\b\u0002\u0010\u001f\u001a\u00020 2\b\b\u0002\u0010!\u001a\u00020\u0007H\u0002¢\u0006\u0002\u00103R\u000e\u0010\u0004\u001a\u00020\u0005X\u0086T¢\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0086T¢\u0006\u0002\n\u0000R\u001a\u0010\b\u001a\u00020\tX\u0086\u000e¢\u0006\u000e\n\u0000\u001a\u0004\b\n\u0010\u000b\"\u0004\b\f\u0010\rR\u000e\u0010\u000e\u001a\u00020\u000fX\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0005X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0007X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u000e¢\u0006\u0002\n\u0000¨\u00064"}, d2 = {"Lcom/example/data/analyzer/CanonicalDecisionEngine;", "", "<init>", "()V", "DEFAULT_REQUIRED_CONFIRMATIONS", "", "STALE_TIMEOUT_MS", "", "currentDecisionMode", "Lcom/example/data/models/DecisionMode;", "getCurrentDecisionMode", "()Lcom/example/data/models/DecisionMode;", "setCurrentDecisionMode", "(Lcom/example/data/models/DecisionMode;)V", "lastCandidateDirection", "Lcom/example/data/models/TradeDirection;", "confirmedCount", "lastUpdateTimeMs", "lastFingerprint", "", "resetState", "", "evaluate", "Lcom/example/data/models/CanonicalDecision;", "val5m", "", "val60m", "val1d", "history", "", "Lcom/example/data/models/MetricSnapshot;", "isApproximate", "", "currentTimeMs", "rmpDecision", "Lcom/example/data/analyzer/ReactiveMarketPressureEngine$PressureDecision;", "matrixResult", "Lcom/example/data/matrix/MatrixEvaluationResult;", "kineticResult", "Lcom/example/data/analyzer/MicroKineticVectorEngine$KineticVectorResult;", "behavior", "Lcom/example/data/analyzer/MovementClassificationResult;", "requiredConfirmations", "decisionMode", "(Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/Double;Ljava/util/List;ZJLcom/example/data/analyzer/ReactiveMarketPressureEngine$PressureDecision;Lcom/example/data/matrix/MatrixEvaluationResult;Lcom/example/data/analyzer/MicroKineticVectorEngine$KineticVectorResult;Lcom/example/data/analyzer/MovementClassificationResult;ILcom/example/data/models/DecisionMode;)Lcom/example/data/models/CanonicalDecision;", "evaluateShortTermStrength", "v5", "v60", "v1d", "(Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/Double;Ljava/util/List;Lcom/example/data/analyzer/ReactiveMarketPressureEngine$PressureDecision;Lcom/example/data/analyzer/MicroKineticVectorEngine$KineticVectorResult;JZ)Lcom/example/data/models/CanonicalDecision;", "evaluateThreeTimeframePressure", "(Ljava/lang/Double;Ljava/lang/Double;Ljava/lang/Double;ZJ)Lcom/example/data/models/CanonicalDecision;", "app"}, k = CanonicalDecisionEngine.DEFAULT_REQUIRED_CONFIRMATIONS, mv = {2, 2, 0}, xi = 48)
/* loaded from: /tmp/apk_extracted/classes7.dex */
public final class CanonicalDecisionEngine {
    public static final int DEFAULT_REQUIRED_CONFIRMATIONS = 1;
    public static final long STALE_TIMEOUT_MS = 15000;
    private static volatile int confirmedCount;
    private static volatile long lastUpdateTimeMs;
    public static final CanonicalDecisionEngine INSTANCE = new CanonicalDecisionEngine();
    private static volatile DecisionMode currentDecisionMode = DecisionMode.LEGACY_MULTILAYER;
    private static volatile TradeDirection lastCandidateDirection = TradeDirection.NEUTRAL;
    private static volatile String lastFingerprint = "";
    public static final int $stable = 8;

    /* compiled from: CanonicalDecisionEngine.kt */
    @Metadata(k = 3, mv = {2, 2, 0}, xi = 48)
    /* loaded from: /tmp/apk_extracted/classes7.dex */
    public static final /* synthetic */ class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;
        public static final /* synthetic */ int[] $EnumSwitchMapping$1;

        static {
            int[] iArr = new int[PressureDirection.values().length];
            try {
                iArr[PressureDirection.UP.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[PressureDirection.DOWN.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[PressureDirection.NO_SIGNAL.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            $EnumSwitchMapping$0 = iArr;
            int[] iArr2 = new int[TradeDirection.values().length];
            try {
                iArr2[TradeDirection.UP.ordinal()] = 1;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr2[TradeDirection.DOWN.ordinal()] = 2;
            } catch (NoSuchFieldError e5) {
            }
            $EnumSwitchMapping$1 = iArr2;
        }
    }

    private CanonicalDecisionEngine() {
    }

    public final DecisionMode getCurrentDecisionMode() {
        return currentDecisionMode;
    }

    public final void setCurrentDecisionMode(DecisionMode decisionMode) {
        Intrinsics.checkNotNullParameter(decisionMode, "<set-?>");
        currentDecisionMode = decisionMode;
    }

    public final void resetState() {
        lastCandidateDirection = TradeDirection.NEUTRAL;
        confirmedCount = 0;
        lastUpdateTimeMs = 0L;
        lastFingerprint = "";
    }

    /* JADX WARN: Code restructure failed: missing block: B:94:0x0239, code lost:
    
        if (r88.getKineticBaseEnergy() < 0.005d) goto L696;
     */
    /* JADX WARN: Removed duplicated region for block: B:100:0x024b  */
    /* JADX WARN: Removed duplicated region for block: B:113:0x037f  */
    /* JADX WARN: Removed duplicated region for block: B:12:0x0034  */
    /* JADX WARN: Removed duplicated region for block: B:150:0x0480  */
    /* JADX WARN: Removed duplicated region for block: B:158:0x0cf8  */
    /* JADX WARN: Removed duplicated region for block: B:161:0x0d19  */
    /* JADX WARN: Removed duplicated region for block: B:164:0x0d3a  */
    /* JADX WARN: Removed duplicated region for block: B:167:0x0da9  */
    /* JADX WARN: Removed duplicated region for block: B:170:0x0dbe  */
    /* JADX WARN: Removed duplicated region for block: B:177:0x0deb  */
    /* JADX WARN: Removed duplicated region for block: B:179:0x0df4  */
    /* JADX WARN: Removed duplicated region for block: B:191:0x0e5b  */
    /* JADX WARN: Removed duplicated region for block: B:194:0x0e5e  */
    /* JADX WARN: Removed duplicated region for block: B:197:0x0df0  */
    /* JADX WARN: Removed duplicated region for block: B:199:0x0dc5  */
    /* JADX WARN: Removed duplicated region for block: B:205:0x0dac  */
    /* JADX WARN: Removed duplicated region for block: B:206:0x0d3f  */
    /* JADX WARN: Removed duplicated region for block: B:207:0x0d1e  */
    /* JADX WARN: Removed duplicated region for block: B:208:0x0cfd  */
    /* JADX WARN: Removed duplicated region for block: B:20:0x0054  */
    /* JADX WARN: Removed duplicated region for block: B:29:0x007c  */
    /* JADX WARN: Removed duplicated region for block: B:32:0x0089  */
    /* JADX WARN: Removed duplicated region for block: B:348:0x0858  */
    /* JADX WARN: Removed duplicated region for block: B:355:0x0877 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:363:0x08b0  */
    /* JADX WARN: Removed duplicated region for block: B:370:0x0b24  */
    /* JADX WARN: Removed duplicated region for block: B:372:0x0b4f A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:377:0x0b62  */
    /* JADX WARN: Removed duplicated region for block: B:381:0x0b70  */
    /* JADX WARN: Removed duplicated region for block: B:384:0x0b93  */
    /* JADX WARN: Removed duplicated region for block: B:387:0x0bb6  */
    /* JADX WARN: Removed duplicated region for block: B:390:0x0c17 A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:395:0x0b98  */
    /* JADX WARN: Removed duplicated region for block: B:396:0x0b75  */
    /* JADX WARN: Removed duplicated region for block: B:399:0x0b29  */
    /* JADX WARN: Removed duplicated region for block: B:411:0x0925  */
    /* JADX WARN: Removed duplicated region for block: B:431:0x0a2f  */
    /* JADX WARN: Removed duplicated region for block: B:472:0x091f  */
    /* JADX WARN: Removed duplicated region for block: B:479:0x0868  */
    /* JADX WARN: Removed duplicated region for block: B:492:0x0cd5  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public final com.example.data.models.CanonicalDecision evaluate(java.lang.Double r111, java.lang.Double r112, java.lang.Double r113, java.util.List<com.example.data.models.MetricSnapshot> r114, boolean r115, long r116, com.example.data.analyzer.ReactiveMarketPressureEngine.PressureDecision r118, com.example.data.matrix.MatrixEvaluationResult r119, com.example.data.analyzer.MicroKineticVectorEngine.KineticVectorResult r120, com.example.data.analyzer.MovementClassificationResult r121, int r122, com.example.data.models.DecisionMode r123) {
        /*
            Method dump skipped, instructions count: 3840
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.example.data.analyzer.CanonicalDecisionEngine.evaluate(java.lang.Double, java.lang.Double, java.lang.Double, java.util.List, boolean, long, com.example.data.analyzer.ReactiveMarketPressureEngine$PressureDecision, com.example.data.matrix.MatrixEvaluationResult, com.example.data.analyzer.MicroKineticVectorEngine$KineticVectorResult, com.example.data.analyzer.MovementClassificationResult, int, com.example.data.models.DecisionMode):com.example.data.models.CanonicalDecision");
    }

    static /* synthetic */ CanonicalDecision evaluateShortTermStrength$default(CanonicalDecisionEngine canonicalDecisionEngine, Double d, Double d2, Double d3, List list, ReactiveMarketPressureEngine.PressureDecision pressureDecision, MicroKineticVectorEngine.KineticVectorResult kineticVectorResult, long j, boolean z, int i, Object obj) {
        List list2;
        if ((i & 8) == 0) {
            list2 = list;
        } else {
            list2 = CollectionsKt.emptyList();
        }
        return canonicalDecisionEngine.evaluateShortTermStrength(d, d2, d3, list2, pressureDecision, kineticVectorResult, j, z);
    }

    /* JADX WARN: Removed duplicated region for block: B:47:0x03a4  */
    /* JADX WARN: Removed duplicated region for block: B:50:0x03af  */
    /* JADX WARN: Removed duplicated region for block: B:53:0x03be  */
    /* JADX WARN: Removed duplicated region for block: B:57:0x03cf  */
    /* JADX WARN: Removed duplicated region for block: B:62:0x03ed  */
    /* JADX WARN: Removed duplicated region for block: B:65:0x0410  */
    /* JADX WARN: Removed duplicated region for block: B:68:0x0435  */
    /* JADX WARN: Removed duplicated region for block: B:71:0x04a5  */
    /* JADX WARN: Removed duplicated region for block: B:74:0x04af  */
    /* JADX WARN: Removed duplicated region for block: B:78:0x04bd A[ADDED_TO_REGION] */
    /* JADX WARN: Removed duplicated region for block: B:84:0x04b7  */
    /* JADX WARN: Removed duplicated region for block: B:85:0x04a8  */
    /* JADX WARN: Removed duplicated region for block: B:86:0x0415  */
    /* JADX WARN: Removed duplicated region for block: B:87:0x03f2  */
    /* JADX WARN: Removed duplicated region for block: B:90:0x03b2  */
    /* JADX WARN: Removed duplicated region for block: B:91:0x03a7  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private final com.example.data.models.CanonicalDecision evaluateShortTermStrength(java.lang.Double r168, java.lang.Double r169, java.lang.Double r170, java.util.List<com.example.data.models.MetricSnapshot> r171, com.example.data.analyzer.ReactiveMarketPressureEngine.PressureDecision r172, com.example.data.analyzer.MicroKineticVectorEngine.KineticVectorResult r173, long r174, boolean r176) {
        /*
            Method dump skipped, instructions count: 1441
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.example.data.analyzer.CanonicalDecisionEngine.evaluateShortTermStrength(java.lang.Double, java.lang.Double, java.lang.Double, java.util.List, com.example.data.analyzer.ReactiveMarketPressureEngine$PressureDecision, com.example.data.analyzer.MicroKineticVectorEngine$KineticVectorResult, long, boolean):com.example.data.models.CanonicalDecision");
    }

    static /* synthetic */ CanonicalDecision evaluateThreeTimeframePressure$default(CanonicalDecisionEngine canonicalDecisionEngine, Double d, Double d2, Double d3, boolean z, long j, int i, Object obj) {
        boolean z2;
        long j2;
        if ((i & 8) == 0) {
            z2 = z;
        } else {
            z2 = false;
        }
        if ((i & 16) == 0) {
            j2 = j;
        } else {
            j2 = System.currentTimeMillis();
        }
        return canonicalDecisionEngine.evaluateThreeTimeframePressure(d, d2, d3, z2, j2);
    }

    private final CanonicalDecision evaluateThreeTimeframePressure(Double v5, Double v60, Double v1d, boolean isApproximate, long currentTimeMs) {
        SourceQuality sourceQuality;
        TradeDirection direction;
        String side;
        String qualityState;
        boolean isDataMissing = v5 == null || v60 == null || v1d == null || Double.isNaN(v5.doubleValue()) || Double.isNaN(v60.doubleValue()) || Double.isNaN(v1d.doubleValue()) || Double.isInfinite(v5.doubleValue()) || Double.isInfinite(v60.doubleValue()) || Double.isInfinite(v1d.doubleValue());
        if (isApproximate) {
            sourceQuality = SourceQuality.APPROXIMATE;
        } else {
            sourceQuality = isDataMissing ? SourceQuality.INCOMPLETE : SourceQuality.VERIFIED;
        }
        SourceQuality sourceQuality2 = sourceQuality;
        ThreeTimeframePressureResult pressureResult = ThreeTimeframePressureCalculator.INSTANCE.calculate(v5, v60, v1d, currentTimeMs, sourceQuality2);
        if (pressureResult.isDataIncomplete()) {
            lastCandidateDirection = TradeDirection.NEUTRAL;
            confirmedCount = 0;
            lastUpdateTimeMs = currentTimeMs;
            lastFingerprint = "FP:NEUTRAL:NONE:DATA_INCOMPLETE";
            if (isApproximate) {
                qualityState = DataQualityState.APPROXIMATE;
            } else {
                qualityState = (v5 == null && v60 == null && v1d == null) ? DataQualityState.UNAVAILABLE : DataQualityState.INCOMPLETE;
            }
            String uuid = UUID.randomUUID().toString();
            Intrinsics.checkNotNullExpressionValue(uuid, "toString(...)");
            return new CanonicalDecision(uuid, TradeDirection.NEUTRAL, "NONE", 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, qualityState, CollectionsKt.emptyList(), CollectionsKt.emptyList(), "FP:NEUTRAL:NONE:DATA_INCOMPLETE", "ডেটা অনুপস্থিত [DATA_INCOMPLETE]: তিনটি টাইমফ্রেমের সম্পূর্ণ ও নিশ্চিত ডেটা প্রয়োজন (v5, v60, v1d)", currentTimeMs, false, 0.0d, 0.0d, 0.0d, null, true, false, 0, 1, "DATA_INCOMPLETE", false, false, false, "ডেটা অনুপস্থিত [DATA_INCOMPLETE]: তিনটি টাইমফ্রেমের সম্পূর্ণ ও নিশ্চিত ডেটা প্রয়োজন (v5, v60, v1d)", null, "3TF_MATH", "ThreeTimeframePressureCalculator (DATA_INCOMPLETE)", 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, "DATA_INCOMPLETE", "তিনটি টাইমফ্রেমের সম্পূর্ণ ও নিশ্চিত ডেটা অনুপস্থিত (DATA_INCOMPLETE)", 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, false, false, 0.0d, 0.0d, 0.0d, -1876951040, 1019516, null);
        }
        switch (WhenMappings.$EnumSwitchMapping$0[pressureResult.getDirection().ordinal()]) {
            case DEFAULT_REQUIRED_CONFIRMATIONS /* 1 */:
                direction = TradeDirection.UP;
                break;
            case 2:
                direction = TradeDirection.DOWN;
                break;
            case 3:
                direction = TradeDirection.NEUTRAL;
                break;
            default:
                throw new NoWhenBranchMatchedException();
        }
        switch (WhenMappings.$EnumSwitchMapping$1[direction.ordinal()]) {
            case DEFAULT_REQUIRED_CONFIRMATIONS /* 1 */:
                side = "BUY";
                break;
            case 2:
                side = "SELL";
                break;
            default:
                side = "NONE";
                break;
        }
        boolean isEligible = direction != TradeDirection.NEUTRAL && pressureResult.getTotalEnergy() > 0.0d;
        String name = pressureResult.getDirection().name();
        String name2 = pressureResult.getPressureBand().name();
        StringCompanionObject stringCompanionObject = StringCompanionObject.INSTANCE;
        String format = String.format(Locale.US, "%.2f", Arrays.copyOf(new Object[]{v5}, 1));
        Intrinsics.checkNotNullExpressionValue(format, "format(...)");
        StringCompanionObject stringCompanionObject2 = StringCompanionObject.INSTANCE;
        String format2 = String.format(Locale.US, "%.2f", Arrays.copyOf(new Object[]{v60}, 1));
        Intrinsics.checkNotNullExpressionValue(format2, "format(...)");
        StringCompanionObject stringCompanionObject3 = StringCompanionObject.INSTANCE;
        String format3 = String.format(Locale.US, "%.2f", Arrays.copyOf(new Object[]{v1d}, 1));
        Intrinsics.checkNotNullExpressionValue(format3, "format(...)");
        String fp = "FP:3TF:" + name + ":" + name2 + ":" + format + ":" + format2 + ":" + format3;
        lastCandidateDirection = direction;
        confirmedCount = isEligible ? 1 : 0;
        lastUpdateTimeMs = currentTimeMs;
        lastFingerprint = fp;
        PressureDirection direction2 = pressureResult.getDirection();
        String name3 = pressureResult.getPressureBand().name();
        StringCompanionObject stringCompanionObject4 = StringCompanionObject.INSTANCE;
        String format4 = String.format(Locale.US, "%.2f", Arrays.copyOf(new Object[]{Double.valueOf(pressureResult.getUpSharePercent())}, 1));
        Intrinsics.checkNotNullExpressionValue(format4, "format(...)");
        StringCompanionObject stringCompanionObject5 = StringCompanionObject.INSTANCE;
        String format5 = String.format(Locale.US, "%.2f", Arrays.copyOf(new Object[]{Double.valueOf(pressureResult.getDownSharePercent())}, 1));
        Intrinsics.checkNotNullExpressionValue(format5, "format(...)");
        StringCompanionObject stringCompanionObject6 = StringCompanionObject.INSTANCE;
        String format6 = String.format(Locale.US, "%+.2f", Arrays.copyOf(new Object[]{Double.valueOf(pressureResult.getNetPressurePercent())}, 1));
        Intrinsics.checkNotNullExpressionValue(format6, "format(...)");
        String explanationText = "৩-টাইমফ্রেম প্রেসার পিওর গণিত | ডিরেকশন: " + direction2 + " | ব্যান্ড: " + name3 + " | UP=" + format4 + "%, DOWN=" + format5 + "%, Net=" + format6 + "% | " + side + " এন্ট্রি প্রস্তুত";
        String uuid2 = UUID.randomUUID().toString();
        Intrinsics.checkNotNullExpressionValue(uuid2, "toString(...)");
        double upSharePercent = pressureResult.getUpSharePercent();
        double downSharePercent = pressureResult.getDownSharePercent();
        double upSharePercent2 = pressureResult.getUpSharePercent();
        double downSharePercent2 = pressureResult.getDownSharePercent();
        double abs = Math.abs(pressureResult.getNetPressurePercent());
        double abs2 = Math.abs(pressureResult.getNetPressurePercent());
        double netPressurePercent = pressureResult.getNetPressurePercent();
        double netPressurePercent2 = pressureResult.getNetPressurePercent();
        boolean z = direction == TradeDirection.NEUTRAL;
        List listOf = CollectionsKt.listOf("3TF_MATH");
        List emptyList = CollectionsKt.emptyList();
        int i = isEligible ? 1 : 0;
        String str = isEligible ? "3TF_CONFIRMED" : "NO_SIGNAL";
        String str2 = "ThreeTimeframePressureCalculator (" + pressureResult.getPressureBand().name() + ")";
        double upEnergy = pressureResult.getUpEnergy();
        double downEnergy = pressureResult.getDownEnergy();
        double abs3 = Math.abs(pressureResult.getNetPressurePercent());
        String str3 = "3TF_PRESSURE_" + pressureResult.getPressureBand().name();
        StringCompanionObject stringCompanionObject7 = StringCompanionObject.INSTANCE;
        String format7 = String.format(Locale.US, "%.6f", Arrays.copyOf(new Object[]{Double.valueOf(pressureResult.getUpEnergy())}, 1));
        Intrinsics.checkNotNullExpressionValue(format7, "format(...)");
        StringCompanionObject stringCompanionObject8 = StringCompanionObject.INSTANCE;
        String format8 = String.format(Locale.US, "%.6f", Arrays.copyOf(new Object[]{Double.valueOf(pressureResult.getDownEnergy())}, 1));
        Intrinsics.checkNotNullExpressionValue(format8, "format(...)");
        StringCompanionObject stringCompanionObject9 = StringCompanionObject.INSTANCE;
        String format9 = String.format(Locale.US, "%+.2f", Arrays.copyOf(new Object[]{Double.valueOf(pressureResult.getNetPressurePercent())}, 1));
        Intrinsics.checkNotNullExpressionValue(format9, "format(...)");
        return new CanonicalDecision(uuid2, direction, side, upSharePercent, downSharePercent, abs, netPressurePercent, 0.0d, netPressurePercent2, 0.0d, 0.0d, DataQualityState.VERIFIED, listOf, emptyList, fp, explanationText, currentTimeMs, isEligible, upSharePercent2, downSharePercent2, abs2, null, z, false, i, 1, str, false, false, false, explanationText, null, "3TF_MATH", str2, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d, str3, "UpEnergy: " + format7 + " | DownEnergy: " + format8 + " | Net: " + format9 + "% (" + pressureResult.getPressureBand().name() + ")", 0.0d, 0.0d, 0.0d, upEnergy, downEnergy, abs3, false, false, 0.0d, 0.0d, 0.0d, -1876951040, 1019516, null);
    }
}
