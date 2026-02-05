package com.portfolio.manager.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.portfolio.manager.presentation.theme.AppAnimations
import com.portfolio.manager.presentation.util.CurrencyFormatter
import kotlin.math.abs

/**
 * TwoWayConverter for Double to preserve precision for large currency values.
 * Float has only ~7 digits of precision, which causes display errors for values
 * like 150,234,567 KRW. Using Double preserves ~15 digits of precision.
 */
private val DoubleToVector: TwoWayConverter<Double, AnimationVector1D> =
    TwoWayConverter(
        convertToVector = { AnimationVector1D(it.toFloat()) },
        convertFromVector = { it.value.toDouble() }
    )

/** Threshold for "significant" change that triggers pulse (1% change) */
private const val SIGNIFICANT_CHANGE_THRESHOLD = 0.01

@Composable
fun AnimatedCurrencyCounter(
    targetValue: Double,
    showInKrw: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.White,
    durationMillis: Int = 800,
    enablePulse: Boolean = true
) {
    var previousValue by remember { mutableDoubleStateOf(targetValue) }
    // Use Double directly to preserve precision for large values (especially KRW)
    val animatable = remember { Animatable(targetValue, DoubleToVector) }

    // Scale animation for pulse effect on significant changes
    val scaleAnimatable = remember { Animatable(1f) }

    LaunchedEffect(targetValue) {
        // Only animate if value actually changed
        if (previousValue != targetValue) {
            // Check if change is significant (>1%) for pulse effect
            val changePercent = if (previousValue != 0.0) {
                abs(targetValue - previousValue) / abs(previousValue)
            } else 0.0

            val isSignificantChange = changePercent > SIGNIFICANT_CHANGE_THRESHOLD

            // Start pulse animation for significant changes
            if (enablePulse && isSignificantChange) {
                scaleAnimatable.animateTo(
                    targetValue = 1.05f,
                    animationSpec = AppAnimations.Springs.Snappy
                )
                scaleAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = AppAnimations.Springs.Snappy
                )
            }

            animatable.animateTo(
                targetValue = targetValue,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = FastOutSlowInEasing
                )
            )
            previousValue = targetValue
        }
    }

    val displayValue = animatable.value
    val formattedValue = if (showInKrw) {
        CurrencyFormatter.formatKrw(displayValue)
    } else {
        CurrencyFormatter.formatUsd(displayValue)
    }

    Text(
        text = formattedValue,
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier.scale(scaleAnimatable.value)
    )
}

@Composable
fun AnimatedPercentCounter(
    targetValue: Double,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.White,
    prefix: String = "",
    suffix: String = "%",
    durationMillis: Int = 600,
    enablePulse: Boolean = true
) {
    var previousValue by remember { mutableDoubleStateOf(targetValue) }
    // Use Double directly to preserve precision
    val animatable = remember { Animatable(targetValue, DoubleToVector) }

    // Scale animation for pulse effect on significant changes
    val scaleAnimatable = remember { Animatable(1f) }

    LaunchedEffect(targetValue) {
        if (previousValue != targetValue) {
            // Check if change is significant (>1 percentage point) for pulse effect
            val changeAmount = abs(targetValue - previousValue)
            val isSignificantChange = changeAmount > 1.0

            // Start pulse animation for significant changes
            if (enablePulse && isSignificantChange) {
                scaleAnimatable.animateTo(
                    targetValue = 1.08f,
                    animationSpec = AppAnimations.Springs.Snappy
                )
                scaleAnimatable.animateTo(
                    targetValue = 1f,
                    animationSpec = AppAnimations.Springs.Snappy
                )
            }

            animatable.animateTo(
                targetValue = targetValue,
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = FastOutSlowInEasing
                )
            )
            previousValue = targetValue
        }
    }

    val displayValue = animatable.value
    val sign = if (displayValue >= 0 && prefix.isEmpty()) "+" else prefix

    Text(
        text = "$sign${CurrencyFormatter.formatPercent(displayValue)}$suffix",
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier.scale(scaleAnimatable.value)
    )
}
