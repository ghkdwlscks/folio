package com.portfolio.manager.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.portfolio.manager.presentation.screen.AccountsScreen
import com.portfolio.manager.presentation.screen.AddHoldingScreen
import com.portfolio.manager.presentation.screen.DashboardScreen
import com.portfolio.manager.presentation.viewmodel.DashboardViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_HOLDING = "add_holding/{accountId}"
    const val EDIT_HOLDING = "edit_holding/{holdingId}"
    const val ACCOUNTS = "accounts"

    fun addHolding(accountId: Long) = "add_holding/$accountId"
    fun editHolding(holdingId: Long) = "edit_holding/$holdingId"
}

@Composable
fun NavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            val viewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = viewModel,
                onAddHolding = { navController.navigate(Routes.addHolding(viewModel.getSelectedAccountId())) },
                onManageAccounts = { navController.navigate(Routes.ACCOUNTS) },
                onEditHolding = { holdingId -> navController.navigate(Routes.editHolding(holdingId)) }
            )
        }
        composable(
            route = Routes.ADD_HOLDING,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) {
            AddHoldingScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.EDIT_HOLDING,
            arguments = listOf(navArgument("holdingId") { type = NavType.LongType })
        ) {
            AddHoldingScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ACCOUNTS) {
            AccountsScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
