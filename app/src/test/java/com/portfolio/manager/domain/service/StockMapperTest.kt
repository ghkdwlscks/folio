package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.data.remote.dto.QuoteResult
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.repository.PriceHistoryData
import org.junit.Test

class StockMapperTest {

    private fun createHolding(
        id: Long = 1,
        accountId: Long = 1,
        symbol: String = "AAPL",
        quantity: Int = 10,
        averagePrice: Double = 100.0,
        currency: String = "USD",
        targetPercentage: Int? = null
    ) = HoldingEntity(
        id = id,
        accountId = accountId,
        symbol = symbol,
        name = symbol,
        quantity = quantity,
        averagePrice = averagePrice,
        currency = currency,
        targetPercentage = targetPercentage
    )

    private fun createQuote(
        symbol: String = "AAPL",
        longName: String? = "Apple Inc.",
        shortName: String? = "Apple",
        regularMarketPrice: Double = 150.0,
        regularMarketChange: Double = 5.0,
        regularMarketChangePercent: Double = 3.45,
        trailingAnnualDividendRate: Double? = 0.96,
        trailingAnnualDividendYield: Double? = 0.0064
    ) = QuoteResult(
        symbol = symbol,
        longName = longName,
        shortName = shortName,
        regularMarketPrice = regularMarketPrice,
        regularMarketChange = regularMarketChange,
        regularMarketChangePercent = regularMarketChangePercent,
        trailingAnnualDividendRate = trailingAnnualDividendRate,
        trailingAnnualDividendYield = trailingAnnualDividendYield
    )

    private fun createAccount(
        id: Long = 1,
        name: String = "Account",
        orderIndex: Int = 0
    ) = AccountEntity(
        id = id,
        name = name,
        orderIndex = orderIndex
    )

    private fun createPriceHistoryData(
        prices: List<Double> = listOf(100.0, 110.0, 120.0),
        timestamps: List<Long> = listOf(1L, 2L, 3L)
    ) = PriceHistoryData(prices, timestamps)

    // createStock tests

    @Test
    fun `createStock - with quote - uses quote data`() {
        val holding = createHolding()
        val quote = createQuote()

        val stock = StockMapper.createStock(holding, quote, null)

        assertThat(stock.id).isEqualTo(1)
        assertThat(stock.symbol).isEqualTo("AAPL")
        assertThat(stock.name).isEqualTo("Apple Inc.")
        assertThat(stock.quantity).isEqualTo(10)
        assertThat(stock.averagePrice).isEqualTo(100.0)
        assertThat(stock.currentPrice).isEqualTo(150.0)
        assertThat(stock.dayChange).isEqualTo(5.0)
        assertThat(stock.dayChangePercent).isEqualTo(3.45)
        assertThat(stock.annualDividend).isEqualTo(0.96)
        assertThat(stock.dividendYield).isEqualTo(0.64) // 0.0064 * 100
    }

    @Test
    fun `createStock - without quote - uses holding data as fallback`() {
        val holding = createHolding()

        val stock = StockMapper.createStock(holding, null, null)

        assertThat(stock.name).isEqualTo("AAPL")
        assertThat(stock.currentPrice).isEqualTo(100.0) // averagePrice as fallback
        assertThat(stock.dayChange).isNull()
        assertThat(stock.dayChangePercent).isNull()
    }

    @Test
    fun `createStock - quote with only shortName - uses shortName`() {
        val holding = createHolding()
        val quote = createQuote(longName = null, shortName = "Apple")

        val stock = StockMapper.createStock(holding, quote, null)

        assertThat(stock.name).isEqualTo("Apple")
    }

    @Test
    fun `createStock - quote with no names - uses symbol`() {
        val holding = createHolding()
        val quote = createQuote(longName = null, shortName = null)

        val stock = StockMapper.createStock(holding, quote, null)

        assertThat(stock.name).isEqualTo("AAPL")
    }

    @Test
    fun `createStock - with price history - includes history data`() {
        val holding = createHolding()
        val quote = createQuote()
        val priceHistory = createPriceHistoryData()

        val stock = StockMapper.createStock(holding, quote, priceHistory)

        assertThat(stock.priceHistory).containsExactly(100.0, 110.0, 120.0)
        assertThat(stock.priceHistoryTimestamps).containsExactly(1L, 2L, 3L)
    }

    @Test
    fun `createStock - preserves target percentage`() {
        val holding = createHolding(targetPercentage = 25)
        val quote = createQuote()

        val stock = StockMapper.createStock(holding, quote, null)

        assertThat(stock.targetPercentage).isEqualTo(25)
    }

    // aggregateHoldings tests

    @Test
    fun `aggregateHoldings - calculates weighted average price`() {
        val holdings = listOf(
            createHolding(id = 1, accountId = 1, quantity = 10, averagePrice = 100.0),
            createHolding(id = 2, accountId = 2, quantity = 20, averagePrice = 130.0)
        )
        val accounts = listOf(
            createAccount(id = 1, name = "Account 1"),
            createAccount(id = 2, name = "Account 2")
        )
        val quote = createQuote()

        val stock = StockMapper.aggregateHoldings(holdings, quote, accounts, null)

        assertThat(stock.quantity).isEqualTo(30) // 10 + 20
        // Weighted avg: (10 * 100 + 20 * 130) / 30 = 3600 / 30 = 120
        assertThat(stock.averagePrice).isEqualTo(120.0)
    }

