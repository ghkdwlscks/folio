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
 * Determines if a value represents a gain (positive or zero).
 */
fun isGain(value: Double): Boolean = value >= 0

/**
 * Gets the trend color for a given value.
 * Use pastel variant for dark backgrounds (e.g., portfolio summary card).
 */
fun getTrendColor(value: Double, usePastel: Boolean = false): Color {
    return if (value >= 0) {
        if (usePastel) GainGreenPastel else GainGreen
    } else {
        if (usePastel) LossRedPastel else LossRed
    }
}

/**
 * Gets the trend background color for a given value.
 */
fun getTrendBackgroundColor(value: Double): Color {
    return if (value >= 0) GainGreenLight else LossRedLight
}

/**
 * Gets the trend icon based on whether the value represents a gain.
 */
fun getTrendIcon(value: Double): ImageVector {
    return if (value >= 0) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown
}

/**
 * Creates a complete TrendIndicator with configurable color scheme.
 * @param value The value to determine gain/loss
 * @param usePastel Use pastel colors for dark backgrounds (e.g., portfolio summary card)
 */
fun createTrendIndicator(value: Double, usePastel: Boolean = false): TrendIndicator {
    val gain = value >= 0
    val (color, backgroundColor) = if (usePastel) {
        val pastelColor = if (gain) GainGreenPastel else LossRedPastel
        pastelColor to pastelColor.copy(alpha = 0.3f)
    } else {
        (if (gain) GainGreen else LossRed) to (if (gain) GainGreenLight else LossRedLight)
    }

    return TrendIndicator(
        isGain = gain,
        color = color,
        backgroundColor = backgroundColor,
        icon = if (gain) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown
    )
}
