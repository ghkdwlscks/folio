package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExchangeRateAdjusterTest {

    @Test
    fun `compoundReturn - calculates correctly`() {
        // 10% asset return + 5% currency return
        val result = ExchangeRateAdjuster.compoundReturn(10.0, 5.0)
        // Expected: 10 + 5 + (10 * 5 / 100) = 15.5
        assertThat(result).isWithin(0.01).of(15.5)
    }

    @Test
    fun `compoundReturn - handles zero returns`() {
        assertThat(ExchangeRateAdjuster.compoundReturn(0.0, 0.0)).isWithin(0.01).of(0.0)
        assertThat(ExchangeRateAdjuster.compoundReturn(10.0, 0.0)).isWithin(0.01).of(10.0)
        assertThat(ExchangeRateAdjuster.compoundReturn(0.0, 5.0)).isWithin(0.01).of(5.0)
    }

    @Test
    fun `compoundReturn - handles negative returns`() {
        val result = ExchangeRateAdjuster.compoundReturn(-10.0, -5.0)
        // Expected: -10 + -5 + (-10 * -5 / 100) = -14.5
        assertThat(result).isWithin(0.01).of(-14.5)
    }

    @Test
    fun `needsAdjustment - USD with KRW display`() {
        assertThat(ExchangeRateAdjuster.needsAdjustment("USD", showInKrw = true)).isTrue()
    }

    @Test
    fun `needsAdjustment - KRW with USD display`() {
        assertThat(ExchangeRateAdjuster.needsAdjustment("KRW", showInKrw = false)).isTrue()
    }

    @Test
    fun `needsAdjustment - USD with USD display`() {
        assertThat(ExchangeRateAdjuster.needsAdjustment("USD", showInKrw = false)).isFalse()
    }

    @Test
    fun `needsAdjustment - KRW with KRW display`() {
        assertThat(ExchangeRateAdjuster.needsAdjustment("KRW", showInKrw = true)).isFalse()
    }

    @Test
    fun `adjustReturnForExchangeRate - USD to KRW with USD strengthening`() {
        // USD strengthened: 1300 → 1400 (7.69% gain)
        val result = ExchangeRateAdjuster.adjustReturnForExchangeRate(
            stockReturn = 10.0,
            currency = "USD",
            showInKrw = true,
            startExchangeRate = 1300.0,
            endExchangeRate = 1400.0
        )
        // FX return = (1400 - 1300) / 1300 * 100 = 7.69%
        // Compound = 10 + 7.69 + (10 * 7.69 / 100) ≈ 18.46
        assertThat(result).isGreaterThan(17.0)
    }

    @Test
    fun `adjustReturnForExchangeRate - USD to KRW with USD weakening`() {
        // USD weakened: 1400 → 1300 (-7.14% loss)
        val result = ExchangeRateAdjuster.adjustReturnForExchangeRate(
            stockReturn = 10.0,
            currency = "USD",
            showInKrw = true,
            startExchangeRate = 1400.0,
            endExchangeRate = 1300.0
        )
        assertThat(result).isLessThan(10.0)
    }

    @Test
    fun `adjustReturnForExchangeRate - no adjustment needed`() {
        val result = ExchangeRateAdjuster.adjustReturnForExchangeRate(
            stockReturn = 10.0,
            currency = "USD",
            showInKrw = false,
            startExchangeRate = 1300.0,
            endExchangeRate = 1400.0
        )
        assertThat(result).isWithin(0.01).of(10.0)
    }

    @Test
    fun `adjustReturnForExchangeRate - invalid exchange rates`() {
        val result1 = ExchangeRateAdjuster.adjustReturnForExchangeRate(10.0, "USD", true, 0.0, 1400.0)
        val result2 = ExchangeRateAdjuster.adjustReturnForExchangeRate(10.0, "USD", true, 1300.0, 0.0)
        val result3 = ExchangeRateAdjuster.adjustReturnForExchangeRate(10.0, "USD", true, -100.0, 1400.0)

        assertThat(result1).isWithin(0.01).of(10.0)
        assertThat(result2).isWithin(0.01).of(10.0)
        assertThat(result3).isWithin(0.01).of(10.0)
    }

    @Test
    fun `calculateBenchmarkReturn - returns null for insufficient data`() {
        val result = ExchangeRateAdjuster.calculateBenchmarkReturn(
            prices = listOf(100.0),
            currency = "USD",
            showInKrw = true,
            startExchangeRate = 1300.0,
            endExchangeRate = 1400.0
        )
        assertThat(result).isNull()
    }

    @Test
    fun `calculateBenchmarkReturn - calculates with adjustment`() {
        val result = ExchangeRateAdjuster.calculateBenchmarkReturn(
            prices = listOf(100.0, 110.0),
            currency = "USD",
            showInKrw = true,
            startExchangeRate = 1300.0,
            endExchangeRate = 1400.0
        )
        assertThat(result).isNotNull()
        // Raw return is 10%, plus FX adjustment
        assertThat(result!!).isGreaterThan(10.0)
    }

    @Test
    fun `calculateBenchmarkReturn - no adjustment when currencies match`() {
        val result = ExchangeRateAdjuster.calculateBenchmarkReturn(
            prices = listOf(100.0, 110.0),
            currency = "USD",
            showInKrw = false,
            startExchangeRate = 1300.0,
            endExchangeRate = 1400.0
        )
        assertThat(result).isNotNull()
        assertThat(result!!).isWithin(0.01).of(10.0)
    }
}
