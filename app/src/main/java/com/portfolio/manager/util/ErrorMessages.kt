package com.portfolio.manager.util

/**
 * Centralized error messages for consistency across the app.
 */
object ErrorMessages {
    // Validation - AddHolding
    const val INVALID_QUANTITY = "Quantity must be between %d and %d"
    const val INVALID_PRICE = "Price must be between %.4f and %.0f"
    const val DUPLICATE_SYMBOL = "This stock already exists in the account"

    // Validation - AddCash
    const val EMPTY_NAME = "Please enter a name"
    const val INVALID_VALUE = "Please enter a valid value"
    const val INVALID_YIELD = "Please enter a valid yield rate"

    // Common validation
    const val SELECT_ACCOUNT = "Please select an account"

    // Loading errors
    const val LOAD_PRICES_FAILED = "Failed to load prices"
    const val NO_PORTFOLIO_DATA = "No portfolio data. Please refresh the dashboard first."

    // Household sharing
    const val HOUSEHOLD_LABEL_REQUIRED = "Please enter a display name"
    const val HOUSEHOLD_CODE_INVALID = "Invalid household code"
    const val HOUSEHOLD_SIGN_IN_FAILED = "Could not connect. Check your network and try again."
    const val HOUSEHOLD_PUBLISH_FAILED = "Paired, but failed to upload your data. Pull to refresh to retry."

    // Helper functions for formatted messages
    fun invalidQuantity(min: Int, max: Int): String = INVALID_QUANTITY.format(min, max)
    fun invalidPrice(min: Double, max: Double): String = INVALID_PRICE.format(min, max)
}
