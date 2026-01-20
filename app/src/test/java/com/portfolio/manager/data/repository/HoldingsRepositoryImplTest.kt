package com.portfolio.manager.data.repository

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.data.local.HoldingDao
import com.portfolio.manager.data.local.HoldingEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class HoldingsRepositoryImplTest {

    private lateinit var dao: HoldingDao
    private lateinit var repository: HoldingsRepositoryImpl

    @Before
    fun setup() {
        dao = mockk()
        repository = HoldingsRepositoryImpl(dao)
    }

    @Test
    fun `getHoldingsByAccount - returns flow from dao`() = runTest {
        val accountId = 1L
        val holdings = listOf(
            HoldingEntity(1, accountId, "AAPL", "Apple Inc.", 10, 150.0, "USD"),
            HoldingEntity(2, accountId, "005930.KS", "삼성전자", 50, 72000.0, "KRW")
        )
        every { dao.getHoldingsByAccount(accountId) } returns flowOf(holdings)

        val result = repository.getHoldingsByAccount(accountId).first()

        assertThat(result).hasSize(2)
        assertThat(result[0].symbol).isEqualTo("AAPL")
        assertThat(result[1].symbol).isEqualTo("005930.KS")
    }

    @Test
    fun `getAllHoldings - returns flow from dao`() = runTest {
        val holdings = listOf(
            HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD"),
            HoldingEntity(2, 2L, "TSLA", "Tesla", 5, 200.0, "USD")
        )
        every { dao.getAllHoldings() } returns flowOf(holdings)

        val result = repository.getAllHoldings().first()

        assertThat(result).hasSize(2)
        assertThat(result[0].symbol).isEqualTo("AAPL")
        assertThat(result[1].symbol).isEqualTo("TSLA")
    }

    @Test
    fun `getHoldingById - returns holding from dao`() = runTest {
        val holding = HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        coEvery { dao.getHoldingById(1) } returns holding

        val result = repository.getHoldingById(1)

        assertThat(result).isNotNull()
        assertThat(result?.symbol).isEqualTo("AAPL")
    }

    @Test
    fun `getHoldingById - returns null when not found`() = runTest {
        coEvery { dao.getHoldingById(999) } returns null

        val result = repository.getHoldingById(999)

        assertThat(result).isNull()
    }

    @Test
    fun `addHolding - calls dao insert`() = runTest {
        val holding = HoldingEntity(0, 1L, "AAPL", "Apple Inc.", 10, 150.0, "USD")
        coEvery { dao.insert(holding) } returns 1L

        val id = repository.addHolding(holding)

        assertThat(id).isEqualTo(1L)
        coVerify { dao.insert(holding) }
    }

    @Test
    fun `updateHolding - calls dao update`() = runTest {
        val holding = HoldingEntity(1, 1L, "AAPL", "Apple Inc.", 20, 160.0, "USD")
        coEvery { dao.update(holding) } returns Unit

        repository.updateHolding(holding)

        coVerify { dao.update(holding) }
    }

    @Test
    fun `deleteHolding - calls dao deleteById`() = runTest {
        coEvery { dao.deleteById(1) } returns Unit

        repository.deleteHolding(1)

        coVerify { dao.deleteById(1) }
    }

    @Test
    fun `getHoldingsCountByAccount - returns count from dao`() = runTest {
        coEvery { dao.getHoldingsCountByAccount(1L) } returns 5

        val count = repository.getHoldingsCountByAccount(1L)

        assertThat(count).isEqualTo(5)
    }
}
