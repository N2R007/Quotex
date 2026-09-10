# Quant Vision AI — Instructions (CLAUDE.md)

## 🎯 Core Operating Rules
1. **Explicit Request Scope**: Execute only the exact modifications requested by the user. Do not perform unsolicited refactoring, file restructuring, or dependency updates.
2. **Preserve Detection Speed**: Maintain 10ms Zero-Delay screen detection for 5m & 60m percentage metrics with instant auto-trade dispatch.
3. **Verified Metrics Only**: Take auto-trade entries strictly when detected metrics are verified (✔) in the rule registry.
4. **Mandatory Rollback Prompt**: Always end every final task response with this exact sentence:
   "কাজটি পছন্দ না হলে লিখুন: [Rollback]"

## 📱 System Specs
- Real-time OCR detection via CameraX.
- 206 directional rules & 165 audit matrices for bullish/bearish trading signals.
- Automated execution via WebSocket & Webhook endpoints.
- Voice/Audio alerts and `FLAG_KEEP_SCREEN_ON` for uninterrupted session scanning.