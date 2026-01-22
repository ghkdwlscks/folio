package com.portfolio.manager.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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

private data class SparklinePoint(val x: Float, val y: Float)

@Composable
fun Sparkline(
    prices: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color? = null,
    showGradientFill: Boolean = true,
    showEndDot: Boolean = true
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
        val verticalPadding = height * 0.1f
        val drawHeight = height - (verticalPadding * 2)
        val stepX = width / (sparklineData.logPrices.size - 1)

        // Calculate all points
        val points = sparklineData.logPrices.mapIndexed { index, logPrice ->
            val x = index * stepX
            val normalizedY = if (sparklineData.logRange > 0) {
                (logPrice - sparklineData.minLog) / sparklineData.logRange
            } else {
                0.5
            }
            val y = verticalPadding + drawHeight - (normalizedY * drawHeight).toFloat()
            SparklinePoint(x, y)
        }

        // Create smooth curve path using quadratic bezier
        val linePath = Path()
        points.forEachIndexed { index, point ->
            if (index == 0) {
                linePath.moveTo(point.x, point.y)
            } else {
                val prevPoint = points[index - 1]
                val controlX = (prevPoint.x + point.x) / 2
                linePath.quadraticTo(prevPoint.x + (controlX - prevPoint.x) * 0.5f, prevPoint.y,
                    controlX, (prevPoint.y + point.y) / 2)
                linePath.quadraticTo(point.x - (point.x - controlX) * 0.5f, point.y,
                    point.x, point.y)
            }
        }

        // Draw gradient fill under the line
        if (showGradientFill && points.isNotEmpty()) {
            val fillPath = Path()
            fillPath.addPath(linePath)
            fillPath.lineTo(points.last().x, height)
            fillPath.lineTo(points.first().x, height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.3f),
                        color.copy(alpha = 0.0f)
                    )
                )
            )
        }

        // Draw the line
        drawPath(
            path = linePath,
            color = color,
            style = Stroke(
                width = 2.5f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Draw end point dot
        if (showEndDot && points.isNotEmpty()) {
            val lastPoint = points.last()
            drawCircle(
                color = color,
                radius = 4f,
                center = Offset(lastPoint.x, lastPoint.y)
            )
        }
    }
}
