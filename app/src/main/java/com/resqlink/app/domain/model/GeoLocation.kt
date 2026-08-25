package com.resqlink.app.domain.model

/** A device location reading. Kept separate from [SosAlert]'s lat/lng fields since other future modules (Map, family tracking) need location without needing an SOS context. */
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val timestampEpochMillis: Long
)

/**
 * Outcome of a location fetch, modeled explicitly rather than as a nullable
 * or an exception -- permission and availability problems are expected,
 * routine conditions for a location-dependent app, not edge cases to throw
 * over. Callers (SosViewModel in particular) must be able to handle every
 * case without a try/catch, since SOS must never be blocked by a location
 * failure.
 */
sealed class LocationResult {
    data class Success(val location: GeoLocation) : LocationResult()
    data object PermissionDenied : LocationResult()
    /** GPS/network location off, no fix yet, or the fetch timed out. */
    data object Unavailable : LocationResult()
}
