package com.quantvision.engine.core

import com.example.data.analyzer.DCHMEngine as BaseDCHMEngine
import com.example.data.analyzer.DeltaSnapshot as BaseDeltaSnapshot
import com.example.data.analyzer.TradeSignal as BaseTradeSignal
import com.example.data.analyzer.MatrixValidator as BaseMatrixValidator
import com.example.data.analyzer.Directional206MatrixValidator as BaseDirectional206MatrixValidator
import com.example.data.models.TradeDirection as BaseTradeDirection

/**
 * Package alias and forwarding bridge for Quant Vision core engine components.
 */
typealias DCHMEngine = BaseDCHMEngine
typealias DeltaSnapshot = BaseDeltaSnapshot
typealias TradeSignal = BaseTradeSignal
typealias MatrixValidator = BaseMatrixValidator
typealias Directional206MatrixValidator = BaseDirectional206MatrixValidator
typealias TradeDirection = BaseTradeDirection
