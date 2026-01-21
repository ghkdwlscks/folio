package com.portfolio.manager.domain.service

import com.portfolio.manager.domain.model.PortfolioStats
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Service for calculating portfolio statistics from historical values.
 */
object PortfolioStatsCalculator {

    private const val TRADING_DAYS_PER_YEAR = 252
    private const val ANNUAL_RISK_FREE_RATE = 0.02 // 2% annual risk-free rate

    /**
     * Calculates comprehensive portfolio statistics from a list of historical portfolio values.
     * @param portfolioValues List of portfolio values over time (at least 2 values required)
     * @return PortfolioStats containing MDD, volatility, Sharpe ratio, best/worst day
     */
    fun calculate(portfolioValues: List<Double>): PortfolioStats {
        if (portfolioValues.size < 2) return PortfolioStats()

        val dailyReturns = calculateDailyReturns(portfolioValues)
        val volatility = calculateVolatility(dailyReturns)

        return PortfolioStats(
            maxDrawdown = calculateMDD(portfolioValues),
            volatility = volatility,
            sharpeRatio = calculateSharpeRatio(dailyReturns, volatility),
            bestDay = dailyReturns.maxOrNull() ?: 0.0,
            worstDay = dailyReturns.minOrNull() ?: 0.0
        )
    }

    /**
     * Calculates daily returns as percentages from portfolio values.
     */
    fun calculateDailyReturns(values: List<Double>): List<Double> {
        return values.zipWithNext { a, b ->
            if (a > 0) ((b - a) / a) * 100 else 0.0
        }
    }

    /**
     * Calculates Maximum Drawdown (MDD) as a percentage.
     * MDD measures the largest peak-to-trough decline in portfolio value.
     */
    fun calculateMDD(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        var maxDrawdown = 0.0
        var peak = values[0]

        for (value in values) {
            if (value > peak) peak = value
            if (peak > 0) {
                val drawdown = (peak - value) / peak * 100
                if (drawdown > maxDrawdown) maxDrawdown = drawdown
            }
        }
        return maxDrawdown
    }

    /**
     * Calculates volatility (standard deviation) of daily returns.
     */
    fun calculateVolatility(dailyReturns: List<Double>): Double {
        if (dailyReturns.isEmpty()) return 0.0
        val mean = dailyReturns.average()
        val variance = dailyReturns.map { (it - mean).pow(2) }.average()
        return sqrt(variance)
    }

    /**
     * Calculates Sharpe Ratio (annualized risk-adjusted return).
     * Uses pre-calculated volatility to avoid duplicate computation.
     */
    fun calculateSharpeRatio(dailyReturns: List<Double>, dailyVolatility: Double): Double {
        if (dailyReturns.isEmpty()) return 0.0
        val avgDailyReturn = dailyReturns.average()

        val annualizedReturn = avgDailyReturn * TRADING_DAYS_PER_YEAR
        val annualizedVolatility = dailyVolatility * sqrt(TRADING_DAYS_PER_YEAR.toDouble())

        return if (annualizedVolatility > 0) {
            (annualizedReturn - ANNUAL_RISK_FREE_RATE) / annualizedVolatility
        } else {
            0.0
        }
    }
}
