package com.resqlink.app.data.remote.firestore

import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Bridges a Play Services [Task] into a suspend function. Firebase's Android
 * SDK is Task-based, not coroutine-based; this is the one shared adapter
 * point rather than repeating the same listener boilerplate in every
 * Firestore data source. Mirrors the same suspendCancellableCoroutine
 * pattern already used in FusedLocationRepository, for consistency.
 */
suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> if (cont.isActive) cont.resume(result) }
    addOnFailureListener { error -> if (cont.isActive) cont.resumeWithException(error) }
}
