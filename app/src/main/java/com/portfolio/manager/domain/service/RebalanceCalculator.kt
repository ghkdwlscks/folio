package com.portfolio.manager.domain.service

import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.Currency
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.util.CurrencyConverter
import kotlin.math.abs

/**
 * Data class representing a rebalance item for the rebalance dialog.
 */
data class RebalanceItemData(
    val holdingId: Long,
    val symbol: String,
    val name: String,
    val currentValue: Double,
    val currentPrice: Double,
    val currentPercentage: Int,
    val currency: Currency,
    val quantity: Int
)

/**
 * Service for calculating rebalancing data.
 */
object RebalanceCalculator {

    /**
     * Checks if an account needs rebalancing.
     * Returns true if any holding's difference from target is >= one share price.
     *
     * @param holdings List of holdings for the account
     * @param stockMap Map of symbol to Stock data
     * @param showInKrw Whether to calculate in KRW
     * @param exchangeRate Current USD/KRW exchange rate
     * @return true if rebalancing is recommended
     */
    fun accountNeedsRebalance(
        holdings: List<HoldingEntity>,
        stockMap: Map<String, Stock>,
        showInKrw: Boolean,
        exchangeRate: Double
    ): Boolean {
        if (holdings.isEmpty()) return false

        // Calculate total portfolio value for this account
        val totalValue = holdings.sumOf { holding ->
            val stock = stockMap[holding.symbol] ?: return@sumOf 0.0
            val value = stock.currentPrice * holding.quantity
            CurrencyConverter.convert(value, stock.currency, showInKrw, exchangeRate)
        }

        if (totalValue <= 0) return false

        // Check each holding
        for (holding in holdings) {
            val targetPercent = holding.targetPercentage ?: continue
            val stock = stockMap[holding.symbol] ?: continue

            val currentValue = CurrencyConverter.convert(
                stock.currentPrice * holding.quantity,
                stock.currency,
                showInKrw,
                exchangeRate
            )
            val targetValue = totalValue * (targetPercent / 100.0)
            val diffAmount = abs(targetValue - currentValue)
            val price = CurrencyConverter.convert(
                stock.currentPrice, stock.currency, showInKrw, exchangeRate
            )

            if (diffAmount >= price && price > 0) {
                return true
            }
        }
        return false
    }

    /**
     * Builds a list of rebalance items from holdings and stocks.
     *
     * @param holdings List of holdings for the account
     * @param stocks List of stocks to match with holdings
     * @param showInKrw Whether to calculate in KRW
     * @param exchangeRate Current USD/KRW exchange rate
     * @return List of RebalanceItemData for the rebalance dialog
     */
    fun buildRebalanceItems(
        holdings: List<HoldingEntity>,
        stocks: List<Stock>,
        showInKrw: Boolean,
        exchangeRate: Double
    ): List<RebalanceItemData> {
        return stocks.mapNotNull { stock ->
            val holding = holdings.find { it.symbol == stock.symbol } ?: return@mapNotNull null
            val value = if (showInKrw) {
                stock.totalValueInKrw(exchangeRate)
            } else {
                stock.totalValueInUsd(exchangeRate)
            }
            val price = CurrencyConverter.convert(
                stock.currentPrice, stock.currency, showInKrw, exchangeRate
            )
            RebalanceItemData(
                holdingId = holding.id,
                symbol = stock.symbol,
                name = stock.name,
                currentValue = value,
                currentPrice = price,
                currentPercentage = holding.targetPercentage ?: 0,
                currency = stock.currency,
                quantity = stock.quantity
            )
        }
    }
}
