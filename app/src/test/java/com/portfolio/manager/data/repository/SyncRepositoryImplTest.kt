package com.portfolio.manager.data.repository

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.domain.model.PortfolioSnapshot
import com.portfolio.manager.domain.repository.SyncDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.IOException

class SyncRepositoryImplTest {

    private lateinit var dataSource: SyncDataSource
    private lateinit var repository: SyncRepositoryImpl

    @Before
    fun setup() {
        dataSource = mockk()
        repository = SyncRepositoryImpl(dataSource)
    }

    @Test
    fun `ensureSignedIn - success - returns uid`() = runTest {
        coEvery { dataSource.signInAnonymously() } returns "uid-123"

        val result = repository.ensureSignedIn()

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo("uid-123")
    }

    @Test
    fun `ensureSignedIn - failure - returns failure`() = runTest {
        coEvery { dataSource.signInAnonymously() } throws IOException("offline")

        val result = repository.ensureSignedIn()

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `publishSnapshot - success - delegates to data source`() = runTest {
        val snapshot = PortfolioSnapshot(displayName = "나")
        coEvery { dataSource.putMember("ABCD-2345", "uid-1", snapshot) } returns Unit

        val result = repository.publishSnapshot("ABCD-2345", "uid-1", snapshot)

        assertThat(result.isSuccess).isTrue()
        coVerify { dataSource.putMember("ABCD-2345", "uid-1", snapshot) }
    }

    @Test
    fun `publishSnapshot - failure - returns failure`() = runTest {
        val snapshot = PortfolioSnapshot(displayName = "나")
        coEvery { dataSource.putMember(any(), any(), any()) } throws IOException("write failed")

        val result = repository.publishSnapshot("ABCD-2345", "uid-1", snapshot)

        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `fetchPartnerSnapshot - returns first other member`() = runTest {
        val partner = PortfolioSnapshot(displayName = "와이프")
        coEvery { dataSource.getOtherMembers("ABCD-2345", "uid-1") } returns listOf(partner)

        val result = repository.fetchPartnerSnapshot("ABCD-2345", "uid-1")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isEqualTo(partner)
    }

    @Test
    fun `fetchPartnerSnapshot - no other members - returns null`() = runTest {
        coEvery { dataSource.getOtherMembers(any(), any()) } returns emptyList()

        val result = repository.fetchPartnerSnapshot("ABCD-2345", "uid-1")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrNull()).isNull()
    }

    @Test
    fun `fetchPartnerSnapshot - failure - returns failure`() = runTest {
        coEvery { dataSource.getOtherMembers(any(), any()) } throws IOException("read failed")

        val result = repository.fetchPartnerSnapshot("ABCD-2345", "uid-1")

        assertThat(result.isFailure).isTrue()
    }
}
