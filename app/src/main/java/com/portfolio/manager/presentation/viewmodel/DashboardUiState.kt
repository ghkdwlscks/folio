package com.portfolio.manager.presentation.viewmodel

import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.domain.model.BenchmarkReturns
import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.PortfolioStats
import com.portfolio.manager.domain.model.SortOption
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.util.AppConstants.KRW_TO_USD_RATE

/**
 * Represents an account with its holdings count for display in the account selector.
 */
@kotlinx.serialization.Serializable
data class AccountWithCount(
    val account: AccountEntity,
    val holdingsCount: Int,
    val needsRebalance: Boolean = false
)

/**
 * Sealed interface representing the possible states of the Dashboard screen.
 */
sealed interface DashboardUiState {
    /**
     * Loading state - shown while fetching initial data.
     */
    data object Loading : DashboardUiState

    /**
     * Success state - contains all data needed to render the dashboard.
     */
    data class Success(
        val stocks: List<Stock>,
        val cashItems: List<CashItem> = emptyList(),
        val accounts: List<AccountWithCount> = emptyList(),
        val selectedAccountId: Long = 1L,
        val filteredAccountIds: Set<Long> = emptySet(),
        val periodReturns: Map<TimePeriod, Double> = emptyMap(),
        val benchmarkReturns: Map<TimePeriod, BenchmarkReturns> = emptyMap(),
        val selectedPeriod: TimePeriod = TimePeriod.ONE_YEAR,
        val isLoadingPeriodReturns: Boolean = false,
        val isRefreshing: Boolean = false,
        val exchangeRate: Double = KRW_TO_USD_RATE,
        val showInKrw: Boolean = true,
        val sparklinePeriod: TimePeriod = TimePeriod.ONE_YEAR,
        val portfolioSparkline: List<Double> = emptyList(),
        val portfolioSparklineTimestamps: List<Long> = emptyList(),
        val benchmarkSparklines: Map<String, List<Double>> = emptyMap(),
        val benchmarkTimestamps: Map<String, List<Long>> = emptyMap(),
        val portfolioStats: PortfolioStats = PortfolioStats(),
        val sortOption: SortOption = SortOption.WEIGHT
    ) : DashboardUiState

    /**
     * Error state - shown when data fetching fails.
     */
    data class Error(val message: String) : DashboardUiState
}
