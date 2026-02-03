package com.portfolio.manager.presentation.util

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
     * Filters input to allow digits and decimal point.
     * Use for decimal inputs like price, value, yield rate.
     */
    fun filterNumeric(input: String): String = input.filter { it.isDigit() || it == '.' }

    /**
     * Formats a currency value for display based on currency type.
     * KRW values are shown as integers, USD values keep decimals.
     *
     * @param value The numeric value to format
     * @param currency The currency type ("KRW" or "USD")
     * @return Formatted string representation
     */
    fun formatValueForCurrency(value: Double, currency: String): String =
        if (currency == "KRW") value.toLong().toString() else value.toString()
}
