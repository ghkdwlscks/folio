package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.PriceHistoryData
import org.junit.Test

class PortfolioCalculationServiceTest {

    // --- calculatePeriodReturn ---

    @Test
    fun `calculatePeriodReturn - empty list - returns zero`() {
        assertThat(PortfolioCalculationService.calculatePeriodReturn(emptyList())).isEqualTo(0.0)
    }

    @Test
    fun `calculatePeriodReturn - single value - returns zero`() {
        assertThat(PortfolioCalculationService.calculatePeriodReturn(listOf(100.0))).isEqualTo(0.0)
    }

    @Test
    fun `calculatePeriodReturn - positive return`() {
        val result = PortfolioCalculationService.calculatePeriodReturn(listOf(100.0, 110.0))
        assertThat(result).isWithin(0.01).of(10.0)
    }

    @Test
    fun `calculatePeriodReturn - negative return`() {
        val result = PortfolioCalculationService.calculatePeriodReturn(listOf(100.0, 90.0))
        assertThat(result).isWithin(0.01).of(-10.0)
    }

    @Test
    fun `calculatePeriodReturn - zero start value - returns zero`() {
        assertThat(PortfolioCalculationService.calculatePeriodReturn(listOf(0.0, 100.0))).isEqualTo(0.0)
    }

    @Test
    fun `calculatePeriodReturn - multiple values uses first and last`() {
        val result = PortfolioCalculationService.calculatePeriodReturn(listOf(100.0, 200.0, 150.0))
        assertThat(result).isWithin(0.01).of(50.0)
    }

    // --- calculateWeightedCashReturn ---

    @Test
    fun `calculateWeightedCashReturn - empty list - returns zero`() {
        assertThat(PortfolioCalculationService.calculateWeightedCashReturn(emptyList(), 1400.0, TimePeriod.ONE_YEAR)).isEqualTo(0.0)
    }

    @Test
    fun `calculateWeightedCashReturn - single USD cash item`() {
        val cashItem = CashItem(1, 1L, "Savings", 10000.0, 5.0, "USD", 0L)
        val result = PortfolioCalculationService.calculateWeightedCashReturn(listOf(cashItem), 1400.0, TimePeriod.ONE_YEAR)
        // One year return for 5% annual yield = 5.0%
        assertThat(result).isWithin(0.01).of(5.0)
    }

    @Test
    fun `calculateWeightedCashReturn - weighted average of multiple items`() {
        val cash1 = CashItem(1, 1L, "High Yield", 10000.0, 5.0, "USD", 0L)
        val cash2 = CashItem(2, 1L, "Low Yield", 10000.0, 3.0, "USD", 0L)
        val result = PortfolioCalculationService.calculateWeightedCashReturn(listOf(cash1, cash2), 1400.0, TimePeriod.ONE_YEAR)
        // Equal weight: (0.5 * 5.0) + (0.5 * 3.0) = 4.0%
        assertThat(result).isWithin(0.01).of(4.0)
    }

    @Test
    fun `calculateWeightedCashReturn - zero value cash - returns zero`() {
        val cashItem = CashItem(1, 1L, "Empty", 0.0, 5.0, "USD", 0L)
        assertThat(PortfolioCalculationService.calculateWeightedCashReturn(listOf(cashItem), 1400.0, TimePeriod.ONE_YEAR)).isEqualTo(0.0)
    }

    // --- buildPortfolioValues ---

    @Test
    fun `buildPortfolioValues - empty stocks - returns null`() {
        val result = PortfolioCalculationService.buildPortfolioValues(
            stocks = emptyList(),
            priceHistoryMap = emptyMap(),
            exchangeRateData = PriceHistoryData(emptyList(), emptyList()),
            exchangeRate = 1400.0,
            showInKrw = false
        )
        assertThat(result).isNull()
    }

    @Test
    fun `buildPortfolioValues - no price history - returns null`() {
        val stock = Stock(
            id = 1L, symbol = "AAPL", name = "Apple", currentPrice = 150.0, averagePrice = 100.0,
            quantity = 10, currency = "USD", priceHistory = emptyList(),
            priceHistoryTimestamps = emptyList()
        )
        val result = PortfolioCalculationService.buildPortfolioValues(
            stocks = listOf(stock),
            priceHistoryMap = emptyMap(),
            exchangeRateData = PriceHistoryData(emptyList(), emptyList()),
            exchangeRate = 1400.0,
            showInKrw = false
        )
        assertThat(result).isNull()
    }

    @Test
    fun `buildPortfolioValues - insufficient history - returns null`() {
        val stock = Stock(
            id = 1L, symbol = "AAPL", name = "Apple", currentPrice = 150.0, averagePrice = 100.0,
            quantity = 10, currency = "USD", priceHistory = listOf(100.0),
            priceHistoryTimestamps = emptyList()
        )
        val result = PortfolioCalculationService.buildPortfolioValues(
            stocks = listOf(stock),
            priceHistoryMap = mapOf("AAPL" to PriceHistoryData(listOf(100.0), listOf(1000L))),
            exchangeRateData = PriceHistoryData(emptyList(), emptyList()),
            exchangeRate = 1400.0,
            showInKrw = false
        )
        assertThat(result).isNull()
    }

    @Test
    fun `buildPortfolioValues - valid data - returns values and dates`() {
        val stock = Stock(
            id = 1L, symbol = "AAPL", name = "Apple", currentPrice = 150.0, averagePrice = 100.0,
            quantity = 10, currency = "USD", priceHistory = listOf(100.0, 110.0, 120.0),
            priceHistoryTimestamps = emptyList()
        )
        val priceHistory = PriceHistoryData(
            prices = listOf(100.0, 110.0, 120.0),
            timestamps = listOf(1704067200L, 1704153600L, 1704240000L) // 2024-01-01, 02, 03
        )
        val result = PortfolioCalculationService.buildPortfolioValues(
            stocks = listOf(stock),
            priceHistoryMap = mapOf("AAPL" to priceHistory),
            exchangeRateData = PriceHistoryData(emptyList(), emptyList()),
            exchangeRate = 1400.0,
            showInKrw = false
        )
        assertThat(result).isNotNull()
        assertThat(result!!.values).hasSize(3)
        assertThat(result.validDates).hasSize(3)
        // 10 shares * $100, $110, $120
        assertThat(result.values[0]).isWithin(0.01).of(1000.0)
        assertThat(result.values[1]).isWithin(0.01).of(1100.0)
        assertThat(result.values[2]).isWithin(0.01).of(1200.0)
    }
}
