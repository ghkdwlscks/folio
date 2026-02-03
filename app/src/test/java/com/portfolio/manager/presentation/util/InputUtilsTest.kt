package com.portfolio.manager.presentation.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class InputUtilsTest {

    // filterDigitsOnly tests

    @Test
    fun `filterDigitsOnly - digits only input - returns unchanged`() {
        assertThat(InputUtils.filterDigitsOnly("12345")).isEqualTo("12345")
    }

    @Test
    fun `filterDigitsOnly - mixed input - returns only digits`() {
        assertThat(InputUtils.filterDigitsOnly("1a2b3c")).isEqualTo("123")
    }

    @Test
    fun `filterDigitsOnly - with decimal point - removes decimal`() {
        assertThat(InputUtils.filterDigitsOnly("123.45")).isEqualTo("12345")
    }

    @Test
    fun `filterDigitsOnly - empty input - returns empty`() {
        assertThat(InputUtils.filterDigitsOnly("")).isEqualTo("")
    }

    @Test
    fun `filterDigitsOnly - no digits - returns empty`() {
        assertThat(InputUtils.filterDigitsOnly("abc")).isEqualTo("")
    }

    @Test
    fun `filterDigitsOnly - special characters - returns empty`() {
        assertThat(InputUtils.filterDigitsOnly("!@#$%")).isEqualTo("")
    }

    // filterNumeric tests

    @Test
    fun `filterNumeric - digits only - returns unchanged`() {
        assertThat(InputUtils.filterNumeric("12345")).isEqualTo("12345")
    }

    @Test
    fun `filterNumeric - with decimal - returns unchanged`() {
        assertThat(InputUtils.filterNumeric("123.45")).isEqualTo("123.45")
    }

    @Test
    fun `filterNumeric - mixed input - returns digits and decimal`() {
        assertThat(InputUtils.filterNumeric("1a2.3b4")).isEqualTo("12.34")
    }

    @Test
    fun `filterNumeric - multiple decimals - keeps all decimals`() {
        assertThat(InputUtils.filterNumeric("1.2.3")).isEqualTo("1.2.3")
    }

    @Test
    fun `filterNumeric - empty input - returns empty`() {
        assertThat(InputUtils.filterNumeric("")).isEqualTo("")
    }

    @Test
    fun `filterNumeric - only decimal - returns decimal`() {
        assertThat(InputUtils.filterNumeric(".")).isEqualTo(".")
    }

    @Test
    fun `filterNumeric - letters only - returns empty`() {
        assertThat(InputUtils.filterNumeric("abc")).isEqualTo("")
    }

    // formatValueForCurrency tests

    @Test
    fun `formatValueForCurrency - KRW - returns integer string`() {
        assertThat(InputUtils.formatValueForCurrency(1234.56, "KRW")).isEqualTo("1234")
    }

    @Test
    fun `formatValueForCurrency - KRW with round number - returns integer`() {
        assertThat(InputUtils.formatValueForCurrency(1000.0, "KRW")).isEqualTo("1000")
    }

    @Test
    fun `formatValueForCurrency - USD - returns decimal string`() {
        assertThat(InputUtils.formatValueForCurrency(123.45, "USD")).isEqualTo("123.45")
    }

    @Test
    fun `formatValueForCurrency - USD round number - keeps decimal format`() {
        assertThat(InputUtils.formatValueForCurrency(100.0, "USD")).isEqualTo("100.0")
    }

    @Test
    fun `formatValueForCurrency - zero KRW - returns zero`() {
        assertThat(InputUtils.formatValueForCurrency(0.0, "KRW")).isEqualTo("0")
    }

    @Test
    fun `formatValueForCurrency - zero USD - returns zero with decimal`() {
        assertThat(InputUtils.formatValueForCurrency(0.0, "USD")).isEqualTo("0.0")
    }

    @Test
    fun `formatValueForCurrency - large KRW value - returns integer`() {
        assertThat(InputUtils.formatValueForCurrency(1000000.99, "KRW")).isEqualTo("1000000")
    }

    @Test
    fun `formatValueForCurrency - unknown currency - treated as non-KRW`() {
        assertThat(InputUtils.formatValueForCurrency(123.45, "EUR")).isEqualTo("123.45")
    }
}
