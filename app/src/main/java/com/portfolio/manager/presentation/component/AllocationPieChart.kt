package com.portfolio.manager.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.portfolio.manager.presentation.theme.ChartColors
import com.portfolio.manager.presentation.util.CurrencyFormatter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp

data class AllocationItem(
    val symbol: String,
    val name: String,
    val value: Double,
    val weight: Double
)

@Composable
fun AllocationPieChart(
    items: List<AllocationItem>,
    totalValue: Double,
    showInKrw: Boolean,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Allocation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pie chart
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DonutChart(
                        items = items,
                        modifier = Modifier.size(120.dp)
                    )
                    val formattedValue = if (showInKrw) {
                        CurrencyFormatter.formatKrw(totalValue)
                    } else {
                        CurrencyFormatter.formatUsd(totalValue)
                    }
                    AutoSizeText(
                        text = formattedValue,
                        maxWidth = 72.dp,  // Inner circle width (120 - 24*2 stroke)
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = (-0.5).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Legend
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items.take(5).forEachIndexed { index, item ->
                        LegendItem(
                            color = ChartColors[index % ChartColors.size],
                            label = item.name,
                            weight = item.weight
                        )
                    }
                    if (items.size > 5) {
                        val othersWeight = items.drop(5).sumOf { it.weight }
                        LegendItem(
                            color = ChartColors[5 % ChartColors.size],
                            label = "Others (${items.size - 5})",
                            weight = othersWeight
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    items: List<AllocationItem>,
    modifier: Modifier = Modifier
) {
    val sweepAngles = if (items.size <= 5) {
        items.map { (it.weight / 100f * 360f).toFloat() }
    } else {
        val top5 = items.take(5).map { (it.weight / 100f * 360f).toFloat() }
        val othersWeight = items.drop(5).sumOf { it.weight }
        top5 + (othersWeight / 100f * 360f).toFloat()
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 24.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        var startAngle = -90f

        sweepAngles.forEachIndexed { index, sweepAngle ->
            drawArc(
                color = ChartColors[index % ChartColors.size],
                startAngle = startAngle,
                sweepAngle = sweepAngle - 2f, // Small gap between segments
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                topLeft = androidx.compose.ui.geometry.Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String,
    weight: Double
) {
    val barFraction = (weight / 100.0).toFloat().coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        // Background bar based on percentage relative to max
        Box(
            modifier = Modifier
                .fillMaxWidth(barFraction)
                .fillMaxHeight()
                .background(color.copy(alpha = 0.15f))
        )
        // Content
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
            Text(
                text = "${CurrencyFormatter.formatPercent(weight)}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AutoSizeText(
    text: String,
    maxWidth: androidx.compose.ui.unit.Dp,
    style: TextStyle,
    fontWeight: FontWeight,
    color: androidx.compose.ui.graphics.Color
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val maxWidthPx = with(density) { maxWidth.toPx() }

    // Find the largest font size that fits
    val fontSize = remember(text, maxWidthPx) {
        var size = 14f
        while (size > 6f) {
            val measuredWidth = textMeasurer.measure(
                text = text,
                style = style.copy(fontSize = size.sp, fontWeight = fontWeight)
            ).size.width
            if (measuredWidth <= maxWidthPx) break
            size -= 0.5f
        }
        size.sp
    }

    Text(
        text = text,
        style = style.copy(fontSize = fontSize),
        fontWeight = fontWeight,
        color = color
    )
}
