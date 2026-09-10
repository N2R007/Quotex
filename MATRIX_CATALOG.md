# 165 Deterministic Quantitative Decision Matrices Catalog

## 1. Executive Summary & Honesty Guarantee

Quant Vision operates using **165 Deterministic Quantitative Decision Matrices** (`M001` through `M165`).
- **Zero Quantum Claims**: This system does NOT use quantum computing, quantum circuits, quantum simulators, or qubits. It is a strictly classical, deterministic rule-based quantitative reasoning engine running natively on-device.
- **Zero Random / Fake Values**: No synthetic confidence percentages, no randomized predictions, no placeholder success rates, and no simulated outputs are ever produced.
- **Offline On-Device Inference**: Screen OCR capture and all 165 decision matrices evaluate natively on the local device without cloud dependencies, API quota requirements, or telemetry transmission.
- **Local Network Execution Webhook**: Auto-trade dispatch operates via a local HTTP webhook (`/api/trade`) on the user's local network/laptop environment. Offline status applies to detection and quantitative evaluation, while execution signals travel through the configured local webhook.

---

## 2. Matrix Architecture & Category Breakdown

The 165 matrices are structured across 11 functional categories:

| Category | Matrix ID Range | Count | Primary Objective |
|---|---|---|---|
| **1. Data Quality & Input Validation** | `M001` – `M014` | 14 | Validates OCR consistency, detection readiness, sanity bounds, and flags bad frames |
| **2. Dead Zone & Minimum Magnitude** | `M015` – `M024` | 10 | Identifies market chop, sub-threshold movements, and strictly enforces NO-TRADE protection |
| **3. Aligned Bullish Multi-Timeframe** | `M025` – `M044` | 20 | Identifies confluence across 5m, 60m, and 1D upward momentum vectors |
| **4. Aligned Bearish Multi-Timeframe** | `M045` – `M064` | 20 | Identifies confluence across 5m, 60m, and 1D downward momentum vectors |
| **5. Counter-Trend Pullback & Divergence** | `M065` – `M080` | 16 | Differentiates healthy counter-trend retracements from trend continuations |
| **6. Momentum Deceleration & Exhaustion** | `M081` – `M094` | 14 | Detects waning velocity before price inflection |
| **7. Reversal Trap & False Breakout** | `M095` – `M108` | 14 | Safeguards capital against bull/bear traps and sudden liquidity wicks |
| **8. Alternation Chop & Compression** | `M109` – `M120` | 12 | Classifies cyclical oscillation and volatility squeeze conditions |
| **9. Macro Confluence & Regime Filters** | `M121` – `M132` | 12 | Integrates daily trend anchors and macro risk parameters |
| **10. Micro-Volatility & Asymmetric Ratios** | `M133` – `M150` | 18 | Captures micro-volatility impulses, harmonic confluence, and volatility squeezes |
| **11. Multi-Timeframe Confluence & Arbitrage** | `M151` – `M165` | 15 | High-order confluence arbitration across 5m, 60m, and 1D horizons |
| **TOTAL** | `M001` – `M165` | **165** | Complete deterministic quantitative decision space |

---

## 3. Detailed Matrix Specifications

Each matrix defines an explicit contract:
- **ID**: Canonical identifier (`M001` - `M165`).
- **Priority**: Lower integer indicates higher precedence during evaluation conflict resolution.
- **Inputs Required**: Subset of `{CHANGE_5M, CHANGE_60M, CHANGE_1D, SNAPSHOT_HISTORY, DATA_QUALITY}`.
- **Condition Description**: Explicit mathematical logic.
- **Output Code**: Machine-readable token.
- **Direction**: `UP`, `DOWN`, `NEUTRAL`, or `NO_TRADE`.
- **Risk Level**: `LOW`, `MODERATE`, `HIGH`, `EXTREME`, or `NO_TRADE`.
- **Test Vector**: Deterministic input snapshot with expected match boolean.

