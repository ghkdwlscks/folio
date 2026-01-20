package com.portfolio.manager.presentation.viewmodel

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.repository.HoldingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddHoldingViewModelTest {

    private lateinit var repository: HoldingsRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() = runTest {
        val viewModel = AddHoldingViewModel(repository, 1L)

        assertThat(viewModel.symbol.value).isEmpty()
        assertThat(viewModel.name.value).isEmpty()
        assertThat(viewModel.quantity.value).isEmpty()
        assertThat(viewModel.averagePrice.value).isEmpty()
        assertThat(viewModel.currency.value).isEqualTo("USD")
    }

    @Test
    fun `saveHolding - valid input - saves and returns success`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val viewModel = AddHoldingViewModel(repository, 1L)

        viewModel.symbol.value = "AAPL"
        viewModel.name.value = "Apple Inc."
        viewModel.quantity.value = "10"
        viewModel.averagePrice.value = "150.00"
        viewModel.currency.value = "USD"

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify {
            repository.addHolding(match {
                it.accountId == 1L &&
                it.symbol == "AAPL" &&
                it.name == "Apple Inc." &&
                it.quantity == 10 &&
                it.averagePrice == 150.0 &&
                it.currency == "USD"
            })
        }
    }

    @Test
    fun `saveHolding - empty symbol - returns false`() = runTest {
        val viewModel = AddHoldingViewModel(repository, 1L)

        viewModel.symbol.value = ""
        viewModel.name.value = "Apple Inc."
        viewModel.quantity.value = "10"
        viewModel.averagePrice.value = "150.00"

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
    }

    @Test
    fun `saveHolding - invalid quantity - returns false`() = runTest {
        val viewModel = AddHoldingViewModel(repository, 1L)

        viewModel.symbol.value = "AAPL"
        viewModel.name.value = "Apple Inc."
        viewModel.quantity.value = "abc"
        viewModel.averagePrice.value = "150.00"

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
    }

    @Test
    fun `saveHolding - invalid price - returns false`() = runTest {
        val viewModel = AddHoldingViewModel(repository, 1L)

        viewModel.symbol.value = "AAPL"
        viewModel.name.value = "Apple Inc."
        viewModel.quantity.value = "10"
        viewModel.averagePrice.value = "invalid"

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
    }

    @Test
    fun `saveHolding - KRW currency - saves correctly`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val viewModel = AddHoldingViewModel(repository, 1L)

        viewModel.symbol.value = "005930.KS"
        viewModel.name.value = "삼성전자"
        viewModel.quantity.value = "50"
        viewModel.averagePrice.value = "72000"
        viewModel.currency.value = "KRW"

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify {
            repository.addHolding(match {
                it.accountId == 1L &&
                it.symbol == "005930.KS" &&
                it.currency == "KRW" &&
                it.averagePrice == 72000.0
            })
        }
    }

    @Test
    fun `saveHolding - duplicate symbol in account - returns false`() = runTest {
        val existingHolding = HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 5, 140.0, "USD")
        coEvery { repository.getHoldingByAccountAndSymbol(1L, "AAPL") } returns existingHolding
        val viewModel = AddHoldingViewModel(repository, 1L)

        viewModel.symbol.value = "AAPL"
        viewModel.name.value = "Apple Inc."
        viewModel.quantity.value = "10"
        viewModel.averagePrice.value = "150.00"

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
        assertThat(viewModel.errorMessage.value).isEqualTo("This stock already exists in the account")
        coVerify(exactly = 0) { repository.addHolding(any()) }
    }

    @Test
    fun `saveHolding - same symbol in different account - succeeds`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(2L, "AAPL") } returns null
        coEvery { repository.addHolding(any()) } returns 2L
        val viewModel = AddHoldingViewModel(repository, 2L)

        viewModel.symbol.value = "AAPL"
        viewModel.name.value = "Apple Inc."
        viewModel.quantity.value = "10"
        viewModel.averagePrice.value = "150.00"

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify { repository.addHolding(any()) }
    }
}
