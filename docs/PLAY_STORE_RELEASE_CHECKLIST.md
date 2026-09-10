# Google Play Store Release Checklist
**Quant Vision Version 41 (`mtf-movement-complete-v41`)**

## 1. Compliance & Security Verification

- [x] **Zero Hardcoded Secrets**:
  - Run verification: `grep -rn "AIza" app/src/main/java/` (Zero occurrences verified).
  - API keys managed strictly through Secrets Gradle Plugin and AI Studio Secrets panel (`BuildConfig.GEMINI_API_KEY`).
  - No secret tokens committed to source control; `.env` listed in `.gitignore`.
- [x] **Play Policy Storage & Permission Standards**:
  - Zero broad storage permissions (`READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`).
  - Camera permission (`android.permission.CAMERA`) properly declared with dynamic runtime Compose request flows.
  - Internet permission (`android.permission.INTERNET`) declared for network/OCR sync.
- [x] **SDK & Compatibility**:
  - `minSdk = 24`, `targetSdk = 35`, `compileSdk = 35` (Complies with latest Google Play requirements).
  - Kotlin version alignment and dependencies maintained in Version Catalog (`gradle/libs.versions.toml`).
- [x] **Signing & Proguard Configuration**:
  - Proguard rules configured in `app/proguard-rules.pro` for Moshi, Room, and CameraX.
  - Release build types configured with minification and resource shrinking.

---

## 2. Core Feature & Architecture Audit

- [x] **Deterministic Multi-Timeframe Engine**:
  - Multi-Timeframe Movement Classification Engine (`MovementClassificationEngine.kt`) operational.
  - All 20 movement families and 27 timeframe permutations tested and validated.
  - Canonical mathematical invariance verified: `TradeDirection`, percentages, net sums, sensitivity ratios never modified or overwritten by movement logic.
  - 1-Day macro context seamlessly integrated (`DAILY_CONFIRMED`, `DAILY_CONFLICT`).
- [x] **Bengali Localization & UX Craft**:
  - Bengali titles, action guidance, and context tags verified for clarity, readability, and non-confusing user instructions.
  - UI layout strictly preserved without unsolicited tabs, sidebars, or layout refactoring.
  - Behavior tags displayed dynamically in the Action Card.
- [x] **Memory & Hardware Lifecycle**:
  - Bitmap allocation wrapped with `try { ... } finally { bitmap.recycle() }`.
  - CameraX preview rebinds guarded against recomposition loops; hardware properly released on `onDispose { cameraProvider.unbindAll() }`.
  - Quota 429 backoff handling properly pauses frame analysis.

---

## 3. Test Suite Pass Verification

- [x] All 95 Canonical Parsing & Engine Tests Passed (`TradingOutputParserTest`).
- [x] All 27 MTF Combinations & Movement Scenario Tests Passed (`MovementClassificationEngineTest`).
- [x] 100% Green Test Suite (`gradle :app:testDebugUnitTest`).
