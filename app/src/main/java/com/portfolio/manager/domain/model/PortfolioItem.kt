package com.portfolio.manager.domain.model

/**
 * Common interface for portfolio items (stocks and cash).
 * Provides a unified API for currency conversion.
 */
interface PortfolioItem {
    val currency: Currency
    fun valueInUsd(exchangeRate: Double): Double
    fun valueInKrw(exchangeRate: Double): Double
}
