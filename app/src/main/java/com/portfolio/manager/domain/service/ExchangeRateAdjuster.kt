package com.portfolio.manager.domain.service

import com.portfolio.manager.domain.util.ReturnCalculator

/**
 * Service for adjusting returns based on exchange rate changes.
 */
object ExchangeRateAdjuster {

    /**
     * Calculates compound return from asset return and currency return.
     * Formula: (1 + r_asset) * (1 + r_fx) - 1 = r_asset + r_fx + r_asset * r_fx
     */
    fun compoundReturn(assetReturnPercent: Double, currencyReturnPercent: Double): Double =
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
     * Determines if a currency needs exchange rate adjustment for the given display preference.
     */
    fun needsAdjustment(currency: String, showInKrw: Boolean): Boolean =
        (currency == "USD" && showInKrw) || (currency == "KRW" && !showInKrw)

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
        val rawReturn = ReturnCalculator.calculateFromList(prices) ?: return null
        return if (needsAdjustment(currency, showInKrw)) {
            adjustReturnForExchangeRate(rawReturn, currency, showInKrw, startExchangeRate, endExchangeRate)
        } else {
            rawReturn
        }
    }
}
