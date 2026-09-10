package com.example.data.models

/**
 * Auto-Entry Decision Mode.
 *
 * LEGACY_MULTILAYER: Preserves the complete 13-priority hierarchy, multi-layer matrix scoring,
 *                    reversal/fakeout/exhaustion/pullback filters, and dead-zone classifications.
 *
 * SHORT_TERM_STRENGTH: ReactiveMarketPressureEngine (upPressure/downPressure) প্রাইমারি সোর্স,
 *                      MicroKineticVectorEngine (kineticUpWeight/kineticDownWeight) কনফার্মেশন/ক্রস-চেক হিসেবে ব্যবহৃত।
 */
enum class DecisionMode(
    val code: String,
    val titleBengali: String,
    val titleEnglish: String
) {
    THREE_TIMEFRAME_PRESSURE(
        code = "THREE_TIMEFRAME_PRESSURE",
        titleBengali = "Three-Timeframe Pure Arithmetic (Direct Auto-Entry)",
        titleEnglish = "Three-Timeframe Pure Arithmetic (Direct Auto-Entry)"
    ),
    LEGACY_MULTILAYER(
        code = "LEGACY_MULTILAYER",
        titleBengali = "Legacy Multilayer Hierarchy (13-Tier)",
        titleEnglish = "Legacy Multilayer (13-Priority)"
    ),
    SHORT_TERM_STRENGTH(
        code = "SHORT_TERM_STRENGTH",
        titleBengali = "Short-Term Kinetic Strength Mode",
        titleEnglish = "Short-Term Kinetic Strength"
    )
}
