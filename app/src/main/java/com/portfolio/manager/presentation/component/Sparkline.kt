package com.portfolio.manager.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.portfolio.manager.presentation.theme.GainGreen
import com.portfolio.manager.presentation.theme.LossRed

@Composable
fun Sparkline(
    prices: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color? = null
) {
    if (prices.size < 2) return

    val minPrice = prices.min()
    val maxPrice = prices.max()
    val priceRange = maxPrice - minPrice
    val isGain = prices.last() >= prices.first()
    val color = lineColor ?: if (isGain) GainGreen else LossRed

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val stepX = width / (prices.size - 1)

        val path = Path()
        prices.forEachIndexed { index, price ->
            val x = index * stepX
            val normalizedY = if (priceRange > 0) {
                (price - minPrice) / priceRange
            } else {
                0.5
            }
            val y = height - (normalizedY * height).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
