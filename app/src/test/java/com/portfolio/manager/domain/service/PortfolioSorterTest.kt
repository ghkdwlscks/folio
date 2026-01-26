package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.SortOption
import com.portfolio.manager.domain.model.Stock
import org.junit.Test

class PortfolioSorterTest {

    private val exchangeRate = 1400.0

    private var stockIdCounter = 1L

    private fun createStock(
        symbol: String,
        name: String,
        quantity: Int = 10,
        averagePrice: Double = 100.0,
        currentPrice: Double = 100.0,
        currency: String = "USD",
        dayChangePercent: Double? = null
    ) = Stock(
        id = stockIdCounter++,
        symbol = symbol,
        name = name,
        quantity = quantity,
        averagePrice = averagePrice,
        currentPrice = currentPrice,
        currency = currency,
        dayChangePercent = dayChangePercent
    )

    private fun createCashItem(
        id: Long,
        name: String,
        originalValue: Double,
        annualYieldRate: Double,
        currency: String = "USD"
    ) = CashItem(
        id = id,
        accountId = 1L,
        name = name,
        originalValue = originalValue,
        annualYieldRate = annualYieldRate,
        currency = currency,
        createdAt = 0L
    )

    // Stock sorting tests

    @Test
    fun `sortStocks - by WEIGHT - sorts by total value descending`() {
        val stocks = listOf(
            createStock("AAPL", "Apple", quantity = 10, currentPrice = 100.0),  // $1000
            createStock("GOOGL", "Google", quantity = 5, currentPrice = 300.0), // $1500
            createStock("MSFT", "Microsoft", quantity = 20, currentPrice = 50.0) // $1000
        )

        val sorted = PortfolioSorter.sortStocks(stocks, SortOption.WEIGHT, exchangeRate)

        assertThat(sorted[0].symbol).isEqualTo("GOOGL")
        assertThat(sorted[1].symbol).isIn(listOf("AAPL", "MSFT"))
    }

    @Test
    fun `sortStocks - by NAME - sorts alphabetically ascending`() {
        val stocks = listOf(
            createStock("MSFT", "Microsoft"),
            createStock("AAPL", "Apple"),
            createStock("GOOGL", "Google")
        )

        val sorted = PortfolioSorter.sortStocks(stocks, SortOption.NAME, exchangeRate)

        assertThat(sorted[0].name).isEqualTo("Apple")
        assertThat(sorted[1].name).isEqualTo("Google")
        assertThat(sorted[2].name).isEqualTo("Microsoft")
    }

    @Test
    fun `sortStocks - by SYMBOL - sorts alphabetically ascending`() {
        val stocks = listOf(
            createStock("MSFT", "Microsoft"),
            createStock("AAPL", "Apple"),
            createStock("GOOGL", "Google")
        )

        val sorted = PortfolioSorter.sortStocks(stocks, SortOption.SYMBOL, exchangeRate)

        assertThat(sorted[0].symbol).isEqualTo("AAPL")
        assertThat(sorted[1].symbol).isEqualTo("GOOGL")
        assertThat(sorted[2].symbol).isEqualTo("MSFT")
    }

    @Test
    fun `sortStocks - by GAIN_LOSS_PERCENT - sorts by gain loss percent descending`() {
        val stocks = listOf(
            createStock("AAPL", "Apple", averagePrice = 100.0, currentPrice = 110.0),   // 10%
            createStock("GOOGL", "Google", averagePrice = 100.0, currentPrice = 80.0),  // -20%
            createStock("MSFT", "Microsoft", averagePrice = 100.0, currentPrice = 150.0) // 50%
        )

        val sorted = PortfolioSorter.sortStocks(stocks, SortOption.GAIN_LOSS_PERCENT, exchangeRate)

        assertThat(sorted[0].symbol).isEqualTo("MSFT")
        assertThat(sorted[1].symbol).isEqualTo("AAPL")
        assertThat(sorted[2].symbol).isEqualTo("GOOGL")
    }

    @Test
    fun `sortStocks - by DAY_CHANGE_PERCENT - sorts by day change percent descending`() {
        val stocks = listOf(
            createStock("AAPL", "Apple", dayChangePercent = 2.0),
            createStock("GOOGL", "Google", dayChangePercent = -1.0),
            createStock("MSFT", "Microsoft", dayChangePercent = 5.0)
        )

        val sorted = PortfolioSorter.sortStocks(stocks, SortOption.DAY_CHANGE_PERCENT, exchangeRate)

        assertThat(sorted[0].symbol).isEqualTo("MSFT")
        assertThat(sorted[1].symbol).isEqualTo("AAPL")
        assertThat(sorted[2].symbol).isEqualTo("GOOGL")
    }

    @Test
    fun `sortStocks - by DAY_CHANGE_PERCENT - handles null day change`() {
        val stocks = listOf(
            createStock("AAPL", "Apple", dayChangePercent = 2.0),
            createStock("GOOGL", "Google", dayChangePercent = null),
            createStock("MSFT", "Microsoft", dayChangePercent = -1.0)
        )

        val sorted = PortfolioSorter.sortStocks(stocks, SortOption.DAY_CHANGE_PERCENT, exchangeRate)

        assertThat(sorted[0].symbol).isEqualTo("AAPL")
        assertThat(sorted[1].symbol).isEqualTo("GOOGL") // null treated as 0.0
        assertThat(sorted[2].symbol).isEqualTo("MSFT")
    }

