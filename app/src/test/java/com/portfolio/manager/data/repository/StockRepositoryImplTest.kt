package com.portfolio.manager.data.repository

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.remote.YahooFinanceApi
import com.portfolio.manager.data.remote.dto.ChartData
import com.portfolio.manager.data.remote.dto.ChartIndicators
import com.portfolio.manager.data.remote.dto.ChartMeta
import com.portfolio.manager.data.remote.dto.ChartQuote
import com.portfolio.manager.data.remote.dto.ChartResult
import com.portfolio.manager.data.remote.dto.YahooChartResponse
import com.portfolio.manager.domain.model.TimePeriod
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException

class StockRepositoryImplTest {

    private lateinit var api: YahooFinanceApi
    private lateinit var repository: StockRepositoryImpl

    @Before
    fun setup() {
        api = mockk()
        repository = StockRepositoryImpl(api)
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
}
