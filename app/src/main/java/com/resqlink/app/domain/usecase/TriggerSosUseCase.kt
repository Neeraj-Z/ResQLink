package com.resqlink.app.domain.usecase

import com.resqlink.app.domain.model.SosAlert
import com.resqlink.app.domain.model.SosSeverity
import com.resqlink.app.domain.model.SosStatus
import com.resqlink.app.domain.repository.MeshRepository
import com.resqlink.app.domain.repository.SosRepository
import java.util.UUID
import javax.inject.Inject

/**
 * Encapsulates the business rule for raising an SOS: always writes offline-first,
 * regardless of caller (SOS screen today; mesh-relay listener or voice assistant
 * later). Keeping this in a use case means new entry points never re-implement
 * the offline-first guarantee.
 *
 * Deliberately coordinates two *independent* delivery paths -- direct network
 * sync (via [SosRepository]) and peer-to-peer mesh relay (via [MeshRepository])
 * -- rather than having one repository depend on the other. Both start from
 * the same persisted alert; whichever path finds a way out first is the one
 * that actually delivers it. This also keeps the dependency graph acyclic:
 * the mesh layer reads from [SosRepository] when *receiving* relayed alerts,
 * so [SosRepository] itself must never depend back on [MeshRepository].
 */
class TriggerSosUseCase @Inject constructor(
    private val sosRepository: SosRepository,
    private val meshRepository: MeshRepository
) {
    suspend operator fun invoke(
        userId: String,
        latitude: Double,
        longitude: Double,
        message: String,
        severity: SosSeverity = SosSeverity.CRITICAL
    ) {
        val alert = SosAlert(
            id = UUID.randomUUID().toString(),
            userId = userId,
            latitude = latitude,
            longitude = longitude,
            message = message,
            severity = severity,
            status = SosStatus.PENDING_SYNC,
            createdAtEpochMillis = System.currentTimeMillis()
        )
        sosRepository.triggerSos(alert)
        meshRepository.broadcast(alert) // no-op if mesh isn't running or has no peers -- see NearbyMeshManager
    }
}
