package com.portfolio.manager.data.repository

import com.portfolio.manager.data.local.PriceHistoryDao
import com.portfolio.manager.data.local.PriceHistoryEntity
import com.portfolio.manager.data.remote.YahooFinanceApi
import com.portfolio.manager.data.remote.dto.QuoteResult
import com.portfolio.manager.domain.model.PeriodReturn
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.repository.StockRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

class StockRepositoryImpl(
    private val api: YahooFinanceApi,
    private val priceHistoryDao: PriceHistoryDao
) : StockRepository {

    private val json = Json { ignoreUnknownKeys = true }

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

    override suspend fun getExchangeRateHistory(from: String, to: String, range: String): PriceHistoryData {
        val symbol = "$from$to=X"
        val today = LocalDate.now().toString()

        // Check cache
        val cached = priceHistoryDao.getPriceHistory(symbol, range)
        if (cached != null && cached.lastUpdatedDate == today) {
            try {
                val prices = json.decodeFromString<List<Double>>(cached.prices)
                val timestamps = json.decodeFromString<List<Long>>(cached.timestamps)
                if (timestamps.isNotEmpty() && timestamps.size == prices.size) {
                    return PriceHistoryData(prices, timestamps)
                }
            } catch (e: Exception) {
                // Continue to fetch from API
            }
        }

        // Fetch from API
        return try {
            val response = api.getChart(symbol, interval = "1d", range = range)
            val result = response.chart.result?.firstOrNull()
            val rawCloses = result?.indicators?.quote?.firstOrNull()?.close ?: emptyList()
            val rawTimestamps = result?.timestamp ?: emptyList()

            // Filter out null prices while keeping timestamps aligned
            val paired = rawTimestamps.zip(rawCloses).filter { it.second != null }
            val timestamps = paired.map { it.first }
            val closes = paired.map { it.second!! }

            // Save to cache
            if (closes.isNotEmpty()) {
                priceHistoryDao.insertPriceHistory(
                    PriceHistoryEntity(
                        symbol = symbol,
                        range = range,
                        prices = json.encodeToString(closes),
                        timestamps = json.encodeToString(timestamps),
                        lastUpdatedDate = today
                    )
                )
            }

            PriceHistoryData(closes, timestamps)
        } catch (e: Exception) {
            // Return cached data if available, even if stale
            cached?.let {
                try {
                    PriceHistoryData(
                        prices = json.decodeFromString<List<Double>>(it.prices),
                        timestamps = json.decodeFromString<List<Long>>(it.timestamps)
                    )
                } catch (e: Exception) {
                    PriceHistoryData(emptyList(), emptyList())
                }
            } ?: PriceHistoryData(emptyList(), emptyList())
        }
    }

    override suspend fun getPriceHistory(symbols: List<String>, range: String): Map<String, PriceHistoryData> {
        if (symbols.isEmpty()) return emptyMap()

        val today = LocalDate.now().toString()

        // Check cache for all symbols
        val cached = priceHistoryDao.getPriceHistoryForSymbols(symbols, range)
        val cachedMap = cached.associateBy { it.symbol }

        // Separate symbols into cached (valid for today with proper timestamps) and needs fetch
        val result = mutableMapOf<String, PriceHistoryData>()
        val needsFetch = mutableListOf<String>()

        symbols.forEach { symbol ->
            val entity = cachedMap[symbol]
            if (entity != null && entity.lastUpdatedDate == today) {
                try {
                    val prices = json.decodeFromString<List<Double>>(entity.prices)
                    val timestamps = json.decodeFromString<List<Long>>(entity.timestamps)
                    if (timestamps.isNotEmpty() && timestamps.size == prices.size) {
                        result[symbol] = PriceHistoryData(prices, timestamps)
                    } else {
                        needsFetch.add(symbol)
                    }
                } catch (e: Exception) {
                    needsFetch.add(symbol)
                }
            } else {
                needsFetch.add(symbol)
            }
        }

        // Fetch missing/stale data from API
        if (needsFetch.isNotEmpty()) {
            coroutineScope {
                val fetchResults = needsFetch.map { symbol ->
                    async {
                        try {
                            val response = api.getChart(symbol, interval = "1d", range = range)
                            val chartResult = response.chart.result?.firstOrNull()
                            val rawCloses = chartResult?.indicators?.quote?.firstOrNull()?.close ?: emptyList()
                            val rawTimestamps = chartResult?.timestamp ?: emptyList()

                            // Filter out null prices while keeping timestamps aligned
                            val paired = rawTimestamps.zip(rawCloses).filter { it.second != null }
                            val timestamps = paired.map { it.first }
                            val closes = paired.map { it.second!! }

                            Triple(symbol, closes, timestamps)
                        } catch (e: Exception) {
                            Triple(symbol, emptyList<Double>(), emptyList<Long>())
                        }
                    }
                }
                val fetched = fetchResults.awaitAll()

                // Save to cache and add to result
                val entitiesToSave = fetched.map { (symbol, prices, timestamps) ->
                    result[symbol] = PriceHistoryData(prices, timestamps)
                    PriceHistoryEntity(
                        symbol = symbol,
                        range = range,
                        prices = json.encodeToString(prices),
                        timestamps = json.encodeToString(timestamps),
                        lastUpdatedDate = today
                    )
                }
                priceHistoryDao.insertPriceHistories(entitiesToSave)
            }
        }

        return result
    }
}
