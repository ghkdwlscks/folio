package com.portfolio.manager.util

object AppConstants {
    const val ALL_ACCOUNTS_ID = -1L

    // Fallback currency conversion rate (KRW to USD) when API unavailable
    const val KRW_TO_USD_RATE = 1400.0

    // Validation bounds
    const val MIN_QUANTITY = 1
    const val MAX_QUANTITY = 1_000_000
    const val MIN_PRICE = 0.0001
    const val MAX_PRICE = 1_000_000_000.0

    // Default account name
    const val DEFAULT_ACCOUNT_NAME = "Default"

    // Benchmark symbols
    const val BENCHMARK_SP500 = "^GSPC"
    const val BENCHMARK_KOSPI = "^KS11"
}

object PreferenceKeys {
    // Dashboard preferences
    const val DASHBOARD_SHOW_IN_KRW = "dashboard_show_in_krw"
    const val DASHBOARD_CACHED_STOCKS_JSON = "dashboard_cached_stocks_json"
    const val DASHBOARD_CACHED_CASH_ITEMS_JSON = "dashboard_cached_cash_items_json"
    const val DASHBOARD_CACHED_ACCOUNTS_JSON = "dashboard_cached_accounts_json"
    const val DASHBOARD_CACHED_EXCHANGE_RATE = "dashboard_cached_exchange_rate"
    const val DASHBOARD_CACHED_PORTFOLIO_SPARKLINE = "dashboard_cached_portfolio_sparkline"
    const val DASHBOARD_CACHED_PORTFOLIO_STATS = "dashboard_cached_portfolio_stats"
    const val DASHBOARD_CACHED_PERIOD_RETURNS = "dashboard_cached_period_returns"
    const val DASHBOARD_CACHED_BENCHMARK_SPARKLINES = "dashboard_cached_benchmark_sparklines"
    const val DASHBOARD_CACHED_BENCHMARK_RETURNS = "dashboard_cached_benchmark_returns"
    const val DASHBOARD_CACHED_SPARKLINE_TIMESTAMPS = "dashboard_cached_sparkline_timestamps"
    const val STOCK_SPARKLINE_PERIOD = "stock_sparkline_period"
    const val PORTFOLIO_SUMMARY_PERIOD = "portfolio_summary_period"
    const val SORT_OPTION = "sort_option"

    // FIRE calculator preferences
    const val FIRE_ANNUAL_RETURN = "fire_annual_return"
    const val FIRE_ANNUAL_INFLATION = "fire_annual_inflation"
    const val FIRE_TARGET_MONTHLY_SPENDING = "fire_target_monthly_spending"
    const val FIRE_SHOW_IN_KRW = "fire_show_in_krw"
}
