package com.portfolio.manager.domain.service

import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.util.CurrencyConverter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Holds stock information needed for portfolio value calculation.
 */
data class StockHolding(
    val symbol: String,
    val quantity: Int,
    val currency: String
)

/**
 * Service for processing price history data for portfolio calculations.
 */
object PriceHistoryProcessor {

    /**
     * Converts Unix timestamp (seconds) to date string YYYY-MM-DD.
     */
    fun timestampToDate(timestamp: Long): String {
        val instant = Instant.ofEpochSecond(timestamp)
        return LocalDate.ofInstant(instant, ZoneId.of("UTC")).toString()
    }

    /**
     * Converts date string YYYY-MM-DD to Unix timestamp (seconds).
     * Returns null for invalid date strings or index-based keys.
     */
    fun dateToTimestamp(dateString: String): Long? {
        if (dateString.startsWith("idx_")) return null
        return try {
            val localDate = LocalDate.parse(dateString)
            localDate.atStartOfDay(ZoneId.of("UTC")).toInstant().epochSecond
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Builds a map of symbol to date-price mapping from price history data.
     */
    fun buildSymbolDatePrices(
        symbols: List<String>,
        priceHistoryMap: Map<String, PriceHistoryData>
    ): Map<String, Map<String, Double>> {
        return symbols.mapNotNull { symbol ->
            val data = priceHistoryMap[symbol] ?: return@mapNotNull null
            val datePriceMap = if (data.timestamps.isNotEmpty() && data.timestamps.size == data.prices.size) {
                data.timestamps.zip(data.prices).associate { (ts, price) ->
                    timestampToDate(ts) to price
                }
            } else {
                data.prices.mapIndexed { index, price ->
                    "idx_$index" to price
                }.toMap()
            }
            symbol to datePriceMap
        }.toMap()
    }

    /**
     * Builds a map of date to exchange rate from price history data.
     */
    fun buildExchangeRateByDate(exchangeRateData: PriceHistoryData): Map<String, Double> {
        return if (exchangeRateData.timestamps.isNotEmpty()) {
            exchangeRateData.timestamps.zip(exchangeRateData.prices).associate { (ts, rate) ->
                timestampToDate(ts) to rate
            }
        } else {
            emptyMap()
        }
    }

    /**
     * Core forward-fill logic for a single time series.
     * @param originalData Map of date to value
     * @param allDates Sorted list of dates to fill
     * @param defaultValue Value to use before first data point (null = skip those dates)
     * @return Map with forward-filled values
     */
    private fun forwardFillSeries(
        originalData: Map<String, Double>,
        allDates: List<String>,
        defaultValue: Double? = null
    ): Map<String, Double> {
        val filled = mutableMapOf<String, Double>()
        var lastValue = defaultValue

        for (date in allDates) {
            val value = originalData[date]
            if (value != null) {
                lastValue = value
            }
            if (lastValue != null) {
                filled[date] = lastValue
            }
        }
        return filled
    }

    /**
     * Forward-fills prices for each symbol across all dates.
     * Missing dates use the last known price.
     */
    fun forwardFillPrices(
        symbols: List<String>,
        symbolDatePrices: Map<String, Map<String, Double>>,
        allDates: List<String>
    ): Map<String, Map<String, Double>> {
        return symbols.associate { symbol ->
            val originalPrices = symbolDatePrices[symbol] ?: emptyMap()
            symbol to forwardFillSeries(originalPrices, allDates)
        }
    }

    /**
     * Calculates portfolio values for each valid date.
     */
    fun calculatePortfolioValues(
        validDates: List<String>,
        holdings: List<StockHolding>,
        filledPrices: Map<String, Map<String, Double>>,
        exchangeRateByDate: Map<String, Double>,
        defaultExchangeRate: Double,
        showInKrw: Boolean
    ): List<Double> {
        return validDates.mapNotNull { date ->
            var totalValue = 0.0
            val exchangeRate = (exchangeRateByDate[date] ?: defaultExchangeRate)
                .takeIf { it > 0 } ?: defaultExchangeRate

            for (holding in holdings) {
                val price = filledPrices[holding.symbol]?.get(date) ?: continue
                val value = price * holding.quantity
                totalValue += CurrencyConverter.convert(value, holding.currency, showInKrw, exchangeRate)
            }
            if (totalValue > 0) totalValue else null
        }
    }

    /**
     * Calculates compound return from asset return and currency return.
     * Formula: (1 + r_asset) * (1 + r_fx) - 1 = r_asset + r_fx + r_asset * r_fx
     */
    private fun compoundReturn(assetReturnPercent: Double, currencyReturnPercent: Double): Double =
        assetReturnPercent + currencyReturnPercent + (assetReturnPercent * currencyReturnPercent / 100)

    /**
     * Adjusts stock return for exchange rate changes based on currency and display preference.
     *
     * When viewing USD stocks in KRW, the return needs to account for exchange rate changes.
     * When viewing KRW stocks in USD, the return needs to account for inverse exchange rate changes.
     */
    fun adjustReturnForExchangeRate(
        stockReturn: Double,
        currency: String,
        showInKrw: Boolean,
        startExchangeRate: Double,
        endExchangeRate: Double
    ): Double {
        if (startExchangeRate <= 0 || endExchangeRate <= 0) return stockReturn

        val needsAdjustment = (currency == "USD" && showInKrw) || (currency == "KRW" && !showInKrw)
        if (!needsAdjustment) return stockReturn

        val fxReturn = if (currency == "USD") {
            // USD→KRW: positive when USD strengthens
            (endExchangeRate - startExchangeRate) / startExchangeRate * 100
        } else {
            // KRW→USD: positive when KRW strengthens (USD weakens)
            (startExchangeRate - endExchangeRate) / endExchangeRate * 100
        }
        return compoundReturn(stockReturn, fxReturn)
    }

    /**
     * Forward-fills exchange rates across all dates.
     * Dates before the first exchange rate data point use the default rate.
     */
    fun forwardFillExchangeRates(
        exchangeRateByDate: Map<String, Double>,
        allDates: List<String>,
        defaultRate: Double
    ): Map<String, Double> = forwardFillSeries(exchangeRateByDate, allDates, defaultRate)

    /**
     * Normalizes portfolio values to start at 100 for percentage comparison.
     */
    fun normalizeValues(values: List<Double>): List<Double> {
        val startValue = values.firstOrNull() ?: 0.0
        return if (startValue > 0) {
            values.map { 100.0 * (it / startValue) }
        } else {
            values
        }
    }

    /**
     * Calculates benchmark return from price history with optional exchange rate adjustment.
     *
     * @param prices List of historical prices
     * @param currency Currency of the benchmark (e.g., "USD" for S&P 500, "KRW" for KOSPI)
     * @param showInKrw Whether to display in KRW
     * @param startExchangeRate Exchange rate at start of period
     * @param endExchangeRate Exchange rate at end of period
     * @return Return percentage, or null if insufficient data
     */
    fun calculateBenchmarkReturn(
        prices: List<Double>,
        currency: String,
        showInKrw: Boolean,
        startExchangeRate: Double,
        endExchangeRate: Double
    ): Double? {
        if (prices.size < 2) return null
        val rawReturn = ((prices.last() - prices.first()) / prices.first()) * 100
        val needsAdjustment = (currency == "USD" && showInKrw) || (currency == "KRW" && !showInKrw)
        return if (needsAdjustment) {
            adjustReturnForExchangeRate(rawReturn, currency, showInKrw, startExchangeRate, endExchangeRate)
        } else {
            rawReturn
        }
    }
}
