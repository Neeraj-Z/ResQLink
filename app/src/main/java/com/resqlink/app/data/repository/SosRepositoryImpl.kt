package com.resqlink.app.data.repository

import com.resqlink.app.data.local.dao.SosDao
import com.resqlink.app.data.local.entity.SosEntity
import com.resqlink.app.data.remote.firestore.SosFirestoreDataSource
import com.resqlink.app.domain.model.SosAlert
import com.resqlink.app.domain.model.SosStatus
import com.resqlink.app.domain.repository.SosRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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

    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO)

    override fun observeAlerts(): Flow<List<SosAlert>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun triggerSos(alert: SosAlert) {
        // Write to local Room DB immediately -- returns in under 2ms
        dao.upsert(alert.toEntity(isSynced = false))

        // Best-effort remote push runs asynchronously in background without blocking the UI
        scope.launch {
            runCatching {
                remote.submitAlert(alert.toEntity(isSynced = false))
            }.onSuccess {
                dao.markSynced(alert.id, SosStatus.SENT.name)
            }
            pushToMasterDashboard(alert.toEntity(isSynced = false))
        }
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
            pushToMasterDashboard(entity)
        }
        syncedCount
    }
}

private fun pushToMasterDashboard(entity: SosEntity) {
    // URL comes from BuildConfig, which reads local.properties at build time.
    // Change MASTER_DASHBOARD_URL in local.properties to point at any server —
    // same LAN, cloud host, or ngrok tunnel. No code change required.
    val masterUrl = "${com.resqlink.app.BuildConfig.MASTER_DASHBOARD_URL}/api/opportunistic_sync"
    try {
        val jsonString = """
            {
              "deviceNodeId": "${android.os.Build.MODEL ?: "ResQLink-Phone"} (Opportunistic Network Link)",
              "alerts": [
                {
                  "id": "${entity.id}",
                  "userId": "${entity.userId}",
                  "latitude": ${entity.latitude},
                  "longitude": ${entity.longitude},
                  "message": "${entity.message.replace("\"", "\\\"")}",
                  "severity": "${entity.severity}",
                  "createdAtEpochMillis": ${entity.createdAtEpochMillis},
                  "isMeshRelayed": ${entity.isMeshRelayed},
                  "hopCount": ${entity.hopCount}
                }
              ]
            }
        """.trimIndent()

        runCatching {
            val url = java.net.URL(masterUrl)
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("X-Device-Node", android.os.Build.MODEL ?: "ResQLink-Node")
                doOutput = true
                outputStream.use { it.write(jsonString.toByteArray(Charsets.UTF_8)) }
            }
            val code = conn.responseCode
            conn.disconnect()
            android.util.Log.i("ResQLink/Sync", "Master dashboard sync → HTTP $code ($masterUrl)")
        }.onFailure { e ->
            android.util.Log.w("ResQLink/Sync", "Master dashboard unreachable ($masterUrl): ${e.message}")
        }
    } catch (e: Exception) {
        // Master server is always optional — app works fully offline without it
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
