package com.portfolio.manager.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.portfolio.manager.domain.model.BenchmarkReturns
import com.portfolio.manager.domain.model.PortfolioStats
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.presentation.component.AllocationItem
import com.portfolio.manager.presentation.component.AllocationPieChart
import com.portfolio.manager.presentation.component.ErrorContent
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
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var deleteConfirmation by remember { mutableStateOf<DeleteConfirmation?>(null) }
    val listState = rememberLazyListState()

    // Track scroll direction for FAB visibility
    var previousScrollOffset by remember { mutableIntStateOf(0) }
    var previousFirstVisibleItem by remember { mutableIntStateOf(0) }
    val fabVisible by remember {
        derivedStateOf {
            val currentFirstVisibleItem = listState.firstVisibleItemIndex
            val currentScrollOffset = listState.firstVisibleItemScrollOffset

            val isScrollingUp = when {
                currentFirstVisibleItem < previousFirstVisibleItem -> true
                currentFirstVisibleItem > previousFirstVisibleItem -> false
                else -> currentScrollOffset <= previousScrollOffset
            }

            previousScrollOffset = currentScrollOffset
            previousFirstVisibleItem = currentFirstVisibleItem

            isScrollingUp || currentFirstVisibleItem == 0
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DashboardTopBarWrapper(
                viewModel = viewModel,
                onOpenDrawer = onOpenDrawer,
                onManageAccounts = onManageAccounts
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = slideInVertically(initialOffsetY = { it * 2 }),
                exit = slideOutVertically(targetOffsetY = { it * 2 })
            ) {
                FloatingActionButton(onClick = onAddHolding) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Holding"
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            AccountDropdownWrapper(
                viewModel = viewModel,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            DashboardStateContent(
                viewModel = viewModel,
                listState = listState,
                onDeleteHolding = { id, symbol, quantity ->
                    deleteConfirmation = DeleteConfirmation(id, symbol, quantity)
                },
                onEditHolding = onEditHolding
            )
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

private enum class DashboardStateType { Loading, Success, Error }

@Composable
private fun DashboardStateContent(
    viewModel: DashboardViewModel,
    listState: LazyListState,
    onDeleteHolding: (Long, String, Int) -> Unit,
    onEditHolding: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Only animate transitions between different state types, not data updates within Success
    val stateType = when (uiState) {
        is DashboardUiState.Loading -> DashboardStateType.Loading
        is DashboardUiState.Success -> DashboardStateType.Success
        is DashboardUiState.Error -> DashboardStateType.Error
    }

    Crossfade(
        targetState = stateType,
        label = "dashboardStateTransition"
    ) { type ->
        when (type) {
            DashboardStateType.Loading -> {
                SkeletonDashboard()
            }
            DashboardStateType.Success -> {
                val state = uiState as? DashboardUiState.Success ?: return@Crossfade
                DashboardContent(
                    stocks = state.stocks,
                    exchangeRate = state.exchangeRate,
                    periodReturns = state.periodReturns,
                    benchmarkReturns = state.benchmarkReturns,
                    selectedPeriod = state.selectedPeriod,
                    isLoadingPeriodReturns = state.isLoadingPeriodReturns,
                    onPeriodSelected = { viewModel.selectPeriod(it) },
                    showInKrw = state.showInKrw,
                    onCurrencyToggle = { viewModel.toggleCurrency() },
                    portfolioSparkline = state.portfolioSparkline,
                    portfolioSparklineTimestamps = state.portfolioSparklineTimestamps,
                    portfolioStats = state.portfolioStats,
                    sparklinePeriod = state.sparklinePeriod,
                    onSparklinePeriodSelected = { viewModel.selectSparklinePeriod(it) },
                    onDeleteHolding = onDeleteHolding,
                    onEditHolding = onEditHolding,
                    listState = listState,
                    isRefreshing = state.isRefreshing,
                    onRefresh = { viewModel.refresh() }
                )
            }
            DashboardStateType.Error -> {
                val state = uiState as? DashboardUiState.Error ?: return@Crossfade
                ErrorContent(
                    message = state.message,
                    title = "Failed to load prices",
                    onRetry = { viewModel.refresh() }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    stocks: List<Stock>,
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
    portfolioStats: PortfolioStats,
    sparklinePeriod: TimePeriod,
    onSparklinePeriodSelected: (TimePeriod) -> Unit,
    onDeleteHolding: (Long, String, Int) -> Unit,
    onEditHolding: (Long) -> Unit,
    listState: LazyListState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPortfolioValue = stocks.sumOf { it.totalValueInUsd(exchangeRate) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
        indicator = {}  // Hide center indicator - refresh state shown in top bar only
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item {
            PortfolioSummary(
                stocks = stocks,
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
                portfolioStats = portfolioStats,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (stocks.isNotEmpty()) {
            item {
                val totalValueForDisplay = if (showInKrw) {
                    stocks.sumOf { it.totalValueInKrw(exchangeRate) }
                } else {
                    totalPortfolioValue
                }
                AllocationPieChart(
                    items = createAllocationItems(stocks, totalPortfolioValue, exchangeRate),
                    totalValue = totalValueForDisplay,
                    showInKrw = showInKrw
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
                onEditAccountHolding = onEditHolding.takeIf { isAggregated },
                modifier = Modifier.animateItemPlacement()
            )
        }
        }
    }
}

@Composable
private fun DashboardTopBarWrapper(
    viewModel: DashboardViewModel,
    onOpenDrawer: () -> Unit,
    onManageAccounts: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing = (uiState as? DashboardUiState.Success)?.isRefreshing ?: false

    DashboardTopBar(
        isRefreshing = isRefreshing,
        onOpenDrawer = onOpenDrawer,
        onManageAccounts = onManageAccounts,
        onRefresh = { viewModel.refresh() }
    )
}

private data class AccountTabState(
    val accounts: List<AccountWithCount>,
    val selectedAccountId: Long
)

@Composable
private fun AccountDropdownWrapper(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val initialState = AccountTabState(emptyList(), ALL_ACCOUNTS_ID)
    val tabState by remember {
        viewModel.uiState
            .map { state ->
                val success = state as? DashboardUiState.Success
                AccountTabState(
                    accounts = success?.accounts ?: emptyList(),
                    selectedAccountId = success?.selectedAccountId ?: ALL_ACCOUNTS_ID
                )
            }
            .runningFold(initialState) { prev, new ->
                // Keep previous accounts during Loading state to prevent component removal
                AccountTabState(
                    accounts = new.accounts.ifEmpty { prev.accounts },
                    selectedAccountId = new.selectedAccountId
                )
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialState)

    if (tabState.accounts.isNotEmpty()) {
        AccountDropdown(
            accounts = tabState.accounts,
            selectedAccountId = tabState.selectedAccountId,
            onAccountSelected = { viewModel.selectAccount(it) },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    isRefreshing: Boolean,
    onOpenDrawer: () -> Unit,
    onManageAccounts: () -> Unit,
    onRefresh: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Open menu"
                )
            }
        },
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

@Composable
private fun AccountDropdown(
    accounts: List<AccountWithCount>,
    selectedAccountId: Long,
    onAccountSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val totalHoldings = accounts.sumOf { it.holdingsCount }

    val selectedLabel = if (selectedAccountId == ALL_ACCOUNTS_ID) {
        "All ($totalHoldings)"
    } else {
        accounts.find { it.account.id == selectedAccountId }?.let {
            "${it.account.name} (${it.holdingsCount})"
        } ?: "All ($totalHoldings)"
    }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = selectedLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Icon(
                imageVector = Icons.Filled.ArrowDropDown,
                contentDescription = "Select account",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = "All ($totalHoldings)",
                        fontWeight = if (selectedAccountId == ALL_ACCOUNTS_ID) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = {
                    onAccountSelected(ALL_ACCOUNTS_ID)
                    expanded = false
                }
            )
            accounts.forEach { accountWithCount ->
                val isSelected = selectedAccountId == accountWithCount.account.id
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${accountWithCount.account.name} (${accountWithCount.holdingsCount})",
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onAccountSelected(accountWithCount.account.id)
                        expanded = false
                    }
                )
            }
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
