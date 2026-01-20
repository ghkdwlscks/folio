package com.portfolio.manager.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.presentation.theme.GainGreenPastel
import com.portfolio.manager.presentation.theme.LossRedPastel
import com.portfolio.manager.presentation.util.CurrencyFormatter

@Composable
fun PortfolioSummary(
    stocks: List<Stock>,
    modifier: Modifier = Modifier
) {
    var showInKrw by remember { mutableStateOf(false) }

    val totalValueUsd = stocks.sumOf { it.totalValueInUsd }
    val totalCostUsd = stocks.sumOf { it.totalCostInUsd }
    val totalValueKrw = stocks.sumOf { it.totalValueInKrw }
    val totalCostKrw = stocks.sumOf { it.totalCostInKrw }

    val totalValue = if (showInKrw) totalValueKrw else totalValueUsd
    val totalCost = if (showInKrw) totalCostKrw else totalCostUsd
    val totalGainLoss = totalValue - totalCost
    val totalGainLossPercent = if (totalCost > 0) ((totalValue - totalCost) / totalCost) * 100 else 0.0

    val isGain = totalGainLoss >= 0
    val trendIcon = if (isGain) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown
    val trendColor = if (isGain) GainGreenPastel else LossRedPastel

    val formatValue: (Double) -> String = if (showInKrw) {
        { CurrencyFormatter.formatKrw(it) }
    } else {
        { CurrencyFormatter.formatUsd(it) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )
            )
    ) {
        CurrencyToggle(
            showInKrw = showInKrw,
            onToggle = { showInKrw = !showInKrw },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Total Portfolio Value",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatValue(totalValue),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontSize = 36.sp,
                    letterSpacing = (-1).sp
                ),
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = if (isGain) "Trending up" else "Trending down",
                        modifier = Modifier.size(20.dp),
                        tint = trendColor
                    )
                    Text(
                        text = "${if (isGain) "+" else ""}${formatValue(totalGainLoss)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = trendColor.copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "${if (isGain) "+" else ""}${CurrencyFormatter.formatPercent(totalGainLossPercent)}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Invested",
                    value = formatValue(totalCost)
                )
                StatItem(
                    label = "Stocks",
                    value = stocks.size.toString()
                )
                StatItem(
                    label = "Today",
                    value = "${if (isGain) "+" else ""}${CurrencyFormatter.formatPercent(totalGainLossPercent / 10)}%"
                )
            }
        }
    }
}

@Composable
private fun CurrencyToggle(
    showInKrw: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.2f),
        modifier = modifier.clickable(onClick = onToggle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = "USD",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (!showInKrw) FontWeight.Bold else FontWeight.Normal,
                color = if (!showInKrw) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
            Text(
                text = "KRW",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (showInKrw) FontWeight.Bold else FontWeight.Normal,
                color = if (showInKrw) Color.White else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}
