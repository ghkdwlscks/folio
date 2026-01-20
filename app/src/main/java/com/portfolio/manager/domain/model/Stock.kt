package com.portfolio.manager.domain.model

data class StockAccountDetail(
    val holdingId: Long,
    val accountId: Long,
    val accountName: String,
    val quantity: Int,
    val averagePrice: Double
)

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
    val accountDetails: List<StockAccountDetail> = emptyList()
) {
    companion object {
        const val KRW_TO_USD_RATE = 1400.0
    }

    val totalValue: Double
        get() = quantity * currentPrice

    val totalCost: Double
        get() = quantity * averagePrice

    val gainLoss: Double
        get() = totalValue - totalCost

    val gainLossPercent: Double
        get() = if (averagePrice == 0.0) 0.0 else ((currentPrice - averagePrice) / averagePrice) * 100

    val totalValueInUsd: Double
        get() = if (currency == "KRW") totalValue / KRW_TO_USD_RATE else totalValue

    val totalCostInUsd: Double
        get() = if (currency == "KRW") totalCost / KRW_TO_USD_RATE else totalCost

    val totalValueInKrw: Double
        get() = if (currency == "KRW") totalValue else totalValue * KRW_TO_USD_RATE

    val totalCostInKrw: Double
        get() = if (currency == "KRW") totalCost else totalCost * KRW_TO_USD_RATE
}
