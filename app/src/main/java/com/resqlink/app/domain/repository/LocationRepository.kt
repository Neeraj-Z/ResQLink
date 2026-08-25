package com.resqlink.app.domain.repository

import com.resqlink.app.domain.model.LocationResult

/**
 * One-shot, best-effort location lookup. Deliberately not a continuous
 * stream -- SOS just needs "where is the user right now", not a live feed --
 * which keeps the ViewModel side simple and avoids leaving a location
 * callback registered longer than necessary.
 */
interface LocationRepository {
    /**
     * @param timeoutMillis how long to wait for a fresh GPS/network fix before
     *   falling back to the last known location, and finally to [LocationResult.Unavailable].
     *   Callers on the emergency path (SOS) should pass a short timeout --
     *   speed matters more than precision when someone is mid-emergency.
     */
    suspend fun getCurrentLocation(timeoutMillis: Long = 5_000L): LocationResult
}
