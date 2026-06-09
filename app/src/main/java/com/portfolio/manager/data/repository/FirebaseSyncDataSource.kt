package com.portfolio.manager.data.repository

import javax.inject.Inject

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import kotlinx.coroutines.tasks.await

import com.portfolio.manager.domain.model.GroupFireSettings
import com.portfolio.manager.domain.model.PortfolioSnapshot
import com.portfolio.manager.domain.repository.SyncDataSource
import com.portfolio.manager.util.JsonSerializer

/**
 * Firestore + anonymous Auth implementation of [SyncDataSource].
 *
 * Each member's snapshot is stored as a JSON string under
 * `households/{code}/members/{uid}` in a single "snapshot" field. This avoids
 * Firestore's nested-type quirks and keeps the schema trivial.
 *
 * This class is the untestable SDK boundary and is excluded from coverage.
 */
class FirebaseSyncDataSource @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SyncDataSource {

    private val json = JsonSerializer.instance

    override suspend fun signInAnonymously(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("Anonymous sign-in returned no user")
    }

    override suspend fun putMember(householdCode: String, uid: String, snapshot: PortfolioSnapshot) {
        val data = mapOf("snapshot" to json.encodeToString(PortfolioSnapshot.serializer(), snapshot))
        membersCollection(householdCode).document(uid).set(data).await()
    }

    override suspend fun getOtherMembers(householdCode: String, excludeUid: String): List<PortfolioSnapshot> {
        val query = membersCollection(householdCode).get().await()
        return query.documents
            .filter { it.id != excludeUid }
            .mapNotNull { it.getString("snapshot") }
            .map { json.decodeFromString(PortfolioSnapshot.serializer(), it) }
    }

    override suspend fun putGroupFireSettings(householdCode: String, settings: GroupFireSettings) {
        val data = mapOf("groupFireSettings" to json.encodeToString(GroupFireSettings.serializer(), settings))
        settingsDocument(householdCode).set(data).await()
    }

    override suspend fun getGroupFireSettings(householdCode: String): GroupFireSettings? {
        val doc = settingsDocument(householdCode).get().await()
        return doc.getString("groupFireSettings")
            ?.let { json.decodeFromString(GroupFireSettings.serializer(), it) }
    }

    private fun membersCollection(householdCode: String) =
        firestore.collection("households").document(householdCode).collection("members")

    private fun settingsDocument(householdCode: String) =
        firestore.collection("households").document(householdCode)
            .collection("settings").document("fire")
}
