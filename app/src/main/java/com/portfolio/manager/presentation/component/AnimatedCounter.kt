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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.portfolio.manager.presentation.util.CurrencyFormatter

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

@Composable
fun AnimatedCurrencyCounter(
    targetValue: Double,
    showInKrw: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayMedium,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = Color.White,
    durationMillis: Int = 800
) {
    var previousValue by remember { mutableDoubleStateOf(targetValue) }
    // Use Double directly to preserve precision for large values (especially KRW)
    val animatable = remember { Animatable(targetValue, DoubleToVector) }

    LaunchedEffect(targetValue) {
        // Only animate if value actually changed
        if (previousValue != targetValue) {
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
        modifier = modifier
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
    durationMillis: Int = 600
) {
    var previousValue by remember { mutableDoubleStateOf(targetValue) }
    // Use Double directly to preserve precision
    val animatable = remember { Animatable(targetValue, DoubleToVector) }

    LaunchedEffect(targetValue) {
        if (previousValue != targetValue) {
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
        modifier = modifier
    )
}
