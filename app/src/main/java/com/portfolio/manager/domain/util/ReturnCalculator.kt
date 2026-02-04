package com.portfolio.manager.domain.util

/**
 * Utility for calculating investment return percentages.
 * Consolidates return calculation logic used across the codebase.
 */
object ReturnCalculator {

    /**
     * Calculates return percentage from start and end values.
     * @param startValue The initial value
     * @param endValue The final value
     * @return Return percentage (positive for gains, negative for losses), or 0.0 if startValue <= 0
     */
    fun calculate(startValue: Double, endValue: Double): Double {
        return if (startValue > 0) {
            ((endValue - startValue) / startValue) * 100
        } else {
            0.0
        }
    }

    /**
     * Calculates return percentage from a list of values (first to last).
     * @param values List of values over time
     * @return Return percentage, or null if values has fewer than 2 elements
     */
    fun calculateFromList(values: List<Double>): Double? {
        if (values.size < 2) return null
        return calculate(values.first(), values.last())
    }

    /**
     * Calculates return percentage from a list of values, returning 0.0 for insufficient data.
     * @param values List of values over time
     * @return Return percentage, or 0.0 if values has fewer than 2 elements
     */
    fun calculateFromListOrZero(values: List<Double>): Double {
        return calculateFromList(values) ?: 0.0
    }
}
