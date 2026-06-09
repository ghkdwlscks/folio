package com.portfolio.manager

import android.content.Context
import android.os.Bundle

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController

import dagger.hilt.android.AndroidEntryPoint

import com.portfolio.manager.presentation.navigation.NavGraph
import com.portfolio.manager.presentation.theme.AppLanguage
import com.portfolio.manager.presentation.theme.FolioLocalization
import com.portfolio.manager.presentation.theme.FolioTheme
import com.portfolio.manager.util.PreferenceKeys

import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFERENCES_NAME = "portfolio_prefs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val sharedPreferences = remember {
                getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            }
            var language by remember {
                mutableStateOf(
                    AppLanguage.fromCode(sharedPreferences.getString(PreferenceKeys.APP_LANGUAGE, null))
                        ?: AppLanguage.fromLocale(Locale.getDefault())
                )
            }

            FolioTheme {
                FolioLocalization(language = language) {
                    val navController = rememberNavController()
                    NavGraph(
                        navController = navController,
                        appLanguage = language,
                        onLanguageSelected = { selectedLanguage ->
                            language = selectedLanguage
                            sharedPreferences.edit()
                                .putString(PreferenceKeys.APP_LANGUAGE, selectedLanguage.code)
                                .apply()
                        }
                    )
                }
            }
        }
    }
}
