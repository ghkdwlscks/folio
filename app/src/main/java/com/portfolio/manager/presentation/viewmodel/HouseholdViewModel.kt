package com.portfolio.manager.presentation.viewmodel

import android.content.SharedPreferences

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import com.portfolio.manager.domain.model.CashItem
import com.portfolio.manager.domain.model.PortfolioSnapshot
import com.portfolio.manager.domain.model.SortOption
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.TimePeriod
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.CashRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.domain.repository.SyncRepository
import com.portfolio.manager.domain.service.BenchmarkDataService
import com.portfolio.manager.domain.service.CacheManager
import com.portfolio.manager.domain.service.CashItemMapper
import com.portfolio.manager.domain.service.HouseholdMerger
import com.portfolio.manager.domain.service.PeriodReturnsService
import com.portfolio.manager.domain.service.PortfolioSorter
import com.portfolio.manager.domain.service.SnapshotMapper
import com.portfolio.manager.domain.service.SparklineService
import com.portfolio.manager.domain.service.StockMapper
import com.portfolio.manager.util.AppConstants.KRW_TO_USD_RATE
import com.portfolio.manager.util.ErrorMessages
import com.portfolio.manager.util.PreferenceKeys
import com.portfolio.manager.util.boolean

/**
 * Drives the dedicated household (combined-portfolio) screen.
 *
 * Reads my holdings/accounts/cash, pulls the partner's snapshot (when paired),
 * merges them via [HouseholdMerger], reprices the combined holdings, and reuses
 * the dashboard's computation services to produce a [DashboardUiState] — the
 * same state shape the dashboard renders, so both screens share the layout.
 *
 * Always the aggregated household view: no per-account caching, account filter,
 * incremental updates, or rebalance (those are dashboard-only concerns).
 */
@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val stockRepository: StockRepository,
    private val holdingsRepository: HoldingsRepository,
    private val accountRepository: AccountRepository,
    private val cashRepository: CashRepository,
    private val syncRepository: SyncRepository,
    private val merger: HouseholdMerger,
    private val snapshotMapper: SnapshotMapper,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    companion object {
        private val PERIOD = TimePeriod.ONE_YEAR
        private const val DEFAULT_LABEL = "Me"
    }

    private var showInKrw by sharedPreferences.boolean(PreferenceKeys.DASHBOARD_SHOW_IN_KRW, true)
    private var currentExchangeRate = KRW_TO_USD_RATE

    private val cacheManager = CacheManager(sharedPreferences)

    private val householdCode: String?
        get() = sharedPreferences.getString(PreferenceKeys.HOUSEHOLD_CODE, null)

    private val myLabel: String
        get() = sharedPreferences.getString(PreferenceKeys.HOUSEHOLD_MY_LABEL, null) ?: DEFAULT_LABEL

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        load()
    }

    fun toggleCurrency() {
        showInKrw = !showInKrw
        load()
    }

    private fun load() {
        viewModelScope.launch {
            stockRepository.getExchangeRate("USD", "KRW").onSuccess { currentExchangeRate = it }

            val myHoldings = holdingsRepository.getAllHoldings().first()
            val myAccounts = accountRepository.getAllAccounts().first()
            val myCash = cashRepository.getAllCashItems().first()

            val partner = syncHousehold(myHoldings, myAccounts, myCash)
            val merged = merger.merge(myAccounts, myHoldings, myCash, myLabel, partner)

            val cashItems = PortfolioSorter.sortCashItems(
                CashItemMapper.fromEntities(merged.cashItems), SortOption.WEIGHT, currentExchangeRate
            )
            val symbols = merged.holdings.map { it.symbol }.distinct()

            if (symbols.isEmpty()) {
                val base = successOf(emptyList(), cashItems, merged.ownerLabels)
                _uiState.value = base
                if (cashItems.isNotEmpty()) {
                    _uiState.value = withAnalytics(base, emptyList(), cashItems)
                }
                return@launch
            }

            val quotes = stockRepository.getQuotes(symbols).getOrElse { error ->
                _uiState.value = DashboardUiState.Error(error.message ?: ErrorMessages.LOAD_PRICES_FAILED)
                return@launch
            }

            val priceHistoryMap = stockRepository.getPriceHistory(symbols, PERIOD.range)
            val stocks = PortfolioSorter.sortStocks(
                merged.holdings.groupBy { it.symbol }.map { (symbol, group) ->
                    StockMapper.aggregateHoldings(
                        group, quotes.find { it.symbol == symbol }, merged.accounts, priceHistoryMap[symbol]
                    )
                },
                SortOption.WEIGHT,
                currentExchangeRate
            )

            val base = successOf(stocks, cashItems, merged.ownerLabels)
            _uiState.value = base
            _uiState.value = withAnalytics(base, stocks, cashItems)
        }
    }

    /**
     * When paired, publishes my latest snapshot (best-effort) and returns the
     * partner's snapshot. Keeps both sides fresh on every screen open/refresh.
     */
    private suspend fun syncHousehold(
        myHoldings: List<com.portfolio.manager.data.local.HoldingEntity>,
        myAccounts: List<com.portfolio.manager.data.local.AccountEntity>,
        myCash: List<com.portfolio.manager.data.local.CashItemEntity>
    ): PortfolioSnapshot? {
        val code = householdCode ?: return null
        val uid = syncRepository.ensureSignedIn().getOrNull() ?: return null
        val mine = snapshotMapper.toSnapshot(myLabel, System.currentTimeMillis(), myAccounts, myHoldings, myCash)
        syncRepository.publishSnapshot(code, uid, mine)
        val partner = syncRepository.fetchPartnerSnapshot(code, uid).getOrNull()
        // Persist partner symbols so the dashboard cache pruner keeps their
        // price history while paired.
        cacheManager.save(
            PreferenceKeys.HOUSEHOLD_PARTNER_SYMBOLS_JSON,
            partner?.holdings?.map { it.symbol }?.distinct() ?: emptyList()
        )
        return partner
    }

    private fun successOf(
        stocks: List<Stock>,
        cashItems: List<CashItem>,
        ownerLabels: Map<Long, String>
    ) = DashboardUiState.Success(
        stocks = stocks,
        cashItems = cashItems,
        selectedPeriod = PERIOD,
        exchangeRate = currentExchangeRate,
        showInKrw = showInKrw,
        sparklinePeriod = PERIOD,
        isRefreshing = false,
        ownerLabels = ownerLabels
    )

    private suspend fun withAnalytics(
        base: DashboardUiState.Success,
        stocks: List<Stock>,
        cashItems: List<CashItem>
    ): DashboardUiState.Success {
        val sparkline = SparklineService.calculateSparkline(
            stockRepository, stocks, cashItems, PERIOD, showInKrw, currentExchangeRate
        )
        val periodReturns = PeriodReturnsService.calculateAllPeriodReturns(
            stockRepository, stocks, cashItems, showInKrw, currentExchangeRate
        )
        val benchmark = BenchmarkDataService.loadBenchmarkData(
            stockRepository, PERIOD, showInKrw, currentExchangeRate
        )
        return base.copy(
            periodReturns = periodReturns,
            benchmarkReturns = mapOf(PERIOD to benchmark.returns),
            portfolioSparkline = sparkline.sparkline,
            portfolioSparklineTimestamps = sparkline.timestamps,
            benchmarkSparklines = benchmark.sparklines,
            benchmarkTimestamps = benchmark.timestamps,
            portfolioStats = sparkline.stats
        )
    }
}
