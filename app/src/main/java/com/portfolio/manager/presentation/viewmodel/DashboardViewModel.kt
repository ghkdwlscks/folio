package com.portfolio.manager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.StockRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AccountWithCount(
    val account: AccountEntity,
    val holdingsCount: Int
)

const val ALL_ACCOUNTS_ID = -1L

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val stocks: List<Stock>,
        val accounts: List<AccountWithCount> = emptyList(),
        val selectedAccountId: Long = 1L
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

class DashboardViewModel(
    private val stockRepository: StockRepository,
    private val holdingsRepository: HoldingsRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var selectedAccountId: Long = ALL_ACCOUNTS_ID
    private var holdingsJob: Job? = null

    init {
        viewModelScope.launch {
            // Ensure default account exists
            if (accountRepository.getAccountCount() == 0) {
                accountRepository.addAccount(AccountEntity(name = "Default"))
            }
            observeHoldings()
        }
    }

    fun selectAccount(accountId: Long) {
        selectedAccountId = accountId
        observeHoldings()
    }

    fun refresh() {
        observeHoldings()
    }

    fun getSelectedAccountId(): Long = selectedAccountId

    private fun observeHoldings() {
        holdingsJob?.cancel()
        holdingsJob = viewModelScope.launch {
            val holdingsFlow = if (selectedAccountId == ALL_ACCOUNTS_ID) {
                holdingsRepository.getAllHoldings()
            } else {
                holdingsRepository.getHoldingsByAccount(selectedAccountId)
            }

            combine(
                holdingsFlow,
                accountRepository.getAllAccounts()
            ) { holdings, accounts ->
                Pair(holdings, accounts)
            }.collectLatest { (holdings, accounts) ->
                val accountsWithCount = accounts.map { account ->
                    AccountWithCount(
                        account = account,
                        holdingsCount = holdingsRepository.getHoldingsCountByAccount(account.id)
                    )
                }
                loadPricesForHoldings(holdings, accountsWithCount)
            }
        }
    }

    private suspend fun loadPricesForHoldings(holdings: List<HoldingEntity>, accounts: List<AccountWithCount>) {
        if (holdings.isEmpty()) {
            _uiState.value = DashboardUiState.Success(
                stocks = emptyList(),
                accounts = accounts,
                selectedAccountId = selectedAccountId
            )
            return
        }

        _uiState.value = DashboardUiState.Loading

        val symbols = holdings.map { it.symbol }
        val result = stockRepository.getQuotes(symbols)

        result.fold(
            onSuccess = { quotes ->
                val stocks = holdings.map { holding ->
                    val quote = quotes.find { it.symbol == holding.symbol }
                    val isKoreanStock = holding.symbol.endsWith(".KS") || holding.symbol.endsWith(".KQ")
                    Stock(
                        id = holding.id,
                        symbol = holding.symbol,
                        name = if (isKoreanStock) holding.name else (quote?.shortName ?: quote?.longName ?: holding.name),
                        quantity = holding.quantity,
                        averagePrice = holding.averagePrice,
                        currentPrice = quote?.regularMarketPrice ?: holding.averagePrice,
                        dayChange = quote?.regularMarketChange,
                        dayChangePercent = quote?.regularMarketChangePercent,
                        currency = holding.currency
                    )
                }
                _uiState.value = DashboardUiState.Success(
                    stocks = stocks,
                    accounts = accounts,
                    selectedAccountId = selectedAccountId
                )
            },
            onFailure = { exception ->
                _uiState.value = DashboardUiState.Error(
                    exception.message ?: "Failed to load prices"
                )
            }
        )
    }
}
