package com.example.data.matrix

import com.example.data.models.MetricSnapshot
import com.example.data.models.TradeDirection

/**
 * Directional Matrix Engine Adapter.
 * Discards legacy matrix rules and delegates strictly to Directional206MatrixEngine
 * containing EXACTLY 206 RULES (103 UP + 103 DOWN).
 */
object Authorized106MatrixEngine {

    data class Matrix106Match(
        val id: String,
        val direction: TradeDirection,
        val outputCode: String,
        val title: String,
        val conditionDescription: String,
        val priority: Int
    )

    /**
     * Evaluates val5m and val60m against the 206 Directional Matrix Rules.
     * Evaluates in deterministic order with Single-Match Guarantee.
     */
    fun evaluate(
        val5m: Double?,
        val60m: Double?,
        val1d: Double? = null,
        history: List<MetricSnapshot> = emptyList()
    ): Matrix106Match? {
        val res = Directional206MatrixEngine.evaluate(
            val5m = val5m,
            val60m = val60m,
            val1d = val1d,
            history = history
        ) ?: return null

        return Matrix106Match(
            id = res.id,
            direction = res.direction,
            outputCode = res.outputCode,
            title = res.title,
            conditionDescription = res.conditionDescription,
            priority = res.priority
        )
    }
}


