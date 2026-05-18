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
    val currentPercentage: Int?,
    val currency: Currency,
    val quantity: Int
)

/**
 * Bundle of data the Rebalance dialog needs: the per-holding items plus
 * the account's currently stored tolerance band (null when unset).
 */
data class RebalanceFormData(
    val items: List<RebalanceItemData>,
    val toleranceBandPercent: Int?
)

/**
 * Service for calculating rebalancing data.
 */
object RebalanceCalculator {

    /**
     * Checks if an account needs rebalancing.
     *
     * If [toleranceBandPercent] is non-null, the band rule applies:
     *   drift = |currentPct - targetPct| / targetPct
     *   needsRebalance = drift > band/100
     *
     * If [toleranceBandPercent] is null, falls back to the legacy per-share rule:
     *   needsRebalance = |targetValue - currentValue| >= pricePerShare
     *
     * Holdings with targetPercentage null or 0 are excluded from both the
     * denominator (totalValue) and the per-row checks — currentPct is computed
     * over targeted holdings only.
     *
     * @param holdings List of holdings for the account
     * @param stockMap Map of symbol to Stock data
     * @param showInKrw Whether to calculate in KRW
     * @param exchangeRate Current USD/KRW exchange rate
     * @param toleranceBandPercent Optional band; null = use per-share rule
     * @return true if rebalancing is recommended
     */
    fun accountNeedsRebalance(
        holdings: List<HoldingEntity>,
        stockMap: Map<String, Stock>,
        showInKrw: Boolean,
        exchangeRate: Double,
        toleranceBandPercent: Int?
    ): Boolean {
        if (holdings.isEmpty()) return false

        val totalValue = holdings.sumOf { holding ->
            val target = holding.targetPercentage ?: return@sumOf 0.0
            if (target == 0) return@sumOf 0.0
            val stock = stockMap[holding.symbol] ?: return@sumOf 0.0
            val value = stock.currentPrice * holding.quantity
            CurrencyConverter.convert(value, stock.currency, showInKrw, exchangeRate)
        }

        if (totalValue <= 0) return false

        if (toleranceBandPercent != null) {
            val bandFraction = toleranceBandPercent / 100.0
            for (holding in holdings) {
                val target = holding.targetPercentage ?: continue
                if (target == 0) continue
                val stock = stockMap[holding.symbol] ?: continue
                val currentValue = CurrencyConverter.convert(
                    stock.currentPrice * holding.quantity,
                    stock.currency, showInKrw, exchangeRate
                )
                val currentPct = (currentValue / totalValue) * 100.0
                val drift = abs(currentPct - target) / target
                if (drift > bandFraction) return true
            }
            return false
        }

        for (holding in holdings) {
            val targetPercent = holding.targetPercentage ?: continue
            if (targetPercent == 0) continue
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
                currentPercentage = holding.targetPercentage?.takeIf { it > 0 },
                currency = stock.currency,
                quantity = stock.quantity
            )
        }
    }
}
