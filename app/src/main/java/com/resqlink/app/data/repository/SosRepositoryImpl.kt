package com.resqlink.app.data.repository

import com.resqlink.app.data.local.dao.SosDao
import com.resqlink.app.data.local.entity.SosEntity
import com.resqlink.app.data.remote.firestore.SosFirestoreDataSource
import com.resqlink.app.domain.model.SosAlert
import com.resqlink.app.domain.model.SosStatus
import com.resqlink.app.domain.repository.SosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Offline-first SOS repository, backed by Firestore (see README section 9
 * for the Firebase project setup this depends on).
 *
 * Contract: [triggerSos] MUST return successfully as soon as the alert is
 * written to Room, regardless of network state. The Firestore write is a
 * best-effort side attempt; genuine delivery guarantees during an outage come
 * from [syncPendingAlerts] (invoked by WorkManager) and the Nearby
 * Connections mesh-relay module (see NearbyMeshManager).
 */
class SosRepositoryImpl @Inject constructor(
    private val dao: SosDao,
    private val remote: SosFirestoreDataSource
) : SosRepository {

    override fun observeAlerts(): Flow<List<SosAlert>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun triggerSos(alert: SosAlert) {
        dao.upsert(alert.toEntity(isSynced = false))
        runCatching {
            remote.submitAlert(alert.toEntity(isSynced = false))
        }.onSuccess {
            dao.markSynced(alert.id, SosStatus.SENT.name)
        }
        // On failure we deliberately swallow the exception here: the alert is
        // already safe in Room, and SosSyncWorker will retry with backoff.
    }

    override suspend fun syncPendingAlerts(): Result<Int> = runCatching {
        val pending = dao.getUnsynced()
        var syncedCount = 0
        for (entity in pending) {
            runCatching {
                remote.submitAlert(entity)
            }.onSuccess {
                dao.markSynced(entity.id, SosStatus.SENT.name)
                syncedCount++
            }
        }
        syncedCount
    }
}

private fun SosEntity.toDomain() = SosAlert(
    id = id, userId = userId, latitude = latitude, longitude = longitude,
    message = message, severity = com.resqlink.app.domain.model.SosSeverity.valueOf(severity),
    status = SosStatus.valueOf(status), createdAtEpochMillis = createdAtEpochMillis,
    isMeshRelayed = isMeshRelayed, hopCount = hopCount
)

private fun SosAlert.toEntity(isSynced: Boolean) = SosEntity(
    id = id, userId = userId, latitude = latitude, longitude = longitude,
    message = message, severity = severity.name, status = status.name,
    createdAtEpochMillis = createdAtEpochMillis, isMeshRelayed = isMeshRelayed,
    isSyncedToServer = isSynced, hopCount = hopCount
)
