package com.portfolio.manager.domain.service

import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.Stock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shared in-memory cache for portfolio data.
 * Used to share stock/cash data between Dashboard and FIRE calculator.
 */
@Singleton
class PortfolioCache @Inject constructor() {
    @Volatile
    var stocks: List<Stock> = emptyList()
        private set

    @Volatile
    var cashItems: List<CashItem> = emptyList()
        private set

    @Volatile
    var exchangeRate: Double = 1400.0
        private set

    @Volatile
    var isInitialized: Boolean = false
        private set

    fun update(stocks: List<Stock>, cashItems: List<CashItem>, exchangeRate: Double) {
        this.stocks = stocks
        this.cashItems = cashItems
        this.exchangeRate = exchangeRate
        this.isInitialized = true
    }

    fun hasData(): Boolean = isInitialized
}
