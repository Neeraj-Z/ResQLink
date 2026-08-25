package com.resqlink.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.resqlink.app.domain.repository.HazardRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Periodically refreshes the local hazard cache so it stays reasonably current even in the background. */
@HiltWorker
class HazardSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val hazardRepository: HazardRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val lat = inputData.getDouble(KEY_LAT, 0.0)
        val lng = inputData.getDouble(KEY_LNG, 0.0)
        val result = hazardRepository.refreshHazards(lat, lng)
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val WORK_NAME = "hazard_sync_worker"
        const val KEY_LAT = "lat"
        const val KEY_LNG = "lng"
    }
}
