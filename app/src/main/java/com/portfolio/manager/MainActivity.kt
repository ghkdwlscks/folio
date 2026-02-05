package com.portfolio.manager

import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController

import dagger.hilt.android.AndroidEntryPoint

import com.portfolio.manager.presentation.navigation.NavGraph
import com.portfolio.manager.presentation.theme.PortfolioManagerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PortfolioManagerTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}
