package com.portfolio.manager.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.portfolio.manager.presentation.util.getTrendColor
import kotlin.math.ln

@Composable
fun Sparkline(
    prices: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color? = null
) {
    if (prices.size < 2) return

    // Use logarithmic scale for better visualization of percentage changes
    val logPrices = prices.filter { it > 0 }.map { ln(it) }
    if (logPrices.size < 2) return

    val minLog = logPrices.min()
    val maxLog = logPrices.max()
    val logRange = maxLog - minLog
    val priceChange = prices.last() - prices.first()
    val color = lineColor ?: getTrendColor(priceChange)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val stepX = width / (logPrices.size - 1)

        val path = Path()
        logPrices.forEachIndexed { index, logPrice ->
            val x = index * stepX
            val normalizedY = if (logRange > 0) {
                (logPrice - minLog) / logRange
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
