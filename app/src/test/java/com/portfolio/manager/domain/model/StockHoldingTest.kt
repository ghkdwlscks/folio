package com.portfolio.manager.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StockHoldingTest {

    @Test
    fun `creates stock holding with all properties`() {
        val holding = StockHolding(
            symbol = "AAPL",
            quantity = 10,
            currency = Currency.USD
        )

        assertThat(holding.symbol).isEqualTo("AAPL")
        assertThat(holding.quantity).isEqualTo(10)
        assertThat(holding.currency).isEqualTo(Currency.USD)
    }

    @Test
    fun `supports KRW currency`() {
        val holding = StockHolding(
            symbol = "005930.KS",
            quantity = 50,
            currency = Currency.KRW
        )

        assertThat(holding.symbol).isEqualTo("005930.KS")
        assertThat(holding.quantity).isEqualTo(50)
        assertThat(holding.currency).isEqualTo(Currency.KRW)
    }

    @Test
    fun `data class equals works correctly`() {
        val holding1 = StockHolding("AAPL", 10, Currency.USD)
        val holding2 = StockHolding("AAPL", 10, Currency.USD)
        val holding3 = StockHolding("GOOG", 10, Currency.USD)

        assertThat(holding1).isEqualTo(holding2)
        assertThat(holding1).isNotEqualTo(holding3)
    }

    @Test
    fun `data class copy works correctly`() {
        val original = StockHolding("AAPL", 10, Currency.USD)
        val modified = original.copy(quantity = 20)

        assertThat(modified.symbol).isEqualTo("AAPL")
        assertThat(modified.quantity).isEqualTo(20)
        assertThat(modified.currency).isEqualTo(Currency.USD)
    }
}
