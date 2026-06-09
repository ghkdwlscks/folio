package com.portfolio.manager.domain.model

import kotlinx.serialization.Serializable

/**
 * FIRE-calculator assumptions shared across a paired household, stored remotely
 * so both members see and edit the same group settings. Personal (non-group)
 * settings stay in each device's local preferences. The display-currency toggle
 * is intentionally excluded — it is a per-viewer preference, not a shared one.
 */
@Serializable
data class GroupFireSettings(
    val annualReturn: Double,
    val annualInflation: Double,
    val targetMonthlySpending: Double,
    val targetSpendingInKrw: Boolean
)
