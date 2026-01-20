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
}
