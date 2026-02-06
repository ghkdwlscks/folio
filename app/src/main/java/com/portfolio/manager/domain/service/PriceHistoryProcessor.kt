package com.portfolio.manager.domain.service

import com.portfolio.manager.domain.model.StockHolding
import com.portfolio.manager.domain.repository.PriceHistoryData
import com.portfolio.manager.domain.util.CurrencyConverter

/**
 * Service for processing price history data for portfolio calculations.
 * Delegates to specialized services for specific operations.
 */
object PriceHistoryProcessor {

    /**
     * Converts Unix timestamp (seconds) to date string YYYY-MM-DD.
     */
    fun timestampToDate(timestamp: Long): String =
        DateTimeConverter.timestampToDate(timestamp)

    /**
     * Converts date string YYYY-MM-DD to Unix timestamp (seconds).
     * Returns null for invalid date strings or index-based keys.
     */
    fun dateToTimestamp(dateString: String): Long? =
        DateTimeConverter.dateToTimestamp(dateString)

    /**
     * Builds a map of symbol to date-price mapping from price history data.
     */
    fun buildSymbolDatePrices(
        symbols: List<String>,
        priceHistoryMap: Map<String, PriceHistoryData>
    ): Map<String, Map<String, Double>> {
        return symbols.mapNotNull { symbol ->
            val data = priceHistoryMap[symbol] ?: return@mapNotNull null
            val datePriceMap = DateTimeConverter.zipWithDateKeys(data.timestamps, data.prices)
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
     * Forward-fills prices for each symbol across all dates.
     * Missing dates use the last known price.
     */
    fun forwardFillPrices(
        symbols: List<String>,
        symbolDatePrices: Map<String, Map<String, Double>>,
        allDates: List<String>
    ): Map<String, Map<String, Double>> =
        TimeSeriesProcessor.forwardFillPrices(symbols, symbolDatePrices, allDates)

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
     * Adjusts stock return for exchange rate changes based on currency and display preference.
     */
    fun adjustReturnForExchangeRate(
        stockReturn: Double,
        currency: String,
        showInKrw: Boolean,
        startExchangeRate: Double,
        endExchangeRate: Double
    ): Double = ExchangeRateAdjuster.adjustReturnForExchangeRate(
        stockReturn, currency, showInKrw, startExchangeRate, endExchangeRate
    )

    /**
     * Forward-fills exchange rates across all dates.
     * Dates before the first exchange rate data point use the default rate.
     */
    fun forwardFillExchangeRates(
        exchangeRateByDate: Map<String, Double>,
        allDates: List<String>,
        defaultRate: Double
    ): Map<String, Double> =
        TimeSeriesProcessor.forwardFillExchangeRates(exchangeRateByDate, allDates, defaultRate)

    /**
     * Normalizes portfolio values to start at 100 for percentage comparison.
     */
    fun normalizeValues(values: List<Double>): List<Double> =
        TimeSeriesProcessor.normalizeValues(values)

    /**
     * Calculates benchmark return from price history with optional exchange rate adjustment.
     */
    fun calculateBenchmarkReturn(
        prices: List<Double>,
        currency: String,
        showInKrw: Boolean,
        startExchangeRate: Double,
        endExchangeRate: Double
    ): Double? = ExchangeRateAdjuster.calculateBenchmarkReturn(
        prices, currency, showInKrw, startExchangeRate, endExchangeRate
    )
}
