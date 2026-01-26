package com.portfolio.manager.domain.model

import com.portfolio.manager.util.AppConstants.KRW_TO_USD_RATE
import kotlinx.serialization.Serializable

@Serializable
data class CashItem(
    val id: Long,
    val accountId: Long,
    val name: String,
    val originalValue: Double,
    val annualYieldRate: Double,
    val currency: String,
    val createdAt: Long
) {
    /**
     * Get the value in USD.
     */
    fun valueInUsd(exchangeRate: Double): Double {
        return if (currency == "KRW") originalValue / exchangeRate else originalValue
    }

    /**
     * Get the value in KRW.
     */
    fun valueInKrw(exchangeRate: Double): Double {
        return if (currency == "USD") originalValue * exchangeRate else originalValue
    }

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

    companion object {
        fun fromEntity(entity: com.portfolio.manager.data.local.CashItemEntity): CashItem {
            return CashItem(
                id = entity.id,
                accountId = entity.accountId,
                name = entity.name,
                originalValue = entity.originalValue,
                annualYieldRate = entity.annualYieldRate,
                currency = entity.currency,
                createdAt = entity.createdAt
            )
        }
    }
}
