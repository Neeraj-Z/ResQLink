package com.resqlink.app.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.resqlink.app.data.local.entity.HazardEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fetches active hazard zones from Firestore.
 *
 * Note: this does a full-collection fetch rather than a geo-radius query --
 * Firestore doesn't support native geo queries without either a geohash
 * scheme or the (paid-tier-only) Cloud Firestore geo-query extensions. For a
 * classroom-project data volume this is fine; if the `hazards` collection
 * grows large, revisit with a geohash field and a bounding-box query instead
 * of fetching everything and filtering client-side.
 */
@Singleton
class HazardFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun fetchActiveHazards(): List<HazardEntity> {
        val snapshot = firestore.collection(FirestoreCollections.HAZARDS).get().awaitResult()
        return snapshot.documents.mapNotNull { doc ->
            runCatching {
                HazardEntity(
                    id = doc.id,
                    type = doc.getString("type") ?: return@runCatching null,
                    severity = doc.getString("severity") ?: return@runCatching null,
                    title = doc.getString("title") ?: return@runCatching null,
                    description = doc.getString("description").orEmpty(),
                    latitude = doc.getDouble("latitude") ?: return@runCatching null,
                    longitude = doc.getDouble("longitude") ?: return@runCatching null,
                    radiusMeters = doc.getDouble("radiusMeters") ?: 0.0,
                    issuedAtEpochMillis = doc.getLong("issuedAtEpochMillis") ?: 0L,
                    source = doc.getString("source").orEmpty()
                )
            }.getOrNull()
        }
    }
}
