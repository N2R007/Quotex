# Multi-Timeframe Movement Classification Engine (MTF Logic)
**Quant Vision Version 41 (`mtf-movement-complete-v41`)**

## 1. Overview & Architectural Principles

The Multi-Timeframe (MTF) Movement Classification Engine (`MovementClassificationEngine.kt`) provides a 100% deterministic, zero-AI, client-side movement classification architecture for Quant Vision.

### Core Tenets:
1. **Mathematical Invariance**: Movement classification NEVER overwrites, mutates, or modifies canonical direction (`TradeDirection`), strength level (`StrengthLevel`), up/down percentage calculations, net sums, or audio event triggers.
2. **Deterministic Rules**: No cloud LLM, heuristic hallucination, or probabilistic ambiguity. Every state is derived via documented threshold comparisons, sign relations, and confirmed snapshot history.
3. **Multi-Timeframe Integration**:
   - **5-minute percentage (`val5m`)**: Immediate short-term momentum and entry pressure.
   - **60-minute percentage (`val60m`)**: Dominant intermediate trend context and structure.
   - **1-day percentage (`val1d`)**: Broader macro market context, daily alignment, and daily conflict filtering.
   - **Historical Snapshots (`confirmedHistory`)**: Confirmed chronological state sequence used to verify persistence, acceleration, exhaustion, fakeout, and repeated alternation.
4. **Failure-Safe & Conservative**: Invalid, ambiguous, or provisional data always produces explicit warning states (`DATA_UNAVAILABLE`, `DATA_AMBIGUOUS`, `PROVISIONAL_DATA`, `APPROXIMATE_DATA`) rather than confident directional forecasts.

---

## 2. Timeframe Roles & Threshold Matrix

| Timeframe | Purpose | Deadband / Neutral | Active Threshold | Extreme / Spike Threshold |
| :--- | :--- | :--- | :--- | :--- |
| **5-Minute (`val5m`)** | Immediate Short-Term Momentum | $[-0.10\%, +0.10\%]$ | $> +0.10\%$ / $< -0.10\%$ | $\ge +0.40\%$ / $\le -0.40\%$ |
| **60-Minute (`val60m`)** | Dominant Trend Context | $[-0.10\%, +0.10\%]$ | $> +0.10\%$ / $< -0.10\%$ | $\ge +0.20\%$ (Trend Lock) |
| **1-Day (`val1d`)** | Macro Market Context | $[-0.10\%, +0.10\%]$ | $> +0.30\%$ / $< -0.30\%$ | Daily Trend Confirmation/Conflict |

---

## 3. Strict 13-Level Precedence Hierarchy

When evaluating market state, conditions are evaluated in strict priority order. The first matched state halts further classification:

1. **Priority 1 — Data Integrity & Availability**:
   - Checks `isValid`, `val5m == null`, `val60m == null`, `dataQuality == UNAVAILABLE` $\to$ `DATA_UNAVAILABLE`
   - Checks `dataQuality == AMBIGUOUS` $\to$ `DATA_AMBIGUOUS`
2. **Priority 2 — Provisional & Approximate Data**:
   - Checks `isProvisional == true` $\to$ `PROVISIONAL_DATA`
   - Checks `isApproximate == true` $\to$ `APPROXIMATE_DATA`
3. **Priority 3 — Still / Dead / No-Trade Zone**:
   - `|val5m| <= 0.10%` AND `|val60m| <= 0.10%`, or total magnitude $\le \epsilon \to$ `STILL_NO_TRADE`
4. **Priority 4 — Repeated Alternation (Choppy Market)**:
   - Evaluates consecutive sign flips across at least 4 confirmed snapshots $\to$ `REPEATED_ALTERNATION`
5. **Priority 5 — Sudden Spike**:
   - Previous 2 snapshots quiescent/still ($\le 0.10\%$), current $|val5m| \ge 0.40\% \to$ `SUDDEN_SPIKE_UP` / `SUDDEN_SPIKE_DOWN`
6. **Priority 6 — Fakeout Risk (Bull/Bear Trap)**:
   - Top Fakeout: Prev $5m > 0$, current $5m < 0$, $60m \ge +0.20\% \to$ `TOP_FAKEOUT_RISK`
   - Bottom Fakeout: Prev $5m < 0$, current $5m > 0$, $60m \le -0.20\% \to$ `BOTTOM_FAKEOUT_RISK`
7. **Priority 7 — Confirmed Reversal**:
   - Requires at least 3 consecutive confirmed $5m$ snapshots in new direction AND $|netShort| \ge 0.20\% \to$ `CONFIRMED_BULLISH_REVERSAL` / `CONFIRMED_BEARISH_REVERSAL`
8. **Priority 8 — Momentum Loss / Exhaustion**:
   - Deceleration of $5m$ into $[0.0, 0.10\%]$ while dominant $60m$ is strongly extended $\to$ `MOMENTUM_LOSS_UP` / `MOMENTUM_LOSS_DOWN`
9. **Priority 9 — Pullbacks vs Reversal Attempts**:
   - $5m$ opposes $60m$. With history showing early multi-frame reversal signs $\to$ `BULLISH_REVERSAL_ATTEMPT` / `BEARISH_REVERSAL_ATTEMPT`. Otherwise standard counter-trend correction $\to$ `UPTREND_PULLBACK_DOWN` / `DOWNTREND_PULLBACK_UP`.
10. **Priority 10 — Momentum Break & Acceleration**:
    - $|val5m| \ge 0.50\%$ AND $|netShort| \ge 0.60\%$ with confirmed persistence $\to$ `UPWARD_MOMENTUM_BREAK` / `DOWNWARD_MOMENTUM_BREAK`.
    - Velocity expansion: $5m \ge 0.40\%$ and $5m - prev5m \ge 0.15\% \to$ `STRONG_ACCELERATION_UP` / `STRONG_ACCELERATION_DOWN`.
    - Volatility breakout: $netShort \ge 0.50\% \to$ `BREAKOUT_EXPANSION_UP` / `BREAKOUT_EXPANSION_DOWN`.
