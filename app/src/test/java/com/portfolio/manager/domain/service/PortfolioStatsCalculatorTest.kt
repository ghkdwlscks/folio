package com.portfolio.manager.domain.service

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.sqrt

class PortfolioStatsCalculatorTest {

    @Test
    fun `calculate - empty list - returns default stats`() {
        val result = PortfolioStatsCalculator.calculate(emptyList())
        assertThat(result.maxDrawdown).isEqualTo(0.0)
        assertThat(result.volatility).isEqualTo(0.0)
        assertThat(result.sharpeRatio).isEqualTo(0.0)
        assertThat(result.bestDay).isEqualTo(0.0)
        assertThat(result.worstDay).isEqualTo(0.0)
    }

    @Test
    fun `calculate - single value - returns default stats`() {
        val result = PortfolioStatsCalculator.calculate(listOf(100.0))
        assertThat(result.maxDrawdown).isEqualTo(0.0)
        assertThat(result.volatility).isEqualTo(0.0)
        assertThat(result.sharpeRatio).isEqualTo(0.0)
        assertThat(result.bestDay).isEqualTo(0.0)
        assertThat(result.worstDay).isEqualTo(0.0)
    }

    @Test
    fun `calculate - constant values - zero volatility and MDD`() {
        val values = listOf(100.0, 100.0, 100.0, 100.0)
        val result = PortfolioStatsCalculator.calculate(values)
        assertThat(result.maxDrawdown).isEqualTo(0.0)
        assertThat(result.volatility).isEqualTo(0.0)
        assertThat(result.bestDay).isEqualTo(0.0)
        assertThat(result.worstDay).isEqualTo(0.0)
    }

    @Test
    fun `calculate - increasing values - calculates correct stats`() {
        val values = listOf(100.0, 110.0, 121.0) // 10% daily gain
        val result = PortfolioStatsCalculator.calculate(values)
        assertThat(result.maxDrawdown).isEqualTo(0.0) // No drawdown when always increasing
        assertThat(result.bestDay).isWithin(0.01).of(10.0)
        assertThat(result.worstDay).isWithin(0.01).of(10.0)
    }

    @Test
    fun `calculate - decreasing values - calculates correct MDD`() {
        val values = listOf(100.0, 80.0, 60.0) // 20% then 25% drops
        val result = PortfolioStatsCalculator.calculate(values)
        assertThat(result.maxDrawdown).isWithin(0.01).of(40.0) // 100 to 60 = 40% drawdown
    }

    @Test
    fun `calculate - recovery after drop - MDD stays at max`() {
        val values = listOf(100.0, 80.0, 100.0) // Drop then recover
        val result = PortfolioStatsCalculator.calculate(values)
        assertThat(result.maxDrawdown).isWithin(0.01).of(20.0) // 100 to 80 = 20% drawdown
    }

    @Test
    fun `calculateDailyReturns - calculates correct percentages`() {
        val values = listOf(100.0, 110.0, 99.0) // +10%, -10%
        val returns = PortfolioStatsCalculator.calculateDailyReturns(values)
        assertThat(returns).hasSize(2)
        assertThat(returns[0]).isWithin(0.01).of(10.0)
        assertThat(returns[1]).isWithin(0.01).of(-10.0)
    }

    @Test
    fun `calculateDailyReturns - zero starting value - returns zero`() {
        val values = listOf(0.0, 100.0)
        val returns = PortfolioStatsCalculator.calculateDailyReturns(values)
        assertThat(returns[0]).isEqualTo(0.0)
    }

    @Test
    fun `calculateDailyReturns - empty list - returns empty`() {
        val returns = PortfolioStatsCalculator.calculateDailyReturns(emptyList())
        assertThat(returns).isEmpty()
    }

    @Test
    fun `calculateMDD - empty list - returns zero`() {
        val result = PortfolioStatsCalculator.calculateMDD(emptyList())
        assertThat(result).isEqualTo(0.0)
    }

