package com.portfolio.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.portfolio.manager.data.local.AppDatabase
import com.portfolio.manager.data.remote.YahooFinanceApi
import com.portfolio.manager.data.repository.AccountRepositoryImpl
import com.portfolio.manager.data.repository.HoldingsRepositoryImpl
import com.portfolio.manager.data.repository.StockRepositoryImpl
import com.portfolio.manager.presentation.navigation.NavGraph
import com.portfolio.manager.presentation.theme.PortfolioManagerTheme
import com.portfolio.manager.presentation.viewmodel.AccountsViewModel
import com.portfolio.manager.presentation.viewmodel.AddHoldingViewModel
import com.portfolio.manager.presentation.viewmodel.DashboardViewModel
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val json = Json { ignoreUnknownKeys = true }

    private val headersInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .header("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(headersInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(YahooFinanceApi.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private val api = retrofit.create(YahooFinanceApi::class.java)
    private val stockRepository = StockRepositoryImpl(api)

    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "portfolio_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
    }

    private val holdingsRepository by lazy {
        HoldingsRepositoryImpl(database.holdingDao())
    }

    private val accountRepository by lazy {
        AccountRepositoryImpl(database.accountDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PortfolioManagerTheme {
                val navController = rememberNavController()
                val dashboardViewModel = viewModel {
                    DashboardViewModel(stockRepository, holdingsRepository, accountRepository)
                }
                NavGraph(
                    navController = navController,
                    dashboardViewModel = dashboardViewModel,
                    addHoldingViewModelProvider = { accountId ->
                        AddHoldingViewModel(holdingsRepository, accountId)
                    },
                    accountsViewModelProvider = {
                        AccountsViewModel(accountRepository)
                    }
                )
            }
        }
    }
}
