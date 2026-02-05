package com.portfolio.manager.presentation.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
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
    showEndDot: Boolean = true,
    onClick: (() -> Unit)? = null,
    animateOnFirstAppearance: Boolean = true
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

    // Draw animation progress (0 to 1) - clips from left to reveal the line
    val drawProgress = remember { Animatable(if (animateOnFirstAppearance) 0f else 1f) }

    // Animate on first appearance when prices change
    LaunchedEffect(prices) {
        if (animateOnFirstAppearance && drawProgress.value < 1f) {
            drawProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing
                )
            )
        } else if (!animateOnFirstAppearance) {
            drawProgress.snapTo(1f)
        }
    }

    val color = lineColor ?: getTrendColor(sparklineData.priceChange)
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
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
                    linePath.quadraticTo(
                        prevPoint.x + (controlX - prevPoint.x) * 0.5f, prevPoint.y,
                        controlX, (prevPoint.y + point.y) / 2
                    )
                    linePath.quadraticTo(
                        point.x - (point.x - controlX) * 0.5f, point.y,
                        point.x, point.y
                    )
                }
            }

            // Animated clip width for draw-in effect
            val animatedWidth = width * drawProgress.value

            clipRect(right = animatedWidth) {
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
            }

            // Draw end point dot (appears at end of animation)
            if (showEndDot && points.isNotEmpty() && drawProgress.value >= 1f) {
                val lastPoint = points.last()
                drawCircle(
                    color = color,
                    radius = 4f,
                    center = Offset(lastPoint.x, lastPoint.y)
                )
            }
        }
    }
}
