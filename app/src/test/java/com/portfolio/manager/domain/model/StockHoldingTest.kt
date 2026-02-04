package com.portfolio.manager.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StockHoldingTest {

    @Test
    fun `creates stock holding with all properties`() {
        val holding = StockHolding(
            symbol = "AAPL",
            quantity = 10,
            currency = "USD"
        )

        assertThat(holding.symbol).isEqualTo("AAPL")
        assertThat(holding.quantity).isEqualTo(10)
        assertThat(holding.currency).isEqualTo("USD")
    }

    @Test
    fun `supports KRW currency`() {
        val holding = StockHolding(
            symbol = "005930.KS",
            quantity = 50,
            currency = "KRW"
        )

        assertThat(holding.symbol).isEqualTo("005930.KS")
        assertThat(holding.quantity).isEqualTo(50)
        assertThat(holding.currency).isEqualTo("KRW")
    }

    @Test
    fun `data class equals works correctly`() {
        val holding1 = StockHolding("AAPL", 10, "USD")
        val holding2 = StockHolding("AAPL", 10, "USD")
        val holding3 = StockHolding("GOOG", 10, "USD")

        assertThat(holding1).isEqualTo(holding2)
        assertThat(holding1).isNotEqualTo(holding3)
    }

    @Test
    fun `data class copy works correctly`() {
        val original = StockHolding("AAPL", 10, "USD")
        val modified = original.copy(quantity = 20)

        assertThat(modified.symbol).isEqualTo("AAPL")
        assertThat(modified.quantity).isEqualTo(20)
        assertThat(modified.currency).isEqualTo("USD")
    }
}
