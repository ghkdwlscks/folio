package com.portfolio.manager.presentation.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.presentation.theme.GainGreen
import com.portfolio.manager.presentation.theme.LossRed
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PortfolioSummary(
    stocks: List<Stock>,
    modifier: Modifier = Modifier
) {
    val totalValue = stocks.sumOf { it.totalValue }
    val totalCost = stocks.sumOf { it.totalCost }
    val totalGainLoss = totalValue - totalCost
    val totalGainLossPercent = if (totalCost > 0) ((totalValue - totalCost) / totalCost) * 100 else 0.0

    val isGain = totalGainLoss >= 0
    val trendColor = if (isGain) GainGreen else LossRed
    val trendIcon = if (isGain) Icons.Rounded.TrendingUp else Icons.Rounded.TrendingDown

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val percentFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
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
                text = currencyFormat.format(totalValue),
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
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isGain) Color(0xFF90EE90) else Color(0xFFFFB6C1)
                    )
                    Text(
                        text = "${if (isGain) "+" else ""}${currencyFormat.format(totalGainLoss)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isGain) Color(0xFF90EE90).copy(alpha = 0.3f) else Color(0xFFFFB6C1).copy(alpha = 0.3f)
                    ) {
                        Text(
                            text = "${if (isGain) "+" else ""}${percentFormat.format(totalGainLossPercent)}%",
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
                    value = currencyFormat.format(totalCost)
                )
                StatItem(
                    label = "Stocks",
                    value = stocks.size.toString()
                )
                StatItem(
                    label = "Today",
                    value = "${if (isGain) "+" else ""}${percentFormat.format(totalGainLossPercent / 10)}%"
                )
            }
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
