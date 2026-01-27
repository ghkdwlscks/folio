package com.portfolio.manager.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
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

private const val COLLAPSED_ITEM_COUNT = 5

@Composable
fun AllocationPieChart(
    items: List<AllocationItem>,
    totalValue: Double,
    showInKrw: Boolean,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val canExpand = items.size > COLLAPSED_ITEM_COUNT
    val remainingCount = items.size - COLLAPSED_ITEM_COUNT

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "arrowRotation"
    )

    GlassSurface(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header with expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (canExpand) Modifier.clickable { expanded = !expanded }
                        else Modifier
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Allocation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (canExpand) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (expanded) "Show less" else "+$remainingCount more",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(arrowRotation),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pie chart (always shows all items)
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
                        maxWidth = 72.dp,
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = (-0.5).sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Legend (shows limited items when collapsed)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Always visible items (top 5)
                    items.take(COLLAPSED_ITEM_COUNT).forEachIndexed { index, item ->
                        LegendItem(
                            color = ChartColors[index % ChartColors.size],
                            label = item.name,
                            weight = item.weight
                        )
                    }

                    // Expandable items (6+)
                    AnimatedVisibility(
                        visible = expanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items.drop(COLLAPSED_ITEM_COUNT).forEachIndexed { index, item ->
                                val colorIndex = index + COLLAPSED_ITEM_COUNT
                                LegendItem(
                                    color = ChartColors[colorIndex % ChartColors.size],
                                    label = item.name,
                                    weight = item.weight
                                )
                            }
                        }
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
    val targetSweepAngles = items.map { (it.weight / 100f * 360f).toFloat() }

    // Animation progress from 0 to 1
    val animationProgress = remember { Animatable(0f) }

    // Trigger animation when items change
    LaunchedEffect(items.map { it.symbol to it.weight }) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 24.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        var startAngle = -90f
        val progress = animationProgress.value

        targetSweepAngles.forEachIndexed { index, targetSweepAngle ->
            val animatedSweepAngle = targetSweepAngle * progress
            val gap = if (animatedSweepAngle > 2f) 2f else 0f

            drawArc(
                color = ChartColors[index % ChartColors.size],
                startAngle = startAngle,
                sweepAngle = (animatedSweepAngle - gap).coerceAtLeast(0f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                topLeft = androidx.compose.ui.geometry.Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
            )
            startAngle += animatedSweepAngle
        }
    }
}

@Composable
private fun LegendItem(
    color: androidx.compose.ui.graphics.Color,
    label: String,
    weight: Double
) {
    val targetFraction = (weight / 100.0).toFloat().coerceIn(0f, 1f)

    // Animate the bar width
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(
            durationMillis = 600,
            delayMillis = 100,
            easing = FastOutSlowInEasing
        ),
        label = "barFraction"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .clip(RoundedCornerShape(4.dp))
    ) {
        // Animated background bar based on percentage
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
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
