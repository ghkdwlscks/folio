package com.portfolio.manager.domain.repository

import com.portfolio.manager.domain.model.GroupFireSettings
import com.portfolio.manager.domain.model.PortfolioSnapshot

/**
 * Low-level transport for member snapshots. The Firebase implementation
 * (Phase 2) is the untestable SDK boundary excluded from coverage.
 */
interface SyncDataSource {
    suspend fun signInAnonymously(): String
    suspend fun putMember(householdCode: String, uid: String, snapshot: PortfolioSnapshot)
    suspend fun getOtherMembers(householdCode: String, excludeUid: String): List<PortfolioSnapshot>
    suspend fun putGroupFireSettings(householdCode: String, settings: GroupFireSettings)
    suspend fun getGroupFireSettings(householdCode: String): GroupFireSettings?
}
