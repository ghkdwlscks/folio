package com.portfolio.manager.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.CashItemEntity
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.CashRepository
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
class AddCashViewModelTest {

    private lateinit var cashRepository: CashRepository
    private lateinit var accountRepository: AccountRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    private val defaultAccount = AccountEntity(1, "Default", 1000L)
    private val secondAccount = AccountEntity(2, "Trading", 2000L)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        cashRepository = mockk()
        accountRepository = mockk()

        every { accountRepository.getAllAccounts() } returns flowOf(listOf(defaultAccount, secondAccount))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init - all accounts mode - sets needsAccountSelection true`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to ALL_ACCOUNTS_ID))

        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)

        val state = viewModel.uiState.value
        assertThat(state.needsAccountSelection).isTrue()
        assertThat(state.selectedAccountId).isEqualTo(defaultAccount.id)
        assertThat(state.accounts).hasSize(2)
        assertThat(state.isEditMode).isFalse()
    }

    @Test
    fun `isInitialLoading - false after accounts loaded`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))

        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)

        // With UnconfinedTestDispatcher, init completes immediately
        assertThat(viewModel.uiState.value.isInitialLoading).isFalse()
        assertThat(viewModel.uiState.value.accounts).isNotEmpty()
    }

    @Test
    fun `init - specific account mode - sets needsAccountSelection false`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))

        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)

        val state = viewModel.uiState.value
        assertThat(state.needsAccountSelection).isFalse()
        assertThat(state.selectedAccountId).isEqualTo(1L)
    }

    @Test
    fun `init - edit mode - loads existing cash item`() = runTest {
        val cashItem = CashItemEntity(
            id = 1,
            accountId = 1,
            name = "Emergency Fund",
            originalValue = 10000.0,
            annualYieldRate = 4.5,
            currency = "USD"
        )
        coEvery { cashRepository.getCashItemById(1L) } returns cashItem
        val savedStateHandle = SavedStateHandle(mapOf("cashItemId" to 1L, "accountId" to ALL_ACCOUNTS_ID))

        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)

        val state = viewModel.uiState.value
        assertThat(state.isEditMode).isTrue()
        assertThat(state.name).isEqualTo("Emergency Fund")
        assertThat(state.value).isEqualTo("10000.0")
        assertThat(state.yieldRate).isEqualTo("4.5")
        assertThat(state.currency).isEqualTo("USD")
        assertThat(state.selectedAccountId).isEqualTo(1L)
    }

    @Test
    fun `updateName - updates state`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)

        viewModel.updateName("My Savings")

        assertThat(viewModel.uiState.value.name).isEqualTo("My Savings")
    }

    @Test
    fun `updateValue - filters non-numeric characters`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)

        viewModel.updateValue("10000.50abc")

        assertThat(viewModel.uiState.value.value).isEqualTo("10000.50")
    }

    @Test
    fun `updateYieldRate - filters non-numeric characters`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)

        viewModel.updateYieldRate("4.5%")

        assertThat(viewModel.uiState.value.yieldRate).isEqualTo("4.5")
    }

    @Test
    fun `updateCurrency - updates state`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)

        viewModel.updateCurrency("KRW")

        assertThat(viewModel.uiState.value.currency).isEqualTo("KRW")
    }

    @Test
    fun `selectAccount - updates state`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to ALL_ACCOUNTS_ID))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)

        viewModel.selectAccount(2L)

        assertThat(viewModel.uiState.value.selectedAccountId).isEqualTo(2L)
    }

    @Test
    fun `saveCashItem - empty name - returns false with error`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)
        viewModel.updateValue("10000")
        viewModel.updateYieldRate("4.5")

        val result = viewModel.saveCashItem()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).contains("name")
    }

    @Test
    fun `saveCashItem - invalid value - returns false with error`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)
        viewModel.updateName("Test")
        viewModel.updateValue("")
        viewModel.updateYieldRate("4.5")

        val result = viewModel.saveCashItem()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).contains("value")
    }

    @Test
    fun `saveCashItem - invalid yield rate - returns false with error`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)
        viewModel.updateName("Test")
        viewModel.updateValue("10000")
        viewModel.updateYieldRate("")

        val result = viewModel.saveCashItem()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).contains("yield")
    }

    @Test
    fun `saveCashItem - no account selected - returns false with error`() = runTest {
        every { accountRepository.getAllAccounts() } returns flowOf(emptyList())
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to ALL_ACCOUNTS_ID))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)
        viewModel.updateName("Test")
        viewModel.updateValue("10000")
        viewModel.updateYieldRate("4.5")

        val result = viewModel.saveCashItem()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).contains("account")
    }

    @Test
    fun `saveCashItem - valid data add mode - calls repository and returns true`() = runTest {
        coEvery { cashRepository.addCashItem(any(), any(), any(), any(), any()) } returns 1L
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)
        viewModel.updateName("Emergency Fund")
        viewModel.updateValue("10000")
        viewModel.updateYieldRate("4.5")
        viewModel.updateCurrency("USD")

        val result = viewModel.saveCashItem()

        assertThat(result).isTrue()
        coVerify { cashRepository.addCashItem(1L, "Emergency Fund", 10000.0, 4.5, "USD") }
    }

    @Test
    fun `saveCashItem - valid data edit mode - calls update and returns true`() = runTest {
        val cashItem = CashItemEntity(
            id = 1,
            accountId = 1,
            name = "Old Name",
            originalValue = 5000.0,
            annualYieldRate = 3.0,
            currency = "USD"
        )
        coEvery { cashRepository.getCashItemById(1L) } returns cashItem
        coEvery { cashRepository.updateCashItem(any(), any(), any(), any(), any()) } returns Unit
        val savedStateHandle = SavedStateHandle(mapOf("cashItemId" to 1L, "accountId" to ALL_ACCOUNTS_ID))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)
        viewModel.updateName("Updated Name")
        viewModel.updateValue("15000")
        viewModel.updateYieldRate("5.0")

        val result = viewModel.saveCashItem()

        assertThat(result).isTrue()
        coVerify { cashRepository.updateCashItem(1L, "Updated Name", 15000.0, 5.0, "USD") }
    }

    @Test
    fun `updateYieldRate - filters minus sign - becomes positive`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)

        viewModel.updateYieldRate("-4.5")

        // Minus sign is filtered out, so "-4.5" becomes "4.5"
        assertThat(viewModel.uiState.value.yieldRate).isEqualTo("4.5")
    }

    @Test
    fun `saveCashItem - zero value - returns false with error`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)
        viewModel.updateName("Test")
        viewModel.updateValue("0")
        viewModel.updateYieldRate("4.5")

        val result = viewModel.saveCashItem()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).contains("value")
    }

    @Test
    fun `saveCashItem - zero yield rate - returns true`() = runTest {
        coEvery { cashRepository.addCashItem(any(), any(), any(), any(), any()) } returns 1L
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)
        viewModel.updateName("Cash")
        viewModel.updateValue("10000")
        viewModel.updateYieldRate("0")

        val result = viewModel.saveCashItem()

        assertThat(result).isTrue()
        coVerify { cashRepository.addCashItem(1L, "Cash", 10000.0, 0.0, "KRW") }
    }

    @Test
    fun `saveCashItem - isSaving resets after validation failure`() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)

        viewModel.updateName("")
        viewModel.updateValue("1000")
        viewModel.updateYieldRate("3.5")

        val result = viewModel.saveCashItem()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.isSaving).isFalse()
    }

    @Test
    fun `saveCashItem - isSaving resets after successful save`() = runTest {
        coEvery { cashRepository.addCashItem(any(), any(), any(), any(), any()) } returns 1L
        val savedStateHandle = SavedStateHandle(mapOf("accountId" to 1L))
        val viewModel = AddCashViewModel(cashRepository, accountRepository, savedStateHandle)

        viewModel.updateName("Savings")
        viewModel.updateValue("1000")
        viewModel.updateYieldRate("3.5")

        val result = viewModel.saveCashItem()

        assertThat(result).isTrue()
        assertThat(viewModel.uiState.value.isSaving).isFalse()
    }
}
