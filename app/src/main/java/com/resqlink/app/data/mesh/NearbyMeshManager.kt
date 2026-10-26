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
import com.resqlink.app.domain.model.MeshPeerInfo
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import android.net.wifi.WifiManager
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
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
    private val localNodeId = java.util.UUID.randomUUID().toString().take(12)

    private fun isSelfAddress(address: InetAddress): Boolean {
        if (address.isLoopbackAddress || address.isAnyLocalAddress) return true
        return try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces() ?: return false
            while (interfaces.hasMoreElements()) {
                val nif = interfaces.nextElement()
                val addrs = nif.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (addr.hostAddress == address.hostAddress) {
                        return true
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private val connectedEndpoints = mutableSetOf<String>()
    private var isRunning = false
    private var bridgePingJob: Job? = null
    private var isBridgeConnected = false

    // Wi-Fi Mesh Socket & State
    private var udpSocket: DatagramSocket? = null
    private var udpReceiverJob: Job? = null
    private var udpBeaconJob: Job? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private val wifiMeshPeers = mutableMapOf<String, Long>()

    private val _relayState = MutableStateFlow<MeshRelayState>(MeshRelayState.Idle)
    override val relayState: StateFlow<MeshRelayState> = _relayState.asStateFlow()

    override fun start() {
        if (isRunning) return
        isRunning = true
        _relayState.value = MeshRelayState.Starting

        // 1. Bluetooth Nearby Connections Cluster
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

        // 2. Local Wi-Fi Mesh Radio (UDP Broadcast on port 8988)
        startWifiMesh()

        // 3. Local USB Bridge Link
        startBridgePingLoop()
    }

    override fun stop() {
        if (!isRunning) return
        isRunning = false
        bridgePingJob?.cancel()
        bridgePingJob = null
        isBridgeConnected = false
        stopWifiMesh()
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
        _relayState.value = MeshRelayState.Idle
    }

    override fun broadcast(alert: SosAlert) {
        val payload = MeshSosPayload.fromDomain(alert)
        // Instant Wi-Fi Mesh blast (under 5ms latency to all phones and laptops on Wi-Fi)
        broadcastWifiMesh(payload)
        // Bluetooth Nearby Connections broadcast
        sendToPeers(payload, excludeEndpointId = null)
        // Local tunnel relay
        relayToBridge(payload)
    }

    private fun startWifiMesh() {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifiManager?.createMulticastLock("resqlink_wifi_mesh")?.apply {
                setReferenceCounted(true)
                acquire()
            }

            udpSocket = DatagramSocket(WIFI_MESH_PORT).apply {
                broadcast = true
                reuseAddress = true
            }

            // Wi-Fi Mesh Packet Receiver Loop
            udpReceiverJob = scope.launch(Dispatchers.IO) {
                val buffer = ByteArray(8192)
                val packet = DatagramPacket(buffer, buffer.size)
                while (isActive) {
                    try {
                        udpSocket?.receive(packet)
                        val address = packet.address
                        if (isSelfAddress(address)) {
                            continue
                        }

                        val text = String(packet.data, 0, packet.length)
                        if (text.contains("\"nodeId\":\"$localNodeId\"")) {
                            continue
                        }

                        if (text.startsWith("{\"beacon\":")) {
                            val peerId = address.hostAddress ?: "wifi_peer"
                            wifiMeshPeers[peerId] = System.currentTimeMillis()
                            updateMeshRelayState()
                        } else if (text.contains("\"id\":") && text.contains("\"latitude\":")) {
                            val incoming = runCatching { json.decodeFromString<MeshSosPayload>(text) }.getOrNull()
                            if (incoming != null) {
                                handleIncomingAlert(incoming, receivedFromEndpointId = "WIFI_${address.hostAddress}")
                            }
                        }
                    } catch (e: Exception) {
                        // ignore socket close / timeout
                    }
                }
            }

            // Wi-Fi Mesh Beacon Loop (Heartbeat every 2.5s)
            udpBeaconJob = scope.launch(Dispatchers.IO) {
                val beaconJson = "{\"beacon\":\"${Build.MODEL ?: "ResQLink"}\",\"nodeId\":\"$localNodeId\"}".toByteArray()
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val beaconPacket = DatagramPacket(beaconJson, beaconJson.size, broadcastAddr, WIFI_MESH_PORT)
                while (isActive) {
                    try {
                        udpSocket?.send(beaconPacket)
                        val now = System.currentTimeMillis()
                        val before = wifiMeshPeers.size
                        wifiMeshPeers.entries.removeIf { now - it.value > 6000 }
                        if (wifiMeshPeers.size != before) {
                            updateMeshRelayState()
                        }
                    } catch (e: Exception) {}
                    delay(2500)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wi-Fi mesh socket init failed: ${e.message}")
        }
    }

    private fun stopWifiMesh() {
        try {
            udpReceiverJob?.cancel()
            udpBeaconJob?.cancel()
            udpSocket?.close()
            udpSocket = null
            wifiMeshPeers.clear()
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (e: Exception) {}
    }

    private fun broadcastWifiMesh(payload: MeshSosPayload) {
        scope.launch(Dispatchers.IO) {
            try {
                val bytes = json.encodeToString(payload).encodeToByteArray()
                val broadcastAddr = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(bytes, bytes.size, broadcastAddr, WIFI_MESH_PORT)
                udpSocket?.send(packet)
            } catch (e: Exception) {
                Log.w(TAG, "Wi-Fi broadcast failed: ${e.message}")
            }
        }
    }

    private fun updateMeshRelayState() {
        val peersList = mutableListOf<MeshPeerInfo>()
        connectedEndpoints.forEach { endpointId ->
            peersList.add(
                MeshPeerInfo(
                    id = endpointId,
                    name = "Nearby Bluetooth Node ($endpointId)",
                    transport = "Bluetooth Nearby / BLE",
                    status = "Connected",
                    signalOrAddress = "P2P Cluster"
                )
            )
        }
        wifiMeshPeers.forEach { (ip, lastSeen) ->
            val agoSec = ((System.currentTimeMillis() - lastSeen) / 1000).coerceAtLeast(0)
            peersList.add(
                MeshPeerInfo(
                    id = "wifi_$ip",
                    name = "Wi-Fi Mesh Node ($ip)",
                    transport = "Wi-Fi Direct / Local Mesh",
                    status = "Active (${agoSec}s ago)",
                    signalOrAddress = "$ip:$WIFI_MESH_PORT"
                )
            )
        }
        if (isBridgeConnected) {
            peersList.add(
                MeshPeerInfo(
                    id = "bridge_node",
                    name = "Rescue Operations Hub / Bridge",
                    transport = "USB / Local HTTP Tunnel",
                    status = "Connected",
                    signalOrAddress = "127.0.0.1:$WIFI_MESH_PORT"
                )
            )
        }
        _relayState.value = MeshRelayState.Ready(peersList.size, peersList)
    }

    private fun startBridgePingLoop() {
        bridgePingJob?.cancel()
        bridgePingJob = scope.launch(Dispatchers.IO) {
            val deviceModel = Build.MODEL ?: "ResQLink device"
            while (isActive) {
                try {
                    val url = URL("http://127.0.0.1:8988/api/ping?device=${java.net.URLEncoder.encode(deviceModel, "UTF-8")}")
                    val conn = (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 1500
                        readTimeout = 1500
                        requestMethod = "GET"
                    }
                    val code = conn.responseCode
                    conn.disconnect()
                    if (code == 200) {
                        isBridgeConnected = true
                        updateMeshRelayState()
                    } else {
                        isBridgeConnected = false
                    }
                } catch (e: Exception) {
                    isBridgeConnected = false
                }
                delay(2500)
            }
        }
    }

    private fun relayToBridge(payload: MeshSosPayload) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("http://127.0.0.1:8988/api/sos")
                val jsonBytes = json.encodeToString(payload).encodeToByteArray()
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 2000
                    readTimeout = 2000
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    outputStream.use { it.write(jsonBytes) }
                }
                conn.responseCode
                conn.disconnect()
            } catch (e: Exception) {
                // Bridge optional; ignore if laptop station not running
            }
        }
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
                updateMeshRelayState()
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoints.remove(endpointId)
            updateMeshRelayState()
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
        private const val WIFI_MESH_PORT = 8988
    }
}
