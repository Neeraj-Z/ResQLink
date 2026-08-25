package com.resqlink.app.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.resqlink.app.domain.model.GeoLocation
import com.resqlink.app.domain.model.LocationResult
import com.resqlink.app.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Wraps [FusedLocationProviderClient] behind [LocationRepository].
 *
 * Strategy: try for a fresh, high-accuracy fix within [LocationRepository.getCurrentLocation]'s
 * timeout; if that doesn't resolve in time (indoors, cold GPS, airplane mode with
 * only cached data, etc.), fall back to whatever last-known location the device
 * already has cached rather than returning nothing -- a slightly stale location
 * on an SOS alert is far more useful than none at all.
 */
@Singleton
class FusedLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationRepository {

    private val client: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    override suspend fun getCurrentLocation(timeoutMillis: Long): LocationResult {
        if (!hasLocationPermission()) return LocationResult.PermissionDenied

        val fresh = withTimeoutOrNull(timeoutMillis) { requestFreshLocation() }
        if (fresh != null) return LocationResult.Success(fresh)

        val lastKnown = requestLastKnownLocation()
        return lastKnown?.let { LocationResult.Success(it) } ?: LocationResult.Unavailable
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission") // hasLocationPermission() is checked by the only caller, getCurrentLocation()
    private suspend fun requestFreshLocation(): GeoLocation? = suspendCancellableCoroutine { cont ->
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        val cancellationSource = com.google.android.gms.tasks.CancellationTokenSource()
        cont.invokeOnCancellation { cancellationSource.cancel() }

        client.getCurrentLocation(request, cancellationSource.token)
            .addOnSuccessListener { location ->
                val result = location?.let {
                    GeoLocation(it.latitude, it.longitude, it.accuracy, it.time)
                }
                if (cont.isActive) cont.resume(result)
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
    }

    @Suppress("MissingPermission")
    private suspend fun requestLastKnownLocation(): GeoLocation? {
        if (!hasLocationPermission()) return null
        return suspendCancellableCoroutine { cont ->
            client.lastLocation
                .addOnSuccessListener { location ->
                    val result = location?.let {
                        GeoLocation(it.latitude, it.longitude, it.accuracy, it.time)
                    }
                    if (cont.isActive) cont.resume(result)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(null)
                }
        }
    }
}
