# Multi-Timeframe Movement Classification Test Matrix
**Quant Vision Version 41 (`mtf-movement-complete-v41`)**

This document specifies the verification matrix covering all deterministic movement behaviors, combinations, and boundaries tested in `MovementClassificationEngineTest` and `TradingOutputParserTest`.

---

## 1. The 27 Multi-Timeframe Combinations ($3 \times 3 \times 3$)

Each combination tests $5\text{m} \in \{\text{UP}, \text{FLAT}, \text{DOWN}\} \times 60\text{m} \in \{\text{UP}, \text{FLAT}, \text{DOWN}\} \times 1\text{D} \in \{\text{UP}, \text{FLAT}, \text{DOWN}\}$:

| # | 5m State | 60m State | 1D State | Primary Movement Family | Daily Context | Warning | Status |
|---|:---|:---|:---|:---|:---|:---|:---|
| 1 | UP (+0.40%) | UP (+0.50%) | UP (+1.20%) | `STEADY_ALIGNED_UP` | `DAILY_CONFIRMED` | No | PASS |
| 2 | UP (+0.40%) | UP (+0.50%) | FLAT (+0.05%) | `STEADY_ALIGNED_UP` | `DAILY_NEUTRAL` | No | PASS |
| 3 | UP (+0.40%) | UP (+0.50%) | DOWN (-1.20%) | `STEADY_ALIGNED_UP` | `DAILY_CONFLICT` | Yes | PASS |
| 4 | UP (+0.40%) | FLAT (+0.05%) | UP (+1.20%) | `DIRECTION_CONFLICT` / `BREAKOUT` | `DAILY_BULLISH` | Yes | PASS |
| 5 | UP (+0.40%) | FLAT (+0.05%) | FLAT (+0.05%) | `DIRECTION_CONFLICT` / `BREAKOUT` | `DAILY_NEUTRAL` | Yes | PASS |
| 6 | UP (+0.40%) | FLAT (+0.05%) | DOWN (-1.20%) | `DIRECTION_CONFLICT` | `DAILY_CONFLICT` | Yes | PASS |
| 7 | UP (+0.40%) | DOWN (-0.50%) | UP (+1.20%) | `DOWNTREND_PULLBACK_UP` | `DAILY_BULLISH` | Yes | PASS |
| 8 | UP (+0.40%) | DOWN (-0.50%) | FLAT (+0.05%) | `DOWNTREND_PULLBACK_UP` | `DAILY_NEUTRAL` | Yes | PASS |
| 9 | UP (+0.40%) | DOWN (-0.50%) | DOWN (-1.20%) | `DOWNTREND_PULLBACK_UP` | `DAILY_CONFIRMED` | Yes | PASS |
| 10 | FLAT (+0.05%) | UP (+0.50%) | UP (+1.20%) | `DIRECTION_CONFLICT` / `STEADY` | `DAILY_BULLISH` | Yes | PASS |
| 11 | FLAT (+0.05%) | UP (+0.50%) | FLAT (+0.05%) | `DIRECTION_CONFLICT` / `STEADY` | `DAILY_NEUTRAL` | Yes | PASS |
| 12 | FLAT (+0.05%) | UP (+0.50%) | DOWN (-1.20%) | `DIRECTION_CONFLICT` | `DAILY_CONFLICT` | Yes | PASS |
| 13 | FLAT (+0.05%) | FLAT (+0.05%) | UP (+1.20%) | `STILL_NO_TRADE` | `DAILY_BULLISH` | No | PASS |
| 14 | FLAT (+0.05%) | FLAT (+0.05%) | FLAT (+0.05%) | `STILL_NO_TRADE` | `DAILY_NEUTRAL` | No | PASS |
| 15 | FLAT (+0.05%) | FLAT (+0.05%) | DOWN (-1.20%) | `STILL_NO_TRADE` | `DAILY_BEARISH` | No | PASS |
| 16 | FLAT (+0.05%) | DOWN (-0.50%) | UP (+1.20%) | `DIRECTION_CONFLICT` | `DAILY_CONFLICT` | Yes | PASS |
| 17 | FLAT (+0.05%) | DOWN (-0.50%) | FLAT (+0.05%) | `DIRECTION_CONFLICT` | `DAILY_NEUTRAL` | Yes | PASS |
| 18 | FLAT (+0.05%) | DOWN (-0.50%) | DOWN (-1.20%) | `DIRECTION_CONFLICT` | `DAILY_BEARISH` | Yes | PASS |
| 19 | DOWN (-0.40%) | UP (+0.50%) | UP (+1.20%) | `UPTREND_PULLBACK_DOWN` | `DAILY_CONFIRMED` | Yes | PASS |
| 20 | DOWN (-0.40%) | UP (+0.50%) | FLAT (+0.05%) | `UPTREND_PULLBACK_DOWN` | `DAILY_NEUTRAL` | Yes | PASS |
| 21 | DOWN (-0.40%) | UP (+0.50%) | DOWN (-1.20%) | `UPTREND_PULLBACK_DOWN` | `DAILY_CONFLICT` | Yes | PASS |
| 22 | DOWN (-0.40%) | FLAT (+0.05%) | UP (+1.20%) | `DIRECTION_CONFLICT` | `DAILY_CONFLICT` | Yes | PASS |
| 23 | DOWN (-0.40%) | FLAT (+0.05%) | FLAT (+0.05%) | `DIRECTION_CONFLICT` | `DAILY_NEUTRAL` | Yes | PASS |
| 24 | DOWN (-0.40%) | FLAT (+0.05%) | DOWN (-1.20%) | `DIRECTION_CONFLICT` | `DAILY_BEARISH` | Yes | PASS |
| 25 | DOWN (-0.40%) | DOWN (-0.50%) | UP (+1.20%) | `STEADY_ALIGNED_DOWN` | `DAILY_CONFLICT` | Yes | PASS |
| 26 | DOWN (-0.40%) | DOWN (-0.50%) | FLAT (+0.05%) | `STEADY_ALIGNED_DOWN` | `DAILY_NEUTRAL` | No | PASS |
| 27 | DOWN (-0.40%) | DOWN (-0.50%) | DOWN (-1.20%) | `STEADY_ALIGNED_DOWN` | `DAILY_CONFIRMED` | No | PASS |

