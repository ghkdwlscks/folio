package com.portfolio.manager.data.repository

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.AccountDao
import com.portfolio.manager.data.local.AccountEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AccountRepositoryImplTest {

    private lateinit var dao: AccountDao
    private lateinit var repository: AccountRepositoryImpl

    @Before
    fun setup() {
        dao = mockk()
        repository = AccountRepositoryImpl(dao)
    }

    @Test
    fun `getAllAccounts - returns flow from dao`() = runTest {
        val accounts = listOf(
            AccountEntity(1, "Retirement", 1000L),
            AccountEntity(2, "Trading", 2000L)
        )
        every { dao.getAllAccounts() } returns flowOf(accounts)

        val result = repository.getAllAccounts().first()

        assertThat(result).hasSize(2)
        assertThat(result[0].name).isEqualTo("Retirement")
        assertThat(result[1].name).isEqualTo("Trading")
    }

    @Test
    fun `getAccountById - returns account from dao`() = runTest {
        val account = AccountEntity(1, "Retirement", 1000L)
        coEvery { dao.getAccountById(1) } returns account

        val result = repository.getAccountById(1)

        assertThat(result).isNotNull()
        assertThat(result?.name).isEqualTo("Retirement")
    }

    @Test
    fun `getAccountById - returns null when not found`() = runTest {
        coEvery { dao.getAccountById(999) } returns null

        val result = repository.getAccountById(999)

        assertThat(result).isNull()
    }

    @Test
    fun `addAccount - inserts and returns id`() = runTest {
        val account = AccountEntity(name = "New Account")
        coEvery { dao.insert(account) } returns 1L

        val result = repository.addAccount(account)

        assertThat(result).isEqualTo(1L)
        coVerify { dao.insert(account) }
    }

    @Test
    fun `updateAccount - updates via dao`() = runTest {
        val account = AccountEntity(1, "Updated Name", 1000L)
        coEvery { dao.update(account) } returns Unit

        repository.updateAccount(account)

        coVerify { dao.update(account) }
    }

    @Test
    fun `deleteAccount - deletes via dao`() = runTest {
        coEvery { dao.deleteById(1) } returns Unit

        repository.deleteAccount(1)

        coVerify { dao.deleteById(1) }
    }

    @Test
    fun `getAccountCount - returns count from dao`() = runTest {
        coEvery { dao.getAccountCount() } returns 3

        val result = repository.getAccountCount()

        assertThat(result).isEqualTo(3)
    }

    @Test
    fun `updateAccounts - calls dao updateAll`() = runTest {
        val accounts = listOf(
            AccountEntity(1, "First", 1000L, 0),
            AccountEntity(2, "Second", 2000L, 1)
        )
        coEvery { dao.updateAll(accounts) } returns Unit

        repository.updateAccounts(accounts)

        coVerify { dao.updateAll(accounts) }
    }

    @Test
    fun `getMaxOrderIndex - returns max from dao`() = runTest {
        coEvery { dao.getMaxOrderIndex() } returns 10

        val max = repository.getMaxOrderIndex()

        assertThat(max).isEqualTo(10)
    }

    @Test
    fun `getOrCreateDefaultAccount - delegates to dao`() = runTest {
        val defaultAccount = AccountEntity(1, "Default", 1000L)
        coEvery { dao.getOrCreateDefaultAccount("Default") } returns defaultAccount

        val result = repository.getOrCreateDefaultAccount("Default")

        assertThat(result).isEqualTo(defaultAccount)
        coVerify { dao.getOrCreateDefaultAccount("Default") }
    }

    @Test
    fun `updatePreferredCurrency - calls dao`() = runTest {
        coEvery { dao.updatePreferredCurrency(1L, "KRW") } returns Unit

        repository.updatePreferredCurrency(1L, "KRW")

        coVerify { dao.updatePreferredCurrency(1L, "KRW") }
    }

    @Test
    fun `updateToleranceBand - forwards non-null band to dao`() = runTest {
        coEvery { dao.updateToleranceBand(1L, 25) } returns Unit

        repository.updateToleranceBand(1L, 25)

        coVerify { dao.updateToleranceBand(1L, 25) }
    }

    @Test
    fun `updateToleranceBand - forwards null band to dao`() = runTest {
        coEvery { dao.updateToleranceBand(1L, null) } returns Unit

        repository.updateToleranceBand(1L, null)

        coVerify { dao.updateToleranceBand(1L, null) }
    }
}
