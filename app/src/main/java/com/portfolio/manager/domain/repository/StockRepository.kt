package com.portfolio.manager.domain.repository

import com.portfolio.manager.data.remote.dto.QuoteResult
import com.portfolio.manager.domain.model.PeriodReturn
import com.portfolio.manager.domain.model.TimePeriod

/**
 * Data class holding price history with timestamps for proper date alignment.
 */
data class PriceHistoryData(
    val prices: List<Double>,
    val timestamps: List<Long>  // Unix epoch seconds
)

interface StockRepository {
    suspend fun getQuotes(symbols: List<String>): Result<List<QuoteResult>>
    suspend fun getPeriodReturn(symbol: String, period: TimePeriod): Result<PeriodReturn>
    suspend fun getExchangeRate(from: String, to: String): Result<Double>
    suspend fun getExchangeRateHistory(from: String, to: String, range: String): PriceHistoryData
    suspend fun getPriceHistory(symbols: List<String>, range: String = "1mo"): Map<String, PriceHistoryData>
    suspend fun getCachedStockName(symbol: String): String?
    suspend fun getCachedStockNames(symbols: List<String>): Map<String, String>

    /**
     * Evicts cached price history and stock names for any symbol not in
     * [keepSymbols], preventing the local database from growing unbounded as
     * holdings change. Benchmark and FX-rate series are always preserved.
     */
    suspend fun pruneCache(keepSymbols: List<String>)
}
