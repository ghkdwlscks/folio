package com.portfolio.manager.domain.service

/**
 * Service for processing time series data with forward-fill and normalization.
 */
object TimeSeriesProcessor {

    /**
     * Core forward-fill logic for a single time series.
     * @param originalData Map of date to value
     * @param allDates Sorted list of dates to fill
     * @param defaultValue Value to use before first data point (null = skip those dates)
     * @return Map with forward-filled values
     */
    fun forwardFillSeries(
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
     * Forward-fills exchange rates across all dates.
     * Dates before the first exchange rate data point use the default rate.
     */
    fun forwardFillExchangeRates(
        exchangeRateByDate: Map<String, Double>,
        allDates: List<String>,
        defaultRate: Double
    ): Map<String, Double> = forwardFillSeries(exchangeRateByDate, allDates, defaultRate)

    /**
     * Normalizes values to start at 100 for percentage comparison.
     */
    fun normalizeValues(values: List<Double>): List<Double> {
        val startValue = values.firstOrNull() ?: 0.0
        return if (startValue > 0) {
            values.map { 100.0 * (it / startValue) }
        } else {
            values
        }
    }
}
