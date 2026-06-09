package com.portfolio.manager.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AppConstantsTest {

    @Test
    fun `ALL_ACCOUNTS_ID - has expected value`() {
        assertThat(AppConstants.ALL_ACCOUNTS_ID).isEqualTo(-1L)
    }

    @Test
    fun `KRW_TO_USD_RATE - has expected value`() {
        assertThat(AppConstants.KRW_TO_USD_RATE).isEqualTo(1400.0)
    }

    @Test
    fun `validation bounds - have expected values`() {
        assertThat(AppConstants.MIN_QUANTITY).isEqualTo(1)
        assertThat(AppConstants.MAX_QUANTITY).isEqualTo(1_000_000)
        assertThat(AppConstants.MIN_PRICE).isEqualTo(0.0001)
        assertThat(AppConstants.MAX_PRICE).isEqualTo(1_000_000_000.0)
    }

    @Test
    fun `DEFAULT_ACCOUNT_NAME - has expected value`() {
        assertThat(AppConstants.DEFAULT_ACCOUNT_NAME).isEqualTo("Default")
    }

    @Test
    fun `benchmark symbols - have expected values`() {
        assertThat(AppConstants.BENCHMARK_SP500).isEqualTo("^GSPC")
        assertThat(AppConstants.BENCHMARK_KOSPI).isEqualTo("^KS11")
    }
}

class PreferenceKeysTest {

    @Test
    fun `dashboard preference keys - have expected values`() {
        assertThat(PreferenceKeys.DASHBOARD_SHOW_IN_KRW).isEqualTo("dashboard_show_in_krw")
        assertThat(PreferenceKeys.DASHBOARD_CACHED_STOCKS_JSON).isEqualTo("dashboard_cached_stocks_json")
        assertThat(PreferenceKeys.DASHBOARD_CACHED_ACCOUNTS_JSON).isEqualTo("dashboard_cached_accounts_json")
        assertThat(PreferenceKeys.DASHBOARD_CACHED_EXCHANGE_RATE).isEqualTo("dashboard_cached_exchange_rate")
        assertThat(PreferenceKeys.DASHBOARD_CACHED_PORTFOLIO_SPARKLINE).isEqualTo("dashboard_cached_portfolio_sparkline")
        assertThat(PreferenceKeys.DASHBOARD_CACHED_PORTFOLIO_STATS).isEqualTo("dashboard_cached_portfolio_stats")
        assertThat(PreferenceKeys.DASHBOARD_CACHED_PERIOD_RETURNS).isEqualTo("dashboard_cached_period_returns")
        assertThat(PreferenceKeys.STOCK_SPARKLINE_PERIOD).isEqualTo("stock_sparkline_period")
        assertThat(PreferenceKeys.PORTFOLIO_SUMMARY_PERIOD).isEqualTo("portfolio_summary_period")
        assertThat(PreferenceKeys.SORT_OPTION).isEqualTo("sort_option")
    }

    @Test
    fun `FIRE calculator preference keys - have expected values`() {
        assertThat(PreferenceKeys.FIRE_ANNUAL_RETURN).isEqualTo("fire_annual_return")
        assertThat(PreferenceKeys.FIRE_ANNUAL_INFLATION).isEqualTo("fire_annual_inflation")
        assertThat(PreferenceKeys.FIRE_TARGET_MONTHLY_SPENDING).isEqualTo("fire_target_monthly_spending")
        assertThat(PreferenceKeys.FIRE_SHOW_IN_KRW).isEqualTo("fire_show_in_krw")
    }

    @Test
    fun `household sharing preference keys - have expected values`() {
        assertThat(PreferenceKeys.HOUSEHOLD_CODE).isEqualTo("household_code")
        assertThat(PreferenceKeys.HOUSEHOLD_MY_UID).isEqualTo("household_my_uid")
        assertThat(PreferenceKeys.HOUSEHOLD_MY_LABEL).isEqualTo("household_my_label")
        assertThat(PreferenceKeys.HOUSEHOLD_PARTNER_SYMBOLS_JSON).isEqualTo("household_partner_symbols_json")
        assertThat(PreferenceKeys.HOUSEHOLD_SUMMARY_PERIOD).isEqualTo("household_summary_period")
        assertThat(PreferenceKeys.HOUSEHOLD_SPARKLINE_PERIOD).isEqualTo("household_sparkline_period")
        assertThat(PreferenceKeys.HOUSEHOLD_SORT_OPTION).isEqualTo("household_sort_option")
    }
}
