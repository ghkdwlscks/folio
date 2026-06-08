package com.portfolio.manager.presentation.viewmodel

import android.content.SharedPreferences

import androidx.lifecycle.ViewModel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import com.portfolio.manager.domain.service.PortfolioCache
import com.portfolio.manager.util.PreferenceKeys
import com.portfolio.manager.util.boolean

/**
 * Drives the dedicated household (combined-portfolio) screen.
 *
 * Phase 3a scaffold: renders the portfolio already computed for the dashboard
 * (shared via [PortfolioCache]) so the new screen and navigation are wired and
 * installable. Phase 3 (full pipeline) replaces the data source with merged
 * (my + partner) holdings repriced from the network.
 */
@HiltViewModel
class HouseholdViewModel @Inject constructor(
    private val portfolioCache: PortfolioCache,
    private val sharedPreferences: SharedPreferences
) : ViewModel() {

    private var showInKrw by sharedPreferences.boolean(PreferenceKeys.DASHBOARD_SHOW_IN_KRW, true)

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
        val current = _uiState.value
        if (current is DashboardUiState.Success) {
            _uiState.value = current.copy(showInKrw = showInKrw)
        }
    }

    private fun load() {
        if (!portfolioCache.hasData()) {
            _uiState.value = DashboardUiState.Loading
            return
        }
        _uiState.value = DashboardUiState.Success(
            stocks = portfolioCache.stocks,
            cashItems = portfolioCache.cashItems,
            exchangeRate = portfolioCache.exchangeRate,
            showInKrw = showInKrw,
            isRefreshing = false
        )
    }
}
