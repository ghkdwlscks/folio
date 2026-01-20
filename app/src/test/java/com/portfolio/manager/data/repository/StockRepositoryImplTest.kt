package com.portfolio.manager.data.repository

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.remote.YahooFinanceApi
import com.portfolio.manager.data.remote.dto.ChartData
import com.portfolio.manager.data.remote.dto.ChartMeta
import com.portfolio.manager.data.remote.dto.ChartResult
import com.portfolio.manager.data.remote.dto.YahooChartResponse
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
}