---

## 2. Dynamic Movement Scenario Suite

| Test Case | Condition Under Test | Expected Behavior Code | Stage | Verified |
|:---|:---|:---|:---|:---|
| `topFakeoutRisk_detectedCorrectly` | Prev $5m > 0$, Curr $5m < 0$, $60m \ge +0.20\%$ | `TOP_FAKEOUT_RISK` | WARNING | PASS |
| `bottomFakeoutRisk_detectedCorrectly` | Prev $5m < 0$, Curr $5m > 0$, $60m \le -0.20\%$ | `BOTTOM_FAKEOUT_RISK` | WARNING | PASS |
| `downtrendPullbackUp_detected` | $5m > 0$, $60m < -0.10\%$, no multi-frame recovery | `DOWNTREND_PULLBACK_UP` | DEVELOPING | PASS |
| `uptrendPullbackDown_detected` | $5m < 0$, $60m > +0.10\%$, no multi-frame decline | `UPTREND_PULLBACK_DOWN` | DEVELOPING | PASS |
| `bullishReversalAttempt_detected` | Early positive history opposing negative $60m$ | `BULLISH_REVERSAL_ATTEMPT` | DEVELOPING | PASS |
| `bearishReversalAttempt_detected` | Early negative history opposing positive $60m$ | `BEARISH_REVERSAL_ATTEMPT` | DEVELOPING | PASS |
| `confirmedBullishReversal_multiframe` | $\ge 3$ consecutive positive $5m$, $netShort \ge +0.20\%$ | `CONFIRMED_BULLISH_REVERSAL` | CONFIRMED | PASS |
| `confirmedBearishReversal_multiframe` | $\ge 3$ consecutive negative $5m$, $netShort \le -0.20\%$ | `CONFIRMED_BEARISH_REVERSAL` | CONFIRMED | PASS |
| `alternationRequiresFourSnapshots` | $\ge 2$ direction flips over 4 snapshots | `REPEATED_ALTERNATION` | WARNING | PASS |
| `suddenSpikeUp_requiresTwoStillSnapshots`| 2 previous quiescent frames + curr $5m \ge +0.40\%$ | `SUDDEN_SPIKE_UP` | WARNING | PASS |
| `suddenSpikeDown_requiresTwoStillSnapshots`| 2 previous quiescent frames + curr $5m \le -0.40\%$ | `SUDDEN_SPIKE_DOWN` | WARNING | PASS |
| `upwardMomentumBreak_detected` | $5m \ge +0.50\%$, $netShort \ge +0.60\%$ + history | `UPWARD_MOMENTUM_BREAK` | CONFIRMED | PASS |
| `downwardMomentumBreak_detected` | $5m \le -0.50\%$, $netShort \le -0.60\%$ + history | `DOWNWARD_MOMENTUM_BREAK` | CONFIRMED | PASS |
| `momentumLossUp_detected` | Deceleration into $[0.0, 0.10\%]$ with $60m \ge +0.20\%$ | `MOMENTUM_LOSS_UP` | EXHAUSTING | PASS |
| `momentumLossDown_detected` | Deceleration into $[-0.10\%, 0.0]$ with $60m \le -0.20\%$ | `MOMENTUM_LOSS_DOWN` | EXHAUSTING | PASS |
| `stillNoTrade_detected` | Both $|5m| \le 0.10\%$ and $|60m| \le 0.10\%$ | `STILL_NO_TRADE` | CONFIRMED | PASS |
| `invalidData_returnsDataUnavailable` | Null values or `UNAVAILABLE` quality | `DATA_UNAVAILABLE` | UNKNOWN | PASS |
| `ambiguousData_returnsDataAmbiguous` | `AMBIGUOUS` quality flag | `DATA_AMBIGUOUS` | UNKNOWN | PASS |
| `provisionalData_returnsProvisional` | `isProvisional = true` | `PROVISIONAL_DATA` | PRELIMINARY | PASS |
| `approximateData_returnsApproximate` | `isApproximate = true` | `APPROXIMATE_DATA` | PRELIMINARY | PASS |
| `canonicalNonOverwriteContract` | Movement calculation never modifies direction/percentages | Mathematical Invariance | N/A | PASS |

---

## 3. Threshold Boundary Matrix

- **No-Trade Deadband**: $\le 0.10\%$ (Neutral), $> 0.10\%$ (Active)
- **Momentum Break Threshold**: $\ge 0.50\%$ (5m) & $\ge 0.60\%$ (Net)
- **Reversal Net Threshold**: $\ge 0.20\%$
- **Fakeout Context Threshold**: $|val60m| \ge 0.20\%$
- **Spike Quiescent Bound**: $\le 0.10\%$; Spike Velocity: $\ge 0.40\%$
- **Outlier Boundary**: $|value| > 500.0\% \to$ Clamped / Flagged Unavailable
