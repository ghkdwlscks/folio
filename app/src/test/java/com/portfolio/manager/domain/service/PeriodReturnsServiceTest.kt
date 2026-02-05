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

class PeriodReturnsServiceTest {

    private lateinit var stockRepository: StockRepository

    @Before
    fun setup() {
        stockRepository = mockk()
    }

    @Test
    fun `calculateAllPeriodReturns - empty stocks and cash - returns empty map`() = runTest {
        val result = PeriodReturnsService.calculateAllPeriodReturns(
            stockRepository,
            stocks = emptyList(),
            cashItems = emptyList(),
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun `calculateAllPeriodReturns - only cash items - returns cash returns for all periods`() = runTest {
        val cashItem = CashItem(1, 1L, "Savings", 10000.0, 5.0, Currency.USD, 0L)

        val result = PeriodReturnsService.calculateAllPeriodReturns(
            stockRepository,
            stocks = emptyList(),
            cashItems = listOf(cashItem),
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        // Should have returns for all time periods
        assertThat(result).hasSize(TimePeriod.entries.size)
        // One year should have 5% return
        assertThat(result[TimePeriod.ONE_YEAR]).isWithin(0.01).of(5.0)
        // Six months should have ~2.5% return
        assertThat(result[TimePeriod.SIX_MONTHS]).isWithin(0.01).of(2.5)
    }

    @Test
    fun `calculateAllPeriodReturns - valid stocks - returns returns for all periods`() = runTest {
        val stock = Stock(
            id = 1L, symbol = "AAPL", name = "Apple", currentPrice = 150.0, averagePrice = 100.0,
            quantity = 10, currency = Currency.USD, priceHistory = emptyList(),
            priceHistoryTimestamps = emptyList()
        )

        // Mock price history with 10% return for all periods
        coEvery { stockRepository.getPriceHistory(any(), any()) } returns mapOf(
            "AAPL" to PriceHistoryData(
                listOf(100.0, 110.0),
                listOf(1704067200L, 1704153600L)
            )
        )

        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns
            PriceHistoryData(listOf(1400.0, 1400.0), listOf(1704067200L, 1704153600L))

        val result = PeriodReturnsService.calculateAllPeriodReturns(
            stockRepository,
            stocks = listOf(stock),
            cashItems = emptyList(),
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        // Should have returns for all time periods
        assertThat(result).hasSize(TimePeriod.entries.size)
        // All periods should have 10% return
        result.values.forEach { returnPercent ->
            assertThat(returnPercent).isWithin(0.5).of(10.0)
        }
    }

    @Test
    fun `calculateAllPeriodReturns - mixed stocks and cash - returns weighted returns`() = runTest {
        // Stock worth $1000 (10 qty * $100)
        val stock = Stock(
            id = 1L, symbol = "AAPL", name = "Apple", currentPrice = 100.0, averagePrice = 100.0,
            quantity = 10, currency = Currency.USD, priceHistory = emptyList(),
            priceHistoryTimestamps = emptyList()
        )
        // Cash worth $1000
        val cashItem = CashItem(1, 1L, "Savings", 1000.0, 10.0, Currency.USD, 0L)

        // Stock has 20% return
        coEvery { stockRepository.getPriceHistory(any(), any()) } returns mapOf(
            "AAPL" to PriceHistoryData(
                listOf(100.0, 120.0),
                listOf(1704067200L, 1704153600L)
            )
        )

        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns
            PriceHistoryData(listOf(1400.0, 1400.0), listOf(1704067200L, 1704153600L))

        val result = PeriodReturnsService.calculateAllPeriodReturns(
            stockRepository,
            stocks = listOf(stock),
            cashItems = listOf(cashItem),
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        // Stock = 50% weight with 20% return = 10%
        // Cash = 50% weight with 10% annual return = 5% for 1 year
        // Combined 1-year return = 0.5 * 20 + 0.5 * 10 = 15%
        assertThat(result[TimePeriod.ONE_YEAR]).isWithin(1.0).of(15.0)
    }

    @Test
    fun `calculateAllPeriodReturns - zero value stocks - returns cash only`() = runTest {
        // Stock with zero current price
        val stock = Stock(
            id = 1L, symbol = "AAPL", name = "Apple", currentPrice = 0.0, averagePrice = 100.0,
            quantity = 10, currency = Currency.USD, priceHistory = emptyList(),
            priceHistoryTimestamps = emptyList()
        )
        val cashItem = CashItem(1, 1L, "Savings", 1000.0, 5.0, Currency.USD, 0L)

        // Mock repository calls (stock has no value, so no price history calculation affects result)
        coEvery { stockRepository.getPriceHistory(any(), any()) } returns mapOf(
            "AAPL" to PriceHistoryData(listOf(0.0, 0.0), listOf(1704067200L, 1704153600L))
        )
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns
            PriceHistoryData(listOf(1400.0, 1400.0), listOf(1704067200L, 1704153600L))

        val result = PeriodReturnsService.calculateAllPeriodReturns(
            stockRepository,
            stocks = listOf(stock),
            cashItems = listOf(cashItem),
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        // Should have cash returns only (5% for 1 year)
        // Stock has zero value, so 100% weight goes to cash
        assertThat(result[TimePeriod.ONE_YEAR]).isWithin(0.01).of(5.0)
    }

    @Test
    fun `calculateAllPeriodReturns - no price history - returns zero for stocks`() = runTest {
        val stock = Stock(
            id = 1L, symbol = "AAPL", name = "Apple", currentPrice = 150.0, averagePrice = 100.0,
            quantity = 10, currency = Currency.USD, priceHistory = emptyList(),
            priceHistoryTimestamps = emptyList()
        )

        coEvery { stockRepository.getPriceHistory(any(), any()) } returns emptyMap()
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } returns
            PriceHistoryData(emptyList(), emptyList())

        val result = PeriodReturnsService.calculateAllPeriodReturns(
            stockRepository,
            stocks = listOf(stock),
            cashItems = emptyList(),
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        // All returns should be 0 when no price history
        result.values.forEach { returnPercent ->
            assertThat(returnPercent).isEqualTo(0.0)
        }
    }

    @Test
    fun `calculateAllPeriodReturns - exception during calculation - returns zero for that period`() = runTest {
        val stock = Stock(
            id = 1L, symbol = "AAPL", name = "Apple", currentPrice = 150.0, averagePrice = 100.0,
            quantity = 10, currency = Currency.USD, priceHistory = emptyList(),
            priceHistoryTimestamps = emptyList()
        )

        // Throw exception for all requests
        coEvery { stockRepository.getPriceHistory(any(), any()) } throws RuntimeException("API Error")
        coEvery { stockRepository.getExchangeRateHistory(any(), any(), any()) } throws RuntimeException("API Error")

        val result = PeriodReturnsService.calculateAllPeriodReturns(
            stockRepository,
            stocks = listOf(stock),
            cashItems = emptyList(),
            showInKrw = false,
            currentExchangeRate = 1400.0
        )

        // All returns should be 0 when exceptions occur
        assertThat(result).hasSize(TimePeriod.entries.size)
        result.values.forEach { returnPercent ->
            assertThat(returnPercent).isEqualTo(0.0)
        }
    }
}
