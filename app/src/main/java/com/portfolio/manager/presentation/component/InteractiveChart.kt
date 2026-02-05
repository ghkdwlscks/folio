package com.portfolio.manager.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portfolio.manager.presentation.util.CurrencyFormatter
import com.portfolio.manager.presentation.util.getTrendColor

import kotlin.math.ln
import kotlin.math.roundToInt

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class ChartData(
    val logPrices: List<Double>,
    val minLog: Double,
    val logRange: Double,
    val priceChange: Double,
    val prices: List<Double>,
    val timestamps: List<Long>
)

private data class ChartPoint(val x: Float, val y: Float, val index: Int)

data class OverlayLine(
    val prices: List<Double>,
    val timestamps: List<Long>,
    val color: Color,
    val label: String = ""
)

@Composable
fun InteractiveChart(
    prices: List<Double>,
    timestamps: List<Long>,
    currency: String,
    modifier: Modifier = Modifier,
    lineColor: Color? = null,
    overlayLines: List<OverlayLine> = emptyList()
) {
    if (prices.size < 2) return

    val chartData = remember(prices, timestamps, overlayLines) {
        val logPrices = prices.filter { it > 0 }.map { ln(it) }
        if (logPrices.size < 2) return@remember null

        // Include overlay lines in min/max calculation for proper scaling
        val allLogPrices = mutableListOf<Double>()
        allLogPrices.addAll(logPrices)
        overlayLines.forEach { overlay ->
            allLogPrices.addAll(overlay.prices.filter { it > 0 }.map { ln(it) })
        }

        val minLog = allLogPrices.min()
        val maxLog = allLogPrices.max()
        ChartData(
            logPrices = logPrices,
            minLog = minLog,
            logRange = maxLog - minLog,
            priceChange = prices.last() - prices.first(),
            prices = prices,
            timestamps = timestamps
        )
    } ?: return

    val color = lineColor ?: getTrendColor(chartData.priceChange)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var tapX by remember { mutableFloatStateOf(0f) }
    var chartWidth by remember { mutableFloatStateOf(0f) }
    var chartHeight by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val tooltipOffsetPx = with(density) { 8.dp.toPx() }

    // Helper to calculate index from x position
    fun calculateIndexFromX(x: Float, width: Float, dataSize: Int): Int {
        if (dataSize <= 1 || width <= 0) return 0
        val stepX = width / (dataSize - 1)
        return (x / stepX).roundToInt().coerceIn(0, dataSize - 1)
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(chartData) {
                    detectTapGestures { offset ->
                        val index = calculateIndexFromX(offset.x, chartWidth, chartData.logPrices.size)
                        selectedIndex = if (selectedIndex == index) null else index
                        tapX = offset.x
                    }
                }
                .pointerInput(chartData) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val index = calculateIndexFromX(offset.x, chartWidth, chartData.logPrices.size)
                            selectedIndex = index
                            tapX = offset.x
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val index = calculateIndexFromX(change.position.x, chartWidth, chartData.logPrices.size)
                            selectedIndex = index
                            tapX = change.position.x.coerceIn(0f, chartWidth)
                        }
                    )
                }
        ) {
            chartWidth = size.width
            chartHeight = size.height
            val width = size.width
            val height = size.height
            val verticalPadding = height * 0.1f
            val drawHeight = height - (verticalPadding * 2)
            val stepX = width / (chartData.logPrices.size - 1)

            // Calculate all points
            val points = chartData.logPrices.mapIndexed { index, logPrice ->
                val x = index * stepX
                val normalizedY = if (chartData.logRange > 0) {
                    (logPrice - chartData.minLog) / chartData.logRange
                } else {
                    0.5
                }
                val y = verticalPadding + drawHeight - (normalizedY * drawHeight).toFloat()
                ChartPoint(x, y, index)
            }

            // Create smooth curve path
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

            // Draw gradient fill
            if (points.isNotEmpty()) {
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

            // Draw overlay lines (benchmarks) using the same scale as main chart
            overlayLines.forEach { overlay ->
                if (overlay.prices.size >= 2) {
                    val overlayLogPrices = overlay.prices.filter { it > 0 }.map { ln(it) }
                    if (overlayLogPrices.size >= 2) {
                        val overlayPath = Path()
                        val overlayStepX = width / (overlayLogPrices.size - 1)

                        overlayLogPrices.forEachIndexed { index, logPrice ->
                            // Use main chart's scale (minLog, logRange) for alignment
                            val normalizedY = if (chartData.logRange > 0) {
                                (logPrice - chartData.minLog) / chartData.logRange
                            } else {
                                0.5
                            }
                            val x = index * overlayStepX
                            val y = verticalPadding + drawHeight - (normalizedY * drawHeight).toFloat()

                            if (index == 0) {
                                overlayPath.moveTo(x, y)
                            } else {
                                overlayPath.lineTo(x, y)
                            }
                        }

                        drawPath(
                            path = overlayPath,
                            color = overlay.color,
                            style = Stroke(
                                width = 1.5f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                            )
                        )
                    }
                }
            }

            // Draw crosshair and highlight point if selected
            selectedIndex?.let { index ->
                if (index < points.size) {
                    val point = points[index]

                    // Vertical crosshair line
                    drawLine(
                        color = color.copy(alpha = 0.5f),
                        start = Offset(point.x, 0f),
                        end = Offset(point.x, height),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )

                    // Horizontal crosshair line
                    drawLine(
                        color = color.copy(alpha = 0.5f),
                        start = Offset(0f, point.y),
                        end = Offset(width, point.y),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )

                    // Outer glow
                    drawCircle(
                        color = color.copy(alpha = 0.2f),
                        radius = 16f,
                        center = Offset(point.x, point.y)
                    )
                    drawCircle(
                        color = color.copy(alpha = 0.1f),
                        radius = 24f,
                        center = Offset(point.x, point.y)
                    )
                    // Highlight circle
                    drawCircle(
                        color = Color.White,
                        radius = 8f,
                        center = Offset(point.x, point.y)
                    )
                    drawCircle(
                        color = color,
                        radius = 6f,
                        center = Offset(point.x, point.y)
                    )
                }
            }
        }

        // Tooltip
        selectedIndex?.let { index ->
            if (index < chartData.prices.size && index < chartData.timestamps.size) {
                val price = chartData.prices[index]
                val timestamp = chartData.timestamps[index]
                val date = Instant.ofEpochSecond(timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .format(dateFormatter)

                val formattedPrice = CurrencyFormatter.format(price, currency)

                // Get benchmark values by matching timestamp
                val benchmarkValues = overlayLines.mapNotNull { overlay ->
                    if (overlay.prices.size >= 2 && overlay.timestamps.isNotEmpty()) {
                        // Find the benchmark index with matching or nearest timestamp
                        val benchmarkIndex = overlay.timestamps.indexOfFirst { it >= timestamp }
                            .takeIf { it >= 0 } ?: (overlay.timestamps.size - 1)
                        Triple(overlay.label, overlay.prices[benchmarkIndex], overlay.color)
                    } else null
                }

                // Position tooltip - avoid edges
                val tooltipX = when {
                    tapX < chartWidth * 0.3f -> tapX + tooltipOffsetPx
                    tapX > chartWidth * 0.7f -> tapX - tooltipOffsetPx - with(density) { 120.dp.toPx() }
                    else -> tapX - with(density) { 60.dp.toPx() }
                }

                Column(
                    modifier = Modifier
                        .offset { IntOffset(tooltipX.roundToInt(), with(density) { 8.dp.toPx() }.roundToInt()) }
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formattedPrice,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Show benchmark values
                    benchmarkValues.forEach { (label, value, benchColor) ->
                        val displayLabel = when (label) {
                            "^GSPC" -> "S&P"
                            "^KS11" -> "KOSPI"
                            else -> label
                        }
                        Text(
                            text = "$displayLabel: ${CurrencyFormatter.format(value, currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = benchColor
                        )
                    }
                    Text(
                        text = date,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
