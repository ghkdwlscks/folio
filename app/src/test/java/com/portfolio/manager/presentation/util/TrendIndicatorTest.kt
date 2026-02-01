package com.portfolio.manager.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.presentation.theme.GainGreen
import com.portfolio.manager.presentation.theme.GainGreenLight
import com.portfolio.manager.presentation.theme.GainGreenPastel
import com.portfolio.manager.presentation.theme.LossRed
import com.portfolio.manager.presentation.theme.LossRedLight
import com.portfolio.manager.presentation.theme.LossRedPastel
import com.portfolio.manager.presentation.theme.NeutralGray
import com.portfolio.manager.presentation.theme.NeutralGrayLight
import org.junit.Test

class TrendIndicatorTest {

    @Test
    fun `isGain - positive value - returns true`() {
        assertThat(isGain(100.0)).isTrue()
    }

    @Test
    fun `isGain - zero value - returns false`() {
        assertThat(isGain(0.0)).isFalse()
    }

    @Test
    fun `isGain - negative value - returns false`() {
        assertThat(isGain(-50.0)).isFalse()
    }

    @Test
    fun `getTrendColor - positive value - returns GainGreen`() {
        assertThat(getTrendColor(100.0)).isEqualTo(GainGreen)
    }

    @Test
    fun `getTrendColor - negative value - returns LossRed`() {
        assertThat(getTrendColor(-50.0)).isEqualTo(LossRed)
    }

    @Test
    fun `getTrendColor - zero value - returns NeutralGray`() {
        assertThat(getTrendColor(0.0)).isEqualTo(NeutralGray)
    }

    @Test
    fun `getTrendColor - positive value with pastel - returns GainGreenPastel`() {
        assertThat(getTrendColor(100.0, usePastel = true)).isEqualTo(GainGreenPastel)
    }

    @Test
    fun `getTrendColor - negative value with pastel - returns LossRedPastel`() {
        assertThat(getTrendColor(-50.0, usePastel = true)).isEqualTo(LossRedPastel)
    }

    @Test
    fun `getTrendColor - zero value with pastel - returns NeutralGray`() {
        assertThat(getTrendColor(0.0, usePastel = true)).isEqualTo(NeutralGray)
    }

    @Test
    fun `getTrendBackgroundColor - positive value - returns GainGreenLight`() {
        assertThat(getTrendBackgroundColor(100.0)).isEqualTo(GainGreenLight)
    }

    @Test
    fun `getTrendBackgroundColor - negative value - returns LossRedLight`() {
        assertThat(getTrendBackgroundColor(-50.0)).isEqualTo(LossRedLight)
    }

    @Test
    fun `getTrendBackgroundColor - zero value - returns NeutralGrayLight`() {
        assertThat(getTrendBackgroundColor(0.0)).isEqualTo(NeutralGrayLight)
    }

    @Test
    fun `getTrendIcon - positive value - returns TrendingUp`() {
        assertThat(getTrendIcon(100.0)).isEqualTo(Icons.Rounded.TrendingUp)
    }

    @Test
    fun `getTrendIcon - negative value - returns TrendingDown`() {
        assertThat(getTrendIcon(-50.0)).isEqualTo(Icons.Rounded.TrendingDown)
    }

    @Test
    fun `getTrendIcon - zero value - returns TrendingUp`() {
        assertThat(getTrendIcon(0.0)).isEqualTo(Icons.Rounded.TrendingUp)
    }

    @Test
    fun `createTrendIndicator - positive value - returns correct indicator`() {
        val indicator = createTrendIndicator(100.0)

        assertThat(indicator.isGain).isTrue()
        assertThat(indicator.color).isEqualTo(GainGreen)
        assertThat(indicator.backgroundColor).isEqualTo(GainGreenLight)
        assertThat(indicator.icon).isEqualTo(Icons.Rounded.TrendingUp)
    }

    @Test
    fun `createTrendIndicator - negative value - returns correct indicator`() {
        val indicator = createTrendIndicator(-50.0)

        assertThat(indicator.isGain).isFalse()
        assertThat(indicator.color).isEqualTo(LossRed)
        assertThat(indicator.backgroundColor).isEqualTo(LossRedLight)
        assertThat(indicator.icon).isEqualTo(Icons.Rounded.TrendingDown)
    }

    @Test
    fun `createTrendIndicator - zero value - returns neutral indicator`() {
        val indicator = createTrendIndicator(0.0)

        assertThat(indicator.isGain).isFalse()
        assertThat(indicator.color).isEqualTo(NeutralGray)
        assertThat(indicator.backgroundColor).isEqualTo(NeutralGrayLight)
        assertThat(indicator.icon).isEqualTo(Icons.Rounded.TrendingUp)
    }

    @Test
    fun `createTrendIndicator - usePastel true - positive value - returns pastel indicator`() {
        val indicator = createTrendIndicator(100.0, usePastel = true)

        assertThat(indicator.isGain).isTrue()
        assertThat(indicator.color).isEqualTo(GainGreenPastel)
        assertThat(indicator.backgroundColor).isEqualTo(GainGreenPastel.copy(alpha = 0.3f))
        assertThat(indicator.icon).isEqualTo(Icons.Rounded.TrendingUp)
    }

    @Test
    fun `createTrendIndicator - usePastel true - negative value - returns pastel indicator`() {
        val indicator = createTrendIndicator(-50.0, usePastel = true)

        assertThat(indicator.isGain).isFalse()
        assertThat(indicator.color).isEqualTo(LossRedPastel)
        assertThat(indicator.backgroundColor).isEqualTo(LossRedPastel.copy(alpha = 0.3f))
        assertThat(indicator.icon).isEqualTo(Icons.Rounded.TrendingDown)
    }

    @Test
    fun `createTrendIndicator - usePastel true - zero value - returns neutral indicator`() {
        val indicator = createTrendIndicator(0.0, usePastel = true)

        assertThat(indicator.isGain).isFalse()
        assertThat(indicator.color).isEqualTo(NeutralGray)
        assertThat(indicator.backgroundColor).isEqualTo(NeutralGray.copy(alpha = 0.3f))
        assertThat(indicator.icon).isEqualTo(Icons.Rounded.TrendingUp)
    }
}
