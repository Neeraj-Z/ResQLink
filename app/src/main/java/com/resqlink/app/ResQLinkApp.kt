package com.resqlink.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.resqlink.app.data.local.seed.DatabaseSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application entry point.
 *
 * Bootstraps Hilt's DI graph and wires [HiltWorkerFactory] so that
 * WorkManager workers (sync, mesh-relay retry, alert polling) can
 * receive constructor-injected dependencies just like any other class.
 */
@HiltAndroidApp
class ResQLinkApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var databaseSeeder: DatabaseSeeder

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Debug-only, and dispatched off the main thread so it never delays
        // cold-start: fills Room with realistic sample data (first-aid guides,
        // hazard zones, a sample profile) so every screen is demoable before
        // a real backend exists. See DatabaseSeeder's doc comment -- this must
        // never run in a release build.
        if (BuildConfig.DEBUG) {
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                databaseSeeder.seedSampleData()
            }
        }
    }
}
