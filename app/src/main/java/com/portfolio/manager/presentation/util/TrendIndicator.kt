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
 * Creates a complete TrendIndicator for standard UI contexts (light backgrounds).
 */
fun createTrendIndicator(value: Double): TrendIndicator {
    val gain = value >= 0
    return TrendIndicator(
        isGain = gain,
        color = if (gain) GainGreen else LossRed,
        backgroundColor = if (gain) GainGreenLight else LossRedLight,
        icon = if (gain) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown
    )
}

/**
 * Creates a TrendIndicator for dark backgrounds using pastel colors.
 */
fun createPastelTrendIndicator(value: Double): TrendIndicator {
    val gain = value >= 0
    return TrendIndicator(
        isGain = gain,
        color = if (gain) GainGreenPastel else LossRedPastel,
        backgroundColor = if (gain) GainGreenPastel.copy(alpha = 0.3f) else LossRedPastel.copy(alpha = 0.3f),
        icon = if (gain) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown
    )
}
