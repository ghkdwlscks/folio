package com.portfolio.manager.data.repository

import javax.inject.Inject

import com.portfolio.manager.domain.model.PortfolioSnapshot
import com.portfolio.manager.domain.repository.SyncDataSource
import com.portfolio.manager.domain.repository.SyncRepository

/**
 * Orchestrates household sync over a [SyncDataSource], wrapping each transport
 * call in a [Result]. The single partner is the first other member returned.
 */
class SyncRepositoryImpl @Inject constructor(
    private val dataSource: SyncDataSource
) : SyncRepository {

    override suspend fun ensureSignedIn(): Result<String> =
        runCatching { dataSource.signInAnonymously() }

    override suspend fun publishSnapshot(
        householdCode: String,
        uid: String,
        snapshot: PortfolioSnapshot
    ): Result<Unit> =
        runCatching { dataSource.putMember(householdCode, uid, snapshot) }

    override suspend fun fetchPartnerSnapshot(
        householdCode: String,
        uid: String
    ): Result<PortfolioSnapshot?> =
        runCatching { dataSource.getOtherMembers(householdCode, uid).firstOrNull() }
}
