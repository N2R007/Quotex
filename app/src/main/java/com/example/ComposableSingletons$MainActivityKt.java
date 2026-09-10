package com.example;

import androidx.compose.foundation.layout.RowScope;
import androidx.compose.foundation.layout.SizeKt;
import androidx.compose.material.icons.Icons;
import androidx.compose.material.icons.filled.SettingsKt;
import androidx.compose.material3.IconKt;
import androidx.compose.material3.TextKt;
import androidx.compose.runtime.Composer;
import androidx.compose.runtime.ComposerKt;
import androidx.compose.runtime.internal.ComposableLambdaKt;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.text.TextStyle;
import androidx.compose.ui.text.font.FontFamily;
import androidx.compose.ui.text.font.FontStyle;
import androidx.compose.ui.text.font.FontWeight;
import androidx.compose.ui.text.style.TextAlign;
import androidx.compose.ui.text.style.TextDecoration;
import androidx.compose.ui.unit.Dp;
import androidx.compose.ui.unit.TextUnitKt;
import com.example.ui.theme.ColorKt;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: MainActivity.kt */
@Metadata(k = 3, mv = {2, 2, 0}, xi = 48)
/* loaded from: /tmp/apk_extracted/classes10.dex */
public final class ComposableSingletons$MainActivityKt {
    public static final ComposableSingletons$MainActivityKt INSTANCE = new ComposableSingletons$MainActivityKt();
    private static Function2<Composer, Integer, Unit> lambda$1489894622 = ComposableLambdaKt.composableLambdaInstance(1489894622, false, new Function2() { // from class: com.example.ComposableSingletons$MainActivityKt$$ExternalSyntheticLambda0
        public final Object invoke(Object obj, Object obj2) {
            return ComposableSingletons$MainActivityKt.lambda_1489894622$lambda$0((Composer) obj, ((Integer) obj2).intValue());
        }
    });
    private static Function2<Composer, Integer, Unit> lambda$1766392692 = ComposableLambdaKt.composableLambdaInstance(1766392692, false, new Function2() { // from class: com.example.ComposableSingletons$MainActivityKt$$ExternalSyntheticLambda1
        public final Object invoke(Object obj, Object obj2) {
            return ComposableSingletons$MainActivityKt.lambda_1766392692$lambda$2((Composer) obj, ((Integer) obj2).intValue());
        }
    });
    private static Function3<RowScope, Composer, Integer, Unit> lambda$26948223 = ComposableLambdaKt.composableLambdaInstance(26948223, false, new Function3() { // from class: com.example.ComposableSingletons$MainActivityKt$$ExternalSyntheticLambda2
        public final Object invoke(Object obj, Object obj2, Object obj3) {
            return ComposableSingletons$MainActivityKt.lambda_26948223$lambda$3((RowScope) obj, (Composer) obj2, ((Integer) obj3).intValue());
        }
    });

    /* renamed from: lambda$-40301464, reason: not valid java name */
    private static Function3<RowScope, Composer, Integer, Unit> f0lambda$40301464 = ComposableLambdaKt.composableLambdaInstance(-40301464, false, new Function3() { // from class: com.example.ComposableSingletons$MainActivityKt$$ExternalSyntheticLambda3
        public final Object invoke(Object obj, Object obj2, Object obj3) {
            return ComposableSingletons$MainActivityKt.lambda__40301464$lambda$4((RowScope) obj, (Composer) obj2, ((Integer) obj3).intValue());
        }
    });

    /* renamed from: getLambda$-40301464$app, reason: not valid java name */
    public final Function3<RowScope, Composer, Integer, Unit> m0getLambda$40301464$app() {
        return f0lambda$40301464;
    }

    public final Function2<Composer, Integer, Unit> getLambda$1489894622$app() {
        return lambda$1489894622;
    }

    public final Function2<Composer, Integer, Unit> getLambda$1766392692$app() {
        return lambda$1766392692;
    }

    public final Function3<RowScope, Composer, Integer, Unit> getLambda$26948223$app() {
        return lambda$26948223;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static final Unit lambda_1489894622$lambda$0(Composer $composer, int $changed) {
        ComposerKt.sourceInformation($composer, "C359@15145L284:MainActivity.kt#to5c3");
        if (($changed & 3) == 2 && $composer.getSkipping()) {
            $composer.skipToGroupEnd();
        } else {
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventStart(1489894622, $changed, -1, "com.example.ComposableSingletons$MainActivityKt.lambda$1489894622.<anonymous> (MainActivity.kt:359)");
            }
            IconKt.Icon-ww6aTOc(SettingsKt.getSettings(Icons.INSTANCE.getDefault()), "Settings", SizeKt.size-3ABfNKs(Modifier.Companion, Dp.constructor-impl(20)), ColorKt.getTextMuted(), $composer, 3504, 0);
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventEnd();
            }
        }
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: Removed duplicated region for block: B:25:0x016b  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static final kotlin.Unit lambda_1766392692$lambda$2(androidx.compose.runtime.Composer r32, int r33) {
        /*
            Method dump skipped, instructions count: 369
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.example.ComposableSingletons$MainActivityKt.lambda_1766392692$lambda$2(androidx.compose.runtime.Composer, int):kotlin.Unit");
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static final Unit lambda_26948223$lambda$3(RowScope $this$Button, Composer $composer, int $changed) {
        Intrinsics.checkNotNullParameter($this$Button, "$this$Button");
        ComposerKt.sourceInformation($composer, "C546@24069L192:MainActivity.kt#to5c3");
        if (($changed & 17) == 16 && $composer.getSkipping()) {
            $composer.skipToGroupEnd();
        } else {
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventStart(26948223, $changed, -1, "com.example.ComposableSingletons$MainActivityKt.lambda$26948223.<anonymous> (MainActivity.kt:546)");
            }
            TextKt.Text--4IGK_g("ক্যামেরার অনুমতি দিন (Grant Camera)", (Modifier) null, 0L, TextUnitKt.getSp(13), (FontStyle) null, FontWeight.Companion.getBold(), (FontFamily) null, 0L, (TextDecoration) null, (TextAlign) null, 0L, 0, false, 0, 0, (Function1) null, (TextStyle) null, $composer, 199686, 0, 131030);
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventEnd();
            }
        }
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static final Unit lambda__40301464$lambda$4(RowScope $this$Button, Composer $composer, int $changed) {
        Intrinsics.checkNotNullParameter($this$Button, "$this$Button");
        ComposerKt.sourceInformation($composer, "C563@24757L143:MainActivity.kt#to5c3");
        if (($changed & 17) == 16 && $composer.getSkipping()) {
            $composer.skipToGroupEnd();
        } else {
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventStart(-40301464, $changed, -1, "com.example.ComposableSingletons$MainActivityKt.lambda$-40301464.<anonymous> (MainActivity.kt:563)");
            }
            TextKt.Text--4IGK_g("টেস্ট চার্ট মোড ব্যবহার করুন (Test Mode)", (Modifier) null, 0L, TextUnitKt.getSp(13), (FontStyle) null, (FontWeight) null, (FontFamily) null, 0L, (TextDecoration) null, (TextAlign) null, 0L, 0, false, 0, 0, (Function1) null, (TextStyle) null, $composer, 3078, 0, 131062);
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventEnd();
            }
        }
        return Unit.INSTANCE;
    }
}
