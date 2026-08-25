package com.resqlink.app.di

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the Firestore client used by the data/remote/firestore data sources. */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        // ResQLink's own offline-first cache is Room, not Firestore's built-in
        // persistence -- keeping Firestore's local cache in-memory only avoids
        // two overlapping offline caches (Room's, which the rest of the app
        // already depends on, and Firestore's disk cache) disagreeing with
        // each other about what "the current data" is.
        firestore.firestoreSettings = firestoreSettings {
            setLocalCacheSettings(memoryCacheSettings {})
        }
        return firestore
    }
}