11. **Priority 11 — Steady Aligned Trend**:
    - $5m > 0$ AND $60m > 0 \to$ `STEADY_ALIGNED_UP`
    - $5m < 0$ AND $60m < 0 \to$ `STEADY_ALIGNED_DOWN`
12. **Priority 12 — Direction Conflict**:
    - Multi-timeframe contradiction without pullback or breakout resolution $\to$ `DIRECTION_CONFLICT`
13. **Priority 13 — Neutral Sideways**:
    - Baseline sideways consolidation $\to$ `NEUTRAL_SIDEWAYS`

---

## 4. Daily Context Matrix (1-Day Integration)

The daily percentage (`val1d`) provides macro market confirmation or conflict tags:
- **`DAILY_BULLISH` / `DAILY_UP`** ($val1d \ge +0.30\%$): Confirms short-term upward flows (`DAILY_CONFIRMED`). If short-term flow is downward, flags `DAILY_CONFLICT` and elevates `isWarningOnly = true`.
- **`DAILY_BEARISH` / `DAILY_DOWN`** ($val1d \le -0.30\%$): Confirms short-term downward flows (`DAILY_CONFIRMED`). If short-term flow is upward, flags `DAILY_CONFLICT` and elevates `isWarningOnly = true`.
- **`DAILY_NEUTRAL`** ($-0.10\% \le val1d \le +0.10\%$): Macro consolidation.

---

## 5. UI Presentation Contract

The existing dashboard layout and structure are strictly preserved:
- **`behaviorCode`**: Machine-readable identifier for testing and telemetry.
- **`behaviorTitle`**: High-contrast, clear Bengali title displayed in the primary action card.
- **`behaviorDescription`**: Descriptive Bengali guidance outlining context and risks.
- **`behaviorTags`**: Dynamic chips showing timeframe status (`STEADY_UP`, `DAILY_CONFIRMED`, `BULL_TRAP`, etc.).
- **`isWarningOnly`**: Triggers amber/warning borders when risks, fakeouts, or macro conflicts are detected.
- **`isProvisional`**: Visual indicator when OCR data is still preliminary.

---

## 6. Three-Timeframe Pressure Mode (Isolated Deterministic Calculation & 30s Cooldown)

In addition to the classical 165 MTF matrix, Quant Vision incorporates a dedicated deterministic **Three-Timeframe Pressure Engine** (`ThreeTimeframePressureCalculator.kt`):

1. **Deterministic Canonical Calculation**:
   - Timeframes: **5m**, **60m**, and **1D** are combined using the authoritative squared-energy formula (`ThreeTimeframePressureCalculator.kt`):
     $$\text{UP Energy} = \frac{\max(p_{5m}, 0)^2 + \max(p_{60m}, 0)^2 + \max(p_{1d}, 0)^2}{3}$$
     $$\text{DOWN Energy} = \frac{\max(-p_{5m}, 0)^2 + \max(-p_{60m}, 0)^2 + \max(-p_{1d}, 0)^2}{3}$$
     $$\text{Total Energy} = \text{UP Energy} + \text{DOWN Energy}$$
   - When $\text{Total Energy} > 0$:
     $$\text{UP Share} = \frac{\text{UP Energy}}{\text{Total Energy}} \times 100$$
     $$\text{DOWN Share} = \frac{\text{DOWN Energy}}{\text{Total Energy}} \times 100$$
     $$\text{Net/P} = \frac{\text{UP Energy} - \text{DOWN Energy}}{\text{Total Energy}} \times 100$$
   - Direction:
     - $\text{Net} > 0 \implies \text{UP}$
     - $\text{Net} < 0 \implies \text{DOWN}$
     - $\text{Total Energy} = 0 \implies \text{NO\_SIGNAL}$
   - Pressure Bands (strictly non-overlapping):
     - $\text{Net} \ge 80$: `VERY_STRONG_UP`
     - $60 \le \text{Net} < 80$: `STRONG_UP`
     - $20 \le \text{Net} < 60$: `MODERATE_UP`
     - $-20 < \text{Net} < 20$: `MIXED_OR_WEAK`
     - $-60 < \text{Net} \le -20$: `MODERATE_DOWN`
     - $-80 < \text{Net} \le -60$: `STRONG_DOWN`
     - $\text{Net} \le -80$: `VERY_STRONG_DOWN`
   - Data Completeness Requirement:
     - All three timeframes ($p_{5m}, p_{60m}, p_{1d}$) must be non-null, non-NaN, non-Infinite, non-approximate, and non-ambiguous (`VERIFIED`).
     - If any timeframe is missing or invalid, the result is strictly `DATA_INCOMPLETE` / `NO_SIGNAL`. Missing metrics are never substituted with `0.0`. Incomplete data cannot produce trade signals or `VERIFIED` status.
2. **Exact 30-Second Cooldown Gate**:
   - Once an automatic signal is emitted, a thread-safe `SignalCooldownGate` enforces an exact 30-second quiet window using `SystemClock.elapsedRealtime()`.
   - Incoming percentages detected during the 30-second cooldown window are **ignored and strictly discarded** (zero queueing, zero replay).
   - After the 30-second cooldown expires, the engine accepts and processes **only the first fresh, complete triple** detected.
3. **Coexistence with Matrix & Kinetic Engine**:
   - The classical 165 scenarios, kinetic movement classifications, and OCR pipelines are fully preserved and operate concurrently for diagnostic validation and dashboard transparency.
