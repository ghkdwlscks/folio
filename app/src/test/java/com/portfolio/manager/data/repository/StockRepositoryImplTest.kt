package com.portfolio.manager.data.repository

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.PriceHistoryDao
import com.portfolio.manager.data.local.PriceHistoryEntity
import com.portfolio.manager.data.remote.YahooFinanceApi
import com.portfolio.manager.data.remote.dto.ChartData
import com.portfolio.manager.data.remote.dto.ChartIndicators
import com.portfolio.manager.data.remote.dto.ChartMeta
import com.portfolio.manager.data.remote.dto.ChartQuote
import com.portfolio.manager.data.remote.dto.ChartResult
import com.portfolio.manager.data.remote.dto.YahooChartResponse
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.PriceHistoryData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.time.LocalDate

class StockRepositoryImplTest {

    private lateinit var api: YahooFinanceApi
    private lateinit var priceHistoryDao: PriceHistoryDao
    private lateinit var repository: StockRepositoryImpl

    @Before
    fun setup() {
        api = mockk()
        priceHistoryDao = mockk()
        repository = StockRepositoryImpl(api, priceHistoryDao)

        // Default: no cached data
        coEvery { priceHistoryDao.getPriceHistoryForSymbols(any(), any()) } returns emptyList()
        coEvery { priceHistoryDao.getPriceHistory(any(), any()) } returns null
        coEvery { priceHistoryDao.insertPriceHistories(any()) } returns Unit
        coEvery { priceHistoryDao.insertPriceHistory(any()) } returns Unit
    }

