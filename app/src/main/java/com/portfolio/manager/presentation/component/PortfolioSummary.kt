package com.portfolio.manager.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.portfolio.manager.domain.model.BenchmarkReturns
import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.PortfolioStats
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.util.CurrencyConverter
import com.portfolio.manager.presentation.theme.GainGreenPastel
import com.portfolio.manager.presentation.theme.LossRedPastel
import com.portfolio.manager.presentation.util.CurrencyFormatter
import com.portfolio.manager.presentation.util.createTrendIndicator

@Composable
fun PortfolioSummary(
    stocks: List<Stock>,
    cashItems: List<CashItem> = emptyList(),
    exchangeRate: Double,
    periodReturns: Map<TimePeriod, Double> = emptyMap(),
    benchmarkReturns: Map<TimePeriod, BenchmarkReturns> = emptyMap(),
    selectedPeriod: TimePeriod = TimePeriod.ONE_YEAR,
    isLoadingPeriodReturns: Boolean = false,
    onPeriodSelected: (TimePeriod) -> Unit = {},
    showInKrw: Boolean = false,
    onCurrencyToggle: () -> Unit = {},
    portfolioSparkline: List<Double> = emptyList(),
    portfolioSparklineTimestamps: List<Long> = emptyList(),
    benchmarkSparklines: Map<String, List<Double>> = emptyMap(),
    benchmarkTimestamps: Map<String, List<Long>> = emptyMap(),
    portfolioStats: PortfolioStats = PortfolioStats(),
    modifier: Modifier = Modifier
) {
    var showFullScreenChart by remember { mutableStateOf(false) }

    val stocksValueUsd = stocks.sumOf { it.totalValueInUsd(exchangeRate) }
    val stocksCostUsd = stocks.sumOf { it.totalCostInUsd(exchangeRate) }
    val stocksValueKrw = stocks.sumOf { it.totalValueInKrw(exchangeRate) }
    val stocksCostKrw = stocks.sumOf { it.totalCostInKrw(exchangeRate) }

    val cashValueUsd = cashItems.sumOf { it.valueInUsd(exchangeRate) }
    val cashValueKrw = cashItems.sumOf { it.valueInKrw(exchangeRate) }

    val totalValueUsd = stocksValueUsd + cashValueUsd
    val totalValueKrw = stocksValueKrw + cashValueKrw

    val totalValue = if (showInKrw) totalValueKrw else totalValueUsd
    val stocksCost = if (showInKrw) stocksCostKrw else stocksCostUsd
    val stocksValue = if (showInKrw) stocksValueKrw else stocksValueUsd
    val cashValue = if (showInKrw) cashValueKrw else cashValueUsd
    // Total invested = stocks cost + cash value (cash is treated as invested amount)
    val totalInvested = stocksCost + cashValue
    // Gain/loss from stocks only (cash has no gain/loss), but % based on total invested
    val totalGainLoss = stocksValue - stocksCost
    val totalGainLossPercent = if (totalInvested > 0) (totalGainLoss / totalInvested) * 100 else 0.0

    // Calculate day change (sum of each stock's day change * quantity, converted to display currency)
    // Cash has no day change but is included in the base for % calculation
    val dayChange = stocks.sumOf { stock ->
        val valueChange = (stock.dayChange ?: 0.0) * stock.quantity
        CurrencyConverter.convert(valueChange, stock.currency, showInKrw, exchangeRate)
    }
    // Yesterday's total = today's total - day change (cash value is the same)
    val yesterdayTotal = totalValue - dayChange
    val dayChangePercent = if (yesterdayTotal > 0) (dayChange / yesterdayTotal) * 100 else 0.0

    // Calculate annual dividend income (stocks + cash savings income)
    val stockDividends = stocks.sumOf { stock ->
        CurrencyConverter.convert(stock.annualDividendIncome, stock.currency, showInKrw, exchangeRate)
    }
    val cashIncome = cashItems.sumOf { cash ->
        val annualIncome = cash.originalValue * cash.annualYieldRate / 100.0
        CurrencyConverter.convert(annualIncome, cash.currency, showInKrw, exchangeRate)
    }
    val annualDividend = stockDividends + cashIncome
    val portfolioDividendYield = if (totalValue > 0) (annualDividend / totalValue) * 100 else 0.0

    val trend = createTrendIndicator(totalGainLoss, usePastel = true)
    val dayTrend = createTrendIndicator(dayChange, usePastel = true)

    val formatValue = CurrencyFormatter.createFormatter(showInKrw)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        // Currency toggle and exchange rate
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "USD/KRW = ${String.format("%,.2f", exchangeRate)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(end = 8.dp)
            )
            CurrencyToggle(
                showInKrw = showInKrw,
                onToggle = onCurrencyToggle
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Total Value with return badge inline
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnimatedCurrencyCounter(
                    targetValue = totalValue,
                    showInKrw = showInKrw,
                    style = MaterialTheme.typography.displayMedium.copy(
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
                        AnimatedPercentCounter(
                            targetValue = totalGainLossPercent,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            prefix = if (trend.isGain) "+" else ""
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Info cards row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Gain/Loss card
                InfoCard(
                    label = "Gain/Loss",
                    value = "${if (trend.isGain) "+" else ""}${formatValue(totalGainLoss)}",
                    valueColor = trend.color,
                    modifier = Modifier.weight(1f)
                )
                // Today's change card
                InfoCard(
                    label = "Today",
                    value = "${if (dayTrend.isGain) "+" else ""}${formatValue(dayChange)}",
                    valueColor = dayTrend.color,
                    modifier = Modifier.weight(1f)
                )
                // Today's change % card
                InfoCard(
                    label = "Today %",
                    value = "${if (dayTrend.isGain) "+" else ""}${CurrencyFormatter.formatPercent(dayChangePercent)}%",
                    valueColor = dayTrend.color,
                    modifier = Modifier.weight(1f)
                )
            }

            // Portfolio sparkline
            if (portfolioSparkline.size >= 2) {
                Spacer(modifier = Modifier.height(12.dp))
                Sparkline(
                    prices = portfolioSparkline,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(100.dp),
                    lineColor = Color.White.copy(alpha = 0.9f),
                    onClick = { showFullScreenChart = true }
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

            // Dividend info row (only show if there's dividend income)
            if (annualDividend > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                DividendInfoRow(
                    annualDividend = annualDividend,
                    dividendYield = portfolioDividendYield,
                    showInKrw = showInKrw
                )
            }

            // Portfolio Statistics Row
            if (portfolioStats != PortfolioStats()) {
                Spacer(modifier = Modifier.height(14.dp))
                StatsRow(stats = portfolioStats)
            }
        }
    }

    // Full-screen chart dialog
    if (showFullScreenChart && portfolioSparkline.size >= 2) {
        // Convert normalized sparkline (starting at 100) back to actual portfolio values
        // normalized[i] = 100 * actual[i] / actual[0]
        // actual[i] = normalized[i] * (totalValue / normalized[last])
        val lastNormalized = portfolioSparkline.last()
        val actualPriceHistory = if (lastNormalized > 0) {
            val scaleFactor = totalValue / lastNormalized
            portfolioSparkline.map { it * scaleFactor }
        } else {
            portfolioSparkline
        }

        // Scale benchmark sparklines to match portfolio's starting value
        // Both start at 100 normalized, so scale by portfolioStart / 100
        val portfolioStart = actualPriceHistory.firstOrNull() ?: 100.0
        val scaledBenchmarks = benchmarkSparklines.mapValues { (_, prices) ->
            prices.map { it * portfolioStart / 100.0 }
        }

        FullScreenChartDialog(
            data = FullScreenChartData(
                symbol = selectedPeriod.label,
                name = "Portfolio",
                currentPrice = totalValue,
                dayChange = dayChange.takeIf { it != 0.0 },
                dayChangePercent = dayChangePercent.takeIf { dayChange != 0.0 },
                currency = if (showInKrw) "KRW" else "USD",
                priceHistory = actualPriceHistory,
                priceHistoryTimestamps = portfolioSparklineTimestamps,
                benchmarkSparklines = scaledBenchmarks,
                benchmarkTimestamps = benchmarkTimestamps,
                periodReturn = periodReturns[selectedPeriod]
            ),
            onDismiss = { showFullScreenChart = false },
            currentPeriod = selectedPeriod
        )
    }
}

@Composable
private fun InfoCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = valueColor
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = valueColor
                )
            }
        }
    }
}

