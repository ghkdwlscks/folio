package com.portfolio.manager.presentation.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.portfolio.manager.presentation.theme.GainGreen
import com.portfolio.manager.presentation.theme.GainGreenLight
import com.portfolio.manager.presentation.theme.GainGreenPastel
import com.portfolio.manager.presentation.theme.LossRed
import com.portfolio.manager.presentation.theme.LossRedLight
import com.portfolio.manager.presentation.theme.LossRedPastel
import com.portfolio.manager.presentation.theme.NeutralGray
import com.portfolio.manager.presentation.theme.NeutralGrayLight

/**
 * Represents a trend indicator with its associated visual properties.
 */
data class TrendIndicator(
    val isGain: Boolean,
    val color: Color,
    val backgroundColor: Color,
    val icon: ImageVector
)

/**
 * Determines if a value represents a gain (strictly positive).
 */
fun isGain(value: Double): Boolean = value > 0

/**
 * Gets the trend color for a given value.
 * Use pastel variant for dark backgrounds (e.g., portfolio summary card).
 */
fun getTrendColor(value: Double, usePastel: Boolean = false): Color {
    return when {
        value > 0 -> if (usePastel) GainGreenPastel else GainGreen
        value < 0 -> if (usePastel) LossRedPastel else LossRed
        else -> NeutralGray
    }
}

/**
 * Gets the trend background color for a given value.
 */
fun getTrendBackgroundColor(value: Double): Color {
    return when {
        value > 0 -> GainGreenLight
        value < 0 -> LossRedLight
        else -> NeutralGrayLight
    }
}

/**
 * Gets the trend icon based on whether the value represents a gain.
 */
fun getTrendIcon(value: Double): ImageVector {
    return if (value >= 0) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown
}

/**
 * Creates a complete TrendIndicator with configurable color scheme.
 * @param value The value to determine gain/loss/neutral
 * @param usePastel Use pastel colors for dark backgrounds (e.g., portfolio summary card)
 */
fun createTrendIndicator(value: Double, usePastel: Boolean = false): TrendIndicator {
    val gain = value > 0
    val (color, backgroundColor) = when {
        value > 0 -> if (usePastel) {
            GainGreenPastel to GainGreenPastel.copy(alpha = 0.3f)
        } else {
            GainGreen to GainGreenLight
        }
        value < 0 -> if (usePastel) {
            LossRedPastel to LossRedPastel.copy(alpha = 0.3f)
        } else {
            LossRed to LossRedLight
        }
        else -> if (usePastel) {
            NeutralGray to NeutralGray.copy(alpha = 0.3f)
        } else {
            NeutralGray to NeutralGrayLight
        }
    }

    return TrendIndicator(
        isGain = gain,
        color = color,
        backgroundColor = backgroundColor,
        icon = if (value >= 0) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown
    )
}
