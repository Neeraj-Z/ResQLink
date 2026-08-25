package com.resqlink.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.resqlink.app.domain.repository.SosRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic/expedited WorkManager job that flushes any SOS alerts still
 * marked unsynced in Room. This is what actually delivers an alert once
 * connectivity returns after an outage -- enqueued with a network
 * constraint from [com.resqlink.app.worker.WorkScheduler].
 */
@HiltWorker
class SosSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val sosRepository: SosRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val syncResult = sosRepository.syncPendingAlerts()
        return if (syncResult.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val WORK_NAME = "sos_sync_worker"
    }
}
