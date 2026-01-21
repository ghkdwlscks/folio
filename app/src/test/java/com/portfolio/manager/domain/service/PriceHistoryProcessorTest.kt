package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.domain.repository.PriceHistoryData
import org.junit.Test

class PriceHistoryProcessorTest {

    @Test
    fun `timestampToDate - converts Unix timestamp to date string`() {
        // 2024-01-15 00:00:00 UTC
        val timestamp = 1705276800L
        val result = PriceHistoryProcessor.timestampToDate(timestamp)
        // Result depends on system timezone, so just check format
        assertThat(result).matches("\\d{4}-\\d{2}-\\d{2}")
    }

    @Test
    fun `buildSymbolDatePrices - with timestamps - builds date-price map`() {
        val priceHistoryMap = mapOf(
            "AAPL" to PriceHistoryData(
                prices = listOf(150.0, 155.0),
                timestamps = listOf(1705276800L, 1705363200L)
            )
        )
        val result = PriceHistoryProcessor.buildSymbolDatePrices(listOf("AAPL"), priceHistoryMap)

        assertThat(result).containsKey("AAPL")
        assertThat(result["AAPL"]).hasSize(2)
    }

    @Test
    fun `buildSymbolDatePrices - without timestamps - uses index keys`() {
        val priceHistoryMap = mapOf(
            "AAPL" to PriceHistoryData(
                prices = listOf(150.0, 155.0),
                timestamps = emptyList()
            )
        )
        val result = PriceHistoryProcessor.buildSymbolDatePrices(listOf("AAPL"), priceHistoryMap)

        assertThat(result["AAPL"]).containsKey("idx_0")
        assertThat(result["AAPL"]).containsKey("idx_1")
    }

    @Test
    fun `buildSymbolDatePrices - missing symbol - skips it`() {
        val priceHistoryMap = mapOf(
            "AAPL" to PriceHistoryData(listOf(150.0), listOf(1705276800L))
        )
        val result = PriceHistoryProcessor.buildSymbolDatePrices(listOf("AAPL", "GOOG"), priceHistoryMap)

        assertThat(result).containsKey("AAPL")
        assertThat(result).doesNotContainKey("GOOG")
    }

