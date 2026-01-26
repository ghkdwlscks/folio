package com.portfolio.manager.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.portfolio.manager.presentation.screen.AccountsScreen
import com.portfolio.manager.presentation.screen.AddCashScreen
import com.portfolio.manager.presentation.screen.AddHoldingScreen
import com.portfolio.manager.presentation.screen.DashboardScreen
import com.portfolio.manager.presentation.screen.FIRECalculatorScreen
import com.portfolio.manager.presentation.viewmodel.DashboardViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_HOLDING = "add_holding/{accountId}"
    const val EDIT_HOLDING = "edit_holding/{holdingId}"
    const val ADD_CASH = "add_cash/{accountId}"
    const val EDIT_CASH = "edit_cash/{cashItemId}"
    const val ACCOUNTS = "accounts"
    const val FIRE_CALCULATOR = "fire_calculator"

    fun addHolding(accountId: Long) = "add_holding/$accountId"
    fun editHolding(holdingId: Long) = "edit_holding/$holdingId"
    fun addCash(accountId: Long) = "add_cash/$accountId"
    fun editCash(cashItemId: Long) = "edit_cash/$cashItemId"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    onOpenDrawer: () -> Unit
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
                onAddCash = { navController.navigate(Routes.addCash(viewModel.getSelectedAccountId())) },
                onManageAccounts = { navController.navigate(Routes.ACCOUNTS) },
                onEditHolding = { holdingId -> navController.navigate(Routes.editHolding(holdingId)) },
                onEditCash = { cashItemId -> navController.navigate(Routes.editCash(cashItemId)) },
                onOpenDrawer = onOpenDrawer
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
        composable(
            route = Routes.ADD_CASH,
            arguments = listOf(navArgument("accountId") { type = NavType.LongType })
        ) {
            AddCashScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.EDIT_CASH,
            arguments = listOf(navArgument("cashItemId") { type = NavType.LongType })
        ) {
            AddCashScreen(
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
        composable(Routes.FIRE_CALCULATOR) {
            FIRECalculatorScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
