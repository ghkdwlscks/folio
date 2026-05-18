package com.portfolio.manager.presentation.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.launch

import com.portfolio.manager.domain.model.BenchmarkReturns
import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.PortfolioStats
import com.portfolio.manager.domain.model.SortOption
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.presentation.component.AccountDropdown
import com.portfolio.manager.presentation.component.AccountFilterDialog
import com.portfolio.manager.presentation.component.AllocationItem
import com.portfolio.manager.presentation.component.AllocationPieChart
import com.portfolio.manager.presentation.component.CashCard
import com.portfolio.manager.presentation.component.DeleteConfirmationDialog
import com.portfolio.manager.presentation.component.ErrorContent
import com.portfolio.manager.presentation.component.PortfolioSummary
import com.portfolio.manager.presentation.component.RebalanceDialog
import com.portfolio.manager.presentation.util.rememberHapticFeedback
import com.portfolio.manager.presentation.component.RebalanceItem
import com.portfolio.manager.presentation.component.SectionHeader
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
 * Creates allocation items from stocks and cash for the pie chart.
 */
private fun createAllocationItems(
    stocks: List<Stock>,
    cashItems: List<CashItem>,
    totalPortfolioValue: Double,
    exchangeRate: Double
): List<AllocationItem> {
    // Create stock allocation items
    val stockItems = stocks.map { stock ->
        AllocationItem(
            symbol = stock.symbol,
            name = stock.name,
            value = stock.totalValueInUsd(exchangeRate),
            weight = calculateWeightPercent(stock, totalPortfolioValue, exchangeRate)
        )
    }

    // Create individual allocation items for each cash item
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

    // Combine and sort all items by weight
    return (stockItems + cashAllocationItems).sortedByDescending { it.weight }
}

private data class DeleteConfirmation(
    val holdingId: Long,
    val symbol: String,
    val quantity: Int
)

