package com.resqlink.app.domain.model

/** Delivery state of an SOS alert as it moves through the offline-first pipeline. */
enum class SosStatus { PENDING_SYNC, SENT, RELAYED_VIA_MESH, ACKNOWLEDGED, RESOLVED }

enum class SosSeverity { CRITICAL, HIGH, MODERATE }

/**
 * Domain representation of an emergency SOS event.
 *
 * Created locally the instant a user triggers SOS -- persisted to Room immediately
 * (offline-first) -- then opportunistically synced to Firestore when connectivity
 * exists, or relayed peer-to-peer via the Nearby Connections mesh module otherwise.
 */
data class SosAlert(
    val id: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val severity: SosSeverity,
    val status: SosStatus,
    val createdAtEpochMillis: Long,
    val isMeshRelayed: Boolean = false,
    /** Number of device-to-device hops this alert has traveled over the mesh. Caps flood propagation -- see MeshRepository. */
    val hopCount: Int = 0
)
