package com.portfolio.manager.domain.service

import com.portfolio.manager.domain.model.BenchmarkReturns
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.util.AppConstants.BENCHMARK_KOSPI
import com.portfolio.manager.util.AppConstants.BENCHMARK_SP500
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Result of loading benchmark data for a given period.
 */
data class BenchmarkDataResult(
    val returns: BenchmarkReturns,
    val sparklines: Map<String, List<Double>>,
    val timestamps: Map<String, List<Long>>
)

/**
 * Service for loading and calculating benchmark data (S&P 500, KOSPI).
 */
object BenchmarkDataService {

    /**
     * Loads benchmark data for the given period.
     *
     * @param stockRepository Repository to fetch price history
     * @param period Time period for benchmark data
     * @param showInKrw Whether to adjust returns for KRW display
     * @param currentExchangeRate Current USD/KRW exchange rate (fallback)
     * @return BenchmarkDataResult containing returns, sparklines, and timestamps
     */
    suspend fun loadBenchmarkData(
        stockRepository: StockRepository,
        period: TimePeriod,
        showInKrw: Boolean,
        currentExchangeRate: Double
    ): BenchmarkDataResult = coroutineScope {
        // Fetch exchange rate history for currency-adjusted returns
        val exchangeRateData = stockRepository.getExchangeRateHistory("USD", "KRW", period.range)
        val startExchangeRate = exchangeRateData.prices.firstOrNull() ?: currentExchangeRate
        val endExchangeRate = exchangeRateData.prices.lastOrNull() ?: currentExchangeRate

        // Fetch benchmark price histories in parallel
        val sp500Deferred = async { stockRepository.getPriceHistory(listOf(BENCHMARK_SP500), period.range) }
        val kospiDeferred = async { stockRepository.getPriceHistory(listOf(BENCHMARK_KOSPI), period.range) }

        val sp500History = sp500Deferred.await()[BENCHMARK_SP500]
        val kospiHistory = kospiDeferred.await()[BENCHMARK_KOSPI]

        // Calculate benchmark returns
        val sp500Return = calculateBenchmarkReturn(sp500History, "USD", showInKrw, startExchangeRate, endExchangeRate)
        val kospiReturn = calculateBenchmarkReturn(kospiHistory, "KRW", showInKrw, startExchangeRate, endExchangeRate)

        // Build sparklines and timestamps from same data
        val sparklines = buildSparklines(sp500History, kospiHistory)
        val timestamps = buildTimestamps(sp500History, kospiHistory)

        BenchmarkDataResult(
            returns = BenchmarkReturns(sp500 = sp500Return, kospi = kospiReturn),
            sparklines = sparklines,
            timestamps = timestamps
        )
    }

    private fun calculateBenchmarkReturn(
        history: PriceHistoryData?,
        currency: String,
        showInKrw: Boolean,
        startExchangeRate: Double,
        endExchangeRate: Double
    ): Double? {
        val prices = history?.prices ?: return null
        return PriceHistoryProcessor.calculateBenchmarkReturn(
            prices, currency, showInKrw, startExchangeRate, endExchangeRate
        )
    }

    private fun buildSparklines(
        sp500History: PriceHistoryData?,
        kospiHistory: PriceHistoryData?
    ): Map<String, List<Double>> = buildMap {
        sp500History?.prices?.takeIf { it.size >= 2 }?.let { prices ->
            put(BENCHMARK_SP500, PriceHistoryProcessor.normalizeValues(prices))
        }
        kospiHistory?.prices?.takeIf { it.size >= 2 }?.let { prices ->
            put(BENCHMARK_KOSPI, PriceHistoryProcessor.normalizeValues(prices))
        }
    }

    private fun buildTimestamps(
        sp500History: PriceHistoryData?,
        kospiHistory: PriceHistoryData?
    ): Map<String, List<Long>> = buildMap {
        sp500History?.timestamps?.takeIf { it.size >= 2 }?.let { put(BENCHMARK_SP500, it) }
        kospiHistory?.timestamps?.takeIf { it.size >= 2 }?.let { put(BENCHMARK_KOSPI, it) }
    }
}
