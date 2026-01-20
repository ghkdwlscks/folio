package com.portfolio.manager.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.portfolio.manager.presentation.screen.AccountsScreen
import com.portfolio.manager.presentation.screen.AddHoldingScreen
import com.portfolio.manager.presentation.screen.DashboardScreen
import com.portfolio.manager.presentation.viewmodel.AccountsViewModel
import com.portfolio.manager.presentation.viewmodel.AddHoldingViewModel
import com.portfolio.manager.presentation.viewmodel.DashboardViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_HOLDING = "add_holding"
    const val ACCOUNTS = "accounts"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    dashboardViewModel: DashboardViewModel,
    addHoldingViewModelProvider: (accountId: Long) -> AddHoldingViewModel,
    accountsViewModelProvider: () -> AccountsViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onAddHolding = { navController.navigate(Routes.ADD_HOLDING) },
                onManageAccounts = { navController.navigate(Routes.ACCOUNTS) }
            )
        }
        composable(Routes.ADD_HOLDING) {
            val accountId = dashboardViewModel.getSelectedAccountId()
            AddHoldingScreen(
                viewModel = addHoldingViewModelProvider(accountId),
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