### Category 1: Data Quality & Input Validation (`M001` - `M014`)
* **M001 (Null/Incomplete Metric Set)**: Priority 1. Condition: `val5m == null || val60m == null`. Risk: `NO_TRADE`. Output: `ERR_DATA_INCOMPLETE`. Direction: `NEUTRAL`.
* **M002 (Corrupted OCR Digit Pattern)**: Priority 1. Condition: Unverified OCR or unparseable text. Risk: `NO_TRADE`. Output: `ERR_OCR_UNVERIFIED`. Direction: `NEUTRAL`.
* **M003 (Unrealistic Flash Spike Artifact)**: Priority 2. Condition: `|val5m| > 30.0% && |val60m| < 1.0%`. Risk: `EXTREME`. Output: `WARN_EXTREME_OUTLIER`. Direction: `NEUTRAL`.
* **M004 (Zero Frame Read Delay)**: Priority 2. Condition: Metrics unchanged across consecutive OCR captures. Output: `DATA_STALE_CACHE`.
* **M005 (Inverted Timeframe Timestamp)**: Priority 2. Condition: 5m frame timestamp older than 60m frame timestamp. Output: `ERR_TIMESTAMP_ANOMALY`.
* **M006 (Extreme Noise Ratio)**: Priority 3. Condition: Rapid oscillation with zero net directional progress. Output: `WARN_HIGH_NOISE_FLOOR`.
* **M007 (Partial OCR Decimal Loss)**: Priority 2. Condition: Integer reading with missing decimal separator. Output: `ERR_DECIMAL_LOSS`.
* **M008 (Scale Mismatch Artifact)**: Priority 2. Condition: 5m metric on crypto scale, 60m metric on forex pip scale. Output: `ERR_SCALE_MISMATCH`.
* **M009 (Missing 1D Macro Anchor)**: Priority 5. Condition: 1D unavailable, requires heightened caution. Output: `WARN_MISSING_1D_ANCHOR`.
* **M010 (Sub-Zero Duration Gap)**: Priority 2. Condition: Negative interval between consecutive frames. Output: `ERR_NEGATIVE_DURATION`.
* **M011 (Text Confidence Under Threshold)**: Priority 3. Condition: OCR confidence score < 65%. Output: `WARN_LOW_OCR_CONFIDENCE`.
* **M012 (Single-Point Discontinuity)**: Priority 3. Condition: Instantaneous 1-tick anomaly reverting on subsequent frame. Output: `WARN_TICK_ANOMALY`.
* **M013 (Unverified Input Format)**: Priority 2. Condition: Input string does not match `[+-]?\d+\.?\d*%?`. Output: `ERR_FORMAT_MISMATCH`.
* **M014 (Valid Verified Metrics Set)**: Priority 10. Condition: Standard verified metrics within normal volatility ranges. Output: `OK_DATA_VERIFIED`.

### Category 2: Dead Zone & Minimum Magnitude (`M015` - `M024`)
* **M015 (Micro-Fluctuation Dead Zone)**: Priority 5. Condition: `|val5m| < 0.05% && |val60m| < 0.05%`. Output: `DEAD_ZONE_MICRO`. Direction: `NEUTRAL`. Risk: `NO_TRADE`.
* **M016 (Sub-Threshold Drift)**: Priority 6. Condition: `|val5m| < 0.10% && |val60m| < 0.15%`. Output: `SUB_THRESHOLD_DRIFT`. Direction: `NEUTRAL`. Risk: `NO_TRADE`.
* **M017 (Flat 5m against Moderate 60m)**: Priority 6. Condition: `|val5m| < 0.04% && |val60m| >= 0.50%`. Output: `5M_FLAT_STAGNATION`. Direction: `NEUTRAL`.
* **M018 (Flat 60m against Spurious 5m)**: Priority 6. Condition: `|val5m| >= 0.30% && |val60m| < 0.05%`. Output: `60M_ANCHOR_FLAT`. Direction: `NEUTRAL`.
* **M019 (Zero Net Sum Compression)**: Priority 5. Condition: `|val5m + val60m| < 0.02% && |val5m| > 0.10%`. Output: `NET_ZERO_COMPRESSION`. Direction: `NEUTRAL`. Risk: `NO_TRADE`.
* **M020 (Low Volatility Rangebound)**: Priority 7. Condition: Trailing 5m range < 0.08% for > 15 minutes. Output: `RANGEBOUND_STAGNANT`. Direction: `NEUTRAL`. Risk: `NO_TRADE`.
* **M021 (Spread Exceeds Expected Move)**: Priority 4. Condition: Bid-ask spread wider than 5m delta. Output: `SPREAD_PROHIBITIVE`. Direction: `NEUTRAL`. Risk: `NO_TRADE`.
* **M022 (Weekend Illiquidity Pocket)**: Priority 4. Condition: Low tick rate + flat delta. Output: `ILLIQUID_POCKET`. Direction: `NEUTRAL`. Risk: `NO_TRADE`.
* **M023 (Lunch-Hour Flatline)**: Priority 7. Condition: Mid-session velocity drop below 0.03%/min. Output: `SESSION_FLATLINE`. Direction: `NEUTRAL`. Risk: `NO_TRADE`.
* **M024 (Pre-Breakout Volatility Squeeze)**: Priority 6. Condition: Both 5m and 60m delta within lowest 5th percentile band. Output: `VOLATILITY_COILING`. Direction: `NEUTRAL`. Risk: `NO_TRADE`.

