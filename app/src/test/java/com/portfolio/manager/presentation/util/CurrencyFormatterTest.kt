package com.portfolio.manager.presentation.util

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.domain.model.Currency
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun `format - USD currency - formats with dollar sign and decimals`() {
        val result = CurrencyFormatter.format(1500.50, Currency.USD)
        assertThat(result).isEqualTo("$1,500.50")
    }

    @Test
    fun `format - KRW currency - formats with won sign and no decimals`() {
        val result = CurrencyFormatter.format(72000.0, Currency.KRW)
        assertThat(result).isEqualTo("₩72,000")
    }

    @Test
    fun `format - KRW large amount - formats with commas`() {
        val result = CurrencyFormatter.format(1500000.0, Currency.KRW)
        assertThat(result).isEqualTo("₩1,500,000")
    }

    @Test
    fun `formatUsd - formats amount in USD`() {
        val result = CurrencyFormatter.formatUsd(2500.75)
        assertThat(result).isEqualTo("$2,500.75")
    }

    @Test
    fun `formatUsd - zero amount - formats correctly`() {
        val result = CurrencyFormatter.formatUsd(0.0)
        assertThat(result).isEqualTo("$0.00")
    }

    @Test
    fun `format - negative amount - formats with minus sign`() {
        val result = CurrencyFormatter.format(-150.50, Currency.USD)
        assertThat(result).isEqualTo("-$150.50")
    }

    @Test
    fun `formatPercent - positive value - formats with two decimals`() {
        val result = CurrencyFormatter.formatPercent(12.5)
        assertThat(result).isEqualTo("12.50")
    }

    @Test
    fun `formatPercent - negative value - formats with two decimals`() {
        val result = CurrencyFormatter.formatPercent(-3.14159)
        assertThat(result).isEqualTo("-3.14")
    }

    @Test
    fun `formatPercent - zero value - formats as zero with decimals`() {
        val result = CurrencyFormatter.formatPercent(0.0)
        assertThat(result).isEqualTo("0.00")
    }

    @Test
    fun `formatPercent - small decimal - pads with zeros`() {
        val result = CurrencyFormatter.formatPercent(5.1)
        assertThat(result).isEqualTo("5.10")
    }

    @Test
    fun `formatKrw - formats amount with won sign`() {
        val result = CurrencyFormatter.formatKrw(72000.0)
        assertThat(result).isEqualTo("₩72,000")
    }

    @Test
    fun `formatKrw - large amount - formats with commas`() {
        val result = CurrencyFormatter.formatKrw(2450000.0)
        assertThat(result).isEqualTo("₩2,450,000")
    }

    @Test
    fun `formatKrw - zero amount - formats correctly`() {
        val result = CurrencyFormatter.formatKrw(0.0)
        assertThat(result).isEqualTo("₩0")
    }
}