    @Test
    fun `aggregateHoldings - creates account details sorted by orderIndex`() {
        val holdings = listOf(
            createHolding(id = 1, accountId = 2, quantity = 10),
            createHolding(id = 2, accountId = 1, quantity = 20)
        )
        val accounts = listOf(
            createAccount(id = 1, name = "First", orderIndex = 0),
            createAccount(id = 2, name = "Second", orderIndex = 1)
        )
        val quote = createQuote()

        val stock = StockMapper.aggregateHoldings(holdings, quote, accounts, null)

        assertThat(stock.accountDetails).hasSize(2)
        assertThat(stock.accountDetails[0].accountName).isEqualTo("First")
        assertThat(stock.accountDetails[1].accountName).isEqualTo("Second")
    }

    @Test
    fun `aggregateHoldings - handles unknown account`() {
        val holdings = listOf(createHolding(accountId = 999))
        val accounts = emptyList<AccountEntity>()
        val quote = createQuote()

        val stock = StockMapper.aggregateHoldings(holdings, quote, accounts, null)

        assertThat(stock.accountDetails[0].accountName).isEqualTo("Unknown")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `aggregateHoldings - throws on empty holding group`() {
        StockMapper.aggregateHoldings(emptyList(), createQuote(), emptyList(), null)
    }

    // mergeWithExisting tests

    @Test
    fun `mergeWithExisting - with existing stock - uses existing price data`() {
        val holding = createHolding(quantity = 15)
        val existingStock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 90.0,
            currentPrice = 160.0,
            dayChange = 3.0,
            dayChangePercent = 1.9,
            priceHistory = listOf(155.0, 158.0, 160.0),
            priceHistoryTimestamps = listOf(1L, 2L, 3L),
            annualDividend = 0.96,
            dividendYield = 0.6
        )

        val stock = StockMapper.mergeWithExisting(holding, existingStock)

        assertThat(stock.quantity).isEqualTo(15) // from holding
        assertThat(stock.averagePrice).isEqualTo(100.0) // from holding
        assertThat(stock.currentPrice).isEqualTo(160.0) // from existing
        assertThat(stock.name).isEqualTo("Apple Inc.") // from existing
        assertThat(stock.dayChange).isEqualTo(3.0) // from existing
        assertThat(stock.priceHistory).containsExactly(155.0, 158.0, 160.0)
    }

    @Test
    fun `mergeWithExisting - without existing stock - uses holding data`() {
        val holding = createHolding(targetPercentage = 30)

        val stock = StockMapper.mergeWithExisting(holding, null)

        assertThat(stock.name).isEqualTo("AAPL")
        assertThat(stock.currentPrice).isEqualTo(100.0)
        assertThat(stock.dayChange).isNull()
        assertThat(stock.priceHistory).isEmpty()
        assertThat(stock.targetPercentage).isEqualTo(30)
    }

    // mergeAggregatedWithExisting tests

    @Test
    fun `mergeAggregatedWithExisting - aggregates and preserves existing price`() {
        val holdings = listOf(
            createHolding(id = 1, accountId = 1, quantity = 10, averagePrice = 100.0),
            createHolding(id = 2, accountId = 2, quantity = 10, averagePrice = 120.0)
        )
        val accounts = listOf(
            createAccount(id = 1, name = "Account 1"),
            createAccount(id = 2, name = "Account 2")
        )
        val existingStock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 20,
            averagePrice = 110.0,
            currentPrice = 150.0,
            dayChange = 2.0,
            dayChangePercent = 1.35
        )

        val stock = StockMapper.mergeAggregatedWithExisting(holdings, existingStock, accounts)

        assertThat(stock.quantity).isEqualTo(20)
        assertThat(stock.averagePrice).isEqualTo(110.0) // weighted avg
        assertThat(stock.currentPrice).isEqualTo(150.0) // from existing
        assertThat(stock.name).isEqualTo("Apple Inc.") // from existing
        assertThat(stock.accountDetails).hasSize(2)
    }

    @Test
    fun `mergeAggregatedWithExisting - without existing stock`() {
        val holdings = listOf(createHolding(quantity = 10))
        val accounts = listOf(createAccount())

        val stock = StockMapper.mergeAggregatedWithExisting(holdings, null, accounts)

        assertThat(stock.name).isEqualTo("AAPL")
        assertThat(stock.currentPrice).isEqualTo(100.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `mergeAggregatedWithExisting - throws on empty holding group`() {
        StockMapper.mergeAggregatedWithExisting(emptyList(), null, emptyList())
    }

    @Test
    fun `aggregateHoldings - zero total quantity - returns zero weighted avg`() {
        val holdings = listOf(
            createHolding(id = 1, quantity = 0, averagePrice = 100.0)
        )
        val accounts = listOf(createAccount())
        val quote = createQuote()

        val stock = StockMapper.aggregateHoldings(holdings, quote, accounts, null)

        assertThat(stock.quantity).isEqualTo(0)
        assertThat(stock.averagePrice).isEqualTo(0.0)
    }
}
