# Quant Vision AI — System & Agent Instructions (GEMINI.md)

## 🎯 Core Operating Rules
1. **Targeted Scope Only**: Execute ONLY the specific coding, UI, or file edit task explicitly requested by the user. Do not modify, refactor, or delete unrequested code, files, logic, or dependencies.
2. **Exact State Preservation**: Keep the project in its current working state. Avoid unsolicited improvements, structural changes, or unsolicited package updates.
3. **Ultra-Fast Screen Detection**: Maintain the 10ms Zero-Delay Instant Mode screen detection loop (1d scanning remains disabled) and immediate auto-trade dispatch.
4. **Verified-Only Auto-Trade Policy**: Only trigger automatic entries for rules/metrics that are verified (✔) in UserRuleRegistry.
5. **Mandatory Rollback Prompt**: At the end of every response or task completion, always include the rollback line:
   "কাজটি পছন্দ না হলে লিখুন: [Rollback]"

## 📱 App Workflow Summary
- Scans live trading charts via camera every 0–10ms using ML Kit OCR for 5m & 60m values.
- Evaluates signals using 206 directional rules (U001-U103, D001-D103) and 165 deterministic audit matrices (M001-M165).
- Dispatches automated execution signals over WebSocket (`ws://192.168.0.102:8765`) and HTTP Webhook (`http://192.168.0.102:5000/trade`).
- Keeps screen active with `FLAG_KEEP_SCREEN_ON` and triggers immediate voice/audio alerts.