    @Test
    fun `calculateMDD - single value - returns zero`() {
        val result = PortfolioStatsCalculator.calculateMDD(listOf(100.0))
        assertThat(result).isEqualTo(0.0)
    }

    @Test
    fun `calculateMDD - new high after drawdown - tracks correctly`() {
        val values = listOf(100.0, 80.0, 120.0, 90.0)
        val result = PortfolioStatsCalculator.calculateMDD(values)
        // Max drawdown is either 100->80 (20%) or 120->90 (25%)
        assertThat(result).isWithin(0.01).of(25.0)
    }

    @Test
    fun `calculateMDD - zero peak - handles gracefully`() {
        val values = listOf(0.0, 0.0, 100.0, 80.0)
        val result = PortfolioStatsCalculator.calculateMDD(values)
        assertThat(result).isWithin(0.01).of(20.0)
    }

    @Test
    fun `calculateVolatility - empty list - returns zero`() {
        val result = PortfolioStatsCalculator.calculateVolatility(emptyList())
        assertThat(result).isEqualTo(0.0)
    }

    @Test
    fun `calculateVolatility - constant returns - zero volatility`() {
        val returns = listOf(5.0, 5.0, 5.0, 5.0)
        val result = PortfolioStatsCalculator.calculateVolatility(returns)
        assertThat(result).isEqualTo(0.0)
    }

    @Test
    fun `calculateVolatility - varying returns - calculates std dev`() {
        val returns = listOf(10.0, -10.0, 10.0, -10.0)
        val result = PortfolioStatsCalculator.calculateVolatility(returns)
        // Mean = 0, variance = 100, std dev = 10
        assertThat(result).isWithin(0.01).of(10.0)
    }

    @Test
    fun `calculateSharpeRatio - empty returns - returns zero`() {
        val result = PortfolioStatsCalculator.calculateSharpeRatio(emptyList(), 0.0)
        assertThat(result).isEqualTo(0.0)
    }

    @Test
    fun `calculateSharpeRatio - zero volatility - returns zero`() {
        val returns = listOf(5.0, 5.0, 5.0)
        val result = PortfolioStatsCalculator.calculateSharpeRatio(returns, 0.0)
        assertThat(result).isEqualTo(0.0)
    }

    @Test
    fun `calculateSharpeRatio - positive returns with volatility - calculates correctly`() {
        // 1% daily return, 1% daily volatility
        val returns = listOf(1.0, 1.0, 1.0, 1.0)
        val volatility = 1.0
        val result = PortfolioStatsCalculator.calculateSharpeRatio(returns, volatility)

        // Annualized return = 1% * 252 = 252%
        // Annualized volatility = 1% * sqrt(252) ≈ 15.87%
        // Sharpe = (252 - 2) / 15.87 ≈ 15.75
        assertThat(result).isGreaterThan(15.0)
    }

    @Test
    fun `calculateSharpeRatio - negative excess returns - returns negative`() {
        // -1% daily return (less than risk-free rate)
        val returns = listOf(-1.0, -1.0, -1.0, -1.0)
        val volatility = 1.0
        val result = PortfolioStatsCalculator.calculateSharpeRatio(returns, volatility)
        assertThat(result).isLessThan(0.0)
    }

    @Test
    fun `calculate - realistic portfolio - all stats calculated`() {
        // Simulate 10 days of trading with some volatility
        val values = listOf(
            10000.0, 10100.0, 9900.0, 10050.0, 10200.0,
            10150.0, 10300.0, 10250.0, 10400.0, 10500.0
        )
        val result = PortfolioStatsCalculator.calculate(values)

        assertThat(result.maxDrawdown).isGreaterThan(0.0)
        assertThat(result.volatility).isGreaterThan(0.0)
        assertThat(result.bestDay).isGreaterThan(0.0)
        assertThat(result.worstDay).isLessThan(0.0)
    }
}
