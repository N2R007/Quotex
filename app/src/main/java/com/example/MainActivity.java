package com.example;

import android.content.Context;
import android.os.Bundle;
import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.compose.ComponentActivityKt;
import androidx.compose.runtime.Composer;
import androidx.compose.runtime.ComposerKt;
import androidx.compose.runtime.CompositionContext;
import androidx.compose.runtime.internal.ComposableLambdaKt;
import androidx.lifecycle.ViewModelLazy;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.lifecycle.viewmodel.CreationExtras;
import com.example.audio.AudioSignalEngine;
import com.example.data.analyzer.CanonicalDecisionEngine;
import com.example.data.matrix.UserRuleRegistry;
import com.example.network.HttpTradeRelay;
import com.example.network.WebSocketTradeRelay;
import com.example.ui.theme.ThemeKt;
import kotlin.Lazy;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Reflection;

/* compiled from: MainActivity.kt */
@Metadata(d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0010\b\n\u0002\b\u0002\b\u0007\u0018\u00002\u00020\u0001B\u0007¢\u0006\u0004\b\u0002\u0010\u0003J\u0012\u0010\n\u001a\u00020\u000b2\b\u0010\f\u001a\u0004\u0018\u00010\rH\u0014J\b\u0010\u000e\u001a\u00020\u000bH\u0014J\b\u0010\u000f\u001a\u00020\u000bH\u0014J\u0010\u0010\u0010\u001a\u00020\u000b2\u0006\u0010\u0011\u001a\u00020\u0012H\u0016J\b\u0010\u0013\u001a\u00020\u000bH\u0014R\u001b\u0010\u0004\u001a\u00020\u00058BX\u0082\u0084\u0002¢\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007¨\u0006\u0014"}, d2 = {"Lcom/example/MainActivity;", "Landroidx/activity/ComponentActivity;", "<init>", "()V", "viewModel", "Lcom/example/MainViewModel;", "getViewModel", "()Lcom/example/MainViewModel;", "viewModel$delegate", "Lkotlin/Lazy;", "onCreate", "", "savedInstanceState", "Landroid/os/Bundle;", "onResume", "onPause", "onTrimMemory", "level", "", "onDestroy", "app"}, k = CanonicalDecisionEngine.DEFAULT_REQUIRED_CONFIRMATIONS, mv = {2, 2, 0}, xi = 48)
/* loaded from: /tmp/apk_extracted/classes10.dex */
public final class MainActivity extends ComponentActivity {
    public static final int $stable = 8;

    /* renamed from: viewModel$delegate, reason: from kotlin metadata */
    private final Lazy viewModel;

    public MainActivity() {
        final MainActivity mainActivity = this;
        final Function0 function0 = null;
        this.viewModel = new ViewModelLazy(Reflection.getOrCreateKotlinClass(MainViewModel.class), new Function0<ViewModelStore>() { // from class: com.example.MainActivity$special$$inlined$viewModels$default$2
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            {
                super(0);
            }

            /* renamed from: invoke, reason: merged with bridge method [inline-methods] */
            public final ViewModelStore m2invoke() {
                return mainActivity.getViewModelStore();
            }
        }, new Function0<ViewModelProvider.Factory>() { // from class: com.example.MainActivity$special$$inlined$viewModels$default$1
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            {
                super(0);
            }

            /* renamed from: invoke, reason: merged with bridge method [inline-methods] */
            public final ViewModelProvider.Factory m1invoke() {
                return mainActivity.getDefaultViewModelProviderFactory();
            }
        }, new Function0<CreationExtras>() { // from class: com.example.MainActivity$special$$inlined$viewModels$default$3
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
            {
                super(0);
            }

            /* renamed from: invoke, reason: merged with bridge method [inline-methods] */
            public final CreationExtras m3invoke() {
                CreationExtras creationExtras;
                Function0 function02 = function0;
                return (function02 == null || (creationExtras = (CreationExtras) function02.invoke()) == null) ? mainActivity.getDefaultViewModelCreationExtras() : creationExtras;
            }
        });
    }

    private final MainViewModel getViewModel() {
        return (MainViewModel) this.viewModel.getValue();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(128);
        EdgeToEdge.enable$default(this, (SystemBarStyle) null, (SystemBarStyle) null, 3, (Object) null);
        AudioSignalEngine audioSignalEngine = AudioSignalEngine.INSTANCE;
        Context applicationContext = getApplicationContext();
        Intrinsics.checkNotNullExpressionValue(applicationContext, "getApplicationContext(...)");
        audioSignalEngine.init(applicationContext);
        UserRuleRegistry userRuleRegistry = UserRuleRegistry.INSTANCE;
        Context applicationContext2 = getApplicationContext();
        Intrinsics.checkNotNullExpressionValue(applicationContext2, "getApplicationContext(...)");
        userRuleRegistry.init(applicationContext2);
        WebSocketTradeRelay webSocketTradeRelay = WebSocketTradeRelay.INSTANCE;
        Context applicationContext3 = getApplicationContext();
        Intrinsics.checkNotNullExpressionValue(applicationContext3, "getApplicationContext(...)");
        webSocketTradeRelay.init(applicationContext3);
        HttpTradeRelay.INSTANCE.setWebhookUrl(HttpTradeRelay.DEFAULT_HTTP_URL);
        WebSocketTradeRelay.INSTANCE.setServerUrl(WebSocketTradeRelay.DEFAULT_SERVER_URL);
        WebSocketTradeRelay.INSTANCE.start();
        ComponentActivityKt.setContent$default(this, (CompositionContext) null, ComposableLambdaKt.composableLambdaInstance(-601144069, true, new Function2() { // from class: com.example.MainActivity$$ExternalSyntheticLambda0
            public final Object invoke(Object obj, Object obj2) {
                return MainActivity.onCreate$lambda$1(MainActivity.this, (Composer) obj, ((Integer) obj2).intValue());
            }
        }), 1, (Object) null);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static final Unit onCreate$lambda$1(final MainActivity this$0, Composer $composer, int $changed) {
        ComposerKt.sourceInformation($composer, "C115@5474L69,115@5455L88:MainActivity.kt#to5c3");
        if (($changed & 3) == 2 && $composer.getSkipping()) {
            $composer.skipToGroupEnd();
        } else {
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventStart(-601144069, $changed, -1, "com.example.MainActivity.onCreate.<anonymous> (MainActivity.kt:115)");
            }
            ThemeKt.MyApplicationTheme(false, false, ComposableLambdaKt.rememberComposableLambda(434770631, true, new Function2() { // from class: com.example.MainActivity$$ExternalSyntheticLambda1
                public final Object invoke(Object obj, Object obj2) {
                    return MainActivity.onCreate$lambda$1$lambda$0(MainActivity.this, (Composer) obj, ((Integer) obj2).intValue());
                }
            }, $composer, 54), $composer, 384, 3);
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventEnd();
            }
        }
        return Unit.INSTANCE;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static final Unit onCreate$lambda$1$lambda$0(MainActivity this$0, Composer $composer, int $changed) {
        ComposerKt.sourceInformation($composer, "C116@5492L37:MainActivity.kt#to5c3");
        if (($changed & 3) == 2 && $composer.getSkipping()) {
            $composer.skipToGroupEnd();
        } else {
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventStart(434770631, $changed, -1, "com.example.MainActivity.onCreate.<anonymous>.<anonymous> (MainActivity.kt:116)");
            }
            MainActivityKt.QuantVisionApp(this$0.getViewModel(), $composer, 0);
            if (ComposerKt.isTraceInProgress()) {
                ComposerKt.traceEventEnd();
            }
        }
        return Unit.INSTANCE;
    }

    protected void onResume() {
        super.onResume();
        getViewModel().startScanning();
    }

    protected void onPause() {
        super.onPause();
        getViewModel().stopScanning();
    }

    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        getViewModel().onTrimMemory(level);
    }

    protected void onDestroy() {
        super.onDestroy();
        WebSocketTradeRelay.INSTANCE.stop();
        AudioSignalEngine.INSTANCE.shutdown();
    }
}
