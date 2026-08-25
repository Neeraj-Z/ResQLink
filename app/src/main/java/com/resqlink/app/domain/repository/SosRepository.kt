package com.resqlink.app.domain.repository

import com.resqlink.app.domain.model.SosAlert
import kotlinx.coroutines.flow.Flow

interface SosRepository {
    fun observeAlerts(): Flow<List<SosAlert>>
    /** Persists locally FIRST (always succeeds offline), then attempts a network sync. */
    suspend fun triggerSos(alert: SosAlert)
    /** Called by [com.resqlink.app.worker.SosSyncWorker] to flush the offline queue. */
    suspend fun syncPendingAlerts(): Result<Int>
}
