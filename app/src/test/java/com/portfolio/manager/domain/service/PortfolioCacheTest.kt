package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.Stock
import org.junit.Test

class PortfolioCacheTest {

    private fun createStock(symbol: String, currentPrice: Double) = Stock(
        id = 1L,
        symbol = symbol,
        name = "Test",
        quantity = 10,
        averagePrice = 100.0,
        currentPrice = currentPrice,
        dayChange = 0.0,
        dayChangePercent = 0.0,
        currency = "USD",
        priceHistory = emptyList(),
        priceHistoryTimestamps = emptyList(),
        accountDetails = emptyList(),
        annualDividend = 0.0,
        dividendYield = 0.0,
        targetPercentage = null
    )

    private fun createCashItem(name: String, value: Double) = CashItem(
        id = 1L,
        accountId = 1L,
        name = name,
        originalValue = value,
        annualYieldRate = 5.0,
        currency = "USD",
        createdAt = 0L
    )

    @Test
    fun `initial state - empty`() {
        val cache = PortfolioCache()

        assertThat(cache.stocks).isEmpty()
        assertThat(cache.cashItems).isEmpty()
        assertThat(cache.exchangeRate).isEqualTo(1400.0)
        assertThat(cache.isInitialized).isFalse()
        assertThat(cache.hasData()).isFalse()
    }

    @Test
    fun `update - sets stocks, cash items, and exchange rate`() {
        val cache = PortfolioCache()
        val stocks = listOf(createStock("AAPL", 150.0))
        val cashItems = listOf(createCashItem("Savings", 1000.0))

        cache.update(stocks, cashItems, 1350.0)

        assertThat(cache.stocks).hasSize(1)
        assertThat(cache.stocks[0].symbol).isEqualTo("AAPL")
        assertThat(cache.cashItems).hasSize(1)
        assertThat(cache.cashItems[0].name).isEqualTo("Savings")
        assertThat(cache.exchangeRate).isEqualTo(1350.0)
    }

    @Test
    fun `hasData - returns true when stocks exist`() {
        val cache = PortfolioCache()
        val stocks = listOf(createStock("AAPL", 150.0))

        cache.update(stocks, emptyList(), 1400.0)

        assertThat(cache.hasData()).isTrue()
        assertThat(cache.isInitialized).isTrue()
    }

    @Test
    fun `hasData - returns true when cash items exist`() {
        val cache = PortfolioCache()
        val cashItems = listOf(createCashItem("Savings", 1000.0))

        cache.update(emptyList(), cashItems, 1400.0)

        assertThat(cache.hasData()).isTrue()
        assertThat(cache.isInitialized).isTrue()
    }

    @Test
    fun `hasData - returns true after update even when both empty`() {
        val cache = PortfolioCache()

        cache.update(emptyList(), emptyList(), 1400.0)

        // hasData returns true after initialization even if lists are empty
        assertThat(cache.hasData()).isTrue()
        assertThat(cache.isInitialized).isTrue()
    }

    @Test
    fun `update - replaces previous data`() {
        val cache = PortfolioCache()

        // First update
        cache.update(
            listOf(createStock("AAPL", 100.0)),
            listOf(createCashItem("Fund1", 500.0)),
            1400.0
        )

        // Second update with different data
        cache.update(
            listOf(createStock("GOOG", 200.0), createStock("MSFT", 300.0)),
            listOf(createCashItem("Fund2", 1000.0)),
            1350.0
        )

        assertThat(cache.stocks).hasSize(2)
        assertThat(cache.stocks.map { it.symbol }).containsExactly("GOOG", "MSFT")
        assertThat(cache.cashItems).hasSize(1)
        assertThat(cache.cashItems[0].name).isEqualTo("Fund2")
        assertThat(cache.exchangeRate).isEqualTo(1350.0)
    }
}
