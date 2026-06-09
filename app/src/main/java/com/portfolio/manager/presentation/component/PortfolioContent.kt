package com.portfolio.manager.presentation.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.portfolio.manager.domain.model.BenchmarkReturns
import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.PortfolioStats
import com.portfolio.manager.domain.model.SortOption
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod

/**
 * Shared scrollable portfolio body (summary, allocation chart, holdings, cash).
 * Reused by both the Dashboard and the Household (combined) screen so they keep
 * an identical layout. Callbacks that a screen does not support (e.g. editing a
 * partner's holding) can be passed as no-ops.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PortfolioContent(
    stocks: List<Stock>,
    cashItems: List<CashItem>,
    exchangeRate: Double,
    periodReturns: Map<TimePeriod, Double>,
    benchmarkReturns: Map<TimePeriod, BenchmarkReturns>,
    selectedPeriod: TimePeriod,
    isLoadingPeriodReturns: Boolean,
    onPeriodSelected: (TimePeriod) -> Unit,
    showInKrw: Boolean,
    onCurrencyToggle: () -> Unit,
    portfolioSparkline: List<Double>,
    portfolioSparklineTimestamps: List<Long>,
    benchmarkSparklines: Map<String, List<Double>>,
    benchmarkTimestamps: Map<String, List<Long>>,
    portfolioStats: PortfolioStats,
    sortOption: SortOption,
    onSortOptionSelected: (SortOption) -> Unit,
    sparklinePeriod: TimePeriod,
    onSparklinePeriodSelected: (TimePeriod) -> Unit,
    onDeleteHolding: (Long, String, Int) -> Unit,
    onEditHolding: (Long) -> Unit,
    onDeleteCash: (Long, String) -> Unit,
    onEditCash: (Long) -> Unit,
    listState: LazyListState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    accountOwnerLabels: Map<Long, String> = emptyMap(),
    readOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val totalStocksValue = stocks.sumOf { it.totalValueInUsd(exchangeRate) }
    val totalCashValue = cashItems.sumOf { it.valueInUsd(exchangeRate) }
    val totalPortfolioValue = totalStocksValue + totalCashValue

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        indicator = {}  // Hide center indicator - refresh state shown in top bar only
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                PortfolioSummary(
                    stocks = stocks,
                    cashItems = cashItems,
                    exchangeRate = exchangeRate,
                    periodReturns = periodReturns,
                    benchmarkReturns = benchmarkReturns,
                    selectedPeriod = selectedPeriod,
                    isLoadingPeriodReturns = isLoadingPeriodReturns,
                    onPeriodSelected = onPeriodSelected,
                    showInKrw = showInKrw,
                    onCurrencyToggle = onCurrencyToggle,
                    portfolioSparkline = portfolioSparkline,
                    portfolioSparklineTimestamps = portfolioSparklineTimestamps,
                    benchmarkSparklines = benchmarkSparklines,
                    benchmarkTimestamps = benchmarkTimestamps,
                    portfolioStats = portfolioStats,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (stocks.isNotEmpty() || cashItems.isNotEmpty()) {
                item {
                    val totalValueForDisplay = if (showInKrw) {
                        stocks.sumOf { it.totalValueInKrw(exchangeRate) } +
                            cashItems.sumOf { it.valueInKrw(exchangeRate) }
                    } else {
                        totalPortfolioValue
                    }
                    AllocationPieChart(
                        items = createAllocationItems(stocks, cashItems, totalPortfolioValue, exchangeRate),
                        totalValue = totalValueForDisplay,
                        showInKrw = showInKrw
                    )
                }
            }

            if (stocks.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "My Holdings",
                        count = stocks.size,
                        sortOption = sortOption,
                        onSortOptionSelected = onSortOptionSelected,
                        sparklinePeriod = sparklinePeriod,
                        onSparklinePeriodSelected = onSparklinePeriodSelected
                    )
                }

                itemsIndexed(
                    items = stocks,
                    key = { _, stock -> stock.id }
                ) { index, stock ->
                    val isAggregated = stock.accountDetails.isNotEmpty()
                    StockCard(
                        stock = stock,
                        weightPercent = calculateWeightPercent(stock, totalPortfolioValue, exchangeRate),
                        accountOwnerLabels = accountOwnerLabels,
                        onDelete = { onDeleteHolding(stock.id, stock.symbol, stock.quantity) }.takeIf { !isAggregated && !readOnly },
                        onDeleteAccountHolding = { holdingId: Long ->
                            val detail = stock.accountDetails.find { it.holdingId == holdingId }
                            if (detail != null) {
                                onDeleteHolding(holdingId, stock.symbol, detail.quantity)
                            }
                        }.takeIf { isAggregated && !readOnly },
                        onEdit = { onEditHolding(stock.id) }.takeIf { !isAggregated && !readOnly },
                        onEditAccountHolding = onEditHolding.takeIf { isAggregated && !readOnly },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(
                                durationMillis = 300,
                                delayMillis = index * 50
                            ),
                            fadeOutSpec = tween(durationMillis = 150)
                        )
                    )
                }
            }

            if (cashItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Cash (${cashItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                itemsIndexed(
                    items = cashItems,
                    key = { _, cashItem -> "cash_${cashItem.id}" }
                ) { index, cashItem ->
                    CashCard(
                        cashItem = cashItem,
                        showInKrw = showInKrw,
                        exchangeRate = exchangeRate,
                        onEdit = { onEditCash(cashItem.id) }.takeIf { !readOnly },
                        onDelete = { onDeleteCash(cashItem.id, cashItem.name) }.takeIf { !readOnly },
                        modifier = Modifier.animateItem(
                            fadeInSpec = tween(
                                durationMillis = 300,
                                delayMillis = index * 50
                            ),
                            fadeOutSpec = tween(durationMillis = 150)
                        )
                    )
                }
            }
        }
    }
}

/**
 * Calculates the weight percentage of a stock in the total portfolio.
 */
private fun calculateWeightPercent(stock: Stock, totalPortfolioValue: Double, exchangeRate: Double): Double {
    return if (totalPortfolioValue > 0) {
        (stock.totalValueInUsd(exchangeRate) / totalPortfolioValue) * 100
    } else {
        0.0
    }
}

/**
 * Creates allocation items from stocks and cash for the pie chart.
 */
private fun createAllocationItems(
    stocks: List<Stock>,
    cashItems: List<CashItem>,
    totalPortfolioValue: Double,
    exchangeRate: Double
): List<AllocationItem> {
    val stockItems = stocks.map { stock ->
        AllocationItem(
            symbol = stock.symbol,
            name = stock.name,
            value = stock.totalValueInUsd(exchangeRate),
            weight = calculateWeightPercent(stock, totalPortfolioValue, exchangeRate)
        )
    }

    val cashAllocationItems = cashItems.map { cash ->
        val cashValue = cash.valueInUsd(exchangeRate)
        val cashWeight = if (totalPortfolioValue > 0) (cashValue / totalPortfolioValue) * 100 else 0.0
        AllocationItem(
            symbol = "CASH_${cash.id}",
            name = cash.name,
            value = cashValue,
            weight = cashWeight
        )
    }

    return (stockItems + cashAllocationItems).sortedByDescending { it.weight }
}
