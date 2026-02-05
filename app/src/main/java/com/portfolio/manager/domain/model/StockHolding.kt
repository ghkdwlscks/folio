package com.portfolio.manager.domain.model

/**
 * Holds stock information needed for portfolio value calculation.
 */
data class StockHolding(
    val symbol: String,
    val quantity: Int,
    val currency: Currency
)
