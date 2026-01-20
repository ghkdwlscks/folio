package com.portfolio.manager.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.portfolio.manager.presentation.screen.AddHoldingScreen
import com.portfolio.manager.presentation.screen.DashboardScreen
import com.portfolio.manager.presentation.viewmodel.AddHoldingViewModel
import com.portfolio.manager.presentation.viewmodel.DashboardViewModel

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_HOLDING = "add_holding"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    dashboardViewModel: DashboardViewModel,
    addHoldingViewModelProvider: () -> AddHoldingViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD
    ) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(
                viewModel = dashboardViewModel,
                onAddHolding = { navController.navigate(Routes.ADD_HOLDING) }
            )
        }
        composable(Routes.ADD_HOLDING) {
            AddHoldingScreen(
                viewModel = addHoldingViewModelProvider(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
