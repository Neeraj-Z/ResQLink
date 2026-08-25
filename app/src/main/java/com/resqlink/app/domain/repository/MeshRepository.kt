package com.resqlink.app.domain.repository

import com.resqlink.app.domain.model.MeshRelayState
import com.resqlink.app.domain.model.SosAlert
import kotlinx.coroutines.flow.StateFlow

/**
 * Peer-to-peer relay for SOS alerts over Bluetooth/Wi-Fi Direct (Google Nearby
 * Connections API under the hood), used when a device has no internet path to
 * the backend but is within range of another ResQLink device that might.
 *
 * This is deliberately a thin contract: the mesh layer's only job is to move
 * an already-persisted [SosAlert] to nearby devices and hand incoming alerts
 * back to the normal offline-first pipeline (Room -> WorkManager sync) --
 * it never bypasses that pipeline or invents a second source of truth.
 */
interface MeshRepository {
    val relayState: StateFlow<MeshRelayState>

    /** Begins advertising this device and discovering peers. Safe to call repeatedly; no-op if already running. */
    fun start()

    /** Stops advertising/discovery and disconnects from all peers. */
    fun stop()

    /** Sends [alert] to every currently connected peer. No-op (not an error) if no peers are connected. */
    fun broadcast(alert: SosAlert)
}
