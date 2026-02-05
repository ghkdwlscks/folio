package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.Currency
import com.portfolio.manager.domain.model.Stock
import org.junit.Test

class RebalanceCalculatorTest {

    // --- accountNeedsRebalance ---

    @Test
    fun `accountNeedsRebalance - empty holdings - returns false`() {
        val result = RebalanceCalculator.accountNeedsRebalance(
            holdings = emptyList(),
            stockMap = emptyMap(),
            showInKrw = false,
            exchangeRate = 1400.0
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `accountNeedsRebalance - no target percentages - returns false`() {
        val holdings = listOf(
            createHolding(1, "AAPL", 10, targetPercentage = null)
        )
        val stockMap = mapOf(
            "AAPL" to createStock("AAPL", currentPrice = 150.0)
        )

        val result = RebalanceCalculator.accountNeedsRebalance(
            holdings, stockMap, showInKrw = false, exchangeRate = 1400.0
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `accountNeedsRebalance - portfolio at target - returns false`() {
        // Two stocks, each 50% target, currently equal value
        val holdings = listOf(
            createHolding(1, "AAPL", quantity = 10, targetPercentage = 50),
            createHolding(2, "GOOGL", quantity = 10, targetPercentage = 50)
        )
        val stockMap = mapOf(
            "AAPL" to createStock("AAPL", currentPrice = 100.0),
            "GOOGL" to createStock("GOOGL", currentPrice = 100.0)
        )

        val result = RebalanceCalculator.accountNeedsRebalance(
            holdings, stockMap, showInKrw = false, exchangeRate = 1400.0
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `accountNeedsRebalance - difference less than one share - returns false`() {
        // AAPL: 10 shares * $100 = $1000 (51% target = $1020 target)
        // GOOGL: 10 shares * $100 = $1000 (49% target = $980 target)
        // Total = $2000
        // AAPL diff = $20, price = $100, diff < price -> no rebalance
        val holdings = listOf(
            createHolding(1, "AAPL", quantity = 10, targetPercentage = 51),
            createHolding(2, "GOOGL", quantity = 10, targetPercentage = 49)
        )
        val stockMap = mapOf(
            "AAPL" to createStock("AAPL", currentPrice = 100.0),
            "GOOGL" to createStock("GOOGL", currentPrice = 100.0)
        )

        val result = RebalanceCalculator.accountNeedsRebalance(
            holdings, stockMap, showInKrw = false, exchangeRate = 1400.0
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `accountNeedsRebalance - difference equals one share - returns true`() {
        // AAPL: 5 shares * $100 = $500 (60% target = $600 target)
        // GOOGL: 5 shares * $100 = $500 (40% target = $400 target)
        // Total = $1000
        // AAPL diff = $100, price = $100, diff >= price -> needs rebalance
        val holdings = listOf(
            createHolding(1, "AAPL", quantity = 5, targetPercentage = 60),
            createHolding(2, "GOOGL", quantity = 5, targetPercentage = 40)
        )
        val stockMap = mapOf(
            "AAPL" to createStock("AAPL", currentPrice = 100.0),
            "GOOGL" to createStock("GOOGL", currentPrice = 100.0)
        )

        val result = RebalanceCalculator.accountNeedsRebalance(
            holdings, stockMap, showInKrw = false, exchangeRate = 1400.0
        )

        assertThat(result).isTrue()
    }

    @Test
    fun `accountNeedsRebalance - zero stock price - returns false`() {
        val holdings = listOf(
            createHolding(1, "AAPL", quantity = 10, targetPercentage = 50)
        )
        val stockMap = mapOf(
            "AAPL" to createStock("AAPL", currentPrice = 0.0)
        )

        val result = RebalanceCalculator.accountNeedsRebalance(
            holdings, stockMap, showInKrw = false, exchangeRate = 1400.0
        )

        assertThat(result).isFalse()
    }

    @Test
    fun `accountNeedsRebalance - missing stock in map - handles gracefully`() {
        val holdings = listOf(
            createHolding(1, "AAPL", quantity = 10, targetPercentage = 50),
            createHolding(2, "GOOGL", quantity = 10, targetPercentage = 50)
        )
        // Only AAPL in stock map, GOOGL missing
        val stockMap = mapOf(
            "AAPL" to createStock("AAPL", currentPrice = 100.0)
        )

        val result = RebalanceCalculator.accountNeedsRebalance(
            holdings, stockMap, showInKrw = false, exchangeRate = 1400.0
        )

        // GOOGL contributes 0, so AAPL is 100% of value but target is 50%
        // This causes diff >= price, so rebalance is detected
        assertThat(result).isTrue()
    }

    // --- buildRebalanceItems ---

    @Test
    fun `buildRebalanceItems - empty holdings - returns empty list`() {
        val result = RebalanceCalculator.buildRebalanceItems(
            holdings = emptyList(),
            stocks = emptyList(),
            showInKrw = false,
            exchangeRate = 1400.0
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun `buildRebalanceItems - matching holdings and stocks - returns items`() {
        val holdings = listOf(
            createHolding(1, "AAPL", quantity = 10, targetPercentage = 60),
            createHolding(2, "GOOGL", quantity = 5, targetPercentage = 40)
        )
        val stocks = listOf(
            createStock("AAPL", currentPrice = 150.0, name = "Apple Inc"),
            createStock("GOOGL", currentPrice = 100.0, name = "Alphabet Inc")
        )

        val result = RebalanceCalculator.buildRebalanceItems(
            holdings, stocks, showInKrw = false, exchangeRate = 1400.0
        )

        assertThat(result).hasSize(2)

        val aaplItem = result.find { it.symbol == "AAPL" }!!
        assertThat(aaplItem.holdingId).isEqualTo(1)
        assertThat(aaplItem.name).isEqualTo("Apple Inc")
        assertThat(aaplItem.currentValue).isWithin(0.01).of(1500.0) // 10 * 150
        assertThat(aaplItem.currentPrice).isWithin(0.01).of(150.0)
        assertThat(aaplItem.currentPercentage).isEqualTo(60)
        assertThat(aaplItem.quantity).isEqualTo(10)

        val googlItem = result.find { it.symbol == "GOOGL" }!!
        assertThat(googlItem.holdingId).isEqualTo(2)
        assertThat(googlItem.currentPercentage).isEqualTo(40)
    }

    @Test
    fun `buildRebalanceItems - stock without holding - skipped`() {
        val holdings = listOf(
            createHolding(1, "AAPL", quantity = 10, targetPercentage = 100)
        )
        val stocks = listOf(
            createStock("AAPL", currentPrice = 150.0),
            createStock("GOOGL", currentPrice = 100.0) // No matching holding
        )

        val result = RebalanceCalculator.buildRebalanceItems(
            holdings, stocks, showInKrw = false, exchangeRate = 1400.0
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].symbol).isEqualTo("AAPL")
    }

    @Test
    fun `buildRebalanceItems - no target percentage - uses zero`() {
        val holdings = listOf(
            createHolding(1, "AAPL", quantity = 10, targetPercentage = null)
        )
        val stocks = listOf(
            createStock("AAPL", currentPrice = 150.0)
        )

        val result = RebalanceCalculator.buildRebalanceItems(
            holdings, stocks, showInKrw = false, exchangeRate = 1400.0
        )

        assertThat(result).hasSize(1)
        assertThat(result[0].currentPercentage).isEqualTo(0)
    }

    @Test
    fun `buildRebalanceItems - showInKrw converts values`() {
        val holdings = listOf(
            createHolding(1, "AAPL", quantity = 10, targetPercentage = 100)
        )
        val stocks = listOf(
            createStock("AAPL", currentPrice = 150.0, currency = Currency.USD)
        )

        val result = RebalanceCalculator.buildRebalanceItems(
            holdings, stocks, showInKrw = true, exchangeRate = 1400.0
        )

        assertThat(result).hasSize(1)
        // Value in KRW = 10 * 150 * 1400 = 2,100,000
        assertThat(result[0].currentValue).isWithin(1.0).of(2100000.0)
        // Price in KRW = 150 * 1400 = 210,000
        assertThat(result[0].currentPrice).isWithin(1.0).of(210000.0)
    }

    // Helper functions

    private fun createHolding(
        id: Long,
        symbol: String,
        quantity: Int,
        targetPercentage: Int? = null,
        accountId: Long = 1L,
        averagePrice: Double = 100.0
    ): HoldingEntity = HoldingEntity(
        id = id,
        accountId = accountId,
        symbol = symbol,
        name = symbol,
        quantity = quantity,
        averagePrice = averagePrice,
        currency = "USD",
        targetPercentage = targetPercentage
    )

    private fun createStock(
        symbol: String,
        currentPrice: Double,
        name: String = symbol,
        currency: Currency = Currency.USD
    ): Stock = Stock(
        id = 1L,
        symbol = symbol,
        name = name,
        currentPrice = currentPrice,
        averagePrice = 100.0,
        quantity = 10,
        currency = currency,
        priceHistory = emptyList(),
        priceHistoryTimestamps = emptyList()
    )
}
