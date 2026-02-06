package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimeSeriesProcessorTest {

    @Test
    fun `forwardFillSeries - fills gaps with last known value`() {
        val originalData = mapOf("2024-01-01" to 100.0, "2024-01-03" to 110.0)
        val allDates = listOf("2024-01-01", "2024-01-02", "2024-01-03")

        val result = TimeSeriesProcessor.forwardFillSeries(originalData, allDates)

        assertThat(result).containsExactly(
            "2024-01-01", 100.0,
            "2024-01-02", 100.0,
            "2024-01-03", 110.0
        )
    }

    @Test
    fun `forwardFillSeries - skips dates before first data point when no default`() {
        val originalData = mapOf("2024-01-02" to 100.0)
        val allDates = listOf("2024-01-01", "2024-01-02", "2024-01-03")

        val result = TimeSeriesProcessor.forwardFillSeries(originalData, allDates)

        assertThat(result).containsExactly(
            "2024-01-02", 100.0,
            "2024-01-03", 100.0
        )
    }

    @Test
    fun `forwardFillSeries - uses default value before first data point`() {
        val originalData = mapOf("2024-01-02" to 100.0)
        val allDates = listOf("2024-01-01", "2024-01-02", "2024-01-03")

        val result = TimeSeriesProcessor.forwardFillSeries(originalData, allDates, defaultValue = 90.0)

        assertThat(result).containsExactly(
            "2024-01-01", 90.0,
            "2024-01-02", 100.0,
            "2024-01-03", 100.0
        )
    }

    @Test
    fun `forwardFillSeries - empty data - returns empty map`() {
        val originalData = emptyMap<String, Double>()
        val allDates = listOf("2024-01-01", "2024-01-02")

        val result = TimeSeriesProcessor.forwardFillSeries(originalData, allDates)

        assertThat(result).isEmpty()
    }

    @Test
    fun `forwardFillPrices - fills multiple symbols`() {
        val symbolDatePrices = mapOf(
            "AAPL" to mapOf("2024-01-01" to 100.0, "2024-01-03" to 110.0),
            "GOOG" to mapOf("2024-01-01" to 200.0)
        )
        val allDates = listOf("2024-01-01", "2024-01-02", "2024-01-03")

        val result = TimeSeriesProcessor.forwardFillPrices(listOf("AAPL", "GOOG"), symbolDatePrices, allDates)

        assertThat(result["AAPL"]).containsExactly(
            "2024-01-01", 100.0,
            "2024-01-02", 100.0,
            "2024-01-03", 110.0
        )
        assertThat(result["GOOG"]).containsExactly(
            "2024-01-01", 200.0,
            "2024-01-02", 200.0,
            "2024-01-03", 200.0
        )
    }

    @Test
    fun `forwardFillPrices - handles missing symbol`() {
        val symbolDatePrices = mapOf(
            "AAPL" to mapOf("2024-01-01" to 100.0)
        )
        val allDates = listOf("2024-01-01")

        val result = TimeSeriesProcessor.forwardFillPrices(listOf("AAPL", "MISSING"), symbolDatePrices, allDates)

        assertThat(result["AAPL"]).containsExactly("2024-01-01", 100.0)
        assertThat(result["MISSING"]).isEmpty()
    }

    @Test
    fun `forwardFillExchangeRates - fills with default rate`() {
        val exchangeRateByDate = mapOf("2024-01-02" to 1350.0)
        val allDates = listOf("2024-01-01", "2024-01-02", "2024-01-03")

        val result = TimeSeriesProcessor.forwardFillExchangeRates(exchangeRateByDate, allDates, defaultRate = 1300.0)

        assertThat(result).containsExactly(
            "2024-01-01", 1300.0,
            "2024-01-02", 1350.0,
            "2024-01-03", 1350.0
        )
    }

    @Test
    fun `normalizeValues - starts at 100`() {
        val values = listOf(50.0, 75.0, 100.0)

        val result = TimeSeriesProcessor.normalizeValues(values)

        assertThat(result).containsExactly(100.0, 150.0, 200.0)
    }

    @Test
    fun `normalizeValues - handles zero start value`() {
        val values = listOf(0.0, 50.0, 100.0)

        val result = TimeSeriesProcessor.normalizeValues(values)

        assertThat(result).containsExactly(0.0, 50.0, 100.0)
    }

    @Test
    fun `normalizeValues - empty list returns empty`() {
        val result = TimeSeriesProcessor.normalizeValues(emptyList())
        assertThat(result).isEmpty()
    }

    @Test
    fun `normalizeValues - single value normalizes to 100`() {
        val result = TimeSeriesProcessor.normalizeValues(listOf(250.0))
        assertThat(result).containsExactly(100.0)
    }
}
