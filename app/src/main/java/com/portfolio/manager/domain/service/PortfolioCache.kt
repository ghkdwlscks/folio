package com.portfolio.manager.domain.service

import javax.inject.Inject
import javax.inject.Singleton

import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.Stock

/**
 * Immutable snapshot of portfolio data.
 * Used for thread-safe atomic updates.
 */
private data class CacheData(
    val stocks: List<Stock> = emptyList(),
    val cashItems: List<CashItem> = emptyList(),
    val exchangeRate: Double = 1400.0,
    val isInitialized: Boolean = false
)

/**
 * Shared in-memory cache for portfolio data.
 * Used to share stock/cash data between Dashboard and FIRE calculator.
 * Thread-safe: uses atomic reference to immutable snapshot.
 */
@Singleton
class PortfolioCache @Inject constructor() {
    @Volatile
    private var data: CacheData = CacheData()

    val stocks: List<Stock> get() = data.stocks

    val cashItems: List<CashItem> get() = data.cashItems

    val exchangeRate: Double get() = data.exchangeRate

    fun update(stocks: List<Stock>, cashItems: List<CashItem>, exchangeRate: Double) {
        data = CacheData(stocks, cashItems, exchangeRate, isInitialized = true)
    }

    fun hasData(): Boolean = data.isInitialized
}
