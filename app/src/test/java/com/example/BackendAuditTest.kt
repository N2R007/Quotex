package com.example

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.GeminiVisionClient
import com.example.data.models.EngineMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackendAuditTest {

    @Test
    fun placeholderApiKey_rejectsAllSpecifiedPlaceholderVariants() {
        val placeholdersToReject = listOf(
            "",
            "   ",
            null,
            "MY_GEMINI_API_KEY",
            "DEFAULT_KEY",
            "your_api_key_here",
            "YOUR_API_KEY_HERE",
            "YOUR_GEMINI_API_KEY",
            "your_gemini_api_key",
            "your-api-key-here",
            "Your-Api-Key-Here",
            "  your_api_key_here  ",
            "  DEFAULT_KEY  "
        )

        for (key in placeholdersToReject) {
            assertTrue(
                "Key '$key' must be rejected as placeholder",
                MainViewModel.isPlaceholderApiKey(key)
            )
        }
    }

    @Test
    fun placeholderApiKey_acceptsRealLookingKeys() {
        val validKeys = listOf(
            "AIzaSyD1234567890abcdefghijklmnopqr",
            "AIzaSyA_sample_valid_gemini_api_key_99",
            "sample_real_key_xyz123"
        )

        for (key in validKeys) {
            assertFalse(
                "Key '$key' must NOT be rejected as placeholder",
                MainViewModel.isPlaceholderApiKey(key)
            )
        }
    }

    @Test
    fun geminiModelCandidates_doesNotContainObsoleteModels() {
        val candidates = GeminiVisionClient.MODEL_CANDIDATES

        assertFalse(
            "MODEL_CANDIDATES must not contain gemini-2.0-flash",
            candidates.contains("gemini-2.0-flash")
        )
        assertFalse(
            "MODEL_CANDIDATES must not contain gemini-1.5-flash",
            candidates.contains("gemini-1.5-flash")
        )
        assertTrue(
            "MODEL_CANDIDATES must contain primary candidate gemini-3.6-flash",
            candidates.contains("gemini-3.6-flash")
        )
        assertTrue(
            "MODEL_CANDIDATES must contain secondary fallback candidate gemini-3.5-flash",
            candidates.contains("gemini-3.5-flash")
        )
    }

    @Test
    fun localMode_withoutApiKey_runsLocalAnalysisWithoutError() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = MainViewModel(app)
        vm.setApiKey("") // empty API key
        vm.setEngineMode(EngineMode.LOCAL)

        assertEquals("", vm.uiState.value.apiKey)
        assertEquals(EngineMode.LOCAL, vm.uiState.value.engineMode)

        // Select sample scenario
        vm.selectSampleScenario(0)

        waitForCondition {
            vm.uiState.value.currentAnalysis != null && !vm.uiState.value.isProcessing
        }

        val analysis = vm.uiState.value.currentAnalysis
        assertNotNull("Local analysis must succeed without API key", analysis)
        assertTrue("Analysis in LOCAL mode must be successful", analysis!!.isSuccess)
        assertNull("LOCAL mode without API key must produce no error", vm.uiState.value.error)
    }

    @Test
    fun autoMode_withoutApiKey_routesDirectlyToLocalEngine() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = MainViewModel(app)
        vm.setApiKey("your_api_key_here") // placeholder key
        assertEquals("", vm.uiState.value.apiKey) // must be sanitized to empty

        vm.setEngineMode(EngineMode.AUTO)
        assertEquals(EngineMode.AUTO, vm.uiState.value.engineMode)

        vm.selectSampleScenario(0)

        waitForCondition {
            vm.uiState.value.currentAnalysis != null && !vm.uiState.value.isProcessing
        }

        val analysis = vm.uiState.value.currentAnalysis
        assertNotNull("AUTO mode must succeed without valid cloud key by routing locally", analysis)
        assertTrue(analysis!!.isSuccess)
        assertNull("AUTO mode must not expose error when routing locally", vm.uiState.value.error)
    }

    @Test
    fun cloudMode_withoutApiKey_safelyExecutesOfflineWithoutNetworkRequest() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = MainViewModel(app)
        vm.setApiKey("") // empty key
        vm.setEngineMode(EngineMode.CLOUD)

        assertEquals(EngineMode.CLOUD, vm.uiState.value.engineMode)

        // Select sample scenario in CLOUD mode - must execute deterministically offline
        vm.selectSampleScenario(0)

        waitForCondition {
            vm.uiState.value.currentAnalysis != null && !vm.uiState.value.isProcessing
        }

        val state = vm.uiState.value
        assertNotNull("Must generate analysis offline without cloud network call", state.currentAnalysis)
        assertTrue("Analysis must be successful offline", state.currentAnalysis!!.isSuccess)
        assertNull("Strictly offline execution must not raise missing API key error", state.error)
    }

    private fun waitForCondition(timeoutMs: Long = 4000L, condition: () -> Boolean) {
        val start = System.currentTimeMillis()
        while (!condition() && System.currentTimeMillis() - start < timeoutMs) {
            ShadowLooper.idleMainLooper()
            Thread.sleep(25)
        }
        assertTrue("Condition timed out after ${timeoutMs}ms", condition())
    }
}