@Composable
private fun DividendInfoRow(
    annualDividend: Double,
    dividendYield: Double,
    showInKrw: Boolean
) {
    val formatValue = CurrencyFormatter.createFormatter(showInKrw)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Annual Dividends",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Text(
                text = formatValue(annualDividend),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = GainGreenPastel
            )
            Text(
                text = "(${CurrencyFormatter.formatPercent(dividendYield)}%)",
                style = MaterialTheme.typography.labelSmall,
                color = GainGreenPastel.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun StatsRow(stats: PortfolioStats) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CompactStatItem(
                label = "MDD",
                value = "-${CurrencyFormatter.formatPercent(stats.maxDrawdown)}%",
                color = LossRedPastel
            )
            StatDivider()
            CompactStatItem(
                label = "Vol",
                value = "${CurrencyFormatter.formatPercent(stats.volatility)}%",
                color = Color.White.copy(alpha = 0.8f)
            )
            StatDivider()
            CompactStatItem(
                label = "Sharpe",
                value = String.format("%.2f", stats.sharpeRatio),
                color = if (stats.sharpeRatio >= 0) GainGreenPastel else LossRedPastel
            )
            StatDivider()
            CompactStatItem(
                label = "Best",
                value = "+${CurrencyFormatter.formatPercent(stats.bestDay)}%",
                color = GainGreenPastel
            )
            StatDivider()
            CompactStatItem(
                label = "Worst",
                value = "${CurrencyFormatter.formatPercent(stats.worstDay)}%",
                color = LossRedPastel
            )
        }
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color.White.copy(alpha = 0.15f))
    )
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

            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.92f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "periodScale"
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onPeriodSelected(period) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = period.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                    )
                    if (returnValue != null) {
                        Text(
                            text = displayValue,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) returnColor else returnColor.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}


