package com.portfolio.manager.domain.model

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
    val priceHistory: List<Double> = emptyList()
) {

    val totalValue: Double
        get() = quantity * currentPrice

    val totalCost: Double
        get() = quantity * averagePrice

    val gainLoss: Double
        get() = totalValue - totalCost

    val gainLossPercent: Double
        get() = if (averagePrice == 0.0) 0.0 else ((currentPrice - averagePrice) / averagePrice) * 100

    fun totalValueInUsd(krwToUsdRate: Double = AppConstants.KRW_TO_USD_RATE): Double =
        if (currency == "KRW") totalValue / krwToUsdRate else totalValue

    fun totalCostInUsd(krwToUsdRate: Double = AppConstants.KRW_TO_USD_RATE): Double =
        if (currency == "KRW") totalCost / krwToUsdRate else totalCost

    fun totalValueInKrw(krwToUsdRate: Double = AppConstants.KRW_TO_USD_RATE): Double =
        if (currency == "KRW") totalValue else totalValue * krwToUsdRate

    fun totalCostInKrw(krwToUsdRate: Double = AppConstants.KRW_TO_USD_RATE): Double =
        if (currency == "KRW") totalCost else totalCost * krwToUsdRate
}
