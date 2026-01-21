package com.portfolio.manager.domain.model

/**
 * FIRE (Financial Independence, Retire Early) calculation results.
 */
data class FIRECalculation(
    val totalPortfolioValue: Double,
    val annualReturn: Double,
    val annualInflation: Double,
    val realReturn: Double,
    val sustainableMonthlySpending: Double,
    val sustainableAnnualSpending: Double
)

/**
 * FIRE target calculation results.
 */
data class FIRETargetCalculation(
    val targetMonthlySpending: Double,
    val requiredPortfolio: Double,
    val currentPortfolio: Double,
    val progressPercent: Double,
    val remainingAmount: Double
)
