package com.portfolio.manager.domain.util

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

    // toDisplayCurrency tests

    @Test
    fun `toDisplayCurrency - showInKrw true - converts USD to KRW`() {
        val result = CurrencyConverter.toDisplayCurrency(100.0, showInKrw = true, exchangeRate)
        assertThat(result).isEqualTo(140000.0)
    }

    @Test
    fun `toDisplayCurrency - showInKrw false - returns same USD value`() {
        val result = CurrencyConverter.toDisplayCurrency(100.0, showInKrw = false, exchangeRate)
        assertThat(result).isEqualTo(100.0)
    }

    @Test
    fun `toDisplayCurrency - uses default exchange rate when not specified`() {
        val result = CurrencyConverter.toDisplayCurrency(1.0, showInKrw = true)
        assertThat(result).isEqualTo(AppConstants.KRW_TO_USD_RATE)
    }

    // fromDisplayCurrency tests

    @Test
    fun `fromDisplayCurrency - wasInKrw true - converts KRW to USD`() {
        val result = CurrencyConverter.fromDisplayCurrency(140000.0, wasInKrw = true, exchangeRate)
        assertThat(result).isEqualTo(100.0)
    }

    @Test
    fun `fromDisplayCurrency - wasInKrw false - returns same USD value`() {
        val result = CurrencyConverter.fromDisplayCurrency(100.0, wasInKrw = false, exchangeRate)
        assertThat(result).isEqualTo(100.0)
    }

    @Test
    fun `fromDisplayCurrency - uses default exchange rate when not specified`() {
        val result = CurrencyConverter.fromDisplayCurrency(AppConstants.KRW_TO_USD_RATE, wasInKrw = true)
        assertThat(result).isEqualTo(1.0)
    }

    @Test
    fun `fromDisplayCurrency - zero exchange rate - returns same value`() {
        val result = CurrencyConverter.fromDisplayCurrency(100.0, wasInKrw = true, 0.0)
        assertThat(result).isEqualTo(100.0)
    }
}
