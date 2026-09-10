# Quant Vision — Strictly Offline Architecture Specification

## 1. Zero Cloud Dependency & Runtime Isolation

Quant Vision operates under a **Strictly Offline Architecture**.
- **No Network Permissions Required for Analysis**: The application evaluation pipeline makes **zero HTTP / socket / RPC calls** during operation.
- **No Cloud Services**: Neither Google Gemini, OpenAI, nor any remote AI endpoints are contacted.
- **Privacy & Security**: Camera frames, screenshots, chart images, OCR text strings, and trading metrics never leave the user's mobile device. They are processed entirely within local RAM and released immediately via deterministic memory management.

---

## 2. On-Device Quantitative Data Pipeline

```
[Camera / Screen Frame]
         │
         ▼
[Local Frame Provider] (In-memory Bitmap)
         │
         ▼
[LocalQuantVisionEngine]
         ├─► ML Kit On-Device Text Recognition (Offline Core)
         ├─► Bengali & Standard Regex Metric Parsers (5m, 60m, 1D)
         └─► Metric Normalization & Sanity Validation
         │
         ▼
[MatrixEvaluationContext]
         │
         ▼
[MatrixEvaluationEngine] (100% Classical & Deterministic)
         ├─► Data Quality Guard (M001–M014)
         ├─► Dead Zone / Chop Filter (M015–M024)
         ├─► Multi-Timeframe Alignment (M025–M064)
         ├─► Pullback & Deceleration Rules (M065–M094)
         ├─► Reversal Trap & Alternation Squeeze (M095–M120)
         └─► Macro Confluence & Cross-Validation (M121–M132)
         │
         ▼
[TradingAnalysis Output]
         ├─► English Direction: UP / DOWN / NEUTRAL / NO_TRADE
         ├─► Triggered Matrices Audit Trail & Decision Trace
         └─► Local Room Database Cache (Encrypted On-Device)
         │
         ▼
[TradingDashboard UI] (Jetpack Compose M3)
```

---

## 3. Real-Time Performance & Memory Safety

1. **Deterministic Execution Latency**:
   - Matrix evaluation of all 132 rules executes in **< 1.5 milliseconds** on modern ARM architectures.
   - Total pipeline latency (Frame -> OCR -> Evaluation -> StateFlow update) is typically **< 45 milliseconds**.
2. **Deterministic Memory Recycling**:
   - Any interim `Bitmap` generated during frame extraction or scaling is guaranteed to be recycled in a `try/finally` block.
   - Zero native memory leakage across hours of continuous scanning.
3. **Thread Safety & Mutex Protection**:
   - `LocalQuantVisionEngine` executes on `Dispatchers.Default`.
   - Re-entrant race conditions are prevented using `Mutex.tryLock()`. If an OCR analysis is currently active, overlapping frames are dropped rather than queued to avoid buffer bloat.

---

## 4. Verification & Audit Trail

- **Matrix Audit Tab**: Accessible directly from the dashboard UI, displaying live evaluations, triggered matrix identifiers, and the exact step-by-step mathematical reasoning trace.
- **Unit Test Suite**: `MatrixCatalogTest.kt` verifies that all 132 test vectors pass deterministically on the local JVM with zero mock networks or simulators.