    @Test
    fun `buildExchangeRateByDate - with timestamps - builds date-rate map`() {
        val exchangeRateData = PriceHistoryData(
            prices = listOf(1300.0, 1350.0),
            timestamps = listOf(1705276800L, 1705363200L)
        )
        val result = PriceHistoryProcessor.buildExchangeRateByDate(exchangeRateData)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `buildExchangeRateByDate - empty timestamps - returns empty map`() {
        val exchangeRateData = PriceHistoryData(
            prices = listOf(1300.0, 1350.0),
            timestamps = emptyList()
        )
        val result = PriceHistoryProcessor.buildExchangeRateByDate(exchangeRateData)

        assertThat(result).isEmpty()
    }

    @Test
    fun `forwardFillPrices - fills missing dates`() {
        val symbolDatePrices = mapOf(
            "AAPL" to mapOf("2024-01-01" to 150.0, "2024-01-03" to 155.0)
        )
        val allDates = listOf("2024-01-01", "2024-01-02", "2024-01-03")

        val result = PriceHistoryProcessor.forwardFillPrices(listOf("AAPL"), symbolDatePrices, allDates)

        assertThat(result["AAPL"]?.get("2024-01-01")).isEqualTo(150.0)
        assertThat(result["AAPL"]?.get("2024-01-02")).isEqualTo(150.0) // Forward-filled
        assertThat(result["AAPL"]?.get("2024-01-03")).isEqualTo(155.0)
    }

    @Test
    fun `forwardFillPrices - skips dates before first price`() {
        val symbolDatePrices = mapOf(
            "AAPL" to mapOf("2024-01-02" to 150.0)
        )
        val allDates = listOf("2024-01-01", "2024-01-02", "2024-01-03")

        val result = PriceHistoryProcessor.forwardFillPrices(listOf("AAPL"), symbolDatePrices, allDates)

        assertThat(result["AAPL"]?.containsKey("2024-01-01")).isFalse()
        assertThat(result["AAPL"]?.get("2024-01-02")).isEqualTo(150.0)
        assertThat(result["AAPL"]?.get("2024-01-03")).isEqualTo(150.0)
    }

    @Test
    fun `forwardFillPrices - empty symbol prices - returns empty map`() {
        val symbolDatePrices = mapOf<String, Map<String, Double>>()
        val allDates = listOf("2024-01-01", "2024-01-02")

        val result = PriceHistoryProcessor.forwardFillPrices(listOf("AAPL"), symbolDatePrices, allDates)

        assertThat(result["AAPL"]).isEmpty()
    }

    @Test
    fun `calculatePortfolioValues - calculates values for each date`() {
        val holdings = listOf(
            StockHolding("AAPL", 10, "USD"),
            StockHolding("GOOG", 5, "USD")
        )
        val filledPrices = mapOf(
            "AAPL" to mapOf("2024-01-01" to 150.0, "2024-01-02" to 155.0),
            "GOOG" to mapOf("2024-01-01" to 100.0, "2024-01-02" to 105.0)
        )
        val exchangeRateByDate = emptyMap<String, Double>()

        val result = PriceHistoryProcessor.calculatePortfolioValues(
            listOf("2024-01-01", "2024-01-02"),
            holdings,
            filledPrices,
            exchangeRateByDate,
            defaultExchangeRate = 1400.0,
            showInKrw = false
        )

        // Day 1: AAPL 10*150 + GOOG 5*100 = 1500 + 500 = 2000
        // Day 2: AAPL 10*155 + GOOG 5*105 = 1550 + 525 = 2075
        assertThat(result).hasSize(2)
        assertThat(result[0]).isEqualTo(2000.0)
        assertThat(result[1]).isEqualTo(2075.0)
    }

    @Test
    fun `calculatePortfolioValues - with KRW display - converts values`() {
        val holdings = listOf(StockHolding("AAPL", 1, "USD"))
        val filledPrices = mapOf(
            "AAPL" to mapOf("2024-01-01" to 100.0)
        )

        val result = PriceHistoryProcessor.calculatePortfolioValues(
            listOf("2024-01-01"),
            holdings,
            filledPrices,
            emptyMap(),
            defaultExchangeRate = 1400.0,
            showInKrw = true
        )

        // 1 share * $100 * 1400 = 140,000 KRW
        assertThat(result[0]).isEqualTo(140000.0)
    }

    @Test
    fun `calculatePortfolioValues - missing price - skips holding`() {
        val holdings = listOf(
            StockHolding("AAPL", 10, "USD"),
            StockHolding("GOOG", 5, "USD")
        )
        val filledPrices = mapOf(
            "AAPL" to mapOf("2024-01-01" to 150.0)
            // GOOG missing
        )

        val result = PriceHistoryProcessor.calculatePortfolioValues(
            listOf("2024-01-01"),
            holdings,
            filledPrices,
            emptyMap(),
            defaultExchangeRate = 1400.0,
            showInKrw = false
        )

        // Only AAPL counted: 10 * 150 = 1500
        assertThat(result[0]).isEqualTo(1500.0)
    }

    @Test
    fun `calculatePortfolioValues - zero total - excluded from result`() {
        val holdings = listOf(StockHolding("AAPL", 0, "USD"))
        val filledPrices = mapOf(
            "AAPL" to mapOf("2024-01-01" to 150.0)
        )

        val result = PriceHistoryProcessor.calculatePortfolioValues(
            listOf("2024-01-01"),
            holdings,
            filledPrices,
            emptyMap(),
            defaultExchangeRate = 1400.0,
            showInKrw = false
        )

        assertThat(result).isEmpty()
    }

    @Test
    fun `adjustReturnForExchangeRate - USD stock in USD - no adjustment`() {
        val result = PriceHistoryProcessor.adjustReturnForExchangeRate(
            stockReturn = 10.0,
            currency = "USD",
            showInKrw = false,
            startExchangeRate = 1300.0,
            endExchangeRate = 1400.0
        )

        assertThat(result).isEqualTo(10.0)
    }

    @Test
    fun `adjustReturnForExchangeRate - KRW stock in KRW - no adjustment`() {
        val result = PriceHistoryProcessor.adjustReturnForExchangeRate(
            stockReturn = 10.0,
            currency = "KRW",
            showInKrw = true,
            startExchangeRate = 1300.0,
            endExchangeRate = 1400.0
        )

        assertThat(result).isEqualTo(10.0)
    }

    @Test
    fun `adjustReturnForExchangeRate - USD stock in KRW - adds exchange rate return`() {
        // Stock return: 10%, Exchange rate change: (1400-1300)/1300 = 7.69%
        val result = PriceHistoryProcessor.adjustReturnForExchangeRate(
            stockReturn = 10.0,
            currency = "USD",
            showInKrw = true,
            startExchangeRate = 1300.0,
            endExchangeRate = 1400.0
        )

        // Combined return: 10 + 7.69 + (10 * 7.69 / 100) ≈ 18.46
        assertThat(result).isGreaterThan(17.0)
        assertThat(result).isLessThan(19.0)
    }

    @Test
    fun `adjustReturnForExchangeRate - KRW stock in USD - adjusts for inverse rate`() {
        // Exchange rate change from KRW holder's USD perspective: (1300-1400)/1400 = -7.14%
        val result = PriceHistoryProcessor.adjustReturnForExchangeRate(
            stockReturn = 10.0,
            currency = "KRW",
            showInKrw = false,
            startExchangeRate = 1300.0,
            endExchangeRate = 1400.0
        )

        // Combined return < 10 because KRW weakened
        assertThat(result).isLessThan(10.0)
    }

    @Test
    fun `normalizeValues - normalizes to start at 100`() {
        val values = listOf(1000.0, 1100.0, 1050.0)
        val result = PriceHistoryProcessor.normalizeValues(values)

        assertThat(result[0]).isWithin(0.001).of(100.0)
        assertThat(result[1]).isWithin(0.001).of(110.0)
        assertThat(result[2]).isWithin(0.001).of(105.0)
    }

    @Test
    fun `normalizeValues - empty list - returns empty`() {
        val result = PriceHistoryProcessor.normalizeValues(emptyList())
        assertThat(result).isEmpty()
    }

    @Test
    fun `normalizeValues - zero start value - returns original`() {
        val values = listOf(0.0, 100.0, 200.0)
        val result = PriceHistoryProcessor.normalizeValues(values)
        assertThat(result).isEqualTo(values)
    }

    @Test
    fun `StockHolding - data class properties`() {
        val holding = StockHolding("AAPL", 100, "USD")
        assertThat(holding.symbol).isEqualTo("AAPL")
        assertThat(holding.quantity).isEqualTo(100)
        assertThat(holding.currency).isEqualTo("USD")
    }
}
