package com.portfolio.manager.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReturnCalculatorTest {

    @Test
    fun `calculate - positive return`() {
        val result = ReturnCalculator.calculate(100.0, 125.0)
        assertThat(result).isEqualTo(25.0)
    }

    @Test
    fun `calculate - negative return`() {
        val result = ReturnCalculator.calculate(100.0, 80.0)
        assertThat(result).isEqualTo(-20.0)
    }

    @Test
    fun `calculate - zero return`() {
        val result = ReturnCalculator.calculate(100.0, 100.0)
        assertThat(result).isEqualTo(0.0)
    }

    @Test
    fun `calculate - start value is zero returns zero`() {
        val result = ReturnCalculator.calculate(0.0, 100.0)
        assertThat(result).isEqualTo(0.0)
    }

    @Test
    fun `calculate - negative start value returns zero`() {
        val result = ReturnCalculator.calculate(-100.0, 100.0)
        assertThat(result).isEqualTo(0.0)
    }

    @Test
    fun `calculateFromList - returns correct percentage`() {
        val values = listOf(100.0, 110.0, 120.0, 125.0)
        val result = ReturnCalculator.calculateFromList(values)
        assertThat(result).isEqualTo(25.0)
    }

    @Test
    fun `calculateFromList - two values`() {
        val values = listOf(100.0, 150.0)
        val result = ReturnCalculator.calculateFromList(values)
        assertThat(result).isEqualTo(50.0)
    }

    @Test
    fun `calculateFromList - single value returns null`() {
        val values = listOf(100.0)
        val result = ReturnCalculator.calculateFromList(values)
        assertThat(result).isNull()
    }

    @Test
    fun `calculateFromList - empty list returns null`() {
        val values = emptyList<Double>()
        val result = ReturnCalculator.calculateFromList(values)
        assertThat(result).isNull()
    }

    @Test
    fun `calculateFromListOrZero - returns correct percentage`() {
        val values = listOf(100.0, 110.0, 125.0)
        val result = ReturnCalculator.calculateFromListOrZero(values)
        assertThat(result).isEqualTo(25.0)
    }

    @Test
    fun `calculateFromListOrZero - insufficient data returns zero`() {
        val values = listOf(100.0)
        val result = ReturnCalculator.calculateFromListOrZero(values)
        assertThat(result).isEqualTo(0.0)
    }

    @Test
    fun `calculateFromListOrZero - empty list returns zero`() {
        val values = emptyList<Double>()
        val result = ReturnCalculator.calculateFromListOrZero(values)
        assertThat(result).isEqualTo(0.0)
    }
}
