package com.portfolio.manager.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StockTest {

    @Test
    fun `totalValue - calculates quantity times currentPrice`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0
        )

        assertThat(stock.totalValue).isEqualTo(1750.0)
    }

    @Test
    fun `totalCost - calculates quantity times averagePrice`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0
        )

        assertThat(stock.totalCost).isEqualTo(1500.0)
    }

    @Test
    fun `gainLoss - returns positive when price increased`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0
        )

        assertThat(stock.gainLoss).isEqualTo(250.0)
    }

    @Test
    fun `gainLoss - returns negative when price decreased`() {
        val stock = Stock(
            id = 1,
            symbol = "TSLA",
            name = "Tesla Inc.",
            quantity = 5,
            averagePrice = 250.0,
            currentPrice = 200.0
        )

        assertThat(stock.gainLoss).isEqualTo(-250.0)
    }

    @Test
    fun `gainLossPercent - calculates percentage gain correctly`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 100.0,
            currentPrice = 125.0
        )

        assertThat(stock.gainLossPercent).isEqualTo(25.0)
    }

    @Test
    fun `gainLossPercent - calculates percentage loss correctly`() {
        val stock = Stock(
            id = 1,
            symbol = "TSLA",
            name = "Tesla Inc.",
            quantity = 5,
            averagePrice = 200.0,
            currentPrice = 150.0
        )

        assertThat(stock.gainLossPercent).isEqualTo(-25.0)
    }

    @Test
    fun `gainLossPercent - returns zero when averagePrice is zero`() {
        val stock = Stock(
            id = 1,
            symbol = "FREE",
            name = "Free Stock",
            quantity = 10,
            averagePrice = 0.0,
            currentPrice = 50.0
        )

        assertThat(stock.gainLossPercent).isEqualTo(0.0)
    }

    @Test
    fun `totalValue - returns zero when quantity is zero`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 0,
            averagePrice = 150.0,
            currentPrice = 175.0
        )

        assertThat(stock.totalValue).isEqualTo(0.0)
    }

    @Test
    fun `dayChange - defaults to null`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0
        )

        assertThat(stock.dayChange).isNull()
        assertThat(stock.dayChangePercent).isNull()
    }

    @Test
    fun `dayChange - stores provided values`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0,
            dayChange = 3.50,
            dayChangePercent = 2.04
        )

        assertThat(stock.dayChange).isEqualTo(3.50)
        assertThat(stock.dayChangePercent).isEqualTo(2.04)
    }

    @Test
    fun `currency - defaults to USD`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0
        )

        assertThat(stock.currency).isEqualTo("USD")
    }

    @Test
    fun `currency - stores provided value`() {
        val stock = Stock(
            id = 1,
            symbol = "005930.KS",
            name = "Samsung Electronics",
            quantity = 50,
            averagePrice = 72000.0,
            currentPrice = 78500.0,
            currency = "KRW"
        )

        assertThat(stock.currency).isEqualTo("KRW")
    }

    @Test
    fun `accountDetails - defaults to empty list`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0
        )

        assertThat(stock.accountDetails).isEmpty()
    }

    @Test
    fun `accountDetails - stores provided values`() {
        val accountDetails = listOf(
            StockAccountDetail(
                holdingId = 1L,
                accountId = 1L,
                accountName = "Default",
                quantity = 10,
                averagePrice = 150.0
            ),
            StockAccountDetail(
                holdingId = 2L,
                accountId = 2L,
                accountName = "Trading",
                quantity = 5,
                averagePrice = 160.0
            )
        )

        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 15,
            averagePrice = 153.33,
            currentPrice = 175.0,
            accountDetails = accountDetails
        )

        assertThat(stock.accountDetails).hasSize(2)
        assertThat(stock.accountDetails[0].accountName).isEqualTo("Default")
        assertThat(stock.accountDetails[0].quantity).isEqualTo(10)
        assertThat(stock.accountDetails[1].accountName).isEqualTo("Trading")
        assertThat(stock.accountDetails[1].quantity).isEqualTo(5)
    }

    @Test
    fun `totalValueInUsd - USD stock returns same value`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0,
            currency = "USD"
        )

        assertThat(stock.totalValueInUsd()).isEqualTo(1750.0)
    }

    @Test
    fun `totalValueInUsd - KRW stock converts to USD`() {
        val stock = Stock(
            id = 1,
            symbol = "005930.KS",
            name = "Samsung Electronics",
            quantity = 10,
            averagePrice = 72000.0,
            currentPrice = 78000.0,
            currency = "KRW"
        )

        // 780,000 KRW / 1400 = 557.14 USD (approximately)
        assertThat(stock.totalValueInUsd()).isWithin(0.01).of(557.14)
    }

    @Test
    fun `totalValueInUsd - uses custom exchange rate`() {
        val stock = Stock(
            id = 1,
            symbol = "005930.KS",
            name = "Samsung Electronics",
            quantity = 10,
            averagePrice = 72000.0,
            currentPrice = 78000.0,
            currency = "KRW"
        )

        // 780,000 KRW / 1300 = 600.0 USD
        assertThat(stock.totalValueInUsd(1300.0)).isEqualTo(600.0)
    }

    @Test
    fun `totalCostInUsd - USD stock returns same value`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0,
            currency = "USD"
        )

        assertThat(stock.totalCostInUsd()).isEqualTo(1500.0)
    }

    @Test
    fun `totalCostInUsd - KRW stock converts to USD`() {
        val stock = Stock(
            id = 1,
            symbol = "005930.KS",
            name = "Samsung Electronics",
            quantity = 10,
            averagePrice = 70000.0,
            currentPrice = 78000.0,
            currency = "KRW"
        )

        // 700,000 KRW / 1400 = 500.0 USD
        assertThat(stock.totalCostInUsd()).isWithin(0.01).of(500.0)
    }

    @Test
    fun `totalCostInUsd - uses custom exchange rate`() {
        val stock = Stock(
            id = 1,
            symbol = "005930.KS",
            name = "Samsung Electronics",
            quantity = 10,
            averagePrice = 70000.0,
            currentPrice = 78000.0,
            currency = "KRW"
        )

        // 700,000 KRW / 1400 = 500.0 USD with default, but with 1000 rate: 700.0 USD
        assertThat(stock.totalCostInUsd(1000.0)).isEqualTo(700.0)
    }

    @Test
    fun `totalValueInKrw - KRW stock returns same value`() {
        val stock = Stock(
            id = 1,
            symbol = "005930.KS",
            name = "Samsung Electronics",
            quantity = 10,
            averagePrice = 70000.0,
            currentPrice = 78000.0,
            currency = "KRW"
        )

        assertThat(stock.totalValueInKrw()).isEqualTo(780000.0)
    }

    @Test
    fun `totalValueInKrw - USD stock converts to KRW`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0,
            currency = "USD"
        )

        // 1750 USD * 1400 = 2,450,000 KRW
        assertThat(stock.totalValueInKrw()).isEqualTo(2450000.0)
    }

    @Test
    fun `totalValueInKrw - uses custom exchange rate`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0,
            currency = "USD"
        )

        // 1750 USD * 1300 = 2,275,000 KRW
        assertThat(stock.totalValueInKrw(1300.0)).isEqualTo(2275000.0)
    }

    @Test
    fun `totalCostInKrw - KRW stock returns same value`() {
        val stock = Stock(
            id = 1,
            symbol = "005930.KS",
            name = "Samsung Electronics",
            quantity = 10,
            averagePrice = 70000.0,
            currentPrice = 78000.0,
            currency = "KRW"
        )

        assertThat(stock.totalCostInKrw()).isEqualTo(700000.0)
    }

    @Test
    fun `totalCostInKrw - USD stock converts to KRW`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0,
            currency = "USD"
        )

        // 1500 USD * 1400 = 2,100,000 KRW
        assertThat(stock.totalCostInKrw()).isEqualTo(2100000.0)
    }

    @Test
    fun `totalCostInKrw - uses custom exchange rate`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0,
            currency = "USD"
        )

        // 1500 USD * 1300 = 1,950,000 KRW
        assertThat(stock.totalCostInKrw(1300.0)).isEqualTo(1950000.0)
    }

    @Test
    fun `priceHistory - defaults to empty list`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0
        )

        assertThat(stock.priceHistory).isEmpty()
    }

    @Test
    fun `priceHistory - stores provided values`() {
        val priceHistory = listOf(170.0, 172.0, 175.0)
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0,
            priceHistory = priceHistory
        )

        assertThat(stock.priceHistory).hasSize(3)
        assertThat(stock.priceHistory).containsExactly(170.0, 172.0, 175.0).inOrder()
    }

    @Test
    fun `priceHistoryTimestamps - defaults to empty list`() {
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0
        )

        assertThat(stock.priceHistoryTimestamps).isEmpty()
    }

    @Test
    fun `priceHistoryTimestamps - stores provided values`() {
        val timestamps = listOf(1704067200L, 1704153600L, 1704240000L)
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0,
            priceHistoryTimestamps = timestamps
        )

        assertThat(stock.priceHistoryTimestamps).hasSize(3)
        assertThat(stock.priceHistoryTimestamps).containsExactly(1704067200L, 1704153600L, 1704240000L).inOrder()
    }

    @Test
    fun `priceHistory and timestamps - can be set together`() {
        val priceHistory = listOf(170.0, 172.0, 175.0)
        val timestamps = listOf(1704067200L, 1704153600L, 1704240000L)
        val stock = Stock(
            id = 1,
            symbol = "AAPL",
            name = "Apple Inc.",
            quantity = 10,
            averagePrice = 150.0,
            currentPrice = 175.0,
            priceHistory = priceHistory,
            priceHistoryTimestamps = timestamps
        )

        assertThat(stock.priceHistory).hasSize(3)
        assertThat(stock.priceHistoryTimestamps).hasSize(3)
    }
}
