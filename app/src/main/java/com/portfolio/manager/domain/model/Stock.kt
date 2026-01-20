package com.portfolio.manager.domain.model

data class StockAccountDetail(
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
    val totalValue: Double
        get() = quantity * currentPrice

    val totalCost: Double
        get() = quantity * averagePrice

    val gainLoss: Double
        get() = totalValue - totalCost

    val gainLossPercent: Double
        get() = if (averagePrice == 0.0) 0.0 else ((currentPrice - averagePrice) / averagePrice) * 100
}
