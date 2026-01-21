package com.portfolio.manager.presentation.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.portfolio.manager.domain.model.PortfolioStats
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.presentation.component.AllocationItem
import com.portfolio.manager.presentation.component.AllocationPieChart
import com.portfolio.manager.presentation.component.PortfolioSummary
import com.portfolio.manager.presentation.component.SkeletonDashboard
import com.portfolio.manager.presentation.component.StockCard
import com.portfolio.manager.presentation.viewmodel.AccountWithCount
import com.portfolio.manager.presentation.viewmodel.DashboardUiState
import com.portfolio.manager.presentation.viewmodel.DashboardViewModel
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID

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
 * Creates allocation items from stocks for the pie chart.
 */
private fun createAllocationItems(stocks: List<Stock>, totalPortfolioValue: Double, exchangeRate: Double): List<AllocationItem> {
    return stocks.map { stock ->
        AllocationItem(
            symbol = stock.symbol,
            name = stock.name,
            value = stock.totalValueInUsd(exchangeRate),
            weight = calculateWeightPercent(stock, totalPortfolioValue, exchangeRate)
        )
    }
}

private data class DeleteConfirmation(
    val holdingId: Long,
    val symbol: String,
    val quantity: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddHolding: () -> Unit,
    onManageAccounts: () -> Unit,
    onEditHolding: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var deleteConfirmation by remember { mutableStateOf<DeleteConfirmation?>(null) }

    val (accounts, selectedAccountId, isRefreshing) = when (val state = uiState) {
        is DashboardUiState.Success -> Triple(state.accounts, state.selectedAccountId, state.isRefreshing)
        else -> Triple(emptyList(), 1L, false)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DashboardTopBar(
                isRefreshing = isRefreshing,
                onManageAccounts = onManageAccounts,
                onRefresh = { viewModel.refresh() }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHolding) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Holding"
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                SkeletonDashboard(modifier = Modifier.padding(paddingValues))
            }
            is DashboardUiState.Success -> {
                DashboardContent(
                    stocks = state.stocks,
                    exchangeRate = state.exchangeRate,
                    accounts = state.accounts,
                    selectedAccountId = state.selectedAccountId,
                    onAccountSelected = { viewModel.selectAccount(it) },
                    periodReturns = state.periodReturns,
                    selectedPeriod = state.selectedPeriod,
                    isLoadingPeriodReturns = state.isLoadingPeriodReturns,
                    onPeriodSelected = { viewModel.selectPeriod(it) },
                    showInKrw = state.showInKrw,
                    onCurrencyToggle = { viewModel.toggleCurrency() },
                    portfolioSparkline = state.portfolioSparkline,
                    portfolioStats = state.portfolioStats,
                    sparklinePeriod = state.sparklinePeriod,
                    onSparklinePeriodSelected = { viewModel.selectSparklinePeriod(it) },
                    onDeleteHolding = { id, symbol, quantity ->
                        deleteConfirmation = DeleteConfirmation(id, symbol, quantity)
                    },
                    onEditHolding = onEditHolding,
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is DashboardUiState.Error -> {
                ErrorContent(
                    message = state.message,
                    onRetry = { viewModel.refresh() },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    deleteConfirmation?.let { confirmation ->
        AlertDialog(
            onDismissRequest = { deleteConfirmation = null },
            title = { Text("Delete Holding") },
            text = {
                Text("Are you sure you want to delete ${confirmation.symbol} (${confirmation.quantity} shares)?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteHolding(confirmation.holdingId)
                        deleteConfirmation = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "Failed to load prices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun DashboardContent(
    stocks: List<Stock>,
    exchangeRate: Double,
    accounts: List<AccountWithCount>,
    selectedAccountId: Long,
    onAccountSelected: (Long) -> Unit,
    periodReturns: Map<TimePeriod, Double>,
    selectedPeriod: TimePeriod,
    isLoadingPeriodReturns: Boolean,
    onPeriodSelected: (TimePeriod) -> Unit,
    showInKrw: Boolean,
    onCurrencyToggle: () -> Unit,
    portfolioSparkline: List<Double>,
    portfolioStats: PortfolioStats,
    sparklinePeriod: TimePeriod,
    onSparklinePeriodSelected: (TimePeriod) -> Unit,
    onDeleteHolding: (Long, String, Int) -> Unit,
    onEditHolding: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPortfolioValue = stocks.sumOf { it.totalValueInUsd(exchangeRate) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AccountTabSelector(
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                onAccountSelected = onAccountSelected
            )
        }

        item {
            PortfolioSummary(
                stocks = stocks,
                exchangeRate = exchangeRate,
                periodReturns = periodReturns,
                selectedPeriod = selectedPeriod,
                isLoadingPeriodReturns = isLoadingPeriodReturns,
                onPeriodSelected = onPeriodSelected,
                showInKrw = showInKrw,
                onCurrencyToggle = onCurrencyToggle,
                portfolioSparkline = portfolioSparkline,
                portfolioStats = portfolioStats,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (stocks.size >= 2) {
            item {
                AllocationPieChart(
                    items = createAllocationItems(stocks, totalPortfolioValue, exchangeRate)
                )
            }
        }

        item {
            SectionHeader(
                title = "My Holdings",
                count = stocks.size,
                sparklinePeriod = sparklinePeriod,
                onSparklinePeriodSelected = onSparklinePeriodSelected
            )
        }

        items(
            items = stocks,
            key = { it.id }
        ) { stock ->
            val isAggregated = stock.accountDetails.isNotEmpty()
            StockCard(
                stock = stock,
                weightPercent = calculateWeightPercent(stock, totalPortfolioValue, exchangeRate),
                onDelete = { onDeleteHolding(stock.id, stock.symbol, stock.quantity) }.takeIf { !isAggregated },
                onDeleteAccountHolding = { holdingId: Long ->
                    val detail = stock.accountDetails.find { it.holdingId == holdingId }
                    if (detail != null) {
                        onDeleteHolding(holdingId, stock.symbol, detail.quantity)
                    }
                }.takeIf { isAggregated },
                onEdit = { onEditHolding(stock.id) }.takeIf { !isAggregated },
                onEditAccountHolding = onEditHolding.takeIf { isAggregated }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    isRefreshing: Boolean,
    onManageAccounts: () -> Unit,
    onRefresh: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = "Portfolio Manager",
                fontWeight = FontWeight.Bold
            )
        },
        actions = {
            IconButton(onClick = onManageAccounts) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Manage Accounts"
                )
            }
            IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountTabSelector(
    accounts: List<AccountWithCount>,
    selectedAccountId: Long,
    onAccountSelected: (Long) -> Unit
) {
    val totalHoldings = accounts.sumOf { it.holdingsCount }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedAccountId == ALL_ACCOUNTS_ID,
            onClick = { onAccountSelected(ALL_ACCOUNTS_ID) },
            label = { Text("All ($totalHoldings)") },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        )
        accounts.forEach { accountWithCount ->
            FilterChip(
                selected = selectedAccountId == accountWithCount.account.id,
                onClick = { onAccountSelected(accountWithCount.account.id) },
                label = { Text("${accountWithCount.account.name} (${accountWithCount.holdingsCount})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    sparklinePeriod: TimePeriod,
    onSparklinePeriodSelected: (TimePeriod) -> Unit
) {
    var sparklineMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "$count items",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            Row(
                modifier = Modifier.clickable { sparklineMenuExpanded = true },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShowChart,
                    contentDescription = "Sparkline Period",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = sparklinePeriod.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            DropdownMenu(
                expanded = sparklineMenuExpanded,
                onDismissRequest = { sparklineMenuExpanded = false }
            ) {
                TimePeriod.entries.forEach { period ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = period.label,
                                fontWeight = if (period == sparklinePeriod) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSparklinePeriodSelected(period)
                            sparklineMenuExpanded = false
                        }
                    )
                }
            }
        }
    }
}
