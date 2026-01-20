package com.portfolio.manager.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.presentation.screen.AccountsScreen
import com.portfolio.manager.presentation.screen.AddHoldingScreen
import com.portfolio.manager.presentation.screen.DashboardScreen
import com.portfolio.manager.presentation.viewmodel.AccountsViewModel
import com.portfolio.manager.presentation.viewmodel.AddHoldingViewModel
import com.portfolio.manager.presentation.viewmodel.DashboardUiState
import com.portfolio.manager.presentation.viewmodel.DashboardViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_HOLDING = "add_holding"
    const val EDIT_HOLDING = "edit_holding/{holdingId}"
    const val ACCOUNTS = "accounts"

    fun editHolding(holdingId: Long) = "edit_holding/$holdingId"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    dashboardViewModel: DashboardViewModel,
    addHoldingViewModelProvider: (accountId: Long, accounts: List<AccountEntity>, holdingId: Long?) -> AddHoldingViewModel,
    accountsViewModelProvider: () -> AccountsViewModel
) {
    val uiState by dashboardViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onAddHolding = { navController.navigate(Routes.ADD_HOLDING) },
                onManageAccounts = { navController.navigate(Routes.ACCOUNTS) },
                onEditHolding = { holdingId -> navController.navigate(Routes.editHolding(holdingId)) }
            )
        }
        composable(Routes.ADD_HOLDING) {
            val accountId = dashboardViewModel.getSelectedAccountId()
            val accounts = when (val state = uiState) {
                is DashboardUiState.Success -> state.accounts.map { it.account }
                else -> emptyList()
            }
            AddHoldingScreen(
                viewModel = addHoldingViewModelProvider(accountId, accounts, null),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.EDIT_HOLDING,
            arguments = listOf(navArgument("holdingId") { type = NavType.LongType })
        ) { backStackEntry ->
            val holdingId = backStackEntry.arguments?.getLong("holdingId") ?: return@composable
            val accountId = dashboardViewModel.getSelectedAccountId()
            val accounts = when (val state = uiState) {
                is DashboardUiState.Success -> state.accounts.map { it.account }
                else -> emptyList()
            }
            AddHoldingScreen(
                viewModel = addHoldingViewModelProvider(accountId, accounts, holdingId),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ACCOUNTS) {
            AccountsScreen(
                viewModel = accountsViewModelProvider(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
