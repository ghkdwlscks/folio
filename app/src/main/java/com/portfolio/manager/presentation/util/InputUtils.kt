package com.portfolio.manager.presentation.util

import com.portfolio.manager.domain.model.Currency

/**
 * Utility functions for input validation and formatting.
 */
object InputUtils {

    /**
     * Filters input to only allow digits.
     * Use for integer inputs like quantity.
     */
    fun filterDigitsOnly(input: String): String = input.filter { it.isDigit() }

    /**
     * Filters input to allow digits and at most one decimal point.
     * Use for decimal inputs like price, value, yield rate.
     */
    fun filterNumeric(input: String): String {
        var hasDecimal = false
        return input.filter { char ->
            when {
                char.isDigit() -> true
                char == '.' && !hasDecimal -> {
                    hasDecimal = true
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Formats a currency value for display based on currency type.
     * KRW values are shown as integers, USD values keep decimals.
     *
     * @param value The numeric value to format
     * @param currency The currency type
     * @return Formatted string representation
     */
    fun formatValueForCurrency(value: Double, currency: Currency): String =
        if (currency.isKrw) value.toLong().toString() else value.toString()
}