    // Cash item sorting tests

    @Test
    fun `sortCashItems - by WEIGHT - sorts by value descending`() {
        val cashItems = listOf(
            createCashItem(1, "Small Fund", 1000.0, 3.0),
            createCashItem(2, "Big Fund", 5000.0, 2.0),
            createCashItem(3, "Medium Fund", 3000.0, 4.0)
        )

        val sorted = PortfolioSorter.sortCashItems(cashItems, SortOption.WEIGHT, exchangeRate)

        assertThat(sorted[0].name).isEqualTo("Big Fund")
        assertThat(sorted[1].name).isEqualTo("Medium Fund")
        assertThat(sorted[2].name).isEqualTo("Small Fund")
    }

    @Test
    fun `sortCashItems - by NAME - sorts alphabetically ascending`() {
        val cashItems = listOf(
            createCashItem(1, "Emergency Fund", 1000.0, 3.0),
            createCashItem(2, "CD", 5000.0, 2.0),
            createCashItem(3, "Savings", 3000.0, 4.0)
        )

        val sorted = PortfolioSorter.sortCashItems(cashItems, SortOption.NAME, exchangeRate)

        assertThat(sorted[0].name).isEqualTo("CD")
        assertThat(sorted[1].name).isEqualTo("Emergency Fund")
        assertThat(sorted[2].name).isEqualTo("Savings")
    }

    @Test
    fun `sortCashItems - by SYMBOL - sorts by name ascending (same as NAME)`() {
        val cashItems = listOf(
            createCashItem(1, "Emergency Fund", 1000.0, 3.0),
            createCashItem(2, "CD", 5000.0, 2.0),
            createCashItem(3, "Savings", 3000.0, 4.0)
        )

        val sorted = PortfolioSorter.sortCashItems(cashItems, SortOption.SYMBOL, exchangeRate)

        assertThat(sorted[0].name).isEqualTo("CD")
        assertThat(sorted[1].name).isEqualTo("Emergency Fund")
        assertThat(sorted[2].name).isEqualTo("Savings")
    }

    @Test
    fun `sortCashItems - by GAIN_LOSS_PERCENT - sorts by yield rate descending`() {
        val cashItems = listOf(
            createCashItem(1, "Low Yield", 1000.0, 2.0),
            createCashItem(2, "High Yield", 5000.0, 5.0),
            createCashItem(3, "Medium Yield", 3000.0, 3.5)
        )

        val sorted = PortfolioSorter.sortCashItems(cashItems, SortOption.GAIN_LOSS_PERCENT, exchangeRate)

        assertThat(sorted[0].name).isEqualTo("High Yield")
        assertThat(sorted[1].name).isEqualTo("Medium Yield")
        assertThat(sorted[2].name).isEqualTo("Low Yield")
    }

    @Test
    fun `sortCashItems - by DAY_CHANGE_PERCENT - sorts by yield rate descending`() {
        val cashItems = listOf(
            createCashItem(1, "Low Yield", 1000.0, 2.0),
            createCashItem(2, "High Yield", 5000.0, 5.0),
            createCashItem(3, "Medium Yield", 3000.0, 3.5)
        )

        val sorted = PortfolioSorter.sortCashItems(cashItems, SortOption.DAY_CHANGE_PERCENT, exchangeRate)

        assertThat(sorted[0].name).isEqualTo("High Yield")
        assertThat(sorted[1].name).isEqualTo("Medium Yield")
        assertThat(sorted[2].name).isEqualTo("Low Yield")
    }

    @Test
    fun `sortCashItems - by WEIGHT with KRW currency - converts correctly`() {
        val cashItems = listOf(
            createCashItem(1, "USD Fund", 1000.0, 3.0, "USD"),      // $1000
            createCashItem(2, "KRW Fund", 2800000.0, 2.0, "KRW")   // ~$2000
        )

        val sorted = PortfolioSorter.sortCashItems(cashItems, SortOption.WEIGHT, exchangeRate)

        assertThat(sorted[0].name).isEqualTo("KRW Fund")
        assertThat(sorted[1].name).isEqualTo("USD Fund")
    }

    @Test
    fun `sortStocks - empty list - returns empty list`() {
        val sorted = PortfolioSorter.sortStocks(emptyList(), SortOption.WEIGHT, exchangeRate)
        assertThat(sorted).isEmpty()
    }

    @Test
    fun `sortCashItems - empty list - returns empty list`() {
        val sorted = PortfolioSorter.sortCashItems(emptyList(), SortOption.WEIGHT, exchangeRate)
        assertThat(sorted).isEmpty()
    }

    @Test
    fun `sortStocks - single item - returns same item`() {
        val stocks = listOf(createStock("AAPL", "Apple"))
        val sorted = PortfolioSorter.sortStocks(stocks, SortOption.NAME, exchangeRate)
        assertThat(sorted).hasSize(1)
        assertThat(sorted[0].symbol).isEqualTo("AAPL")
    }

    @Test
    fun `sortCashItems - single item - returns same item`() {
        val cashItems = listOf(createCashItem(1, "Fund", 1000.0, 3.0))
        val sorted = PortfolioSorter.sortCashItems(cashItems, SortOption.NAME, exchangeRate)
        assertThat(sorted).hasSize(1)
        assertThat(sorted[0].name).isEqualTo("Fund")
    }
}
