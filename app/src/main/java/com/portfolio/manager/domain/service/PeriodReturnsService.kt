package com.portfolio.manager.domain.service

import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.StockRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Service for calculating portfolio period returns.
 */
object PeriodReturnsService {

    /**
     * Calculates period returns for all time periods.
     *
     * @param stockRepository Repository to fetch price history
     * @param stocks List of stocks in portfolio
     * @param cashItems List of cash items in portfolio
     * @param showInKrw Whether to display values in KRW
     * @param currentExchangeRate Current USD/KRW exchange rate
     * @return Map of TimePeriod to return percentage
     */
    suspend fun calculateAllPeriodReturns(
        stockRepository: StockRepository,
        stocks: List<Stock>,
        cashItems: List<CashItem>,
        showInKrw: Boolean,
        currentExchangeRate: Double
    ): Map<TimePeriod, Double> = coroutineScope {
        if (stocks.isEmpty() && cashItems.isEmpty()) {
            return@coroutineScope emptyMap()
        }

        val totalCashValueUsd = cashItems.sumOf { it.valueInUsd(currentExchangeRate) }

        // Cash-only portfolio
        if (stocks.isEmpty()) {
            return@coroutineScope TimePeriod.entries.associateWith { period ->
                PortfolioCalculationService.calculateWeightedCashReturn(cashItems, currentExchangeRate, period)
            }
        }

        val symbols = stocks.map { it.symbol }.distinct()
        val stocksForCalc = stocks.filter { it.totalValueInUsd(currentExchangeRate) > 0 }
        if (stocksForCalc.isEmpty() && cashItems.isEmpty()) {
            return@coroutineScope emptyMap()
        }

        // Calculate weights
        val totalStocksValueUsd = stocksForCalc.sumOf { it.totalValueInUsd(currentExchangeRate) }
        val totalPortfolioValue = totalStocksValueUsd + totalCashValueUsd
        val stocksWeight = if (totalPortfolioValue > 0) totalStocksValueUsd / totalPortfolioValue else 1.0
        val cashWeight = if (totalPortfolioValue > 0) totalCashValueUsd / totalPortfolioValue else 0.0

        // Calculate returns for all periods in parallel
        TimePeriod.entries.map { period ->
            async {
                try {
                    val periodReturn = calculatePeriodReturn(
                        stockRepository,
                        symbols,
                        stocksForCalc,
                        cashItems,
                        period,
                        showInKrw,
                        currentExchangeRate,
                        stocksWeight,
                        cashWeight
                    )
                    period to periodReturn
                } catch (e: Exception) {
                    period to 0.0
                }
            }
        }.awaitAll().toMap()
    }

    private suspend fun calculatePeriodReturn(
        stockRepository: StockRepository,
        symbols: List<String>,
        stocks: List<Stock>,
        cashItems: List<CashItem>,
        period: TimePeriod,
        showInKrw: Boolean,
        currentExchangeRate: Double,
        stocksWeight: Double,
        cashWeight: Double
    ): Double {
        val priceHistoryMap = stockRepository.getPriceHistory(symbols, period.range)
        val exchangeRateData = stockRepository.getExchangeRateHistory("USD", "KRW", period.range)

        val result = PortfolioCalculationService.buildPortfolioValues(
            stocks, priceHistoryMap, exchangeRateData, currentExchangeRate, showInKrw
        )
        val stocksReturn = if (result != null) {
            PortfolioCalculationService.calculatePeriodReturn(result.values)
        } else {
            0.0
        }

        val cashReturn = PortfolioCalculationService.calculateWeightedCashReturn(cashItems, currentExchangeRate, period)
        return (stocksWeight * stocksReturn) + (cashWeight * cashReturn)
    }
}
