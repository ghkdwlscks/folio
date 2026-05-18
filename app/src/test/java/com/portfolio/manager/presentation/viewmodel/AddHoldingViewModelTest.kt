package com.portfolio.manager.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.model.Currency
import com.portfolio.manager.domain.repository.AccountRepository
import com.portfolio.manager.domain.repository.HoldingsRepository
import com.portfolio.manager.util.AppConstants.ALL_ACCOUNTS_ID
import io.mockk.coEvery
import io.mockk.coVerify
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

        val state = viewModel.uiState.value
        assertThat(state.symbol).isEmpty()
        assertThat(state.quantity).isEmpty()
        assertThat(state.averagePrice).isEmpty()
        assertThat(state.currency).isEqualTo(Currency.USD)
    }

    @Test
    fun `isInitialLoading - false after accounts loaded`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        // With UnconfinedTestDispatcher, init completes immediately
        assertThat(viewModel.uiState.value.isInitialLoading).isFalse()
        assertThat(viewModel.uiState.value.accounts).isNotEmpty()
    }

    @Test
    fun `saveHolding - valid input - saves and returns success`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("AAPL")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("150.00")
        viewModel.updateCurrency(Currency.USD)

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

        viewModel.updateSymbol("")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("150.00")

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
    }

    @Test
    fun `saveHolding - invalid quantity - returns false`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("AAPL")
        viewModel.updateQuantity("abc")
        viewModel.updateAveragePrice("150.00")

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
    }

    @Test
    fun `saveHolding - invalid price - returns false`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("AAPL")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("invalid")

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
    }

    @Test
    fun `saveHolding - KRW currency - saves correctly`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("005930.KS")
        viewModel.updateQuantity("50")
        viewModel.updateAveragePrice("72000")
        viewModel.updateCurrency(Currency.KRW)

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

        viewModel.updateSymbol("AAPL")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("150.00")

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("This stock already exists in the account")
        coVerify(exactly = 0) { repository.addHolding(any()) }
    }

    @Test
    fun `saveHolding - same symbol in different account - succeeds`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(2L, "AAPL") } returns null
        coEvery { repository.addHolding(any()) } returns 2L
        val savedStateHandle = createSavedStateHandle(accountId = 2L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("AAPL")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("150.00")

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify { repository.addHolding(any()) }
    }

    @Test
    fun `needsAccountSelection - true when ALL_ACCOUNTS_ID with accounts`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = ALL_ACCOUNTS_ID)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        assertThat(viewModel.uiState.value.needsAccountSelection).isTrue()
        assertThat(viewModel.uiState.value.selectedAccountId).isEqualTo(1L)
    }

    @Test
    fun `needsAccountSelection - false when specific account`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        assertThat(viewModel.uiState.value.needsAccountSelection).isFalse()
    }

    @Test
    fun `selectAccount - updates selectedAccountId`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = ALL_ACCOUNTS_ID)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.selectAccount(2L)

        assertThat(viewModel.uiState.value.selectedAccountId).isEqualTo(2L)
    }

    @Test
    fun `saveHolding - uses selected account when ALL_ACCOUNTS_ID`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = ALL_ACCOUNTS_ID)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.selectAccount(2L)
        viewModel.updateSymbol("AAPL")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("150.00")

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

        val state = viewModel.uiState.value
        assertThat(state.isEditMode).isTrue()
        assertThat(state.symbol).isEqualTo("AAPL")
        assertThat(state.quantity).isEqualTo("10")
        assertThat(state.averagePrice).isEqualTo("150.0")
        assertThat(state.currency).isEqualTo(Currency.USD)
    }

    @Test
    fun `edit mode - updates existing holding`() = runTest {
        val existingHolding = HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        coEvery { repository.getHoldingById(1L) } returns existingHolding
        coEvery { repository.getHoldingByAccountAndSymbol(1L, "AAPL") } returns existingHolding
        coEvery { repository.updateHolding(any()) } returns Unit
        val savedStateHandle = createSavedStateHandle(holdingId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateQuantity("20")
        viewModel.updateAveragePrice("160.0")

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

        viewModel.updateSymbol("AAPL")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("150.00")

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isEqualTo("Please select an account")
    }

    @Test
    fun `updateSymbol - filters to uppercase`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("aapl")

        assertThat(viewModel.uiState.value.symbol).isEqualTo("AAPL")
    }

    @Test
    fun `updateQuantity - filters non-digits`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateQuantity("10abc20")

        assertThat(viewModel.uiState.value.quantity).isEqualTo("1020")
    }

    @Test
    fun `updateAveragePrice - filters invalid characters`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateAveragePrice("150.50abc")

        assertThat(viewModel.uiState.value.averagePrice).isEqualTo("150.50")
    }

    @Test
    fun `updateSymbol - does not update in edit mode`() = runTest {
        val existingHolding = HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        coEvery { repository.getHoldingById(1L) } returns existingHolding
        val savedStateHandle = createSavedStateHandle(holdingId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("GOOGL")

        assertThat(viewModel.uiState.value.symbol).isEqualTo("AAPL")
    }

    @Test
    fun `saveHolding - quantity below minimum - returns error`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("AAPL")
        viewModel.updateQuantity("0")
        viewModel.updateAveragePrice("150.0")

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).contains("Quantity must be between")
    }

    @Test
    fun `saveHolding - quantity above maximum - returns error`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("AAPL")
        viewModel.updateQuantity("1000001")
        viewModel.updateAveragePrice("150.0")

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).contains("Quantity must be between")
    }

    @Test
    fun `saveHolding - price below minimum - returns error`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("AAPL")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("0.00001")

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).contains("Price must be between")
    }

    @Test
    fun `saveHolding - price above maximum - returns error`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("AAPL")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("1000000001.0")

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).contains("Price must be between")
    }

    @Test
    fun `edit mode - duplicate symbol check with different holding - returns error`() = runTest {
        val existingHolding = HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        val differentHolding = HoldingEntity(2, 1L, "AAPL", "Apple Inc.", 5, 160.0, "USD")
        coEvery { repository.getHoldingById(1L) } returns existingHolding
        coEvery { repository.getHoldingByAccountAndSymbol(1L, "AAPL") } returns differentHolding
        val savedStateHandle = createSavedStateHandle(holdingId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).contains("already exists")
    }

    @Test
    fun `edit mode - no duplicate check if symbol not found - succeeds`() = runTest {
        val existingHolding = HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        coEvery { repository.getHoldingById(1L) } returns existingHolding
        coEvery { repository.getHoldingByAccountAndSymbol(1L, "AAPL") } returns null
        coEvery { repository.updateHolding(any()) } returns Unit
        val savedStateHandle = createSavedStateHandle(holdingId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
    }

    @Test
    fun `saveHolding - 6-digit number - appends KS suffix`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("005930")
        viewModel.updateQuantity("50")
        viewModel.updateAveragePrice("72000")
        viewModel.updateCurrency(Currency.KRW)

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify {
            repository.addHolding(match {
                it.symbol == "005930.KS" &&
                it.name == "005930.KS"
            })
        }
    }

    @Test
    fun `saveHolding - symbol with KS suffix - remains unchanged`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("005930.KS")
        viewModel.updateQuantity("50")
        viewModel.updateAveragePrice("72000")
        viewModel.updateCurrency(Currency.KRW)

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify {
            repository.addHolding(match {
                it.symbol == "005930.KS"
            })
        }
    }

    @Test
    fun `saveHolding - 5-digit number - no KS suffix added`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("12345")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("100")

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify {
            repository.addHolding(match {
                it.symbol == "12345"
            })
        }
    }

    @Test
    fun `saveHolding - 7-digit number - no KS suffix added`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("1234567")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("100")

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify {
            repository.addHolding(match {
                it.symbol == "1234567"
            })
        }
    }

    @Test
    fun `saveHolding - 6-char alphanumeric Korean code - appends KS suffix`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("0060H0")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("13000")
        viewModel.updateCurrency(Currency.KRW)

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify {
            repository.addHolding(match {
                it.symbol == "0060H0.KS" &&
                it.name == "0060H0.KS"
            })
        }
    }

    @Test
    fun `saveHolding - lowercase 6-char alphanumeric Korean code - normalized and appends KS`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("0060h0")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("13000")
        viewModel.updateCurrency(Currency.KRW)

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify {
            repository.addHolding(match {
                it.symbol == "0060H0.KS"
            })
        }
    }

    @Test
    fun `saveHolding - 6-char all letters - no KS suffix added`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("ABCDEF")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("100")

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        coVerify {
            repository.addHolding(match {
                it.symbol == "ABCDEF"
            })
        }
    }

    @Test
    fun `onSymbolFocusLost - 6-digit number - sets currency to KRW`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("005930")
        viewModel.onSymbolFocusLost()

        assertThat(viewModel.uiState.value.currency).isEqualTo(Currency.KRW)
    }

    @Test
    fun `onSymbolFocusLost - non-6-digit symbol - keeps current currency`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("AAPL")
        viewModel.onSymbolFocusLost()

        assertThat(viewModel.uiState.value.currency).isEqualTo(Currency.USD)
    }

    @Test
    fun `onSymbolFocusLost - 5-digit number - keeps current currency`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("12345")
        viewModel.onSymbolFocusLost()

        assertThat(viewModel.uiState.value.currency).isEqualTo(Currency.USD)
    }

    @Test
    fun `onSymbolFocusLost - 6-char alphanumeric Korean code - sets currency to KRW`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("0060H0")
        viewModel.onSymbolFocusLost()

        assertThat(viewModel.uiState.value.currency).isEqualTo(Currency.KRW)
    }

    @Test
    fun `onSymbolFocusLost - 6-char all letters - keeps current currency`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("ABCDEF")
        viewModel.onSymbolFocusLost()

        assertThat(viewModel.uiState.value.currency).isEqualTo(Currency.USD)
    }

    @Test
    fun `saveHolding - isSaving resets after validation failure`() = runTest {
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("AAPL")
        viewModel.updateQuantity("0")
        viewModel.updateAveragePrice("150.0")

        val result = viewModel.saveHolding()

        assertThat(result).isFalse()
        assertThat(viewModel.uiState.value.isSaving).isFalse()
    }

    @Test
    fun `saveHolding - isSaving resets after successful save`() = runTest {
        coEvery { repository.getHoldingByAccountAndSymbol(any(), any()) } returns null
        coEvery { repository.addHolding(any()) } returns 1L
        val savedStateHandle = createSavedStateHandle(accountId = 1L)
        val viewModel = AddHoldingViewModel(repository, accountRepository, savedStateHandle)

        viewModel.updateSymbol("AAPL")
        viewModel.updateQuantity("10")
        viewModel.updateAveragePrice("150.0")

        val result = viewModel.saveHolding()

        assertThat(result).isTrue()
        assertThat(viewModel.uiState.value.isSaving).isFalse()
    }
}
