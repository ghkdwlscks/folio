package com.portfolio.manager.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.portfolio.manager.presentation.screen.HouseholdScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val ADD_HOLDING = "add_holding/{accountId}"
    const val EDIT_HOLDING = "edit_holding/{holdingId}"
    const val ADD_CASH = "add_cash/{accountId}"
    const val EDIT_CASH = "edit_cash/{cashItemId}"
    const val ACCOUNTS = "accounts"
    const val FIRE_CALCULATOR = "fire_calculator"
    const val HOUSEHOLD = "household"

    fun addHolding(accountId: Long) = "add_holding/$accountId"
    fun editHolding(holdingId: Long) = "edit_holding/$holdingId"
    fun addCash(accountId: Long) = "add_cash/$accountId"
    fun editCash(cashItemId: Long) = "edit_cash/$cashItemId"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val transitionDuration = 300

    NavHost(
        navController = navController,
        startDestination = Routes.DASHBOARD,
        modifier = modifier,
        enterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(transitionDuration))
        },
        exitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.Start,
                animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(transitionDuration / 2))
        },
        popEnterTransition = {
            slideIntoContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(transitionDuration))
        },
        popExitTransition = {
            slideOutOfContainer(
                towards = AnimatedContentTransitionScope.SlideDirection.End,
                animationSpec = tween(transitionDuration, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(transitionDuration / 2))
        }
    ) {
        composable(
            route = Routes.DASHBOARD,
            enterTransition = { fadeIn(animationSpec = tween(transitionDuration)) },
            exitTransition = {
                fadeOut(animationSpec = tween(transitionDuration / 2))
            }
        ) {
            val viewModel = hiltViewModel<com.portfolio.manager.presentation.viewmodel.DashboardViewModel>()
            DashboardScreen(
                viewModel = viewModel,
                onAddHolding = {
                    val accountId = viewModel.getSelectedAccountId()
                    navController.navigate(Routes.addHolding(accountId))
                },
                onAddCash = {
                    val accountId = viewModel.getSelectedAccountId()
                    navController.navigate(Routes.addCash(accountId))
                },
                onManageAccounts = {
                    navController.navigate(Routes.ACCOUNTS)
                },
                onEditHolding = { holdingId ->
                    navController.navigate(Routes.editHolding(holdingId))
                },
                onEditCash = { cashItemId ->
                    navController.navigate(Routes.editCash(cashItemId))
                },
                onNavigateToFIRE = {
                    navController.navigate(Routes.FIRE_CALCULATOR)
                },
                onNavigateToHousehold = {
                    navController.navigate(Routes.HOUSEHOLD)
                }
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
        composable(Routes.HOUSEHOLD) {
            HouseholdScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
