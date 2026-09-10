package com.example.data.matrix

import com.example.data.models.TradeDirection
import java.util.Locale

/**
 * Named configuration for evidence aggregation category weights.
 */
data class MatrixEvidenceWeightConfig(
    val multiTimeframeWeight: Double = 0.25,
    val multiTimeframeConfluenceWeight: Double = 0.15,
    val momentumWeight: Double = 0.15,
    val pullbackReversalWeight: Double = 0.10,
    val microPressureWeight: Double = 0.15,
    val macroContextWeight: Double = 0.10,
    val volatilityWeight: Double = 0.05,
    val safetyConflictWeight: Double = 0.05
)

/**
 * Deterministic Matrix Evaluation Engine.
 * Evaluates all 165 quantitative decision matrices against incoming market metrics,
 * enforces safety interlocks, detects conflicts, and resolves primary output deterministically.
 */
object MatrixEvaluationEngine {

    val defaultConfig = MatrixEvidenceWeightConfig()

    /**
     * Convenience evaluator taking direct parameter values.
     */
    fun evaluate(
        val5m: Double,
        val60m: Double,
        val1d: Double? = null,
        dataQuality: String = "VERIFIED",
        history: List<com.example.data.models.MetricSnapshot> = emptyList(),
        weightConfig: MatrixEvidenceWeightConfig = defaultConfig
    ): MatrixEvaluationResult = evaluate(
        MatrixEvaluationContext(
            val5m = val5m,
            val60m = val60m,
            val1d = val1d,
            dataQuality = dataQuality,
            history = history,
            is5mPresent = true,
            is60mPresent = true,
            is1dPresent = val1d != null
        ),
        weightConfig = weightConfig
    )

    /**
     * Evaluates all 165 matrices against the given context.
     */
    fun evaluate(
        context: MatrixEvaluationContext,
        weightConfig: MatrixEvidenceWeightConfig = defaultConfig
    ): MatrixEvaluationResult =
        evaluateMatrices(MatrixCatalog.allMatrices, context, weightConfig)

