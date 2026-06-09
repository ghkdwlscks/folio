package com.portfolio.manager.presentation.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.portfolio.manager.presentation.component.PortfolioContent
import com.portfolio.manager.presentation.viewmodel.DashboardUiState
import com.portfolio.manager.presentation.viewmodel.HouseholdViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdScreen(
    viewModel: HouseholdViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToShare: () -> Unit,
    onNavigateToFIRE: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val isRefreshing = (uiState as? DashboardUiState.Success)?.isRefreshing == true

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Household", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToFIRE) {
                        Icon(
                            imageVector = Icons.Outlined.LocalFireDepartment,
                            contentDescription = "FIRE Calculator"
                        )
                    }
                    IconButton(onClick = { viewModel.refresh() }, enabled = !isRefreshing) {
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
                    IconButton(onClick = onNavigateToShare) {
                        Icon(
                            imageVector = Icons.Outlined.GroupAdd,
                            contentDescription = "Household sharing"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is DashboardUiState.Success -> {
                    PortfolioContent(
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
                        onDeleteHolding = { _, _, _ -> },
                        onEditHolding = {},
                        onDeleteCash = { _, _ -> },
                        onEditCash = {},
                        listState = listState,
                        isRefreshing = state.isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        accountOwnerLabels = state.ownerLabels,
                        readOnly = true
                    )
                }

                is DashboardUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is DashboardUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message)
                    }
                }
            }
        }
    }
}
