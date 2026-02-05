package com.portfolio.manager.domain.service

import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.PortfolioStats
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.StockRepository

/**
 * Result of calculating portfolio sparkline data.
 */
data class SparklineResult(
    val sparkline: List<Double>,
    val timestamps: List<Long>,
    val stats: PortfolioStats
) {
    companion object {
        val EMPTY = SparklineResult(emptyList(), emptyList(), PortfolioStats())
    }
}

/**
 * Service for calculating portfolio sparkline data.
 */
object SparklineService {

    /**
     * Calculates portfolio sparkline data for the given period.
     *
     * @param stockRepository Repository to fetch price history
     * @param stocks List of stocks in portfolio
     * @param cashItems List of cash items in portfolio
     * @param period Time period for sparkline
     * @param showInKrw Whether to display values in KRW
     * @param currentExchangeRate Current USD/KRW exchange rate
     * @return SparklineResult containing normalized values, timestamps, and statistics
     */
    suspend fun calculateSparkline(
        stockRepository: StockRepository,
        stocks: List<Stock>,
        cashItems: List<CashItem>,
        period: TimePeriod,
        showInKrw: Boolean,
        currentExchangeRate: Double
    ): SparklineResult {
        if (stocks.isEmpty() && cashItems.isEmpty()) {
            return SparklineResult.EMPTY
        }

        val totalCashValue = if (showInKrw) {
            cashItems.sumOf { it.valueInKrw(currentExchangeRate) }
        } else {
            cashItems.sumOf { it.valueInUsd(currentExchangeRate) }
        }

        if (stocks.isEmpty()) {
            return SparklineResult.EMPTY
        }

        return try {
            val symbols = stocks.map { it.symbol }.distinct()
            val priceHistoryMap = stockRepository.getPriceHistory(symbols, period.range)
            val exchangeRateData = stockRepository.getExchangeRateHistory("USD", "KRW", period.range)

            val result = PortfolioCalculationService.buildPortfolioValues(
                stocks, priceHistoryMap, exchangeRateData, currentExchangeRate, showInKrw
            ) ?: return SparklineResult.EMPTY

            val portfolioValues = result.values.map { it + totalCashValue }
            if (portfolioValues.size < 2) {
                return SparklineResult.EMPTY
            }

            val timestamps = result.validDates.mapNotNull { PriceHistoryProcessor.dateToTimestamp(it) }
            val stats = PortfolioStatsCalculator.calculate(portfolioValues)
            val normalizedPortfolio = PriceHistoryProcessor.normalizeValues(portfolioValues)

            SparklineResult(normalizedPortfolio, timestamps, stats)
        } catch (e: Exception) {
            SparklineResult.EMPTY
        }
    }
}