    /**
     * Evaluates a specific list of matrices against the given context.
     * Reports actual evaluated, skipped, and failed counts, and captures deterministic failure traces.
     */
    fun evaluateMatrices(
        allMatrices: List<QuantitativeMatrix>,
        context: MatrixEvaluationContext,
        weightConfig: MatrixEvidenceWeightConfig = defaultConfig
    ): MatrixEvaluationResult {
        val totalCount = allMatrices.size
        val matchedList = mutableListOf<QuantitativeMatrix>()
        val matrixRecords = mutableListOf<EvaluatedMatrixRecord>()

        var evaluatedCount = 0
        var skippedCount = 0
        var failedCount = 0
        val failureTraces = mutableListOf<String>()

        val eligibleMatrixIds = mutableListOf<String>()
        val cancelledMatrixIds = mutableListOf<String>()
        val uncheckedMatrixIds = mutableListOf<String>()

        // 1. Evaluate every matrix deterministically
        for (matrix in allMatrices) {
            val isSkipped = (matrix.requiredInputs.contains(InputType.CHANGE_5M) && !context.is5mPresent) ||
                    (matrix.requiredInputs.contains(InputType.CHANGE_60M) && !context.is60mPresent) ||
                    (matrix.requiredInputs.contains(InputType.CHANGE_1D) && !context.is1dPresent)

            if (isSkipped) {
                skippedCount++
                continue
            }

            var matched = false
            try {
                evaluatedCount++
                matched = matrix.evaluator(context)
                if (matched) {
                    matchedList.add(matrix)
                }
            } catch (ex: Exception) {
                failedCount++
                val err = "Matrix ${matrix.id} (${matrix.title}) evaluator failed: ${ex.javaClass.simpleName}: ${ex.message}"
                failureTraces.add(err)
            }

            // User and system state enforcement
            val userState = MatrixUserStateRegistry.getUserState(matrix)
            val category = matrix.category

            var rawScore = 0.0
            var normalizedScore = 0.0
            var reliabilityWeight = 0.0
            var contributesToTrade = false
            var cancelledReason: String? = null

            when (userState) {
                MatrixUserState.CANCELLED -> {
                    cancelledMatrixIds.add(matrix.id)
                    cancelledReason = "ঝুঁকিপূর্ণ / নো-ট্রেড ইন্টারলক বা ইউজার কর্তৃক বাতিল"
                    // Cancelled matrices contribute ZERO to signal calculation
                    rawScore = 0.0
                    normalizedScore = 0.0
                    reliabilityWeight = 0.0
                    contributesToTrade = false
                }
                MatrixUserState.UNCHECKED -> {
                    uncheckedMatrixIds.add(matrix.id)
                    cancelledReason = "আন-চেকড বা পর্যালোচনায় রয়েছে (Diagnostic only)"
                    rawScore = 0.0
                    normalizedScore = 0.0
                    reliabilityWeight = 0.0
                    contributesToTrade = false
                }
                MatrixUserState.CHECKED -> {
                    if (matched) {
                        eligibleMatrixIds.add(matrix.id)
                        val effectiveDirection = when (matrix.direction) {
                            TradeDirection.UP -> TradeDirection.UP
                            TradeDirection.DOWN -> TradeDirection.DOWN
                            else -> {
                                when {
                                    context.val5m > 0.0 -> TradeDirection.UP
                                    context.val5m < 0.0 -> TradeDirection.DOWN
                                    context.val60m > 0.0 -> TradeDirection.UP
                                    context.val60m < 0.0 -> TradeDirection.DOWN
                                    (context.val1d ?: 0.0) >= 0.0 -> TradeDirection.UP
                                    else -> TradeDirection.DOWN
                                }
                            }
                        }
                        val dirSign = if (effectiveDirection == TradeDirection.UP) 1.0 else -1.0
                        val netSumAbs = kotlin.math.abs(context.netSum)
                        val breachDepth = kotlin.math.tanh(netSumAbs / 1.0)
                        val continuousScore = (0.30 + 0.70 * breachDepth).coerceIn(0.10, 1.0)
                        reliabilityWeight = (matrix.priority.coerceIn(100, 1000) / 1000.0)
                        rawScore = dirSign * continuousScore
                        normalizedScore = (dirSign * kotlin.math.tanh(continuousScore)).coerceIn(-1.0, 1.0)
                        contributesToTrade = true
                    }
                }
            }

            val explanation = if (matched) {
                if (userState == MatrixUserState.CANCELLED) {
                    "${matrix.id}: শর্ত পূরণ হয়েছে কিন্তু বাতিল হওয়ায় সিগন্যালে কোনো প্রভাব নেই"
                } else if (userState == MatrixUserState.UNCHECKED) {
                    "${matrix.id}: শর্ত পূরণ হয়েছে কিন্তু আন-চেকড হওয়ায় অটো-ট্রেডে নিষ্ক্রিয়"
                } else {
                    "${matrix.id}: সক্রিয় ও অনুমোদিত (${matrix.conditionDescription})"
                }
            } else {
                "${matrix.id}: শর্ত পূরণ হয়নি"
            }

            matrixRecords.add(
                EvaluatedMatrixRecord(
                    matrixId = matrix.id,
                    title = matrix.title,
                    category = category,
                    userState = userState,
                    direction = matrix.direction,
                    rawScore = rawScore,
                    normalizedScore = normalizedScore,
                    reliabilityWeight = reliabilityWeight,
                    matched = matched,
                    cancelledReason = cancelledReason,
                    contributesToTrade = contributesToTrade,
                    explanation = explanation
                )
            )
        }

        // Category-Level Evidence Aggregation with Diminishing Marginal Weight for Redundant Rules
        // (ONLY Checked and non-cancelled contributing matrices)
        val contributingRecords = matrixRecords.filter { it.matched && it.contributesToTrade && it.userState == MatrixUserState.CHECKED }
        val categoryScores = mutableMapOf<String, Double>()

        val distinctCategories = listOf(
            "Multi-timeframe alignment",
            "Momentum",
            "Micro pressure",
            "Velocity",
            "Acceleration",
            "Pullback/reversal",
            "Macro/1D context",
            "Historical persistence",
            "Volatility",
            "Data quality",
            "Safety/conflict",
            "Dead-zone/no-trade"
        )

        for (cat in distinctCategories) {
            val catRecords = contributingRecords.filter { it.category == cat }.sortedByDescending { it.reliabilityWeight }
            if (catRecords.isNotEmpty()) {
                var weightedSum = 0.0
                var effectiveTotalWeight = 0.0
                catRecords.forEachIndexed { index, record ->
                    // Diminishing marginal weight to eliminate redundancy when multiple similar matrices match
                    val diminishingFactor = 1.0 / (1.0 + 0.35 * index)
                    val effectiveWeight = record.reliabilityWeight * diminishingFactor
                    weightedSum += record.normalizedScore * effectiveWeight
                    effectiveTotalWeight += effectiveWeight
                }
                categoryScores[cat] = if (effectiveTotalWeight > 1e-4) {
                    (weightedSum / effectiveTotalWeight).coerceIn(-1.0, 1.0)
                } else {
                    0.0
                }
            } else {
                categoryScores[cat] = 0.0
            }
        }

        // Aggregate final matrix evidence based on named configuration
        val categoryWeightMap = mapOf(
            "Multi-timeframe alignment" to weightConfig.multiTimeframeWeight,
            "Multi-timeframe confluence" to weightConfig.multiTimeframeConfluenceWeight,
            "Momentum" to weightConfig.momentumWeight,
            "Pullback/reversal" to weightConfig.pullbackReversalWeight,
            "Micro pressure" to weightConfig.microPressureWeight,
            "Macro/1D context" to weightConfig.macroContextWeight,
            "Volatility" to weightConfig.volatilityWeight,
            "Safety/conflict" to weightConfig.safetyConflictWeight
        )

        var totalNominalWeight = 0.0
        var aggregatedEvidenceScore = 0.0
        for ((cat, weight) in categoryWeightMap) {
            val score = categoryScores[cat] ?: 0.0
            aggregatedEvidenceScore += weight * score
            totalNominalWeight += weight
        }

        val matrixEvidence = if (totalNominalWeight > 1e-4) {
            (aggregatedEvidenceScore / totalNominalWeight).coerceIn(-1.0, 1.0)
        } else {
            0.0
        }

        val trace = mutableListOf<String>()
        val rejectedNotes = mutableListOf<String>()

        // Format input metrics trace line
        val str5m = if (context.is5mPresent) String.format(Locale.US, "%+.2f%%", context.val5m) else "--"
        val str60m = if (context.is60mPresent) String.format(Locale.US, "%+.2f%%", context.val60m) else "--"
        val str1d = if (context.is1dPresent) context.val1d?.let { String.format(Locale.US, "%+.2f%%", it) } ?: "--" else "--"
        val strNet = String.format(Locale.US, "%+.2f%%", context.netSum)

        trace.add("ইন্টারনাল ডেটা ইনপুট: 5m=$str5m, 60m=$str60m, 1d=$str1d, Net=$strNet, Quality=${context.dataQuality}")
        trace.add("মোট ক্যাটালগ ম্যাট্রিক্স: $totalCount টি")
        trace.add("মূল্যায়িত ম্যাট্রিক্স: $evaluatedCount টি (বাদ/অনুপস্থিত ইনপুট: $skippedCount টি, ত্রুটি: $failedCount টি)")
        trace.add("শর্ত পূরণকারী ম্যাট্রিক্স: ${matchedList.size} টি -> [${matchedList.joinToString(", ") { it.id }}]")
        trace.add("সক্রিয় অনুমোদিত ম্যাট্রিক্স: ${contributingRecords.size} টি (বাতিল/ক্যানসেলড: ${cancelledMatrixIds.size} টি)")
        trace.add("ম্যাট্রিক্স এভিডেন্স স্কোর: ${String.format(Locale.US, "%+.4f", matrixEvidence)}")

        if (failureTraces.isNotEmpty()) {
            trace.addAll(failureTraces)
            trace.add("⚠️ Evaluator failures detected: $failedCount matrices failed safely without creating signals.")
        }

        // 2. Conflict Detection among matched active directional matrices (strictly CHECKED, excluding cancelled and unchecked matrices)
        val activeDirectional = matchedList.filter { matrix ->
            !matrix.warningOnly &&
            (matrix.direction == TradeDirection.UP || matrix.direction == TradeDirection.DOWN) &&
            MatrixUserStateRegistry.getUserState(matrix) == MatrixUserState.CHECKED &&
            !cancelledMatrixIds.contains(matrix.id)
        }
        val hasUp = activeDirectional.any { it.direction == TradeDirection.UP }
        val hasDown = activeDirectional.any { it.direction == TradeDirection.DOWN }
        val opposingTimeframes = context.is5mPresent && context.is60mPresent &&
                (context.val5m * context.val60m < 0) &&
                (kotlin.math.abs(context.val5m) >= 0.50 && kotlin.math.abs(context.val60m) >= 0.50)
        val isConflicting = (hasUp && hasDown) || opposingTimeframes

        var conflictReason: String? = null
        if (isConflicting) {
            val upIds = activeDirectional.filter { it.direction == TradeDirection.UP }.map { it.id }
            val downIds = activeDirectional.filter { it.direction == TradeDirection.DOWN }.map { it.id }
            conflictReason = if (hasUp && hasDown) {
                "সক্রিয় ম্যাট্রিক্সে বিরোধ: UP=$upIds বনাম DOWN=$downIds. নিরাপত্তা ইন্টারলক সক্রিয়।"
            } else {
                "টাইমফ্রেম তীব্র ডাইভারজেন্স (5m=${context.val5m}%, 60m=${context.val60m}%). নিরাপত্তা ইন্টারলক সক্রিয়।"
            }
            trace.add("⚠️ বিরোধ শনাক্ত: $conflictReason")
        }

        // 3. Priority & Specificity Sorting
        // Safety matrices (Priority >= 850) always take precedent over standard trends
        val sortedCandidates = matchedList.sortedWith(
            compareByDescending<QuantitativeMatrix> { it.priority }
                .thenByDescending { it.requiredInputs.size }
        )

        // 4. Primary Matrix Selection:
        // STRICT RULE: Primary candidate MUST come exclusively from CHECKED matrices.
        // Cancelled (❌) and unchecked matrices stay ONLY in audit trace & rejected notes.
        val checkedCandidates = sortedCandidates.filter {
            MatrixUserStateRegistry.getUserState(it) == MatrixUserState.CHECKED && !cancelledMatrixIds.contains(it.id)
        }

        // Directional checked candidates (BUY / SELL)
        val checkedDirectionalCandidates = checkedCandidates.filter {
            !it.warningOnly && (it.direction == TradeDirection.UP || it.direction == TradeDirection.DOWN)
        }

        val primary: QuantitativeMatrix? = when {
            isConflicting -> {
                // Conflict resolution: pick highest priority CHECKED safety matrix or M130
                val checkedSafetyCandidate = checkedCandidates.firstOrNull {
                    it.riskLevel == RiskLevel.NO_TRADE || it.riskLevel == RiskLevel.EXTREME
                }
                checkedSafetyCandidate ?: MatrixCatalog.getById("M130")
            }
            checkedDirectionalCandidates.isNotEmpty() -> {
                checkedDirectionalCandidates.first()
            }
            checkedCandidates.isNotEmpty() -> {
                checkedCandidates.first()
            }
            else -> {
                // No checked candidate matched; neutral safety anchor
                MatrixCatalog.getById("M132")
            }
        }

        if (primary != null) {
            val pState = MatrixUserStateRegistry.getUserState(primary)
            trace.add("নির্বাচিত প্রাইমারি ডিসিশন: ${primary.id} - ${primary.title} (Priority: ${primary.priority}, State: $pState, Code: ${primary.outputCode})")
            trace.add("লজিক শর্ত: ${primary.conditionDescription}")
            trace.add("ঝুঁকি স্তর: ${primary.riskLevel.bengaliLabel}")
            if (pState == MatrixUserState.CANCELLED) {
                trace.add("⚠️ প্রাইমারি ম্যাট্রিক্স ক্যানসেলড থাকায় কোনো অটো-ট্রেড নেওয়া হবে না।")
            }
        }

        // 5. Document rejected / superseded candidates
        if (sortedCandidates.size > 1 && primary != null) {
            for (m in sortedCandidates) {
                if (m.id != primary.id) {
                    val mState = MatrixUserStateRegistry.getUserState(m)
                    val note = when {
                        mState == MatrixUserState.CANCELLED -> "${m.id} (${m.title}): ক্যানসেলড/ঝুঁকিপূর্ণ হওয়ায় বাতিল"
                        isConflicting && m.direction != primary.direction -> "${m.id} (${m.title}): নির্দেশমূলক বিরোধের কারণে বাতিল"
                        else -> "${m.id} (${m.title}): অগ্রাধিকার কম (${m.priority} < ${primary.priority})"
                    }
                    rejectedNotes.add(note)
                }
            }
        }

        return MatrixEvaluationResult(
            totalMatricesCount = totalCount,
            evaluatedMatricesCount = evaluatedCount,
            matchedMatrices = matchedList,
            primaryMatrix = primary,
            isConflicting = isConflicting,
            conflictReason = conflictReason,
            decisionTrace = trace,
            rejectedMatrixNotes = rejectedNotes,
            skippedMatricesCount = skippedCount,
            failedMatricesCount = failedCount,
            failureTraces = failureTraces,
            matrixRecords = matrixRecords,
            categoryScores = categoryScores,
            matrixEvidence = matrixEvidence,
            eligibleMatrixIds = eligibleMatrixIds,
            cancelledMatrixIds = cancelledMatrixIds,
            uncheckedMatrixIds = uncheckedMatrixIds,
            activeCheckedMatchesCount = contributingRecords.size
        )
    }

