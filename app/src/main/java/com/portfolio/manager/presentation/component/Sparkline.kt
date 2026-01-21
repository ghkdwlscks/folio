package com.portfolio.manager.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.portfolio.manager.presentation.util.getTrendColor
import kotlin.math.ln

private data class SparklineData(
    val logPrices: List<Double>,
    val minLog: Double,
    val logRange: Double,
    val priceChange: Double
)

@Composable
fun Sparkline(
    prices: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color? = null
) {
    if (prices.size < 2) return

    // Memoize logarithmic transformation to avoid recomputation on recomposition
    val sparklineData = remember(prices) {
        val logPrices = prices.filter { it > 0 }.map { ln(it) }
        if (logPrices.size < 2) return@remember null

        val minLog = logPrices.min()
        val maxLog = logPrices.max()
        SparklineData(
            logPrices = logPrices,
            minLog = minLog,
            logRange = maxLog - minLog,
            priceChange = prices.last() - prices.first()
        )
    } ?: return

    val color = lineColor ?: getTrendColor(sparklineData.priceChange)

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val stepX = width / (sparklineData.logPrices.size - 1)

        val path = Path()
        sparklineData.logPrices.forEachIndexed { index, logPrice ->
            val x = index * stepX
            val normalizedY = if (sparklineData.logRange > 0) {
                (logPrice - sparklineData.minLog) / sparklineData.logRange
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
