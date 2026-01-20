package com.portfolio.manager.util

object AppConstants {
    const val ALL_ACCOUNTS_ID = -1L

    // Currency conversion rate (KRW to USD)
    // TODO: Consider fetching from API for real-time rates
    const val KRW_TO_USD_RATE = 1400.0

    // Validation bounds
    const val MIN_QUANTITY = 1
    const val MAX_QUANTITY = 1_000_000
    const val MIN_PRICE = 0.0001
    const val MAX_PRICE = 1_000_000_000.0

    // Default account name
    const val DEFAULT_ACCOUNT_NAME = "Default"
}
