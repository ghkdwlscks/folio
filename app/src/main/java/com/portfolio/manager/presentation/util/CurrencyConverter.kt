package com.portfolio.manager.presentation.util

import com.portfolio.manager.util.AppConstants

/**
 * Utility for currency conversion between USD and KRW.
 */
object CurrencyConverter {

    /**
     * Converts a value from its original currency to USD.
     * @param value The value to convert
     * @param currency The original currency ("USD" or "KRW")
     * @param exchangeRate The KRW to USD exchange rate
     */
    fun toUsd(value: Double, currency: String, exchangeRate: Double = AppConstants.KRW_TO_USD_RATE): Double {
        return if (currency == "KRW") value / exchangeRate else value
    }

    /**
     * Converts a value from its original currency to KRW.
     * @param value The value to convert
     * @param currency The original currency ("USD" or "KRW")
     * @param exchangeRate The KRW to USD exchange rate
     */
    fun toKrw(value: Double, currency: String, exchangeRate: Double = AppConstants.KRW_TO_USD_RATE): Double {
        return if (currency == "KRW") value else value * exchangeRate
    }

    /**
     * Converts a value to the target currency based on display preference.
     * @param value The value to convert
     * @param currency The original currency ("USD" or "KRW")
     * @param showInKrw Whether to display in KRW (true) or USD (false)
     * @param exchangeRate The KRW to USD exchange rate
     */
    fun convert(
        value: Double,
        currency: String,
        showInKrw: Boolean,
        exchangeRate: Double = AppConstants.KRW_TO_USD_RATE
    ): Double {
        return if (showInKrw) {
            toKrw(value, currency, exchangeRate)
        } else {
            toUsd(value, currency, exchangeRate)
        }
    }
}
