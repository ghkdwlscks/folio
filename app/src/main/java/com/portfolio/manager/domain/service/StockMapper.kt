package com.portfolio.manager.domain.service

import android.util.Log
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.data.remote.dto.QuoteResult
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.StockAccountDetail
import com.portfolio.manager.domain.repository.PriceHistoryData

/**
 * Service for mapping holding entities and quotes to Stock domain model.
 */
object StockMapper {
    private const val TAG = "StockMapper"

    /**
     * Creates a Stock from a single holding and quote.
     */
    fun createStock(
        holding: HoldingEntity,
        quote: QuoteResult?,
        priceHistory: PriceHistoryData? = null
    ): Stock {
        val stockName = quote?.longName ?: quote?.shortName ?: holding.symbol
        return Stock(
            id = holding.id,
            symbol = holding.symbol,
            name = stockName,
            quantity = holding.quantity,
            averagePrice = holding.averagePrice,
            currentPrice = quote?.regularMarketPrice ?: holding.averagePrice,
            dayChange = quote?.regularMarketChange,
            dayChangePercent = quote?.regularMarketChangePercent,
            currency = holding.currency,
            priceHistory = priceHistory?.prices ?: emptyList(),
            priceHistoryTimestamps = priceHistory?.timestamps ?: emptyList(),
            annualDividend = quote?.trailingAnnualDividendRate,
            dividendYield = quote?.trailingAnnualDividendYield?.let { it * 100 },
            targetPercentage = holding.targetPercentage
        )
    }

    /**
     * Aggregates multiple holdings of the same symbol into a single Stock.
     */
    fun aggregateHoldings(
        holdingGroup: List<HoldingEntity>,
        quote: QuoteResult?,
        accounts: List<AccountEntity>,
        priceHistory: PriceHistoryData? = null
    ): Stock {
        val (symbol, totalQuantity, weightedAvgPrice, accountDetails) =
            aggregateHoldingGroup(holdingGroup, accounts)
        val stockName = quote?.longName ?: quote?.shortName ?: symbol

        return Stock(
            id = holdingGroup.first().id,
            symbol = symbol,
            name = stockName,
            quantity = totalQuantity,
            averagePrice = weightedAvgPrice,
            currentPrice = quote?.regularMarketPrice ?: weightedAvgPrice,
            dayChange = quote?.regularMarketChange,
            dayChangePercent = quote?.regularMarketChangePercent,
            currency = holdingGroup.first().currency,
            accountDetails = accountDetails,
            priceHistory = priceHistory?.prices ?: emptyList(),
            priceHistoryTimestamps = priceHistory?.timestamps ?: emptyList(),
            annualDividend = quote?.trailingAnnualDividendRate,
            dividendYield = quote?.trailingAnnualDividendYield?.let { it * 100 }
        )
    }

    /**
     * Merges a holding with an existing Stock (updates quantity and price, keeps other data).
     */
    fun mergeWithExisting(
        holding: HoldingEntity,
        existingStock: Stock?
    ): Stock {
        return Stock(
            id = holding.id,
            symbol = holding.symbol,
            name = existingStock?.name ?: holding.symbol,
            quantity = holding.quantity,
            averagePrice = holding.averagePrice,
            currentPrice = existingStock?.currentPrice ?: holding.averagePrice,
            dayChange = existingStock?.dayChange,
            dayChangePercent = existingStock?.dayChangePercent,
            currency = holding.currency,
            priceHistory = existingStock?.priceHistory ?: emptyList(),
            priceHistoryTimestamps = existingStock?.priceHistoryTimestamps ?: emptyList(),
            annualDividend = existingStock?.annualDividend,
            dividendYield = existingStock?.dividendYield,
            targetPercentage = holding.targetPercentage
        )
    }

    /**
     * Merges aggregated holdings with an existing Stock.
     */
    fun mergeAggregatedWithExisting(
        holdingGroup: List<HoldingEntity>,
        existingStock: Stock?,
        accounts: List<AccountEntity>
    ): Stock {
        val (symbol, totalQuantity, weightedAvgPrice, accountDetails) =
            aggregateHoldingGroup(holdingGroup, accounts)

        return Stock(
            id = holdingGroup.first().id,
            symbol = symbol,
            name = existingStock?.name ?: symbol,
            quantity = totalQuantity,
            averagePrice = weightedAvgPrice,
            currentPrice = existingStock?.currentPrice ?: weightedAvgPrice,
            dayChange = existingStock?.dayChange,
            dayChangePercent = existingStock?.dayChangePercent,
            currency = holdingGroup.first().currency,
            accountDetails = accountDetails,
            priceHistory = existingStock?.priceHistory ?: emptyList(),
            priceHistoryTimestamps = existingStock?.priceHistoryTimestamps ?: emptyList(),
            annualDividend = existingStock?.annualDividend,
            dividendYield = existingStock?.dividendYield
        )
    }

    private data class AggregatedHoldings(
        val symbol: String,
        val totalQuantity: Int,
        val weightedAvgPrice: Double,
        val accountDetails: List<StockAccountDetail>
    )

    private fun aggregateHoldingGroup(
        holdingGroup: List<HoldingEntity>,
        accounts: List<AccountEntity>
    ): AggregatedHoldings {
        require(holdingGroup.isNotEmpty()) { "Holding group cannot be empty" }

        val accountMap = accounts.associateBy { it.id }
        val accountOrderMap = accounts.associate { it.id to it.orderIndex }

        val symbol = holdingGroup.first().symbol
        val totalQuantity = holdingGroup.sumOf { it.quantity }
        val totalCost = holdingGroup.sumOf { it.quantity * it.averagePrice }
        val weightedAvgPrice = if (totalQuantity > 0) totalCost / totalQuantity else 0.0

        val accountDetails = holdingGroup.map { holding ->
            val accountName = accountMap[holding.accountId]?.name ?: run {
                Log.w(TAG, "Account ${holding.accountId} not found for holding ${holding.id}")
                "Unknown"
            }
            StockAccountDetail(
                holdingId = holding.id,
                accountId = holding.accountId,
                accountName = accountName,
                quantity = holding.quantity,
                averagePrice = holding.averagePrice
            )
        }.sortedBy { accountOrderMap[it.accountId] ?: Int.MAX_VALUE }

        return AggregatedHoldings(symbol, totalQuantity, weightedAvgPrice, accountDetails)
    }
}