    data class ReplayStepResult(
        val timestamp: Long,
        val val5m: Double,
        val val60m: Double,
        val val1d: Double?,
        val direction: TradeDirection,
        val primaryMatrixId: String?,
        val executionEligibility: Boolean,
        val reason: String
    )

    data class ReplaySummary(
        val totalSteps: Int,
        val upSignals: Int,
        val downSignals: Int,
        val neutralSignals: Int,
        val executedTrades: Int,
        val conflictSteps: Int,
        val stepResults: List<ReplayStepResult>
    )

    /**
     * Executes a historical replay / backtest over an ordered series of MetricSnapshots.
     * Evaluates chronological state transitions deterministically without external dependencies.
     */
    fun runHistoricalReplay(snapshots: List<com.example.data.models.MetricSnapshot>): ReplaySummary {
        val chronological = snapshots.filter { it.isValid }.sortedBy { it.timestamp }
        val stepResults = mutableListOf<ReplayStepResult>()
        var upCount = 0
        var downCount = 0
        var neutralCount = 0
        var executedCount = 0
        var conflictCount = 0

        val window = mutableListOf<com.example.data.models.MetricSnapshot>()
        for (snap in chronological) {
            val res = evaluate(
                val5m = snap.val5m,
                val60m = snap.val60m,
                val1d = snap.val1d,
                dataQuality = "VERIFIED",
                history = window.toList()
            )
            val dir = res.primaryMatrix?.direction ?: TradeDirection.NEUTRAL
            when (dir) {
                TradeDirection.UP -> upCount++
                TradeDirection.DOWN -> downCount++
                else -> neutralCount++
            }
            if (res.isConflicting) conflictCount++
            val isEligible = !res.isConflicting && res.eligibleMatrixIds.isNotEmpty() && dir != TradeDirection.NEUTRAL
            if (isEligible) executedCount++

            stepResults.add(
                ReplayStepResult(
                    timestamp = snap.timestamp,
                    val5m = snap.val5m,
                    val60m = snap.val60m,
                    val1d = snap.val1d,
                    direction = dir,
                    primaryMatrixId = res.primaryMatrix?.id,
                    executionEligibility = isEligible,
                    reason = res.conflictReason ?: res.primaryMatrix?.title ?: "Neutral"
                )
            )

            window.add(snap)
            if (window.size > 8) window.removeAt(0)
        }

        return ReplaySummary(
            totalSteps = chronological.size,
            upSignals = upCount,
            downSignals = downCount,
            neutralSignals = neutralCount,
            executedTrades = executedCount,
            conflictSteps = conflictCount,
            stepResults = stepResults
        )
    }
}
