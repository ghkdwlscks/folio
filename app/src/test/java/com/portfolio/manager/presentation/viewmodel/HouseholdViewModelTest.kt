package com.portfolio.manager.presentation.viewmodel

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.domain.model.Currency
import com.portfolio.manager.domain.model.Stock
import com.portfolio.manager.domain.service.PortfolioCache
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test

class HouseholdViewModelTest {

    private lateinit var portfolioCache: PortfolioCache
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private val prefValues = mutableMapOf<String, Any>("dashboard_show_in_krw" to true)

    private val sampleStock = Stock(
        id = 1L,
        symbol = "AAPL",
        name = "Apple",
        quantity = 2,
        averagePrice = 100.0,
        currentPrice = 150.0,
        currency = Currency.USD
    )

    @Before
    fun setup() {
        portfolioCache = PortfolioCache()
        sharedPreferences = mockk()
        editor = mockk(relaxed = true)
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } answers {
            prefValues[firstArg()] = secondArg<Boolean>()
            editor
        }
        every { sharedPreferences.getBoolean("dashboard_show_in_krw", any()) } answers {
            prefValues["dashboard_show_in_krw"] as Boolean
        }
    }

    private fun createViewModel() = HouseholdViewModel(portfolioCache, sharedPreferences)

    @Test
    fun `init - with cached data - emits success`() {
        portfolioCache.update(listOf(sampleStock), emptyList(), 1300.0)

        val vm = createViewModel()

        val state = vm.uiState.value
        assertThat(state).isInstanceOf(DashboardUiState.Success::class.java)
        state as DashboardUiState.Success
        assertThat(state.stocks).containsExactly(sampleStock)
        assertThat(state.exchangeRate).isEqualTo(1300.0)
        assertThat(state.showInKrw).isTrue()
    }

    @Test
    fun `init - without cached data - emits loading`() {
        val vm = createViewModel()

        assertThat(vm.uiState.value).isInstanceOf(DashboardUiState.Loading::class.java)
    }

    @Test
    fun `refresh - after cache populated - emits success`() {
        val vm = createViewModel()
        assertThat(vm.uiState.value).isInstanceOf(DashboardUiState.Loading::class.java)

        portfolioCache.update(listOf(sampleStock), emptyList(), 1300.0)
        vm.refresh()

        assertThat(vm.uiState.value).isInstanceOf(DashboardUiState.Success::class.java)
    }

    @Test
    fun `toggleCurrency - on success - flips and persists`() {
        portfolioCache.update(listOf(sampleStock), emptyList(), 1300.0)
        val vm = createViewModel()

        vm.toggleCurrency()

        val state = vm.uiState.value as DashboardUiState.Success
        assertThat(state.showInKrw).isFalse()
        assertThat(prefValues["dashboard_show_in_krw"]).isEqualTo(false)
    }

    @Test
    fun `toggleCurrency - on loading - persists but stays loading`() {
        val vm = createViewModel()

        vm.toggleCurrency()

        assertThat(vm.uiState.value).isInstanceOf(DashboardUiState.Loading::class.java)
        assertThat(prefValues["dashboard_show_in_krw"]).isEqualTo(false)
    }
}
