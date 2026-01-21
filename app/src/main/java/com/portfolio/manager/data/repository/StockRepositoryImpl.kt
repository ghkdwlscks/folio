package com.portfolio.manager.data.repository

import com.portfolio.manager.data.remote.YahooFinanceApi
import com.portfolio.manager.data.remote.dto.QuoteResult
import com.portfolio.manager.domain.model.PeriodReturn
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.StockRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class StockRepositoryImpl(
    private val api: YahooFinanceApi
) : StockRepository {

    // Exchange rate cache
    private var cachedExchangeRate: Double? = null
    private var cacheTimestamp: Long = 0
    private val cacheValidityMs = 60 * 60 * 1000L // 1 hour

    override suspend fun getQuotes(symbols: List<String>): Result<List<QuoteResult>> {
        if (symbols.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {
            coroutineScope {
                val results = symbols.map { symbol ->
                    async {
                        try {
                            val response = api.getChart(symbol)
                            val meta = response.chart.result?.firstOrNull()?.meta
                            if (meta != null) {
                                QuoteResult(
                                    symbol = meta.symbol,
                                    shortName = meta.shortName,
                                    longName = meta.longName,
                                    regularMarketPrice = meta.regularMarketPrice,
                                    regularMarketPreviousClose = meta.chartPreviousClose,
                                    regularMarketChange = meta.regularMarketPrice - meta.chartPreviousClose,
                                    regularMarketChangePercent = if (meta.chartPreviousClose > 0) {
                                        ((meta.regularMarketPrice - meta.chartPreviousClose) / meta.chartPreviousClose) * 100
                                    } else 0.0,
                                    currency = meta.currency
                                )
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                val quotes = results.awaitAll().filterNotNull()
                Result.success(quotes)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPeriodReturn(symbol: String, period: TimePeriod): Result<PeriodReturn> {
        return try {
            val response = api.getChart(symbol, interval = "1d", range = period.range)
            val result = response.chart.result?.firstOrNull()
                ?: return Result.failure(Exception("No data for $symbol"))

            val currentPrice = result.meta.regularMarketPrice
            val closes = result.indicators?.quote?.firstOrNull()?.close?.filterNotNull()

            val startPrice = if (!closes.isNullOrEmpty()) {
                closes.first()
            } else {
                result.meta.chartPreviousClose
            }

            val returnPercent = if (startPrice > 0) {
                ((currentPrice - startPrice) / startPrice) * 100
            } else 0.0

            Result.success(PeriodReturn(symbol, period, returnPercent))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getExchangeRate(from: String, to: String): Result<Double> {
        // Check cache validity
        val now = System.currentTimeMillis()
        val cached = cachedExchangeRate
        if (cached != null && (now - cacheTimestamp) < cacheValidityMs) {
            return Result.success(cached)
        }

        return try {
            val symbol = "$from$to=X"
            val response = api.getChart(symbol)
            val rate = response.chart.result?.firstOrNull()?.meta?.regularMarketPrice
                ?: return Result.failure(Exception("No exchange rate data"))

            // Update cache
            cachedExchangeRate = rate
            cacheTimestamp = now

            Result.success(rate)
        } catch (e: Exception) {
            // Return cached value if available, even if expired
            cached?.let { Result.success(it) } ?: Result.failure(e)
        }
    }

    override suspend fun getPriceHistory(symbols: List<String>): Map<String, List<Double>> {
        if (symbols.isEmpty()) return emptyMap()

        return coroutineScope {
            val results = symbols.map { symbol ->
                async {
                    try {
                        val response = api.getChart(symbol, interval = "1d", range = "1mo")
                        val closes = response.chart.result?.firstOrNull()
                            ?.indicators?.quote?.firstOrNull()?.close
                            ?.filterNotNull()
                            ?: emptyList()
                        symbol to closes
                    } catch (e: Exception) {
                        symbol to emptyList()
                    }
                }
            }
            results.awaitAll().toMap()
        }
    }
}
