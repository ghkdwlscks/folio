package com.portfolio.manager.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PortfolioStatsTest {

    @Test
    fun `default values - all zeros`() {
        val stats = PortfolioStats()

        assertThat(stats.maxDrawdown).isEqualTo(0.0)
        assertThat(stats.volatility).isEqualTo(0.0)
        assertThat(stats.sharpeRatio).isEqualTo(0.0)
        assertThat(stats.bestDay).isEqualTo(0.0)
        assertThat(stats.worstDay).isEqualTo(0.0)
    }

    @Test
    fun `custom values - stored correctly`() {
        val stats = PortfolioStats(
            maxDrawdown = 15.5,
            volatility = 2.3,
            sharpeRatio = 1.2,
            bestDay = 5.0,
            worstDay = -3.5
        )

        assertThat(stats.maxDrawdown).isEqualTo(15.5)
        assertThat(stats.volatility).isEqualTo(2.3)
        assertThat(stats.sharpeRatio).isEqualTo(1.2)
        assertThat(stats.bestDay).isEqualTo(5.0)
        assertThat(stats.worstDay).isEqualTo(-3.5)
    }

    @Test
    fun `copy - creates new instance with modified values`() {
        val original = PortfolioStats(maxDrawdown = 10.0)
        val copied = original.copy(volatility = 5.0)

        assertThat(copied.maxDrawdown).isEqualTo(10.0)
        assertThat(copied.volatility).isEqualTo(5.0)
    }

    @Test
    fun `equals - same values are equal`() {
        val stats1 = PortfolioStats(maxDrawdown = 10.0, volatility = 2.0)
        val stats2 = PortfolioStats(maxDrawdown = 10.0, volatility = 2.0)

        assertThat(stats1).isEqualTo(stats2)
    }

    @Test
    fun `equals - different values are not equal`() {
        val stats1 = PortfolioStats(maxDrawdown = 10.0)
        val stats2 = PortfolioStats(maxDrawdown = 15.0)

        assertThat(stats1).isNotEqualTo(stats2)
    }
}
