package com.resqlink.app.data.mesh

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.resqlink.app.data.local.dao.SosDao
import com.resqlink.app.data.mesh.dto.MeshSosPayload
import com.resqlink.app.domain.model.MeshRelayState
import com.resqlink.app.domain.model.SosAlert
import com.resqlink.app.domain.repository.MeshRepository
import com.resqlink.app.domain.repository.SosRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline SOS relay over Google's Nearby Connections API (Bluetooth Classic,
 * BLE, and Wi-Fi Direct, chosen automatically per link by the API).
 *
 * Design, in plain terms: every device both advertises itself AND searches
 * for others at the same time ([Strategy.P2P_CLUSTER]), so any two ResQLink
 * phones in range connect automatically with no pairing UI. When an SOS
 * alert can't reach the backend directly, it's flooded to every connected
 * peer; each peer stores it (offline-first, same as a locally-triggered
 * alert) and re-floods it onward, capped by [MeshSosPayload.MAX_HOPS] and
 * de-duplicated via [SosDao.exists] so the same alert can't loop forever.
 * The first device in the chain that regains internet connectivity is the
 * one that actually delivers it -- [SosSyncWorker][com.resqlink.app.worker.SosSyncWorker]
 * doesn't know or care whether an alert originated locally or arrived over
 * the mesh, which is what keeps this integration simple.
 *
 * Lifecycle: [start]/[stop] are called by [com.resqlink.app.service.SosForegroundService],
 * not directly by any ViewModel -- that's what lets mesh advertising survive
 * the user backgrounding the app or leaving the SOS screen, matching real
 * disaster-scenario behavior rather than only working while a screen is open.
 */
@Singleton
class NearbyMeshManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sosDao: SosDao,
    private val sosRepository: SosRepository
) : MeshRepository {

    private val scope = CoroutineScope(SupervisorJob())
    private val connectionsClient: ConnectionsClient by lazy { Nearby.getConnectionsClient(context) }
    private val json = Json { ignoreUnknownKeys = true }

    private val connectedEndpoints = mutableSetOf<String>()
    private var isRunning = false

    private val _relayState = MutableStateFlow<MeshRelayState>(MeshRelayState.Idle)
    override val relayState: StateFlow<MeshRelayState> = _relayState.asStateFlow()

    override fun start() {
        if (isRunning) return
        isRunning = true
        _relayState.value = MeshRelayState.Starting

        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startAdvertising(
            Build.MODEL ?: "ResQLink device",
            SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnFailureListener { onError("Advertising failed: ${it.message}") }

        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        connectionsClient.startDiscovery(
            SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnFailureListener { onError("Discovery failed: ${it.message}") }
    }

    override fun stop() {
        if (!isRunning) return
        isRunning = false
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
        _relayState.value = MeshRelayState.Idle
    }

    override fun broadcast(alert: SosAlert) {
        sendToPeers(MeshSosPayload.fromDomain(alert), excludeEndpointId = null)
    }

    private fun sendToPeers(payload: MeshSosPayload, excludeEndpointId: String?) {
        if (connectedEndpoints.isEmpty()) return
        val bytes = json.encodeToString(payload).encodeToByteArray()
        val targets = connectedEndpoints.filter { it != excludeEndpointId }
        if (targets.isEmpty()) return
        connectionsClient.sendPayload(targets, Payload.fromBytes(bytes))
    }

    private fun onError(message: String) {
        Log.w(TAG, message)
        _relayState.value = MeshRelayState.Error(message)
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            if (info.serviceId != SERVICE_ID) return
            connectionsClient.requestConnection(Build.MODEL ?: "ResQLink device", endpointId, connectionLifecycleCallback)
                .addOnFailureListener { Log.w(TAG, "requestConnection failed for $endpointId: ${it.message}") }
        }

        override fun onEndpointLost(endpointId: String) {
            // Nothing to do -- if it was connected, onDisconnected below handles cleanup.
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Auto-accept: ResQLink's mesh is meant to work with zero user interaction
            // in an emergency -- there is no pairing prompt shown to the person.
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            if (resolution.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                connectedEndpoints.add(endpointId)
                _relayState.value = MeshRelayState.Ready(connectedEndpoints.size)
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            _relayState.value = if (connectedEndpoints.isEmpty()) {
                MeshRelayState.Ready(0)
            } else {
                MeshRelayState.Ready(connectedEndpoints.size)
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            val bytes = payload.asBytes() ?: return
            val incoming = runCatching { json.decodeFromString<MeshSosPayload>(bytes.decodeToString()) }.getOrNull() ?: return
            handleIncomingAlert(incoming, receivedFromEndpointId = endpointId)
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Single-shot BYTES payloads complete in one update; nothing to track for progress here.
        }
    }

    /**
     * Core relay logic: store-and-forward with hop capping and dedup.
     * This is what turns point-to-point connections into an actual mesh --
     * without it, an alert would only ever reach a device's *direct*
     * neighbors instead of propagating across the whole connected cluster.
     */
    private fun handleIncomingAlert(payload: MeshSosPayload, receivedFromEndpointId: String) {
        if (payload.hopCount >= MeshSosPayload.MAX_HOPS) return

        scope.launch {
            val alreadySeen = sosDao.exists(payload.id)
            if (alreadySeen) return@launch // stops loops: we've already stored/forwarded this alert once

            sosDao.upsert(payload.toEntity())
            // Opportunistic: if *this* device happens to have connectivity, push it now
            // rather than waiting for the next WorkManager sync cycle.
            sosRepository.syncPendingAlerts()

            sendToPeers(payload.copy(hopCount = payload.hopCount + 1), excludeEndpointId = receivedFromEndpointId)
        }
    }

    companion object {
        private const val TAG = "NearbyMeshManager"
        private const val SERVICE_ID = "com.resqlink.app.MESH_SERVICE"
    }
}