### Category 3: Aligned Bullish Multi-Timeframe (`M025` - `M044`)
* **M025 (Standard Confluent Bullish)**: Priority 8. Condition: `val5m > 0.20% && val60m > 0.40%`. Output: `ALIGNED_BULLISH_STD`. Direction: `UP`. Risk: `LOW`.
* **M026 (Accelerated Bullish Momentum)**: Priority 7. Condition: `val5m > 0.60% && val60m > 0.80% && val5m > val60m * 0.5`. Output: `BULLISH_ACCELERATING`. Direction: `UP`. Risk: `LOW`.
* **M027 (Macro-Anchor Confluent Bullish)**: Priority 7. Condition: `val5m > 0.25% && val60m > 0.50% && val1d > 0.50%`. Output: `TRIPLE_ALIGNED_BULLISH`. Direction: `UP`. Risk: `LOW`.
* **M028 (Institutional Breakout Flow)**: Priority 6. Condition: `val5m > 1.20% && val60m > 1.50%`. Output: `INSTITUTIONAL_BULLISH_FLOW`. Direction: `UP`. Risk: `MODERATE`.
* **M029 to M044**: Cover varying degrees of impulse, trend continuation, volume support, and velocity acceleration vectors with explicit thresholds. Direction: `UP`.

### Category 4: Aligned Bearish Multi-Timeframe (`M045` - `M064`)
* **M045 (Standard Confluent Bearish)**: Priority 8. Condition: `val5m < -0.20% && val60m < -0.40%`. Output: `ALIGNED_BEARISH_STD`. Direction: `DOWN`. Risk: `LOW`.
* **M046 (Accelerated Bearish Momentum)**: Priority 7. Condition: `val5m < -0.60% && val60m < -0.80% && val5m < val60m * 0.5`. Output: `BEARISH_ACCELERATING`. Direction: `DOWN`. Risk: `LOW`.
* **M047 (Macro-Anchor Confluent Bearish)**: Priority 7. Condition: `val5m < -0.25% && val60m < -0.50% && val1d < -0.50%`. Output: `TRIPLE_ALIGNED_BEARISH`. Direction: `DOWN`. Risk: `LOW`.
* **M048 (Cascade Liquidation Flow)**: Priority 6. Condition: `val5m < -1.20% && val60m < -1.50%`. Output: `LIQUIDATION_BEARISH_FLOW`. Direction: `DOWN`. Risk: `MODERATE`.
* **M049 to M064**: Cover varying degrees of sell-off velocity, breakdown confirmation, downward continuation, and volume pressure vectors. Direction: `DOWN`.

### Categories 5-11: Retracements, Exhaustion, Traps, Chop, Confluence & Squeeze Regimes (`M065` - `M165`)
* Detailed rules evaluate pullbacks within wider trends (`M065` - `M080`), velocity deceleration preceding inflection points (`M081` - `M094`), bull/bear trap identification (`M095` - `M108`), cyclical chop classification (`M109` - `M120`), daily macro anchor alignment (`M121` - `M132`), micro-volatility impulse & asymmetric squeeze ratios (`M133` - `M150`), and high-order multi-timeframe confluence arbitration (`M151` - `M165`).

---

## 4. Deterministic Evaluation Engine Logic

1. **Context Construction**: Form `MatrixEvaluationContext` containing separate `rawNetSum` (`val5m + val60m`) and `normalizedMovement` (velocity-weighted RMP score), 1D macro anchor, data quality flag, and historical snapshot sequence.
2. **Sequential Matrix Testing**: Every matrix in `MatrixCatalog.allMatrices` (165 total) executes its condition function against the context.
3. **Filtering & Deduplication**:
   - Collect all matching matrices.
   - Filter out warning-only matrices from primary directional voting.
4. **Precedence Ranking**: Sort matching matrices by priority (higher number = higher precedence / priority, e.g. Safety Matrices at Priority 850-1000 take absolute precedent over standard trends), then by specificity of condition.
5. **Conflict Interlock & Hard HOLD**:
   - If both bullish (`UP`) and bearish (`DOWN`) matrices match simultaneously with opposing conviction (or 5m and 60m are opposing beyond noise threshold):
   - Invoke **Hard HOLD Rule**: Force directional bias strictly to `NEUTRAL`, reset directional balance to exactly `50.0% UP / 50.0% DOWN`, activate `isNoTradeZone = true`, suppress audio alerts (`SOUND_NONE`), and block execution. No net-sum arithmetic is permitted to override this safety interlock.
6. **1D Macro Anchor Interlock**:
   - If 1D heavily opposes intraday momentum (`1D <= -1.50%` against `UP` or `1D >= +1.50%` against `DOWN`), intraday direction is blocked as a high-risk counter-trend trap (`NEUTRAL` / `isNoTradeZone = true`).
   - If 1D moderately opposes intraday momentum (`|1D| in 0.30%..1.50%`), directional alignment score is scaled down proportionally to reflect macro headwind resistance.
7. **Core Timeframe & Data Quality Prerequisites**:
   - Both `5m` and `60m` are mandatory core inputs. If either is missing, direction defaults to `NEUTRAL` (`NO_TRADE`).
   - Approximate OCR (`isApproximate == true`) or unverified data quality (`dataQuality != VERIFIED`) strictly revokes `executionEligibility` to prevent spurious automated orders.
8. **Output Assembly**: Populate `MatrixEvaluationResult` and `CanonicalDecision` with evidence score, directional alignment balance (formula-based confluence, not statistical probability), separated strength classification (`NORMAL`, `MEDIUM`, `HIGH`), and full audit decision trace.
