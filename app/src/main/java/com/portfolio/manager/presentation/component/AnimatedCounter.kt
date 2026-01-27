package com.portfolio.manager.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
    val animatable = remember { Animatable(targetValue.toFloat()) }

    LaunchedEffect(targetValue) {
        // Only animate if value actually changed
        if (previousValue != targetValue) {
            animatable.animateTo(
                targetValue = targetValue.toFloat(),
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = FastOutSlowInEasing
                )
            )
            previousValue = targetValue
        }
    }

    val displayValue = animatable.value.toDouble()
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
    val animatable = remember { Animatable(targetValue.toFloat()) }

    LaunchedEffect(targetValue) {
        if (previousValue != targetValue) {
            animatable.animateTo(
                targetValue = targetValue.toFloat(),
                animationSpec = tween(
                    durationMillis = durationMillis,
                    easing = FastOutSlowInEasing
                )
            )
            previousValue = targetValue
        }
    }

    val displayValue = animatable.value.toDouble()
    val sign = if (displayValue >= 0 && prefix.isEmpty()) "+" else prefix

    Text(
        text = "$sign${CurrencyFormatter.formatPercent(displayValue)}$suffix",
        style = style,
        fontWeight = fontWeight,
        color = color,
        modifier = modifier
    )
}
