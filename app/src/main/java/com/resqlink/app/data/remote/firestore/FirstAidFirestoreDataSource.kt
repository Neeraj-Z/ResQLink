package com.resqlink.app.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.resqlink.app.data.local.entity.FirstAidEntity
import javax.inject.Inject
import javax.inject.Singleton

/** Fetches first-aid guides from Firestore to refresh the local offline cache. */
@Singleton
class FirstAidFirestoreDataSource @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun fetchGuides(): List<FirstAidEntity> {
        val snapshot = firestore.collection(FirestoreCollections.FIRST_AID_GUIDES).get().awaitResult()
        return snapshot.documents.mapNotNull { doc ->
            runCatching {
                @Suppress("UNCHECKED_CAST")
                val steps = (doc.get("steps") as? List<String>) ?: emptyList()
                FirstAidEntity(
                    id = doc.id,
                    title = doc.getString("title") ?: return@runCatching null,
                    category = doc.getString("category").orEmpty(),
                    summary = doc.getString("summary").orEmpty(),
                    steps = steps,
                    criticalWarning = doc.getString("criticalWarning"),
                    imageUrl = doc.getString("imageUrl")
                )
            }.getOrNull()
        }
    }
}
