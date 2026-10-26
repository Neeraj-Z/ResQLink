package com.resqlink.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.resqlink.app.R
import com.resqlink.app.domain.model.MeshRelayState
import com.resqlink.app.domain.repository.MeshRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Persistent foreground service hosting the mesh relay's lifecycle.
 *
 * This is the production home for [MeshRepository.start]/[stop] described in
 * NearbyMeshManager's doc comment: advertising/discovery now survives the
 * user backgrounding the app or leaving the SOS screen, which matters
 * because a relayed alert needs to keep hopping across the mesh even if the
 * person who triggered it isn't staring at their phone.
 *
 * [MeshRepository] itself is a `@Singleton`, so the SOS screen's
 * [com.resqlink.app.presentation.sos.MeshViewModel] can keep observing
 * [MeshRepository.relayState] for UI display regardless of the fact that
 * *this* service, not the ViewModel, now owns start()/stop().
 */
@AndroidEntryPoint
class SosForegroundService : Service() {

    @Inject lateinit var meshRepository: MeshRepository

    private var serviceScope: CoroutineScope? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                buildNotification(MeshRelayState.Starting),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(MeshRelayState.Starting))
        }
        meshRepository.start()

        // Keep the persistent notification's text in sync with peer count --
        // a bare "relay is running" notification with no feedback would look
        // broken the moment the first peer actually connects.
        if (serviceScope == null) {
            val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            serviceScope = scope
            meshRepository.relayState
                .onEach { state -> updateNotification(state) }
                .launchIn(scope)
        }

        // START_STICKY: if the OS kills this process under memory pressure,
        // restart it and resume relaying rather than silently dropping out
        // of the mesh -- appropriate for a service whose entire job is
        // carrying emergency traffic for other devices, not just this one.
        return START_STICKY
    }

    override fun onDestroy() {
        meshRepository.stop()
        serviceScope?.cancel()
        serviceScope = null
        super.onDestroy()
    }

    private fun updateNotification(state: MeshRelayState) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(state))
    }

    private fun buildNotification(state: MeshRelayState): Notification {
        ensureChannel()

        val statusText = when (state) {
            is MeshRelayState.Idle, is MeshRelayState.Starting -> "Searching for nearby devices\u2026"
            is MeshRelayState.Ready -> {
                if (state.connectedPeerCount > 0) {
                    val plural = if (state.connectedPeerCount == 1) "" else "s"
                    "Relaying via ${state.connectedPeerCount} nearby device$plural"
                } else {
                    "No nearby devices connected yet"
                }
            }
            is MeshRelayState.Error -> "Mesh relay unavailable"
        }

        val stopIntent = Intent(this, SosForegroundService::class.java).setAction(ACTION_STOP)
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ResQLink is relaying emergency alerts")
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Stop relay", stopPendingIntent)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Emergency relay", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "sos_relay_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.resqlink.app.action.STOP_MESH_RELAY"

        /** Starts (or is a no-op if already running -- see MeshRepository.start) mesh relay as a persistent foreground service. */
        fun start(context: Context) {
            val intent = Intent(context, SosForegroundService::class.java)
            context.startForegroundService(intent)
        }

        /** Stops the relay service. Also reachable from the notification's own "Stop relay" action. */
        fun stop(context: Context) {
            context.stopService(Intent(context, SosForegroundService::class.java))
        }
    }
}
