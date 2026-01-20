package com.portfolio.manager.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private lateinit var accountRepository: AccountRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    private val defaultAccounts = listOf(
        AccountEntity(1, "Default", 0),
        AccountEntity(2, "Trading", 1)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        accountRepository = mockk()
        coEvery { accountRepository.getAllAccounts() } returns flowOf(defaultAccounts)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createSavedStateHandle(accountId: Long? = null, holdingId: Long? = null): SavedStateHandle {
        val map = mutableMapOf<String, Any?>()
        accountId?.let { map["accountId"] = it }
        holdingId?.let { map["holdingId"] = it }
        return SavedStateHandle(map)
    }

    @Test
    fun `initial state is empty`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        assertThat(viewModel.symbol.value).isEmpty()
        assertThat(viewModel.quantity.value).isEmpty()
        assertThat(viewModel.averagePrice.value).isEmpty()
        assertThat(viewModel.currency.value).isEqualTo("USD")
    }

    @Test
    fun `saveHolding - valid input - saves and returns success`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.symbol.value = "AAPL"
        viewModel.quantity.value = "10"
        viewModel.averagePrice.value = "150.00"
        viewModel.currency.value = "USD"

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify {
            repository.addHolding(match {
                it.accountId == 1L &&
                it.symbol == "AAPL" &&
                it.name == "AAPL" &&
                it.quantity == 10 &&
                it.averagePrice == 150.0 &&
                it.currency == "USD"
            })
        }
    }

    @Test
    fun `saveHolding - empty symbol - returns false`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.symbol.value = ""
        viewModel.quantity.value = "10"
        viewModel.averagePrice.value = "150.00"

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
    }

    @Test
    fun `saveHolding - invalid quantity - returns false`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.symbol.value = "AAPL"
        viewModel.quantity.value = "abc"
        viewModel.averagePrice.value = "150.00"

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
    }

    @Test
    fun `saveHolding - invalid price - returns false`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.symbol.value = "AAPL"
        viewModel.quantity.value = "10"
        viewModel.averagePrice.value = "invalid"

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
    }

    @Test
    fun `saveHolding - KRW currency - saves correctly`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.symbol.value = "005930.KS"
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
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.symbol.value = "AAPL"
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
        val savedStateHandle = createSavedStateHandle(accountId = 2L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.symbol.value = "AAPL"
        viewModel.quantity.value = "10"
        viewModel.averagePrice.value = "150.00"

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify { repository.addHolding(any()) }
    }

    @Test
    fun `needsAccountSelection - true when ALL_ACCOUNTS_ID with accounts`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = ALL_ACCOUNTS_ID)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        assertThat(viewModel.needsAccountSelection.value).isTrue()
        assertThat(viewModel.selectedAccountId.value).isEqualTo(1L)
    }

    @Test
    fun `needsAccountSelection - false when specific account`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        assertThat(viewModel.needsAccountSelection.value).isFalse()
    }

    @Test
    fun `selectAccount - updates selectedAccountId`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = ALL_ACCOUNTS_ID)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.selectAccount(2L)

        assertThat(viewModel.selectedAccountId.value).isEqualTo(2L)
    }

    @Test
    fun `saveHolding - uses selected account when ALL_ACCOUNTS_ID`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = ALL_ACCOUNTS_ID)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.selectAccount(2L)
        viewModel.symbol.value = "AAPL"
        viewModel.quantity.value = "10"
        viewModel.averagePrice.value = "150.00"

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify {
            repository.addHolding(match { it.accountId == 2L })
        }
    }

    @Test
    fun `edit mode - loads existing holding`() = runTest {
        val existingHolding = HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        coEvery { repository.getHoldingById(1L) } returns existingHolding
        val savedStateHandle = createSavedStateHandle(holdingId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        assertThat(viewModel.isEditMode).isTrue()
        assertThat(viewModel.symbol.value).isEqualTo("AAPL")
        assertThat(viewModel.quantity.value).isEqualTo("10")
        assertThat(viewModel.averagePrice.value).isEqualTo("150.0")
        assertThat(viewModel.currency.value).isEqualTo("USD")
    }

    @Test
    fun `edit mode - updates existing holding`() = runTest {
        val existingHolding = HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        coEvery { repository.getHoldingById(1L) } returns existingHolding
        coEvery { repository.updateHolding(any()) } returns Unit
        val savedStateHandle = createSavedStateHandle(holdingId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.quantity.value = "20"
        viewModel.averagePrice.value = "160.0"

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify {
            repository.updateHolding(match {
                it.id == 1L &&
                it.symbol == "AAPL" &&
                it.quantity == 20 &&
                it.averagePrice == 160.0
            })
        }
    }

    @Test
    fun `saveHolding - ALL_ACCOUNTS_ID without selection - returns false`() = runTest {
        coEvery { accountRepository.getAllAccounts() } returns flowOf(emptyList())
        val savedStateHandle = createSavedStateHandle(accountId = ALL_ACCOUNTS_ID)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.symbol.value = "AAPL"
        viewModel.quantity.value = "10"
        viewModel.averagePrice.value = "150.00"
        viewModel.selectedAccountId.value = ALL_ACCOUNTS_ID

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
        assertThat(viewModel.errorMessage.value).isEqualTo("Please select an account")
    }
}
