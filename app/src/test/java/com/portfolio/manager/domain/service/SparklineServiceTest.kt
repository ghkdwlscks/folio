package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.Currency
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.repository.StockRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class SparklineServiceTest {

    private lateinit var stockRepository: StockRepository

    @Before
    fun setup() {
        stockRepository = mockk()
    }

    @Test
    fun `calculateSparkline - empty stocks and cash - returns empty result`() = runTest {
        val result = SparklineService.calculateSparkline(
            stockRepository,
            stocks = emptyList(),
            cashItems = emptyList(),
            period = TimePeriod.ONE_MONTH,
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        assertThat(result).isEqualTo(SparklineResult.EMPTY)
    }

    @Test
    fun `calculateSparkline - only cash items - returns empty result`() = runTest {
        val cashItem = CashItem(1, 1L, "Savings", 10000.0, 5.0, Currency.USD, 0L)

        val result = SparklineService.calculateSparkline(
            stockRepository,
            stocks = emptyList(),
            cashItems = listOf(cashItem),
            period = TimePeriod.ONE_MONTH,
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        assertThat(result).isEqualTo(SparklineResult.EMPTY)
    }

    @Test
    fun `calculateSparkline - valid stocks - returns sparkline data`() = runTest {
        val stock = Stock(
            id = 1L, symbol = "AAPL", name = "Apple", currentPrice = 150.0, averagePrice = 100.0,
            quantity = 10, currency = Currency.USD, priceHistory = emptyList(),
            priceHistoryTimestamps = emptyList()
        )

        // Mock price history
        coEvery { stockRepository.getPriceHistory(any(), any()) } returns mapOf(
            "AAPL" to PriceHistoryData(
                listOf(100.0, 110.0, 120.0),
                listOf(1704067200L, 1704153600L, 1704240000L) // 2024-01-01, 02, 03
            )
        )

        // Mock exchange rate (stable)
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns
            PriceHistoryData(listOf(1400.0, 1400.0, 1400.0), listOf(1704067200L, 1704153600L, 1704240000L))

        val result = SparklineService.calculateSparkline(
            stockRepository,
            stocks = listOf(stock),
            cashItems = emptyList(),
            period = TimePeriod.ONE_MONTH,
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        assertThat(result.sparkline).isNotEmpty()
        // Sparkline should be normalized to start at 100
        assertThat(result.sparkline.first()).isEqualTo(100.0)
        assertThat(result.timestamps).isNotEmpty()
        assertThat(result.stats).isNotNull()
    }

    @Test
    fun `calculateSparkline - includes cash value in portfolio`() = runTest {
        val stock = Stock(
            id = 1L, symbol = "AAPL", name = "Apple", currentPrice = 100.0, averagePrice = 100.0,
            quantity = 10, currency = Currency.USD, priceHistory = emptyList(),
            priceHistoryTimestamps = emptyList()
        )
        val cashItem = CashItem(1, 1L, "Savings", 1000.0, 5.0, Currency.USD, 0L)

        coEvery { stockRepository.getPriceHistory(any(), any()) } returns mapOf(
            "AAPL" to PriceHistoryData(
                listOf(100.0, 100.0), // No change in stock price
                listOf(1704067200L, 1704153600L)
            )
        )

        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns
            PriceHistoryData(listOf(1400.0, 1400.0), listOf(1704067200L, 1704153600L))

        val result = SparklineService.calculateSparkline(
            stockRepository,
            stocks = listOf(stock),
            cashItems = listOf(cashItem),
            period = TimePeriod.ONE_MONTH,
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        // Cash value (1000) should be added to stock value (10 * 100 = 1000)
        // Total = 2000, so sparkline should still start at 100 (normalized)
        assertThat(result.sparkline).isNotEmpty()
        assertThat(result.sparkline.first()).isEqualTo(100.0)
    }

    @Test
    fun `calculateSparkline - insufficient price history - returns empty`() = runTest {
        val stock = Stock(
            id = 1L, symbol = "AAPL", name = "Apple", currentPrice = 150.0, averagePrice = 100.0,
            quantity = 10, currency = Currency.USD, priceHistory = emptyList(),
            priceHistoryTimestamps = emptyList()
        )

        // Only single data point
        coEvery { stockRepository.getPriceHistory(any(), any()) } returns mapOf(
            "AAPL" to PriceHistoryData(listOf(100.0), listOf(1704067200L))
        )

        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns
            PriceHistoryData(listOf(1400.0), listOf(1704067200L))

        val result = SparklineService.calculateSparkline(
            stockRepository,
            stocks = listOf(stock),
            cashItems = emptyList(),
            period = TimePeriod.ONE_MONTH,
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        assertThat(result).isEqualTo(SparklineResult.EMPTY)
    }

    @Test
    fun `calculateSparkline - no price history - returns empty`() = runTest {
        val stock = Stock(
            id = 1L, symbol = "AAPL", name = "Apple", currentPrice = 150.0, averagePrice = 100.0,
            quantity = 10, currency = Currency.USD, priceHistory = emptyList(),
            priceHistoryTimestamps = emptyList()
        )

        coEvery { stockRepository.getPriceHistory(any(), any()) } returns emptyMap()
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns
            PriceHistoryData(emptyList(), emptyList())

        val result = SparklineService.calculateSparkline(
            stockRepository,
            stocks = listOf(stock),
            cashItems = emptyList(),
            period = TimePeriod.ONE_MONTH,
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        assertThat(result).isEqualTo(SparklineResult.EMPTY)
    }

    @Test
    fun `calculateSparkline - showInKrw converts cash value`() = runTest {
        val stock = Stock(
            id = 1L, symbol = "AAPL", name = "Apple", currentPrice = 100.0, averagePrice = 100.0,
            quantity = 10, currency = Currency.USD, priceHistory = emptyList(),
            priceHistoryTimestamps = emptyList()
        )
        val cashItem = CashItem(1, 1L, "Savings", 1000.0, 5.0, Currency.USD, 0L)

        coEvery { stockRepository.getPriceHistory(any(), any()) } returns mapOf(
            "AAPL" to PriceHistoryData(
                listOf(100.0, 110.0),
                listOf(1704067200L, 1704153600L)
            )
        )

        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns
            PriceHistoryData(listOf(1400.0, 1400.0), listOf(1704067200L, 1704153600L))

        val result = SparklineService.calculateSparkline(
            stockRepository,
            stocks = listOf(stock),
            cashItems = listOf(cashItem),
            period = TimePeriod.ONE_MONTH,
            showInKrw = true,
            currentExchangeRate = 1400.0
        )

        // Cash value in KRW = 1000 * 1400 = 1,400,000
        // Stock value at start in KRW = 10 * 100 * 1400 = 1,400,000
        assertThat(result.sparkline).isNotEmpty()
    }
}
