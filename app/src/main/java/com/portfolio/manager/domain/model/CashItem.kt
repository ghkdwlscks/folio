package com.portfolio.manager.domain.model

import kotlinx.serialization.Serializable

import com.portfolio.manager.domain.util.CurrencyConverter

@Serializable
data class CashItem(
    val id: Long,
    val accountId: Long,
    val name: String,
    val originalValue: Double,
    val annualYieldRate: Double,
    override val currency: String,
    val createdAt: Long
) : PortfolioItem {
    /**
     * Get the value in USD.
     */
    override fun valueInUsd(exchangeRate: Double): Double =
        CurrencyConverter.toUsd(originalValue, currency, exchangeRate)

    /**
     * Get the value in KRW.
     */
    override fun valueInKrw(exchangeRate: Double): Double =
        CurrencyConverter.toKrw(originalValue, currency, exchangeRate)

    /**
     * Calculate period return based on the annual yield rate.
     * This returns the pro-rata yield for the given period.
     */
    fun periodReturnPercent(period: TimePeriod): Double {
        return when (period) {
            TimePeriod.ONE_WEEK -> annualYieldRate / 52
            TimePeriod.ONE_MONTH -> annualYieldRate / 12
            TimePeriod.THREE_MONTHS -> annualYieldRate / 4
            TimePeriod.SIX_MONTHS -> annualYieldRate / 2
            TimePeriod.ONE_YEAR -> annualYieldRate
        }
    }
}
