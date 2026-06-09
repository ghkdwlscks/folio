package com.portfolio.manager.domain.service

import android.content.SharedPreferences

import javax.inject.Inject

import com.portfolio.manager.data.local.AccountEntity
import com.portfolio.manager.data.local.CashItemEntity
import com.portfolio.manager.data.local.HoldingEntity
import com.portfolio.manager.domain.repository.SyncRepository
import com.portfolio.manager.util.PreferenceKeys

/**
 * Publishes my portfolio snapshot to the shared household whenever my data
 * changes, so the partner sees my latest values without me opening the
 * household screen. A no-op when not paired or anonymous sign-in fails.
 */
class HouseholdPublisher @Inject constructor(
    private val syncRepository: SyncRepository,
    private val snapshotMapper: SnapshotMapper,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val DEFAULT_LABEL = "Me"
    }

    suspend fun publish(
        accounts: List<AccountEntity>,
        holdings: List<HoldingEntity>,
        cashItems: List<CashItemEntity>
    ) {
        val code = sharedPreferences.getString(PreferenceKeys.HOUSEHOLD_CODE, null) ?: return
        val uid = syncRepository.ensureSignedIn().getOrNull() ?: return
        val label = sharedPreferences.getString(PreferenceKeys.HOUSEHOLD_MY_LABEL, null) ?: DEFAULT_LABEL
        val snapshot = snapshotMapper.toSnapshot(label, System.currentTimeMillis(), accounts, holdings, cashItems)
        syncRepository.publishSnapshot(code, uid, snapshot)
    }
}
