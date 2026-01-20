package com.portfolio.manager.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StockExtensionsTest {

    @Test
    fun `isKoreanStock - KS suffix - returns true`() {
        assertThat("005930.KS".isKoreanStock()).isTrue()
    }

    @Test
    fun `isKoreanStock - KQ suffix - returns true`() {
        assertThat("035720.KQ".isKoreanStock()).isTrue()
    }

    @Test
    fun `isKoreanStock - US stock - returns false`() {
        assertThat("AAPL".isKoreanStock()).isFalse()
    }

    @Test
    fun `isKoreanStock - stock with different suffix - returns false`() {
        assertThat("005930.SS".isKoreanStock()).isFalse()
    }

    @Test
    fun `isKoreanStock - empty string - returns false`() {
        assertThat("".isKoreanStock()).isFalse()
    }

    @Test
    fun `isKoreanStock - lowercase ks - returns false`() {
        assertThat("005930.ks".isKoreanStock()).isFalse()
    }
}
