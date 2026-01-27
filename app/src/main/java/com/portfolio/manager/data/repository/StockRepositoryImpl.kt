package com.portfolio.manager.data.repository

import com.portfolio.manager.data.local.PriceHistoryDao
import com.portfolio.manager.data.local.PriceHistoryEntity
import com.portfolio.manager.data.local.StockNameDao
import com.portfolio.manager.data.local.StockNameEntity
import com.portfolio.manager.data.remote.YahooFinanceApi
import com.portfolio.manager.data.remote.dto.QuoteResult
import com.portfolio.manager.domain.model.PeriodReturn
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.repository.StockRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.portfolio.manager.util.JsonSerializer
import kotlinx.serialization.encodeToString
import java.time.LocalDate

class StockRepositoryImpl(
    private val api: YahooFinanceApi,
    private val priceHistoryDao: PriceHistoryDao,
    private val stockNameDao: StockNameDao
) : StockRepository {

    private val json = JsonSerializer.instance

    // Exchange rate cache with thread-safe access
    private val exchangeRateMutex = Mutex()
    @Volatile private var cachedExchangeRate: Double? = null
    @Volatile private var cacheTimestamp: Long = 0
    private val cacheValidityMs = 60 * 60 * 1000L // 1 hour

    /**
     * Decodes price history from a cached entity.
     * Returns null if decoding fails or data is invalid.
     */
    private fun decodePriceHistory(entity: PriceHistoryEntity): PriceHistoryData? {
        return try {
            val prices = json.decodeFromString<List<Double>>(entity.prices)
            val timestamps = json.decodeFromString<List<Long>>(entity.timestamps)
            if (timestamps.isNotEmpty() && timestamps.size == prices.size) {
                PriceHistoryData(prices, timestamps)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parses raw API response data into aligned price/timestamp pairs.
     * Filters out any null prices while keeping timestamps aligned.
     */
    private fun parsePriceData(rawCloses: List<Double?>, rawTimestamps: List<Long>): PriceHistoryData {
        val paired = rawTimestamps.zip(rawCloses)
            .mapNotNull { (timestamp, price) -> price?.let { timestamp to it } }
        val timestamps = paired.map { it.first }
        val prices = paired.map { it.second }
        return PriceHistoryData(prices, timestamps)
    }

    override suspend fun getQuotes(symbols: List<String>): Result<List<QuoteResult>> {
        if (symbols.isEmpty()) {
            return Result.success(emptyList())
        }

        return try {
            coroutineScope {
                // Pre-fetch cached stock names
                val cachedNames = stockNameDao.getStockNames(symbols).associateBy { it.symbol }

                // Fetch chart data with dividend events (1y range to get annual dividends)
                val chartResults = symbols.map { symbol ->
                    async {
                        try {
                            val response = api.getChart(symbol, range = "1y", events = "div")
                            val result = response.chart.result?.firstOrNull()
                            if (result != null) symbol to result else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                val chartData = chartResults.awaitAll().filterNotNull().toMap()

                // Collect names to cache
                val namesToCache = mutableListOf<StockNameEntity>()

                val quotes = symbols.mapNotNull { symbol ->
                    val result = chartData[symbol]
                    val meta = result?.meta
                    if (meta != null) {
                        // Calculate trailing annual dividend from dividend events
                        val dividendEvents = result.events?.dividends?.values ?: emptyList()
                        val trailingAnnualDividend = dividendEvents.sumOf { it.amount }
                        val dividendYield = if (meta.regularMarketPrice > 0 && trailingAnnualDividend > 0) {
                            trailingAnnualDividend / meta.regularMarketPrice
                        } else null

                        // Determine stock name: API longName > API shortName > cached name > symbol
                        val apiName = meta.longName ?: meta.shortName
                        val cachedName = cachedNames[symbol]?.name
                        val finalName = apiName ?: cachedName

                        // Cache new name if API provided one
                        if (apiName != null) {
                            namesToCache.add(StockNameEntity(symbol = symbol, name = apiName))
                        }

                        // Get previous close from price history (second-to-last close)
                        // chartPreviousClose is relative to chart range (1 year ago for 1y range)
                        val closes = result.indicators?.quote?.firstOrNull()?.close?.filterNotNull()
                        val prevClose = if (!closes.isNullOrEmpty() && closes.size >= 2) {
                            closes[closes.size - 2]
                        } else {
                            meta.chartPreviousClose
                        }

                        QuoteResult(
                            symbol = meta.symbol,
                            shortName = finalName ?: meta.shortName,
                            longName = if (apiName != null) meta.longName else finalName,
                            regularMarketPrice = meta.regularMarketPrice,
                            regularMarketPreviousClose = prevClose,
                            regularMarketChange = meta.regularMarketPrice - prevClose,
                            regularMarketChangePercent = if (prevClose > 0) {
                                ((meta.regularMarketPrice - prevClose) / prevClose) * 100
                            } else 0.0,
                            currency = meta.currency,
                            trailingAnnualDividendRate = trailingAnnualDividend.takeIf { it > 0 },
                            trailingAnnualDividendYield = dividendYield
                        )
                    } else null
                }

                // Cache stock names
                if (namesToCache.isNotEmpty()) {
                    stockNameDao.insertStockNames(namesToCache)
                }

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
        // Check cache validity (read without lock for performance)
        val now = System.currentTimeMillis()
        val cached = cachedExchangeRate
        if (cached != null && cached > 0 && (now - cacheTimestamp) < cacheValidityMs) {
            return Result.success(cached)
        }

        // Use mutex to prevent concurrent API calls
        return exchangeRateMutex.withLock {
            // Double-check cache after acquiring lock
            val cachedAfterLock = cachedExchangeRate
            val nowAfterLock = System.currentTimeMillis()
            if (cachedAfterLock != null && cachedAfterLock > 0 && (nowAfterLock - cacheTimestamp) < cacheValidityMs) {
                return@withLock Result.success(cachedAfterLock)
            }

            try {
                val symbol = "$from$to=X"
                val response = api.getChart(symbol)
                val rate = response.chart.result?.firstOrNull()?.meta?.regularMarketPrice
                    ?: return@withLock Result.failure(Exception("No exchange rate data"))

                // Validate rate is positive
                if (rate <= 0) {
                    // Return cached value if available, otherwise use default constant
                    return@withLock cachedAfterLock?.takeIf { it > 0 }?.let { Result.success(it) }
                        ?: Result.success(com.portfolio.manager.util.AppConstants.KRW_TO_USD_RATE)
                }

                // Update cache
                cachedExchangeRate = rate
                cacheTimestamp = nowAfterLock

                Result.success(rate)
            } catch (e: Exception) {
                // Return cached value if available, even if expired
                cachedAfterLock?.let { Result.success(it) } ?: Result.failure(e)
            }
        }
    }

    override suspend fun getExchangeRateHistory(from: String, to: String, range: String): PriceHistoryData {
        val symbol = "$from$to=X"
        val today = LocalDate.now().toString()

        // Check cache
        val cached = priceHistoryDao.getPriceHistory(symbol, range)
        if (cached != null && cached.lastUpdatedDate == today) {
            decodePriceHistory(cached)?.let { return it }
        }

        // Fetch from API
        return try {
            val response = api.getChart(symbol, interval = "1d", range = range)
            val result = response.chart.result?.firstOrNull()
            val rawCloses = result?.indicators?.quote?.firstOrNull()?.close ?: emptyList()
            val rawTimestamps = result?.timestamp ?: emptyList()

            val priceData = parsePriceData(rawCloses, rawTimestamps)

            // Save to cache
            if (priceData.prices.isNotEmpty()) {
                priceHistoryDao.insertPriceHistory(
                    PriceHistoryEntity(
                        symbol = symbol,
                        range = range,
                        prices = json.encodeToString(priceData.prices),
                        timestamps = json.encodeToString(priceData.timestamps),
                        lastUpdatedDate = today
                    )
                )
            }

            priceData
        } catch (e: Exception) {
            // Return cached data if available, even if stale
            cached?.let { decodePriceHistory(it) } ?: PriceHistoryData(emptyList(), emptyList())
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
                val decoded = decodePriceHistory(entity)
                if (decoded != null) {
                    result[symbol] = decoded
                } else {
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

                            symbol to parsePriceData(rawCloses, rawTimestamps)
                        } catch (e: Exception) {
                            symbol to PriceHistoryData(emptyList(), emptyList())
                        }
                    }
                }
                val fetched = fetchResults.awaitAll()

                // Save to cache and add to result
                val entitiesToSave = fetched.map { (symbol, priceData) ->
                    result[symbol] = priceData
                    PriceHistoryEntity(
                        symbol = symbol,
                        range = range,
                        prices = json.encodeToString(priceData.prices),
                        timestamps = json.encodeToString(priceData.timestamps),
                        lastUpdatedDate = today
                    )
                }
                priceHistoryDao.insertPriceHistories(entitiesToSave)
            }
        }

        return result
    }

    override suspend fun getCachedStockName(symbol: String): String? {
        return stockNameDao.getStockName(symbol)?.name
    }

    override suspend fun getCachedStockNames(symbols: List<String>): Map<String, String> {
        return stockNameDao.getStockNames(symbols).associate { it.symbol to it.name }
    }
}
