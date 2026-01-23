package com.portfolio.manager.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.getValue
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.portfolio.manager.domain.model.PortfolioStats
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.presentation.theme.GainGreenPastel
import com.portfolio.manager.presentation.theme.LossRedPastel
import com.portfolio.manager.presentation.util.CurrencyFormatter
import com.portfolio.manager.presentation.util.createTrendIndicator

@Composable
fun PortfolioSummary(
    stocks: List<Stock>,
    exchangeRate: Double,
    periodReturns: Map<TimePeriod, Double> = emptyMap(),
    selectedPeriod: TimePeriod = TimePeriod.ONE_YEAR,
    isLoadingPeriodReturns: Boolean = false,
    onPeriodSelected: (TimePeriod) -> Unit = {},
    showInKrw: Boolean = false,
    onCurrencyToggle: () -> Unit = {},
    portfolioSparkline: List<Double> = emptyList(),
    portfolioStats: PortfolioStats = PortfolioStats(),
    modifier: Modifier = Modifier
) {

    val totalValueUsd = stocks.sumOf { it.totalValueInUsd(exchangeRate) }
    val totalCostUsd = stocks.sumOf { it.totalCostInUsd(exchangeRate) }
    val totalValueKrw = stocks.sumOf { it.totalValueInKrw(exchangeRate) }
    val totalCostKrw = stocks.sumOf { it.totalCostInKrw(exchangeRate) }

    val totalValue = if (showInKrw) totalValueKrw else totalValueUsd
    val totalCost = if (showInKrw) totalCostKrw else totalCostUsd
    val totalGainLoss = totalValue - totalCost
    val totalGainLossPercent = if (totalCost > 0) ((totalValue - totalCost) / totalCost) * 100 else 0.0

    // Calculate day change (sum of each stock's day change * quantity, converted to display currency)
    val dayChangeUsd = stocks.sumOf { stock ->
        val change = stock.dayChange ?: 0.0
        val valueChange = change * stock.quantity
        if (stock.currency == "KRW") valueChange / exchangeRate else valueChange
    }
    val dayChangeKrw = stocks.sumOf { stock ->
        val change = stock.dayChange ?: 0.0
        val valueChange = change * stock.quantity
        if (stock.currency == "KRW") valueChange else valueChange * exchangeRate
    }
    val dayChange = if (showInKrw) dayChangeKrw else dayChangeUsd
    val dayChangePercent = if (totalValue > 0) (dayChange / (totalValue - dayChange)) * 100 else 0.0

    val trend = createTrendIndicator(totalGainLoss, usePastel = true)
    val dayTrend = createTrendIndicator(dayChange, usePastel = true)

    val formatValue = CurrencyFormatter.createFormatter(showInKrw)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
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
            onToggle = onCurrencyToggle,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 40.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Total Value with return badge inline
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = formatValue(totalValue),
                    style = MaterialTheme.typography.displaySmall.copy(
                        letterSpacing = (-1.5).sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = trend.backgroundColor
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = trend.icon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                        Text(
                            text = "${if (trend.isGain) "+" else ""}${CurrencyFormatter.formatPercent(totalGainLossPercent)}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Gain/loss amount, invested, and day change in one compact row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (trend.isGain) "+" else ""}${formatValue(totalGainLoss)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.4f)
                )
                Text(
                    text = "${formatValue(totalCost)} invested",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
                if (dayChange != 0.0) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Icon(
                            imageVector = dayTrend.icon,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = dayTrend.color
                        )
                        Text(
                            text = "${if (dayTrend.isGain) "+" else ""}${CurrencyFormatter.formatPercent(dayChangePercent)}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = dayTrend.color
                        )
                    }
                }
            }

            // Portfolio sparkline
            if (portfolioSparkline.size >= 2) {
                Spacer(modifier = Modifier.height(12.dp))
                Sparkline(
                    prices = portfolioSparkline,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(70.dp),
                    lineColor = Color.White.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            PeriodSelector(
                periods = TimePeriod.entries,
                selectedPeriod = selectedPeriod,
                periodReturns = periodReturns,
                isLoading = isLoadingPeriodReturns,
                onPeriodSelected = onPeriodSelected
            )

            // Portfolio Statistics Row
            if (portfolioStats != PortfolioStats()) {
                Spacer(modifier = Modifier.height(14.dp))
                StatsRow(stats = portfolioStats)
            }
        }
    }
}

@Composable
private fun StatsRow(stats: PortfolioStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        CompactStatItem(
            label = "MDD",
            value = "-${CurrencyFormatter.formatPercent(stats.maxDrawdown)}%",
            color = LossRedPastel
        )
        CompactStatItem(
            label = "Vol",
            value = "${CurrencyFormatter.formatPercent(stats.volatility)}%",
            color = Color.White.copy(alpha = 0.8f)
        )
        CompactStatItem(
            label = "Sharpe",
            value = String.format("%.2f", stats.sharpeRatio),
            color = if (stats.sharpeRatio >= 0) GainGreenPastel else LossRedPastel
        )
        CompactStatItem(
            label = "Best",
            value = "+${CurrencyFormatter.formatPercent(stats.bestDay)}%",
            color = GainGreenPastel
        )
        CompactStatItem(
            label = "Worst",
            value = "${CurrencyFormatter.formatPercent(stats.worstDay)}%",
            color = LossRedPastel
        )
    }
}

@Composable
private fun CompactStatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun PeriodSelector(
    periods: List<TimePeriod>,
    selectedPeriod: TimePeriod,
    periodReturns: Map<TimePeriod, Double>,
    isLoading: Boolean,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        periods.forEach { period ->
            val isSelected = period == selectedPeriod
            val returnValue = periodReturns[period]
            val displayValue = when {
                isLoading && isSelected -> "..."
                returnValue != null -> {
                    val sign = if (returnValue >= 0) "+" else ""
                    "$sign${CurrencyFormatter.formatPercent(returnValue)}%"
                }
                else -> period.label
            }
            val returnColor = when {
                returnValue == null -> Color.White.copy(alpha = 0.7f)
                returnValue >= 0 -> GainGreenPastel
                else -> LossRedPastel
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                modifier = Modifier.clickable { onPeriodSelected(period) }
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = period.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                    )
                    if (isSelected && returnValue != null) {
                        AnimatedContent(
                            targetState = displayValue,
                            transitionSpec = {
                                (fadeIn() + slideInVertically { it / 2 }) togetherWith
                                    (fadeOut() + slideOutVertically { -it / 2 })
                            },
                            label = "periodReturnAnimation"
                        ) { value ->
                            Text(
                                text = value,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = returnColor
                            )
                        }
                    }
                }
            }
        }
    }
}


