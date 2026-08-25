package com.resqlink.app.worker

import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Central place to enqueue all background work -- keeps constraints/backoff policy consistent. */
@Singleton
class WorkScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun scheduleSosSync() {
        val request = OneTimeWorkRequestBuilder<SosSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        workManager.enqueueUniqueWork(SosSyncWorker.WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    fun scheduleHazardRefresh() {
        val request = PeriodicWorkRequestBuilder<HazardSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        workManager.enqueueUniquePeriodicWork(
            HazardSyncWorker.WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
        )
    }
}
