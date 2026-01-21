package com.portfolio.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.portfolio.manager.presentation.navigation.NavGraph
import com.portfolio.manager.presentation.navigation.Routes
import com.portfolio.manager.presentation.theme.PortfolioManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PortfolioManagerTheme {
                val navController = rememberNavController()
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Portfolio Manager",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                                label = { Text("Dashboard") },
                                selected = currentRoute == Routes.DASHBOARD,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    if (currentRoute != Routes.DASHBOARD) {
                                        navController.navigate(Routes.DASHBOARD) {
                                            popUpTo(Routes.DASHBOARD) { inclusive = true }
                                        }
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Outlined.Calculate, contentDescription = null) },
                                label = { Text("FIRE Calculator") },
                                selected = currentRoute == Routes.FIRE_CALCULATOR,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    if (currentRoute != Routes.FIRE_CALCULATOR) {
                                        navController.navigate(Routes.FIRE_CALCULATOR)
                                    }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                        }
                    }
                ) {
                    NavGraph(
                        navController = navController,
                        onOpenDrawer = { scope.launch { drawerState.open() } }
                    )
                }
            }
        }
    }
}
