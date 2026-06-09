package com.portfolio.manager.domain.service

import android.content.SharedPreferences

import com.google.common.truth.Truth.assertThat
import com.portfolio.manager.domain.model.PortfolioSnapshot
import com.portfolio.manager.domain.repository.SyncRepository
import com.portfolio.manager.util.PreferenceKeys

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot

import kotlinx.coroutines.test.runTest

import org.junit.Before
import org.junit.Test

import java.io.IOException

class HouseholdPublisherTest {

    private lateinit var syncRepository: SyncRepository
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var publisher: HouseholdPublisher

    @Before
    fun setup() {
        syncRepository = mockk()
        sharedPreferences = mockk()
        publisher = HouseholdPublisher(syncRepository, SnapshotMapper(), sharedPreferences)

        every { sharedPreferences.getString(PreferenceKeys.HOUSEHOLD_CODE, null) } returns "ABCD-2345"
        every { sharedPreferences.getString(PreferenceKeys.HOUSEHOLD_MY_LABEL, null) } returns "남편"
        coEvery { syncRepository.ensureSignedIn() } returns Result.success("uid-1")
        coEvery { syncRepository.publishSnapshot(any(), any(), any()) } returns Result.success(Unit)
    }

    @Test
    fun `publish - not paired - does nothing`() = runTest {
        every { sharedPreferences.getString(PreferenceKeys.HOUSEHOLD_CODE, null) } returns null

        publisher.publish(emptyList(), emptyList(), emptyList())

        coVerify(exactly = 0) { syncRepository.ensureSignedIn() }
        coVerify(exactly = 0) { syncRepository.publishSnapshot(any(), any(), any()) }
    }

    @Test
    fun `publish - sign-in fails - does not publish`() = runTest {
        coEvery { syncRepository.ensureSignedIn() } returns Result.failure(IOException("offline"))

        publisher.publish(emptyList(), emptyList(), emptyList())

        coVerify(exactly = 0) { syncRepository.publishSnapshot(any(), any(), any()) }
    }

    @Test
    fun `publish - paired and signed in - publishes snapshot with label`() = runTest {
        val snapshot = slot<PortfolioSnapshot>()
        coEvery { syncRepository.publishSnapshot("ABCD-2345", "uid-1", capture(snapshot)) } returns
            Result.success(Unit)

        publisher.publish(emptyList(), emptyList(), emptyList())

        coVerify { syncRepository.publishSnapshot("ABCD-2345", "uid-1", any()) }
        assertThat(snapshot.captured.displayName).isEqualTo("남편")
    }

    @Test
    fun `publish - no label - falls back to default`() = runTest {
        every { sharedPreferences.getString(PreferenceKeys.HOUSEHOLD_MY_LABEL, null) } returns null
        val snapshot = slot<PortfolioSnapshot>()
        coEvery { syncRepository.publishSnapshot(any(), any(), capture(snapshot)) } returns Result.success(Unit)

        publisher.publish(emptyList(), emptyList(), emptyList())

        assertThat(snapshot.captured.displayName).isEqualTo("Me")
    }
}
