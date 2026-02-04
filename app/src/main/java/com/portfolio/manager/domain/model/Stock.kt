package com.portfolio.manager.domain.model

import com.portfolio.manager.domain.util.CurrencyConverter
import com.portfolio.manager.util.AppConstants
import kotlinx.serialization.Serializable

@Serializable
data class StockAccountDetail(
    val holdingId: Long,
    val accountId: Long,
    val accountName: String,
    val quantity: Int,
    val averagePrice: Double
)

@Serializable
data class Stock(
    val id: Long,
    val symbol: String,
    val name: String,
    val quantity: Int,
    val averagePrice: Double,
    val currentPrice: Double,
    val dayChange: Double? = null,
    val dayChangePercent: Double? = null,
    val currency: String = "USD",
    val accountDetails: List<StockAccountDetail> = emptyList(),
    val priceHistory: List<Double> = emptyList(),
    val priceHistoryTimestamps: List<Long> = emptyList(),
    val annualDividend: Double? = null,
    val dividendYield: Double? = null,
    val targetPercentage: Int? = null
) {

    val totalValue: Double
        get() = quantity * currentPrice

    val totalCost: Double
        get() = quantity * averagePrice

    val gainLoss: Double
        get() = totalValue - totalCost

    val gainLossPercent: Double
        get() = if (averagePrice == 0.0) 0.0 else ((currentPrice - averagePrice) / averagePrice) * 100

    val annualDividendIncome: Double
        get() = (annualDividend ?: 0.0) * quantity

    fun totalValueInUsd(exchangeRate: Double = AppConstants.KRW_TO_USD_RATE): Double =
        CurrencyConverter.toUsd(totalValue, currency, exchangeRate)

    fun totalCostInUsd(exchangeRate: Double = AppConstants.KRW_TO_USD_RATE): Double =
        CurrencyConverter.toUsd(totalCost, currency, exchangeRate)

    fun totalValueInKrw(exchangeRate: Double = AppConstants.KRW_TO_USD_RATE): Double =
        CurrencyConverter.toKrw(totalValue, currency, exchangeRate)

    fun totalCostInKrw(exchangeRate: Double = AppConstants.KRW_TO_USD_RATE): Double =
        CurrencyConverter.toKrw(totalCost, currency, exchangeRate)
}
