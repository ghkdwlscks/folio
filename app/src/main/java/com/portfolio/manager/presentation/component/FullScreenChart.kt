package com.portfolio.manager.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.presentation.util.CurrencyFormatter
import com.portfolio.manager.presentation.util.getTrendColor

data class FullScreenChartData(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val dayChange: Double?,
    val dayChangePercent: Double?,
    val currency: String,
    val priceHistory: List<Double>,
    val priceHistoryTimestamps: List<Long>,
    val benchmarkSparklines: Map<String, List<Double>> = emptyMap(),
    val benchmarkTimestamps: Map<String, List<Long>> = emptyMap(),
    val periodReturn: Double? = null
)

@Composable
fun FullScreenChartDialog(
    data: FullScreenChartData,
    onDismiss: () -> Unit,
    onPeriodChange: ((TimePeriod) -> Unit)? = null,
    currentPeriod: TimePeriod = TimePeriod.ONE_YEAR
) {
    var selectedPeriod by remember { mutableStateOf(currentPeriod) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Price info
                val formattedPrice = CurrencyFormatter.format(data.currentPrice, data.currency)
                Text(
                    text = formattedPrice,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )

                // Show period return instead of daily change
                data.periodReturn?.let { periodReturn ->
                    val trendColor = getTrendColor(periodReturn)
                    val sign = if (periodReturn >= 0) "+" else ""

                    Text(
                        text = "$sign${String.format("%.2f", periodReturn)}% (${selectedPeriod.label})",
                        style = MaterialTheme.typography.bodyLarge,
                        color = trendColor,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Chart area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    if (data.priceHistory.size >= 2 && data.priceHistoryTimestamps.size >= 2) {
                        // Create colored overlay lines for benchmarks
                        val overlays = data.benchmarkSparklines.mapNotNull { (symbol, prices) ->
                            val timestamps = data.benchmarkTimestamps[symbol] ?: emptyList()
                            val color = when (symbol) {
                                "^GSPC" -> Color(0xFF2196F3) // Blue for S&P 500
                                "^KS11" -> Color(0xFFFF9800) // Orange for KOSPI
                                else -> Color.Gray
                            }
                            if (prices.size >= 2 && timestamps.size >= 2) {
                                OverlayLine(prices, timestamps, color.copy(alpha = 0.6f), symbol)
                            } else null
                        }
                        InteractiveChart(
                            prices = data.priceHistory,
                            timestamps = data.priceHistoryTimestamps,
                            currency = data.currency,
                            modifier = Modifier.fillMaxSize(),
                            overlayLines = overlays
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No chart data available",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Legend for benchmark lines
                if (data.benchmarkSparklines.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Portfolio legend
                        LegendItem(
                            color = getTrendColor(
                                (data.priceHistory.lastOrNull() ?: 0.0) - (data.priceHistory.firstOrNull() ?: 0.0)
                            ),
                            label = "Portfolio"
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        // S&P 500 legend
                        if (data.benchmarkSparklines.containsKey("^GSPC")) {
                            LegendItem(
                                color = Color(0xFF2196F3),
                                label = "S&P 500",
                                dashed = true
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                        // KOSPI legend
                        if (data.benchmarkSparklines.containsKey("^KS11")) {
                            LegendItem(
                                color = Color(0xFFFF9800),
                                label = "KOSPI",
                                dashed = true
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Statistics
                if (data.priceHistory.size >= 2) {
                    Text(
                        text = "${selectedPeriod.label} Statistics",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ChartStatistics(
                        priceHistory = data.priceHistory,
                        currency = data.currency,
                        periodReturn = data.periodReturn
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Period selector
                if (onPeriodChange != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        TimePeriod.entries.forEach { period ->
                            FilterChip(
                                selected = selectedPeriod == period,
                                onClick = {
                                    selectedPeriod = period
                                    onPeriodChange(period)
                                },
                                label = {
                                    Text(
                                        text = period.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Hint text
                Text(
                    text = "Tap on chart to see price details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun ChartStatistics(
    priceHistory: List<Double>,
    currency: String,
    periodReturn: Double? = null
) {
    val high = priceHistory.maxOrNull() ?: 0.0
    val low = priceHistory.minOrNull() ?: 0.0
    val startPrice = priceHistory.firstOrNull() ?: 0.0
    val endPrice = priceHistory.lastOrNull() ?: 0.0
    // Use provided period return if available, otherwise calculate from price history
    val displayPeriodReturn = periodReturn ?: if (startPrice > 0) ((endPrice - startPrice) / startPrice) * 100 else 0.0
    val average = if (priceHistory.isNotEmpty()) priceHistory.average() else 0.0

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "High",
                    value = CurrencyFormatter.format(high, currency),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Low",
                    value = CurrencyFormatter.format(low, currency),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    label = "Average",
                    value = CurrencyFormatter.format(average, currency),
                    modifier = Modifier.weight(1f)
                )
                StatItem(
                    label = "Period Return",
                    value = "${if (displayPeriodReturn >= 0) "+" else ""}${String.format("%.2f", displayPeriodReturn)}%",
                    valueColor = getTrendColor(displayPeriodReturn),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    dashed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (dashed) {
            Canvas(modifier = Modifier.size(width = 20.dp, height = 12.dp)) {
                drawLine(
                    color = color,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
