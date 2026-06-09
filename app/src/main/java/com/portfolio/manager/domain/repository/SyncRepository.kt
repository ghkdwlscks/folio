package com.portfolio.manager.domain.repository

import com.portfolio.manager.domain.model.GroupFireSettings
import com.portfolio.manager.domain.model.PortfolioSnapshot

/**
 * Household sync orchestration. Implemented in Phase 2 by SyncRepositoryImpl
 * over a [SyncDataSource]; all orchestration logic there is unit-tested.
 */
interface SyncRepository {
    suspend fun ensureSignedIn(): Result<String>
    suspend fun publishSnapshot(householdCode: String, uid: String, snapshot: PortfolioSnapshot): Result<Unit>
    suspend fun fetchPartnerSnapshot(householdCode: String, uid: String): Result<PortfolioSnapshot?>
    suspend fun saveGroupFireSettings(householdCode: String, settings: GroupFireSettings): Result<Unit>
    suspend fun fetchGroupFireSettings(householdCode: String): Result<GroupFireSettings?>
}
