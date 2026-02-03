package com.portfolio.manager.domain.service

import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.SortOption
import com.portfolio.manager.domain.model.Stock

/**
 * Service for sorting portfolio items (stocks and cash) by various criteria.
 */
object PortfolioSorter {

    /**
     * Sorts stocks by the given sort option.
     *
     * @param stocks List of stocks to sort
     * @param option Sort criteria
     * @param exchangeRate Current USD/KRW exchange rate (used for WEIGHT sorting)
     * @return Sorted list of stocks
     */
    fun sortStocks(
        stocks: List<Stock>,
        option: SortOption,
        exchangeRate: Double
    ): List<Stock> = when (option) {
        SortOption.WEIGHT -> stocks.sortedByDescending { it.totalValueInUsd(exchangeRate) }
        SortOption.NAME -> stocks.sortedBy { it.name.lowercase() }
        SortOption.SYMBOL -> stocks.sortedBy { it.symbol.lowercase() }
        SortOption.GAIN_LOSS_PERCENT -> stocks.sortedByDescending { it.gainLossPercent }
        SortOption.DAY_CHANGE_PERCENT -> stocks.sortedByDescending { it.dayChangePercent ?: 0.0 }
    }

    /**
     * Sorts cash items by the given sort option.
     *
     * @param cashItems List of cash items to sort
     * @param option Sort criteria
     * @param exchangeRate Current USD/KRW exchange rate (used for WEIGHT sorting)
     * @return Sorted list of cash items
     */
    fun sortCashItems(
        cashItems: List<CashItem>,
        option: SortOption,
        exchangeRate: Double
    ): List<CashItem> = when (option) {
        SortOption.WEIGHT -> cashItems.sortedByDescending { it.valueInUsd(exchangeRate) }
        SortOption.NAME, SortOption.SYMBOL -> cashItems.sortedBy { it.name.lowercase() }
        SortOption.GAIN_LOSS_PERCENT, SortOption.DAY_CHANGE_PERCENT ->
            cashItems.sortedByDescending { it.annualYieldRate }
    }
}