    @Test
    fun `getQuotes - success - returns quotes`() = runTest {
        coEvery { api.getChart("AAPL", any(), any()) } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(
                            symbol = "AAPL",
                            shortName = "Apple Inc.",
                            regularMarketPrice = 178.50,
                            chartPreviousClose = 175.00,
                            currency = "USD"
                        )
                    )
                )
            )
        )

        val result = repository.getQuotes(listOf("AAPL"))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).hasSize(1)
        val quote = result.getOrNull()?.first()
        assertThat(quote?.symbol).isEqualTo("AAPL")
        assertThat(quote?.regularMarketPrice).isEqualTo(178.50)
        assertThat(quote?.regularMarketChange).isWithin(0.01).of(3.50)
    }

    @Test
    fun `getQuotes - multiple symbols - fetches each`() = runTest {
        coEvery { api.getChart("AAPL", any(), any()) } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(meta = ChartMeta(symbol = "AAPL", regularMarketPrice = 178.50, chartPreviousClose = 175.00))
                )
            )
        )
        coEvery { api.getChart("GOOGL", any(), any()) } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(meta = ChartMeta(symbol = "GOOGL", regularMarketPrice = 141.80, chartPreviousClose = 140.00))
                )
            )
        )

        val result = repository.getQuotes(listOf("AAPL", "GOOGL"))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).hasSize(2)
    }

    @Test
    fun `getQuotes - one fails - returns successful ones`() = runTest {
        coEvery { api.getChart("AAPL", any(), any()) } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(meta = ChartMeta(symbol = "AAPL", regularMarketPrice = 178.50, chartPreviousClose = 175.00))
                )
            )
        )
        coEvery { api.getChart("INVALID", any(), any()) } throws IOException("Not found")

        val result = repository.getQuotes(listOf("AAPL", "INVALID"))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).hasSize(1)
        assertThat(result.getOrNull()?.first()?.symbol).isEqualTo("AAPL")
    }

    @Test
    fun `getQuotes - empty symbols - returns empty list`() = runTest {
        val result = repository.getQuotes(emptyList())

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun `getQuotes - null result - skips symbol`() = runTest {
        coEvery { api.getChart("AAPL", any(), any()) } returns YahooChartResponse(
            chart = ChartData(result = null)
        )

        val result = repository.getQuotes(listOf("AAPL"))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEmpty()
    }

    @Test
    fun `getPeriodReturn - success with closing prices - calculates return`() = runTest {
        coEvery { api.getChart("AAPL", "1d", "1mo") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(
                            symbol = "AAPL",
                            regularMarketPrice = 180.0,
                            chartPreviousClose = 175.0
                        ),
                        indicators = ChartIndicators(
                            quote = listOf(
                                ChartQuote(close = listOf(150.0, 160.0, 170.0, 180.0))
                            )
                        )
                    )
                )
            )
        )

        val result = repository.getPeriodReturn("AAPL", TimePeriod.ONE_MONTH)

        assertThat(result.isSuccess).isTrue()
        val periodReturn = result.getOrNull()
        assertThat(periodReturn?.symbol).isEqualTo("AAPL")
        assertThat(periodReturn?.period).isEqualTo(TimePeriod.ONE_MONTH)
        // Return: (180 - 150) / 150 * 100 = 20%
        assertThat(periodReturn?.returnPercent).isWithin(0.01).of(20.0)
    }

    @Test
    fun `getPeriodReturn - no closing prices - uses chartPreviousClose`() = runTest {
        coEvery { api.getChart("AAPL", "1d", "1d") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(
                            symbol = "AAPL",
                            regularMarketPrice = 180.0,
                            chartPreviousClose = 175.0
                        ),
                        indicators = null
                    )
                )
            )
        )

        val result = repository.getPeriodReturn("AAPL", TimePeriod.ONE_DAY)

        assertThat(result.isSuccess).isTrue()
        val periodReturn = result.getOrNull()
        // Return: (180 - 175) / 175 * 100 = 2.857%
        assertThat(periodReturn?.returnPercent).isWithin(0.01).of(2.857)
    }

    @Test
    fun `getPeriodReturn - null result - returns failure`() = runTest {
        coEvery { api.getChart("INVALID", "1d", "1mo") } returns YahooChartResponse(
            chart = ChartData(result = null)
        )

        val result = repository.getPeriodReturn("INVALID", TimePeriod.ONE_MONTH)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `getPeriodReturn - api throws exception - returns failure`() = runTest {
        coEvery { api.getChart("AAPL", "1d", "1y") } throws IOException("Network error")

        val result = repository.getPeriodReturn("AAPL", TimePeriod.ONE_YEAR)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `getPeriodReturn - empty closing prices - uses chartPreviousClose`() = runTest {
        coEvery { api.getChart("AAPL", "1d", "5d") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(
                            symbol = "AAPL",
                            regularMarketPrice = 180.0,
                            chartPreviousClose = 170.0
                        ),
                        indicators = ChartIndicators(
                            quote = listOf(ChartQuote(close = emptyList()))
                        )
                    )
                )
            )
        )

        val result = repository.getPeriodReturn("AAPL", TimePeriod.ONE_WEEK)

        assertThat(result.isSuccess).isTrue()
        val periodReturn = result.getOrNull()
        // Return: (180 - 170) / 170 * 100 = 5.882%
        assertThat(periodReturn?.returnPercent).isWithin(0.01).of(5.882)
    }

    @Test
    fun `getPeriodReturn - zero start price - returns zero percent`() = runTest {
        coEvery { api.getChart("AAPL", "1d", "6mo") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(
                            symbol = "AAPL",
                            regularMarketPrice = 180.0,
                            chartPreviousClose = 0.0
                        ),
                        indicators = ChartIndicators(
                            quote = listOf(ChartQuote(close = listOf(0.0)))
                        )
                    )
                )
            )
        )

        val result = repository.getPeriodReturn("AAPL", TimePeriod.SIX_MONTHS)

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()?.returnPercent).isEqualTo(0.0)
    }

    @Test
    fun `getExchangeRate - success - returns rate`() = runTest {
        coEvery { api.getChart("USDKRW=X", any(), any()) } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(
                            symbol = "USDKRW=X",
                            regularMarketPrice = 1350.50,
                            chartPreviousClose = 1345.00
                        )
                    )
                )
            )
        )

        val result = repository.getExchangeRate("USD", "KRW")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(1350.50)
    }

    @Test
    fun `getExchangeRate - uses cached rate within validity period`() = runTest {
        coEvery { api.getChart("USDKRW=X", any(), any()) } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(
                            symbol = "USDKRW=X",
                            regularMarketPrice = 1350.50,
                            chartPreviousClose = 1345.00
                        )
                    )
                )
            )
        )

        // First call fetches from API
        val result1 = repository.getExchangeRate("USD", "KRW")
        assertThat(result1.getOrNull()).isEqualTo(1350.50)

        // Second call should use cache (mock not called again)
        coEvery { api.getChart("USDKRW=X", any(), any()) } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(
                            symbol = "USDKRW=X",
                            regularMarketPrice = 1400.00,
                            chartPreviousClose = 1345.00
                        )
                    )
                )
            )
        )

        val result2 = repository.getExchangeRate("USD", "KRW")
        assertThat(result2.getOrNull()).isEqualTo(1350.50) // Still cached value
    }

    @Test
    fun `getExchangeRate - api failure - returns cached rate if available`() = runTest {
        // First call succeeds and caches
        coEvery { api.getChart("USDKRW=X", any(), any()) } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(
                            symbol = "USDKRW=X",
                            regularMarketPrice = 1350.50,
                            chartPreviousClose = 1345.00
                        )
                    )
                )
            )
        )
        repository.getExchangeRate("USD", "KRW")

        // Create new repository to simulate expired cache scenario
        // Note: In real test we'd need a way to manipulate time
        // For now, testing that failure returns cached value
        coEvery { api.getChart("USDKRW=X", any(), any()) } throws IOException("Network error")

        // Still returns cached value because cache is valid
        val result = repository.getExchangeRate("USD", "KRW")
        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(1350.50)
    }

    @Test
    fun `getExchangeRate - api failure no cache - returns failure`() = runTest {
        // Fresh repository with no cache
        val freshRepository = StockRepositoryImpl(api, priceHistoryDao)
        coEvery { api.getChart("USDKRW=X", any(), any()) } throws IOException("Network error")

        val result = freshRepository.getExchangeRate("USD", "KRW")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `getExchangeRate - null result - returns failure`() = runTest {
        coEvery { api.getChart("USDKRW=X", any(), any()) } returns YahooChartResponse(
            chart = ChartData(result = null)
        )

        val result = repository.getExchangeRate("USD", "KRW")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `getExchangeRate - empty result - returns failure`() = runTest {
        coEvery { api.getChart("USDKRW=X", any(), any()) } returns YahooChartResponse(
            chart = ChartData(result = emptyList())
        )

        val result = repository.getExchangeRate("USD", "KRW")

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `getPriceHistory - success - returns closing prices with timestamps`() = runTest {
        coEvery { api.getChart("AAPL", "1d", "1y") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(
                            symbol = "AAPL",
                            regularMarketPrice = 180.0,
                            chartPreviousClose = 175.0
                        ),
                        timestamp = listOf(1000L, 2000L, 3000L, 4000L, 5000L),
                        indicators = ChartIndicators(
                            quote = listOf(
                                ChartQuote(close = listOf(150.0, 155.0, 160.0, 170.0, 180.0))
                            )
                        )
                    )
                )
            )
        )

        val result = repository.getPriceHistory(listOf("AAPL"), "1y")

        assertThat(result).containsKey("AAPL")
        assertThat(result["AAPL"]?.prices).containsExactly(150.0, 155.0, 160.0, 170.0, 180.0).inOrder()
        assertThat(result["AAPL"]?.timestamps).containsExactly(1000L, 2000L, 3000L, 4000L, 5000L).inOrder()
    }

    @Test
    fun `getPriceHistory - multiple symbols - returns map of prices`() = runTest {
        coEvery { api.getChart("AAPL", "1d", "5d") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(symbol = "AAPL", regularMarketPrice = 180.0, chartPreviousClose = 175.0),
                        timestamp = listOf(1000L, 2000L),
                        indicators = ChartIndicators(quote = listOf(ChartQuote(close = listOf(170.0, 180.0))))
                    )
                )
            )
        )
        coEvery { api.getChart("GOOGL", "1d", "5d") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(symbol = "GOOGL", regularMarketPrice = 140.0, chartPreviousClose = 135.0),
                        timestamp = listOf(1000L, 2000L),
                        indicators = ChartIndicators(quote = listOf(ChartQuote(close = listOf(135.0, 140.0))))
                    )
                )
            )
        )

        val result = repository.getPriceHistory(listOf("AAPL", "GOOGL"), "5d")

        assertThat(result).hasSize(2)
        assertThat(result["AAPL"]?.prices).containsExactly(170.0, 180.0).inOrder()
        assertThat(result["GOOGL"]?.prices).containsExactly(135.0, 140.0).inOrder()
    }

    @Test
    fun `getPriceHistory - empty symbols - returns empty map`() = runTest {
        val result = repository.getPriceHistory(emptyList())

        assertThat(result).isEmpty()
    }

    @Test
    fun `getPriceHistory - api failure - returns empty data for symbol`() = runTest {
        coEvery { api.getChart("AAPL", "1d", "1mo") } throws IOException("Network error")

        val result = repository.getPriceHistory(listOf("AAPL"), "1mo")

        assertThat(result).containsKey("AAPL")
        assertThat(result["AAPL"]?.prices).isEmpty()
        assertThat(result["AAPL"]?.timestamps).isEmpty()
    }

    @Test
    fun `getPriceHistory - null result - returns empty data for symbol`() = runTest {
        coEvery { api.getChart("AAPL", "1d", "1mo") } returns YahooChartResponse(
            chart = ChartData(result = null)
        )

        val result = repository.getPriceHistory(listOf("AAPL"), "1mo")

        assertThat(result).containsKey("AAPL")
        assertThat(result["AAPL"]?.prices).isEmpty()
        assertThat(result["AAPL"]?.timestamps).isEmpty()
    }

    @Test
    fun `getPriceHistory - closes with nulls - filters them out with aligned timestamps`() = runTest {
        coEvery { api.getChart("AAPL", "1d", "6mo") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(symbol = "AAPL", regularMarketPrice = 180.0, chartPreviousClose = 175.0),
                        timestamp = listOf(1000L, 2000L, 3000L, 4000L, 5000L),
                        indicators = ChartIndicators(
                            quote = listOf(ChartQuote(close = listOf(150.0, null, 160.0, null, 180.0)))
                        )
                    )
                )
            )
        )

        val result = repository.getPriceHistory(listOf("AAPL"), "6mo")

        assertThat(result["AAPL"]?.prices).containsExactly(150.0, 160.0, 180.0).inOrder()
        assertThat(result["AAPL"]?.timestamps).containsExactly(1000L, 3000L, 5000L).inOrder()
    }

    @Test
    fun `getPriceHistory - uses default range when not specified`() = runTest {
        coEvery { api.getChart("AAPL", "1d", "1mo") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(symbol = "AAPL", regularMarketPrice = 180.0, chartPreviousClose = 175.0),
                        timestamp = listOf(1000L, 2000L),
                        indicators = ChartIndicators(quote = listOf(ChartQuote(close = listOf(170.0, 180.0))))
                    )
                )
            )
        )

        val result = repository.getPriceHistory(listOf("AAPL"))

        assertThat(result["AAPL"]?.prices).containsExactly(170.0, 180.0).inOrder()
    }

    @Test
    fun `getPriceHistory - uses cache when data is from today with valid timestamps`() = runTest {
        val today = LocalDate.now().toString()
        coEvery { priceHistoryDao.getPriceHistoryForSymbols(listOf("AAPL"), "1y") } returns listOf(
            PriceHistoryEntity(
                symbol = "AAPL",
                range = "1y",
                prices = "[150.0,160.0,170.0]",
                timestamps = "[1000,2000,3000]",
                lastUpdatedDate = today
            )
        )

        val result = repository.getPriceHistory(listOf("AAPL"), "1y")

        assertThat(result["AAPL"]?.prices).containsExactly(150.0, 160.0, 170.0).inOrder()
        assertThat(result["AAPL"]?.timestamps).containsExactly(1000L, 2000L, 3000L).inOrder()
        // API should not be called since cache is valid
        coVerify(exactly = 0) { api.getChart("AAPL", any(), any()) }
    }

    @Test
    fun `getPriceHistory - fetches from API when cache is stale`() = runTest {
        val yesterday = LocalDate.now().minusDays(1).toString()
        coEvery { priceHistoryDao.getPriceHistoryForSymbols(listOf("AAPL"), "1y") } returns listOf(
            PriceHistoryEntity(
                symbol = "AAPL",
                range = "1y",
                prices = "[100.0,110.0]",
                timestamps = "[1000,2000]",
                lastUpdatedDate = yesterday
            )
        )
        coEvery { api.getChart("AAPL", "1d", "1y") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(symbol = "AAPL", regularMarketPrice = 180.0, chartPreviousClose = 175.0),
                        timestamp = listOf(1000L, 2000L, 3000L),
                        indicators = ChartIndicators(quote = listOf(ChartQuote(close = listOf(150.0, 160.0, 170.0))))
                    )
                )
            )
        )

        val result = repository.getPriceHistory(listOf("AAPL"), "1y")

        assertThat(result["AAPL"]?.prices).containsExactly(150.0, 160.0, 170.0).inOrder()
        coVerify { api.getChart("AAPL", "1d", "1y") }
        coVerify { priceHistoryDao.insertPriceHistories(any()) }
    }

    @Test
    fun `getPriceHistory - saves fetched data to cache`() = runTest {
        coEvery { api.getChart("AAPL", "1d", "1mo") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(symbol = "AAPL", regularMarketPrice = 180.0, chartPreviousClose = 175.0),
                        timestamp = listOf(1000L, 2000L),
                        indicators = ChartIndicators(quote = listOf(ChartQuote(close = listOf(170.0, 180.0))))
                    )
                )
            )
        )

        repository.getPriceHistory(listOf("AAPL"), "1mo")

        coVerify { priceHistoryDao.insertPriceHistories(any()) }
    }

    @Test
    fun `getPriceHistory - mixed cache hit and miss`() = runTest {
        val today = LocalDate.now().toString()
        // AAPL is cached, GOOGL is not
        coEvery { priceHistoryDao.getPriceHistoryForSymbols(listOf("AAPL", "GOOGL"), "1y") } returns listOf(
            PriceHistoryEntity(
                symbol = "AAPL",
                range = "1y",
                prices = "[150.0,160.0]",
                timestamps = "[1000,2000]",
                lastUpdatedDate = today
            )
        )
        coEvery { api.getChart("GOOGL", "1d", "1y") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(symbol = "GOOGL", regularMarketPrice = 140.0, chartPreviousClose = 135.0),
                        timestamp = listOf(1000L, 2000L),
                        indicators = ChartIndicators(quote = listOf(ChartQuote(close = listOf(130.0, 140.0))))
                    )
                )
            )
        )

        val result = repository.getPriceHistory(listOf("AAPL", "GOOGL"), "1y")

        assertThat(result["AAPL"]?.prices).containsExactly(150.0, 160.0).inOrder()
        assertThat(result["GOOGL"]?.prices).containsExactly(130.0, 140.0).inOrder()
        // Only GOOGL should be fetched from API
        coVerify(exactly = 0) { api.getChart("AAPL", any(), any()) }
        coVerify(exactly = 1) { api.getChart("GOOGL", "1d", "1y") }
    }

    @Test
    fun `getExchangeRateHistory - success - returns rate history with timestamps`() = runTest {
        coEvery { api.getChart("USDKRW=X", "1d", "1y") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(
                            symbol = "USDKRW=X",
                            regularMarketPrice = 1400.0,
                            chartPreviousClose = 1350.0
                        ),
                        timestamp = listOf(1000L, 2000L, 3000L, 4000L, 5000L),
                        indicators = ChartIndicators(
                            quote = listOf(
                                ChartQuote(close = listOf(1300.0, 1320.0, 1350.0, 1380.0, 1400.0))
                            )
                        )
                    )
                )
            )
        )

        val result = repository.getExchangeRateHistory("USD", "KRW", "1y")

        assertThat(result.prices).containsExactly(1300.0, 1320.0, 1350.0, 1380.0, 1400.0).inOrder()
        assertThat(result.timestamps).containsExactly(1000L, 2000L, 3000L, 4000L, 5000L).inOrder()
    }

    @Test
    fun `getExchangeRateHistory - uses cache when data is from today with valid timestamps`() = runTest {
        val today = LocalDate.now().toString()
        coEvery { priceHistoryDao.getPriceHistory("USDKRW=X", "1y") } returns PriceHistoryEntity(
            symbol = "USDKRW=X",
            range = "1y",
            prices = "[1300.0,1350.0,1400.0]",
            timestamps = "[1000,2000,3000]",
            lastUpdatedDate = today
        )

        val result = repository.getExchangeRateHistory("USD", "KRW", "1y")

        assertThat(result.prices).containsExactly(1300.0, 1350.0, 1400.0).inOrder()
        assertThat(result.timestamps).containsExactly(1000L, 2000L, 3000L).inOrder()
        coVerify(exactly = 0) { api.getChart("USDKRW=X", any(), any()) }
    }

    @Test
    fun `getExchangeRateHistory - fetches from API when cache is stale`() = runTest {
        val yesterday = LocalDate.now().minusDays(1).toString()
        coEvery { priceHistoryDao.getPriceHistory("USDKRW=X", "1y") } returns PriceHistoryEntity(
            symbol = "USDKRW=X",
            range = "1y",
            prices = "[1250.0,1280.0]",
            timestamps = "[1000,2000]",
            lastUpdatedDate = yesterday
        )
        coEvery { api.getChart("USDKRW=X", "1d", "1y") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(symbol = "USDKRW=X", regularMarketPrice = 1400.0, chartPreviousClose = 1350.0),
                        timestamp = listOf(1000L, 2000L, 3000L),
                        indicators = ChartIndicators(
                            quote = listOf(ChartQuote(close = listOf(1300.0, 1350.0, 1400.0)))
                        )
                    )
                )
            )
        )

        val result = repository.getExchangeRateHistory("USD", "KRW", "1y")

        assertThat(result.prices).containsExactly(1300.0, 1350.0, 1400.0).inOrder()
        coVerify { api.getChart("USDKRW=X", "1d", "1y") }
        coVerify { priceHistoryDao.insertPriceHistory(any()) }
    }

    @Test
    fun `getExchangeRateHistory - api failure with stale cache - returns cached data`() = runTest {
        val yesterday = LocalDate.now().minusDays(1).toString()
        coEvery { priceHistoryDao.getPriceHistory("USDKRW=X", "1mo") } returns PriceHistoryEntity(
            symbol = "USDKRW=X",
            range = "1mo",
            prices = "[1300.0,1350.0]",
            timestamps = "[1000,2000]",
            lastUpdatedDate = yesterday
        )
        coEvery { api.getChart("USDKRW=X", "1d", "1mo") } throws IOException("Network error")

        val result = repository.getExchangeRateHistory("USD", "KRW", "1mo")

        assertThat(result.prices).containsExactly(1300.0, 1350.0).inOrder()
    }

    @Test
    fun `getExchangeRateHistory - api failure no cache - returns empty data`() = runTest {
        coEvery { api.getChart("USDKRW=X", "1d", "1mo") } throws IOException("Network error")

        val result = repository.getExchangeRateHistory("USD", "KRW", "1mo")

        assertThat(result.prices).isEmpty()
        assertThat(result.timestamps).isEmpty()
    }

    @Test
    fun `getExchangeRateHistory - null result - returns empty data`() = runTest {
        coEvery { api.getChart("USDKRW=X", "1d", "1mo") } returns YahooChartResponse(
            chart = ChartData(result = null)
        )

        val result = repository.getExchangeRateHistory("USD", "KRW", "1mo")

        assertThat(result.prices).isEmpty()
        assertThat(result.timestamps).isEmpty()
    }

    @Test
    fun `getExchangeRateHistory - filters null values with aligned timestamps`() = runTest {
        coEvery { api.getChart("USDKRW=X", "1d", "1mo") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(symbol = "USDKRW=X", regularMarketPrice = 1400.0, chartPreviousClose = 1350.0),
                        timestamp = listOf(1000L, 2000L, 3000L, 4000L, 5000L),
                        indicators = ChartIndicators(
                            quote = listOf(ChartQuote(close = listOf(1300.0, null, 1350.0, null, 1400.0)))
                        )
                    )
                )
            )
        )

        val result = repository.getExchangeRateHistory("USD", "KRW", "1mo")

        assertThat(result.prices).containsExactly(1300.0, 1350.0, 1400.0).inOrder()
        assertThat(result.timestamps).containsExactly(1000L, 3000L, 5000L).inOrder()
    }

    @Test
    fun `getExchangeRateHistory - saves fetched data to cache`() = runTest {
        coEvery { api.getChart("USDKRW=X", "1d", "1mo") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(symbol = "USDKRW=X", regularMarketPrice = 1400.0, chartPreviousClose = 1350.0),
                        timestamp = listOf(1000L, 2000L),
                        indicators = ChartIndicators(
                            quote = listOf(ChartQuote(close = listOf(1300.0, 1400.0)))
                        )
                    )
                )
            )
        )

        repository.getExchangeRateHistory("USD", "KRW", "1mo")

        coVerify { priceHistoryDao.insertPriceHistory(any()) }
    }

    @Test
    fun `getExchangeRateHistory - cache parse error - fetches from API`() = runTest {
        val today = LocalDate.now().toString()
        coEvery { priceHistoryDao.getPriceHistory("USDKRW=X", "1mo") } returns PriceHistoryEntity(
            symbol = "USDKRW=X",
            range = "1mo",
            prices = "invalid json",
            timestamps = "[]",
            lastUpdatedDate = today
        )
        coEvery { api.getChart("USDKRW=X", "1d", "1mo") } returns YahooChartResponse(
            chart = ChartData(
                result = listOf(
                    ChartResult(
                        meta = ChartMeta(symbol = "USDKRW=X", regularMarketPrice = 1400.0, chartPreviousClose = 1350.0),
                        timestamp = listOf(1000L, 2000L),
                        indicators = ChartIndicators(
                            quote = listOf(ChartQuote(close = listOf(1300.0, 1400.0)))
                        )
                    )
                )
            )
        )

        val result = repository.getExchangeRateHistory("USD", "KRW", "1mo")

        // Parse error triggers API fetch
        assertThat(result.prices).containsExactly(1300.0, 1400.0).inOrder()
    }
}
