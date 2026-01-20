package com.portfolio.manager.presentation.viewmodel

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.domain.repository.AccountRepository
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
class AccountsViewModelTest {

    private lateinit var repository: AccountRepository
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
    fun `initial state - loads accounts from repository`() = runTest {
        val accounts = listOf(
            AccountEntity(1, "Default", 1000L),
            AccountEntity(2, "Trading", 2000L)
        )
        every { repository.getAllAccounts() } returns flowOf(accounts)

        val viewModel = AccountsViewModel(repository)

        assertThat(viewModel.uiState.value).isInstanceOf(AccountsUiState.Success::class.java)
        val state = viewModel.uiState.value as AccountsUiState.Success
        assertThat(state.accounts).hasSize(2)
        assertThat(state.accounts[0].name).isEqualTo("Default")
        assertThat(state.accounts[1].name).isEqualTo("Trading")
    }

    @Test
    fun `empty accounts - returns empty success`() = runTest {
        every { repository.getAllAccounts() } returns flowOf(emptyList())

        val viewModel = AccountsViewModel(repository)

        assertThat(viewModel.uiState.value).isInstanceOf(AccountsUiState.Success::class.java)
        val state = viewModel.uiState.value as AccountsUiState.Success
        assertThat(state.accounts).isEmpty()
    }

    @Test
    fun `addAccount - saves new account`() = runTest {
        every { repository.getAllAccounts() } returns flowOf(emptyList())
        coEvery { repository.getMaxOrderIndex() } returns 0
        coEvery { repository.addAccount(any()) } returns 1L

        val viewModel = AccountsViewModel(repository)
        viewModel.addAccount("Retirement")

        coVerify {
            repository.addAccount(match { it.name == "Retirement" })
        }
    }

    @Test
    fun `deleteAccount - removes account`() = runTest {
        val accounts = listOf(AccountEntity(1, "Default", 1000L))
        every { repository.getAllAccounts() } returns flowOf(accounts)
        coEvery { repository.deleteAccount(1L) } returns Unit

        val viewModel = AccountsViewModel(repository)
        viewModel.deleteAccount(1L)

        coVerify { repository.deleteAccount(1L) }
    }

    @Test
    fun `renameAccount - updates account name`() = runTest {
        val account = AccountEntity(1, "Default", 1000L)
        every { repository.getAllAccounts() } returns flowOf(listOf(account))
        coEvery { repository.getAccountById(1L) } returns account
        coEvery { repository.updateAccount(any()) } returns Unit

        val viewModel = AccountsViewModel(repository)
        viewModel.renameAccount(1L, "Main Portfolio")

        coVerify {
            repository.updateAccount(match { it.id == 1L && it.name == "Main Portfolio" })
        }
    }

    @Test
    fun `renameAccount - account not found - does nothing`() = runTest {
        every { repository.getAllAccounts() } returns flowOf(emptyList())
        coEvery { repository.getAccountById(999L) } returns null

        val viewModel = AccountsViewModel(repository)
        viewModel.renameAccount(999L, "New Name")

        coVerify(exactly = 0) { repository.updateAccount(any()) }
    }

    @Test
    fun `addAccount - sets orderIndex based on maxOrderIndex`() = runTest {
        every { repository.getAllAccounts() } returns flowOf(emptyList())
        coEvery { repository.getMaxOrderIndex() } returns 5
        coEvery { repository.addAccount(any()) } returns 1L

        val viewModel = AccountsViewModel(repository)
        viewModel.addAccount("New Account")

        coVerify {
            repository.addAccount(match { it.orderIndex == 6 })
        }
    }

    @Test
    fun `reorderAccounts - updates all accounts with new order`() = runTest {
        val accounts = listOf(
            AccountEntity(1, "First", 1000L, 0),
            AccountEntity(2, "Second", 2000L, 1)
        )
        every { repository.getAllAccounts() } returns flowOf(accounts)
        coEvery { repository.updateAccounts(any()) } returns Unit

        val viewModel = AccountsViewModel(repository)
        val reordered = listOf(accounts[1], accounts[0])
        viewModel.reorderAccounts(reordered)

        coVerify {
            repository.updateAccounts(match {
                it[0].id == 2L && it[0].orderIndex == 0 &&
                it[1].id == 1L && it[1].orderIndex == 1
            })
        }
    }
}
