package com.resqlink.app.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.resqlink.app.data.local.entity.SosEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pushes SOS alerts to Firestore. Fields are written explicitly (a plain
 * `HashMap`, not Firestore's reflection-based POJO mapping) so the wire
 * schema is visible right here rather than depending on an entity class's
 * exact property layout matching Firestore's no-arg-constructor expectations.
 *
 * Uses the alert's own ID as the Firestore document ID (`.document(id).set(...)`)
 * rather than `.add(...)`, which auto-generates an ID -- this makes a retried
 * submit idempotent: resending the same alert overwrites the same document
 * instead of creating a duplicate.
 */
@Singleton
class SosFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun submitAlert(entity: SosEntity) {
        val data = hashMapOf(
            "id" to entity.id,
            "userId" to entity.userId,
            "latitude" to entity.latitude,
            "longitude" to entity.longitude,
            "message" to entity.message,
            "severity" to entity.severity,
            "createdAtEpochMillis" to entity.createdAtEpochMillis,
            "isMeshRelayed" to entity.isMeshRelayed,
            "hopCount" to entity.hopCount
        )
        firestore.collection(FirestoreCollections.SOS_ALERTS)
            .document(entity.id)
            .set(data)
            .awaitResult()
    }
}
