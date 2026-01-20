package com.portfolio.manager.presentation.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
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
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.presentation.component.PortfolioSummary
import com.portfolio.manager.presentation.component.StockCard
import com.portfolio.manager.presentation.viewmodel.AccountWithCount
import com.portfolio.manager.presentation.viewmodel.DashboardUiState
import com.portfolio.manager.presentation.viewmodel.DashboardViewModel
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID

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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    var deleteConfirmation by remember { mutableStateOf<DeleteConfirmation?>(null) }

    val accounts = when (val state = uiState) {
        is DashboardUiState.Success -> state.accounts
        else -> emptyList()
    }
    val selectedAccountId = when (val state = uiState) {
        is DashboardUiState.Success -> state.selectedAccountId
        else -> 1L
    }
    val isRefreshing = when (val state = uiState) {
        is DashboardUiState.Success -> state.isRefreshing
        else -> false
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            DashboardTopBar(
                scrollBehavior = scrollBehavior,
                accounts = accounts,
                selectedAccountId = selectedAccountId,
                isRefreshing = isRefreshing,
                onAccountSelected = { viewModel.selectAccount(it) },
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
                LoadingContent(modifier = Modifier.padding(paddingValues))
            }
            is DashboardUiState.Success -> {
                DashboardContent(
                    stocks = state.stocks,
                    periodReturns = state.periodReturns,
                    selectedPeriod = state.selectedPeriod,
                    isLoadingPeriodReturns = state.isLoadingPeriodReturns,
                    onPeriodSelected = { viewModel.selectPeriod(it) },
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
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Loading prices...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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
    periodReturns: Map<TimePeriod, Double>,
    selectedPeriod: TimePeriod,
    isLoadingPeriodReturns: Boolean,
    onPeriodSelected: (TimePeriod) -> Unit,
    onDeleteHolding: (Long, String, Int) -> Unit,
    onEditHolding: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalPortfolioValue = stocks.sumOf { it.totalValueInUsd }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PortfolioSummary(
                stocks = stocks,
                periodReturns = periodReturns,
                selectedPeriod = selectedPeriod,
                isLoadingPeriodReturns = isLoadingPeriodReturns,
                onPeriodSelected = onPeriodSelected,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            SectionHeader(
                title = "My Holdings",
                count = stocks.size
            )
        }

        items(
            items = stocks,
            key = { it.id }
        ) { stock ->
            val isAggregated = stock.accountDetails.isNotEmpty()
            val weightPercent = if (totalPortfolioValue > 0) {
                (stock.totalValueInUsd / totalPortfolioValue) * 100
            } else {
                0.0
            }
            StockCard(
                stock = stock,
                weightPercent = weightPercent,
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
    scrollBehavior: TopAppBarScrollBehavior,
    accounts: List<AccountWithCount>,
    selectedAccountId: Long,
    isRefreshing: Boolean,
    onAccountSelected: (Long) -> Unit,
    onManageAccounts: () -> Unit,
    onRefresh: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAccount = accounts.find { it.account.id == selectedAccountId }
    val totalHoldings = accounts.sumOf { it.holdingsCount }
    val selectedName = if (selectedAccountId == ALL_ACCOUNTS_ID) {
        "All"
    } else {
        selectedAccount?.account?.name ?: "Select Account"
    }

    LargeTopAppBar(
        title = {
            Column {
                Text(
                    text = "Portfolio",
                    fontWeight = FontWeight.Bold
                )
                if (accounts.isNotEmpty()) {
                    Box {
                        Row(
                            modifier = Modifier.clickable { expanded = true },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Select Account",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All ($totalHoldings)") },
                                onClick = {
                                    onAccountSelected(ALL_ACCOUNTS_ID)
                                    expanded = false
                                }
                            )
                            accounts.forEach { accountWithCount ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${accountWithCount.account.name} (${accountWithCount.holdingsCount})")
                                    },
                                    onClick = {
                                        onAccountSelected(accountWithCount.account.id)
                                        expanded = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Manage Accounts...",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    expanded = false
                                    onManageAccounts()
                                }
                            )
                        }
                    }
                }
            }
        },
        actions = {
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
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
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
}
