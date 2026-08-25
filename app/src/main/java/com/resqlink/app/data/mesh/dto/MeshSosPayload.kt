package com.resqlink.app.data.mesh.dto

import com.resqlink.app.data.local.entity.SosEntity
import com.resqlink.app.domain.model.SosAlert
import kotlinx.serialization.Serializable

/**
 * Wire format sent over a Nearby Connections BYTES payload. Kept as its own
 * small DTO (rather than reusing [SosAlert] directly) so the on-the-wire
 * schema can evolve independently of the domain model -- e.g. adding a
 * `messageType` discriminator later for ack/resource-request packets without
 * touching SosAlert at all.
 */
@Serializable
data class MeshSosPayload(
    val id: String,
    val userId: String,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val severity: String,
    val createdAtEpochMillis: Long,
    val hopCount: Int
) {
    companion object {
        /** Hard cap on relay hops so a single alert can't flood-propagate forever across a large mesh. */
        const val MAX_HOPS = 6

        fun fromDomain(alert: SosAlert): MeshSosPayload = MeshSosPayload(
            id = alert.id, userId = alert.userId, latitude = alert.latitude, longitude = alert.longitude,
            message = alert.message, severity = alert.severity.name,
            createdAtEpochMillis = alert.createdAtEpochMillis, hopCount = alert.hopCount
        )
    }

    fun toEntity(isSyncedToServer: Boolean = false): SosEntity = SosEntity(
        id = id, userId = userId, latitude = latitude, longitude = longitude, message = message,
        severity = severity, status = "RELAYED_VIA_MESH", createdAtEpochMillis = createdAtEpochMillis,
        isMeshRelayed = true, isSyncedToServer = isSyncedToServer, hopCount = hopCount
    )
}