private data class CashDeleteConfirmation(
    val cashItemId: Long,
    val name: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddHolding: () -> Unit,
    onAddCash: () -> Unit,
    onManageAccounts: () -> Unit,
    onEditHolding: (Long) -> Unit,
    onEditCash: (Long) -> Unit,
    onNavigateToFIRE: () -> Unit,
    modifier: Modifier = Modifier
) {
    var deleteConfirmation by remember { mutableStateOf<DeleteConfirmation?>(null) }
    var cashDeleteConfirmation by remember { mutableStateOf<CashDeleteConfirmation?>(null) }
    var showRebalanceDialog by remember { mutableStateOf(false) }
    var showAddChoiceDialog by remember { mutableStateOf(false) }
    var rebalanceItems by remember { mutableStateOf<List<RebalanceItem>>(emptyList()) }
    var rebalanceInitialBand by remember { mutableStateOf<Int?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Reactively compute whether rebalance button should show
    val uiState by viewModel.uiState.collectAsState()
    val selectedAccountId = viewModel.getSelectedAccountId()
    val isSpecificAccount = selectedAccountId != ALL_ACCOUNTS_ID
    val canShowRebalance = remember(uiState, selectedAccountId) {
        isSpecificAccount &&
            (uiState as? DashboardUiState.Success)?.stocks?.isNotEmpty() == true
    }

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
                listState = listState,
                onNavigateToFIRE = onNavigateToFIRE,
                onManageAccounts = onManageAccounts
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = fabVisible,
                enter = slideInVertically(
                    initialOffsetY = { it * 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) + fadeIn() + scaleIn(initialScale = 0.8f),
                exit = slideOutVertically(targetOffsetY = { it * 2 }) + fadeOut() + scaleOut(targetScale = 0.8f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (canShowRebalance) {
                        FloatingActionButton(
                            onClick = {
                                coroutineScope.launch {
                                    try {
                                        val data = viewModel.getRebalanceFormData()
                                        rebalanceItems = data.items.map { item ->
                                            RebalanceItem(
                                                holdingId = item.holdingId,
                                                symbol = item.symbol,
                                                name = item.name,
                                                currentValue = item.currentValue,
                                                currentPrice = item.currentPrice,
                                                currentPercentage = item.currentPercentage,
                                                currency = item.currency,
                                                quantity = item.quantity
                                            )
                                        }
                                        rebalanceInitialBand = data.toleranceBandPercent
                                        showRebalanceDialog = true
                                    } catch (e: Exception) {
                                        android.util.Log.e("DashboardScreen", "Failed to open rebalance dialog", e)
                                    }
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Balance,
                                contentDescription = "Rebalance"
                            )
                        }
                    }
                    FloatingActionButton(
                        onClick = { showAddChoiceDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add"
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            AccountDropdownWrapper(
                viewModel = viewModel,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
            )
            DashboardStateContent(
                viewModel = viewModel,
                listState = listState,
                onDeleteHolding = { id, symbol, quantity ->
                    deleteConfirmation = DeleteConfirmation(id, symbol, quantity)
                },
                onEditHolding = onEditHolding,
                onDeleteCash = { id, name ->
                    cashDeleteConfirmation = CashDeleteConfirmation(id, name)
                },
                onEditCash = onEditCash
            )
        }
    }

    deleteConfirmation?.let { confirmation ->
        DeleteConfirmationDialog(
            title = "Delete Holding",
            message = "Are you sure you want to delete ${confirmation.symbol} (${confirmation.quantity} shares)?",
            onConfirm = {
                viewModel.deleteHolding(confirmation.holdingId)
                deleteConfirmation = null
            },
            onDismiss = { deleteConfirmation = null }
        )
    }

    cashDeleteConfirmation?.let { confirmation ->
        DeleteConfirmationDialog(
            title = "Delete Cash",
            message = "Are you sure you want to delete \"${confirmation.name}\"?",
            onConfirm = {
                viewModel.deleteCashItem(confirmation.cashItemId)
                cashDeleteConfirmation = null
            },
            onDismiss = { cashDeleteConfirmation = null }
        )
    }

    if (showRebalanceDialog && rebalanceItems.isNotEmpty()) {
        RebalanceDialog(
            items = rebalanceItems,
            initialToleranceBandPercent = rebalanceInitialBand,
            showInKrw = viewModel.isShowingInKrw(),
            onDismiss = { showRebalanceDialog = false },
            onSave = { percentages, band ->
                viewModel.saveRebalance(percentages, band)
                showRebalanceDialog = false
            },
            onReset = {
                viewModel.resetRebalance()
                showRebalanceDialog = false
            }
        )
    }

    if (showAddChoiceDialog) {
        val bottomSheetHaptic = rememberHapticFeedback()
        ModalBottomSheet(
            onDismissRequest = { showAddChoiceDialog = false },
            sheetState = rememberModalBottomSheetState(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Add to Portfolio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Button(
                    onClick = {
                        bottomSheetHaptic.click()
                        showAddChoiceDialog = false
                        onAddHolding()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ShowChart,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Add Stock Holding")
                }
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = {
                        bottomSheetHaptic.click()
                        showAddChoiceDialog = false
                        onAddCash()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Add Cash / Savings")
                }
            }
        }
    }

}

private enum class DashboardStateType { Loading, Success, Error }

@Composable
private fun DashboardStateContent(
    viewModel: DashboardViewModel,
    listState: LazyListState,
    onDeleteHolding: (Long, String, Int) -> Unit,
    onEditHolding: (Long) -> Unit,
    onDeleteCash: (Long, String) -> Unit,
    onEditCash: (Long) -> Unit
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
                    cashItems = state.cashItems,
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
                    benchmarkSparklines = state.benchmarkSparklines,
                    benchmarkTimestamps = state.benchmarkTimestamps,
                    portfolioStats = state.portfolioStats,
                    sortOption = state.sortOption,
                    onSortOptionSelected = { viewModel.selectSortOption(it) },
                    sparklinePeriod = state.sparklinePeriod,
                    onSparklinePeriodSelected = { viewModel.selectSparklinePeriod(it) },
                    onDeleteHolding = onDeleteHolding,
                    onEditHolding = onEditHolding,
                    onDeleteCash = onDeleteCash,
                    onEditCash = onEditCash,
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
                    targetWeight = stock.targetPercentage.takeIf { !isAggregated },
                    onDelete = { onDeleteHolding(stock.id, stock.symbol, stock.quantity) }.takeIf { !isAggregated },
                    onDeleteAccountHolding = { holdingId: Long ->
                        val detail = stock.accountDetails.find { it.holdingId == holdingId }
                        if (detail != null) {
                            onDeleteHolding(holdingId, stock.symbol, detail.quantity)
                        }
                    }.takeIf { isAggregated },
                    onEdit = { onEditHolding(stock.id) }.takeIf { !isAggregated },
                    onEditAccountHolding = onEditHolding.takeIf { isAggregated },
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
                    onEdit = { onEditCash(cashItem.id) },
                    onDelete = { onDeleteCash(cashItem.id, cashItem.name) },
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

@Composable
private fun DashboardTopBarWrapper(
    viewModel: DashboardViewModel,
    listState: LazyListState,
    onNavigateToFIRE: () -> Unit,
    onManageAccounts: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing = (uiState as? DashboardUiState.Success)?.isRefreshing ?: false
    val showCompactValue by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    val successState = uiState as? DashboardUiState.Success
    val compactTitle = if (successState != null) {
        val exchangeRate = successState.exchangeRate
        val totalValueUsd = successState.stocks.sumOf { it.totalValueInUsd(exchangeRate) } +
            successState.cashItems.sumOf { it.valueInUsd(exchangeRate) }
        val totalValue = if (successState.showInKrw) {
            successState.stocks.sumOf { it.totalValueInKrw(exchangeRate) } +
                successState.cashItems.sumOf { it.valueInKrw(exchangeRate) }
        } else totalValueUsd
        val formatter = com.portfolio.manager.presentation.util.CurrencyFormatter.createFormatter(successState.showInKrw)
        formatter(totalValue)
    } else null

    DashboardTopBar(
        isRefreshing = isRefreshing,
        showCompactValue = showCompactValue,
        compactTitle = compactTitle,
        onNavigateToFIRE = onNavigateToFIRE,
        onManageAccounts = onManageAccounts,
        onRefresh = { viewModel.refresh() }
    )
}

private data class AccountTabState(
    val accounts: List<AccountWithCount>,
    val selectedAccountId: Long,
    val filteredAccountIds: Set<Long> = emptySet()
)

@Composable
private fun AccountDropdownWrapper(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    var showFilterDialog by remember { mutableStateOf(false) }
    val initialState = AccountTabState(emptyList(), ALL_ACCOUNTS_ID)
    val tabState by remember {
        viewModel.uiState
            .map { state ->
                val success = state as? DashboardUiState.Success
                AccountTabState(
                    accounts = success?.accounts ?: emptyList(),
                    selectedAccountId = success?.selectedAccountId ?: ALL_ACCOUNTS_ID,
                    filteredAccountIds = success?.filteredAccountIds ?: emptySet()
                )
            }
            .runningFold(initialState) { prev, new ->
                // Keep previous accounts during Loading state to prevent component removal
                AccountTabState(
                    accounts = new.accounts.ifEmpty { prev.accounts },
                    selectedAccountId = new.selectedAccountId,
                    filteredAccountIds = new.filteredAccountIds
                )
            }
            .distinctUntilChanged()
    }.collectAsState(initial = initialState)

    if (tabState.accounts.isNotEmpty()) {
        AccountDropdown(
            accounts = tabState.accounts,
            selectedAccountId = tabState.selectedAccountId,
            filteredAccountIds = tabState.filteredAccountIds,
            onAccountSelected = { viewModel.selectAccount(it) },
            onFilterClick = { showFilterDialog = true },
            modifier = modifier
        )
    }

    if (showFilterDialog) {
        AccountFilterDialog(
            accounts = tabState.accounts,
            selectedAccountIds = tabState.filteredAccountIds,
            onApply = { selectedIds ->
                viewModel.updateAccountFilter(selectedIds)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    isRefreshing: Boolean,
    showCompactValue: Boolean = false,
    compactTitle: String? = null,
    onNavigateToFIRE: () -> Unit,
    onManageAccounts: () -> Unit,
    onRefresh: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Folio",
                    fontWeight = FontWeight.Bold,
                    style = if (showCompactValue && compactTitle != null) {
                        MaterialTheme.typography.titleMedium
                    } else {
                        MaterialTheme.typography.titleLarge
                    }
                )
                AnimatedVisibility(visible = showCompactValue && compactTitle != null) {
                    compactTitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(onClick = onNavigateToFIRE) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = "FIRE Calculator"
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
            IconButton(onClick = onManageAccounts) {
                Icon(
                    imageVector = Icons.Outlined.ManageAccounts,
                    contentDescription = "Manage Accounts"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

