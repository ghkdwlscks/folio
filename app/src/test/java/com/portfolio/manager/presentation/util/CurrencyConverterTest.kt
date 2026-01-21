package com.portfolio.manager.presentation.util

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.util.AppConstants
import org.junit.Test

class CurrencyConverterTest {

    private val exchangeRate = 1400.0

    @Test
    fun `toUsd - USD currency - returns same value`() {
        val result = CurrencyConverter.toUsd(100.0, "USD", exchangeRate)
        assertThat(result).isEqualTo(100.0)
    }

    @Test
    fun `toUsd - KRW currency - converts to USD`() {
        val result = CurrencyConverter.toUsd(1400.0, "KRW", exchangeRate)
        assertThat(result).isEqualTo(1.0)
    }

    @Test
    fun `toUsd - uses default exchange rate when not specified`() {
        val result = CurrencyConverter.toUsd(AppConstants.KRW_TO_USD_RATE, "KRW")
        assertThat(result).isEqualTo(1.0)
    }

    @Test
    fun `toKrw - KRW currency - returns same value`() {
        val result = CurrencyConverter.toKrw(1400.0, "KRW", exchangeRate)
        assertThat(result).isEqualTo(1400.0)
    }

    @Test
    fun `toKrw - USD currency - converts to KRW`() {
        val result = CurrencyConverter.toKrw(1.0, "USD", exchangeRate)
        assertThat(result).isEqualTo(1400.0)
    }

    @Test
    fun `toKrw - uses default exchange rate when not specified`() {
        val result = CurrencyConverter.toKrw(1.0, "USD")
        assertThat(result).isEqualTo(AppConstants.KRW_TO_USD_RATE)
    }

    @Test
    fun `convert - showInKrw true with USD currency - converts to KRW`() {
        val result = CurrencyConverter.convert(1.0, "USD", showInKrw = true, exchangeRate)
        assertThat(result).isEqualTo(1400.0)
    }

    @Test
    fun `convert - showInKrw true with KRW currency - returns same value`() {
        val result = CurrencyConverter.convert(1400.0, "KRW", showInKrw = true, exchangeRate)
        assertThat(result).isEqualTo(1400.0)
    }

    @Test
    fun `convert - showInKrw false with USD currency - returns same value`() {
        val result = CurrencyConverter.convert(100.0, "USD", showInKrw = false, exchangeRate)
        assertThat(result).isEqualTo(100.0)
    }

    @Test
    fun `convert - showInKrw false with KRW currency - converts to USD`() {
        val result = CurrencyConverter.convert(1400.0, "KRW", showInKrw = false, exchangeRate)
        assertThat(result).isEqualTo(1.0)
    }

    @Test
    fun `convert - uses default exchange rate when not specified`() {
        val result = CurrencyConverter.convert(1.0, "USD", showInKrw = true)
        assertThat(result).isEqualTo(AppConstants.KRW_TO_USD_RATE)
    }
}
