package com.portfolio.manager.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.model.StockAccountDetail
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.domain.repository.StockRepository
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID
import com.portfolio.manager.util.isKoreanStock
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountWithCount(
    val account: AccountEntity,
    val holdingsCount: Int
)

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val stocks: List<Stock>,
        val accounts: List<AccountWithCount> = emptyList(),
        val selectedAccountId: Long = 1L
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
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

    fun deleteHolding(holdingId: Long) {
        viewModelScope.launch {
            holdingsRepository.deleteHolding(holdingId)
        }
    }

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
                loadPricesForHoldings(holdings, accountsWithCount, accounts)
            }
        }
    }

    private suspend fun loadPricesForHoldings(
        holdings: List<HoldingEntity>,
        accounts: List<AccountWithCount>,
        allAccounts: List<AccountEntity>
    ) {
        if (holdings.isEmpty()) {
            _uiState.value = DashboardUiState.Success(
                stocks = emptyList(),
                accounts = accounts,
                selectedAccountId = selectedAccountId
            )
            return
        }

        _uiState.value = DashboardUiState.Loading

        val symbols = holdings.map { it.symbol }.distinct()
        val result = stockRepository.getQuotes(symbols)

        result.fold(
            onSuccess = { quotes ->
                val stocks = if (selectedAccountId == ALL_ACCOUNTS_ID) {
                    // Aggregate holdings by symbol when viewing all accounts
                    aggregateHoldings(holdings, quotes, allAccounts)
                } else {
                    // Normal view for single account
                    holdings.map { holding ->
                        val quote = quotes.find { it.symbol == holding.symbol }
                        val stockName = if (holding.symbol.isKoreanStock()) {
                            quote?.longName ?: quote?.shortName ?: holding.name
                        } else {
                            quote?.shortName ?: quote?.longName ?: holding.name
                        }
                        Stock(
                            id = holding.id,
                            symbol = holding.symbol,
                            name = stockName,
                            quantity = holding.quantity,
                            averagePrice = holding.averagePrice,
                            currentPrice = quote?.regularMarketPrice ?: holding.averagePrice,
                            dayChange = quote?.regularMarketChange,
                            dayChangePercent = quote?.regularMarketChangePercent,
                            currency = holding.currency
                        )
                    }
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

    private fun aggregateHoldings(
        holdings: List<HoldingEntity>,
        quotes: List<com.portfolio.manager.data.remote.dto.QuoteResult>,
        accounts: List<AccountEntity>
    ): List<Stock> {
        val accountMap = accounts.associateBy { it.id }
        val accountOrderMap = accounts.associate { it.id to it.orderIndex }

        return holdings.groupBy { it.symbol }.map { (symbol, holdingGroup) ->
            val quote = quotes.find { it.symbol == symbol }
            val firstHolding = holdingGroup.first()

            val totalQuantity = holdingGroup.sumOf { it.quantity }
            val totalCost = holdingGroup.sumOf { it.quantity * it.averagePrice }
            val weightedAvgPrice = if (totalQuantity > 0) totalCost / totalQuantity else 0.0

            val accountDetails = holdingGroup.map { holding ->
                StockAccountDetail(
                    holdingId = holding.id,
                    accountId = holding.accountId,
                    accountName = accountMap[holding.accountId]?.name ?: "Unknown",
                    quantity = holding.quantity,
                    averagePrice = holding.averagePrice
                )
            }.sortedBy { accountOrderMap[it.accountId] ?: Int.MAX_VALUE }

            val stockName = if (symbol.isKoreanStock()) {
                quote?.longName ?: quote?.shortName ?: firstHolding.name
            } else {
                quote?.shortName ?: quote?.longName ?: firstHolding.name
            }

            Stock(
                id = firstHolding.id,
                symbol = symbol,
                name = stockName,
                quantity = totalQuantity,
                averagePrice = weightedAvgPrice,
                currentPrice = quote?.regularMarketPrice ?: weightedAvgPrice,
                dayChange = quote?.regularMarketChange,
                dayChangePercent = quote?.regularMarketChangePercent,
                currency = firstHolding.currency,
                accountDetails = accountDetails
            )
        }
    }
}
