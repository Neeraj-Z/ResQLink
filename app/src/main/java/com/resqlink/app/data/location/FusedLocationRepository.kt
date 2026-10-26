package com.resqlink.app.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
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
 * Wraps [FusedLocationProviderClient] behind [LocationRepository] with
 * multi-tier fallbacks:
 * 1. Immediate cached fix from Google Play Services or native Android LocationManager.
 * 2. Fresh high-accuracy satellite GPS fix within timeout.
 * 3. Fresh balanced network/Wi-Fi fix (crucial for indoor use).
 * 4. Stored native Android cell/Wi-Fi cached coordinates.
 */
@Singleton
class FusedLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : LocationRepository {

    private val client: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val locationManager: LocationManager? by lazy {
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    override suspend fun getCurrentLocation(timeoutMillis: Long): LocationResult {
        if (!hasLocationPermission()) return LocationResult.PermissionDenied

        // 1. Check if we already have a recent cached location
        val cached = requestLastKnownLocation() ?: getNativeFallbackLocation()
        if (cached != null && System.currentTimeMillis() - cached.timestampEpochMillis < 60_000L) {
            // Very fresh (< 1 minute old), return immediately
            return LocationResult.Success(cached)
        }

        // 2. Try fresh high-accuracy satellite GPS
        val fresh = withTimeoutOrNull(timeoutMillis) {
            requestFreshLocation(Priority.PRIORITY_HIGH_ACCURACY)
        }
        if (fresh != null) return LocationResult.Success(fresh)

        // 3. Fallback: try balanced network/Wi-Fi accuracy (works indoors)
        val networkFresh = withTimeoutOrNull(3_000L) {
            requestFreshLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
        }
        if (networkFresh != null) return LocationResult.Success(networkFresh)

        // 4. Return any last known location (even if older)
        if (cached != null) return LocationResult.Success(cached)

        // 5. Final fallback: Native Android OS LocationManager
        val nativeLocation = getNativeFallbackLocation()
        return nativeLocation?.let { LocationResult.Success(it) } ?: LocationResult.Unavailable
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @Suppress("MissingPermission")
    private suspend fun requestFreshLocation(priority: Int): GeoLocation? = suspendCancellableCoroutine { cont ->
        val request = CurrentLocationRequest.Builder()
            .setPriority(priority)
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

    @Suppress("MissingPermission")
    private fun getNativeFallbackLocation(): GeoLocation? {
        if (!hasLocationPermission()) return null
        val lm = locationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var bestLoc: Location? = null
        for (provider in providers) {
            try {
                if (lm.isProviderEnabled(provider)) {
                    val loc = lm.getLastKnownLocation(provider)
                    if (loc != null) {
                        if (bestLoc == null || loc.time > bestLoc.time) {
                            bestLoc = loc
                        }
                    }
                }
            } catch (e: Exception) {
                // provider might not be supported or restricted
            }
        }
        return bestLoc?.let { GeoLocation(it.latitude, it.longitude, it.accuracy, it.time) }
    }
}
