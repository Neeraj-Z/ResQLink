package com.resqlink.app.presentation.analytics

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resqlink.app.domain.model.HazardZone
import com.resqlink.app.domain.model.MeshRelayedPacket
import com.resqlink.app.domain.model.MeshRelayState
import com.resqlink.app.domain.model.RelayDirection
import com.resqlink.app.domain.model.SosAlert
import com.resqlink.app.domain.model.SosSeverity
import com.resqlink.app.domain.repository.HazardRepository
import com.resqlink.app.domain.repository.MeshRepository
import com.resqlink.app.domain.repository.SosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AnalyticsUiState(
    val totalAlerts: Int = 0,
    val pendingSync: Int = 0,
    val meshRelayedCount: Int = 0,
    val connectedPeerCount: Int = 0,
    val relayedByCount: Int = 0,
    val relayedThroughCount: Int = 0,
    val extremeHazards: Int = 0,
    val moderateHazards: Int = 0,
    val lowHazards: Int = 0,
    val packets: List<MeshRelayedPacket> = emptyList(),
    val filteredPackets: List<MeshRelayedPacket> = emptyList(),
    val activeFilter: String = "ALL", // "ALL", "THROUGH", "BY"
    val searchQuery: String = "",
    val thisDeviceName: String = "ResQLink (${Build.MODEL ?: "Android"})",
    val selectedPacket: MeshRelayedPacket? = null,
    val actionFeedback: String? = null
)

private data class FilterParams(
    val filter: String = "ALL",
    val query: String = "",
    val selectedPacket: MeshRelayedPacket? = null,
    val actionFeedback: String? = null
)

private data class BaseData(
    val allPackets: List<MeshRelayedPacket>,
    val peerCount: Int,
    val throughCount: Int,
    val byCount: Int,
    val extremeCount: Int,
    val moderateCount: Int,
    val lowCount: Int,
    val pendingSyncCount: Int
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val sosRepository: SosRepository,
    private val hazardRepository: HazardRepository,
    private val meshRepository: MeshRepository
) : ViewModel() {

    private val _manualPackets = MutableStateFlow<List<MeshRelayedPacket>>(emptyList())
    private val _filterParams = MutableStateFlow(FilterParams())

    private val _baseDataFlow = combine(
        sosRepository.observeAlerts(),
        hazardRepository.observeHazards(),
        meshRepository.relayState,
        _manualPackets
    ) { alerts: List<SosAlert>, hazards: List<HazardZone>, meshState: MeshRelayState, manualList: List<MeshRelayedPacket> ->
        val roomPackets = alerts.map { it.toRelayedPacket() }
        val allPackets = (manualList + roomPackets).distinctBy { it.id }

        val peerCount = when (meshState) {
            is MeshRelayState.Ready -> meshState.connectedPeerCount
            else -> 0
        }

        val throughCount = allPackets.count { it.direction == RelayDirection.THROUGH_THIS_DEVICE }
        val byCount = allPackets.count { it.direction == RelayDirection.BY_THIS_DEVICE }

        val extremeCount = hazards.count { it.severity.name == "EXTREME" || it.severity.name == "HIGH" }
        val moderateCount = hazards.count { it.severity.name == "MODERATE" }
        val lowCount = hazards.count { it.severity.name == "LOW" }
        val pending = alerts.count { it.status.name == "PENDING_SYNC" }

        BaseData(
            allPackets = allPackets,
            peerCount = peerCount,
            throughCount = throughCount,
            byCount = byCount,
            extremeCount = extremeCount,
            moderateCount = moderateCount,
            lowCount = lowCount,
            pendingSyncCount = pending
        )
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        _baseDataFlow,
        _filterParams
    ) { base: BaseData, params: FilterParams ->
        val allPackets = base.allPackets

        val filtered = allPackets.filter { packet ->
            val matchesFilter = when (params.filter) {
                "THROUGH" -> packet.direction == RelayDirection.THROUGH_THIS_DEVICE
                "BY" -> packet.direction == RelayDirection.BY_THIS_DEVICE
                else -> true
            }

            val matchesQuery = if (params.query.isBlank()) {
                true
            } else {
                val q = params.query.trim().lowercase()
                packet.senderName.lowercase().contains(q) ||
                packet.senderDeviceName.lowercase().contains(q) ||
                packet.targetDeviceName.lowercase().contains(q) ||
                packet.customMessage.lowercase().contains(q) ||
                (packet.senderPhone?.lowercase()?.contains(q) ?: false)
            }

            matchesFilter && matchesQuery
        }

        AnalyticsUiState(
            totalAlerts = allPackets.size,
            pendingSync = base.pendingSyncCount,
            meshRelayedCount = allPackets.size,
            connectedPeerCount = base.peerCount,
            relayedByCount = base.byCount,
            relayedThroughCount = base.throughCount,
            extremeHazards = base.extremeCount,
            moderateHazards = base.moderateCount,
            lowHazards = base.lowCount,
            packets = allPackets,
            filteredPackets = filtered,
            activeFilter = params.filter,
            searchQuery = params.query,
            thisDeviceName = "ResQLink (${Build.MODEL ?: "Android"})",
            selectedPacket = params.selectedPacket,
            actionFeedback = params.actionFeedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun setFilter(filter: String) {
        _filterParams.update { it.copy(filter = filter) }
    }

    fun setSearchQuery(query: String) {
        _filterParams.update { it.copy(query = query) }
    }

    fun selectPacket(packet: MeshRelayedPacket?) {
        _filterParams.update { it.copy(selectedPacket = packet) }
    }

    fun clearRelayedPackets() {
        _manualPackets.value = emptyList()
        _filterParams.update { it.copy(actionFeedback = "Relayed messages log cleared.", selectedPacket = null) }
    }

    fun dismissFeedback() {
        _filterParams.update { it.copy(actionFeedback = null) }
    }

    fun simulateRelay(direction: RelayDirection) {
        val now = System.currentTimeMillis()
        val thisDev = Build.MODEL ?: "Android Device"

        val packet = if (direction == RelayDirection.THROUGH_THIS_DEVICE) {
            val peerProfiles = listOf(
                Triple("Meera Patel", "Galaxy A54 (Rescue Scout)", "Flash flood level rising, 2 adults trapped on balcony."),
                Triple("Kavita Roy", "OnePlus 12 Node (Relay)", "Elderly citizen needing oxygen support. Road blocked."),
                Triple("Devendra Singh", "Redmi Note 13 (Volunteer)", "Clear evacuation route verified via elevated bypass.")
            )
            val randomPeer = peerProfiles.random()

            MeshRelayedPacket(
                id = "sim-through-$now",
                senderId = "node-${(1000..9999).random()}",
                senderName = randomPeer.first,
                senderPhone = "+91 98211 44556",
                senderDeviceName = randomPeer.second,
                targetDeviceId = "hub-ops-central",
                targetDeviceName = "District Ops Command Center",
                customMessage = "URGENT RELAY: ${randomPeer.third} Relayed through $thisDev via BLE Mesh.",
                severity = SosSeverity.HIGH,
                latitude = 28.6184,
                longitude = 77.2452,
                accuracyMeters = 8.0,
                bloodGroup = "B+",
                emergencyContact = "+91 98211 44556",
                medicalNotes = randomPeer.third,
                createdAtEpochMillis = now,
                hopCount = 2,
                relayPath = listOf(randomPeer.second, "$thisDev (Transit Relay)", "District Ops Command Center"),
                transport = "BLE_MESH",
                deliveryStatus = "CONFIRMED",
                rssi = "-56 dBm (Strong)",
                direction = RelayDirection.THROUGH_THIS_DEVICE
            )
        } else {
            MeshRelayedPacket(
                id = "sim-by-$now",
                senderId = "local_user",
                senderName = "Local Device User",
                senderPhone = "+91 98765 43210",
                senderDeviceName = "$thisDev (This Device)",
                targetDeviceId = "mesh-broadcast-all",
                targetDeviceName = "Nearby Mesh Peers (Sector Broadcast)",
                customMessage = "Local Broadcast: Rapid mesh heartbeat test. Forwarding operational status across perimeter.",
                severity = SosSeverity.MODERATE,
                latitude = 28.6139,
                longitude = 77.2090,
                accuracyMeters = 10.0,
                bloodGroup = "O+",
                emergencyContact = "+91 91234 56789",
                medicalNotes = "Perimeter checked, safe assembly secured.",
                createdAtEpochMillis = now,
                hopCount = 1,
                relayPath = listOf("$thisDev (This Device)", "Nearby Mesh Node (Ambulance Alpha)"),
                transport = "BLE_MESH",
                deliveryStatus = "DELIVERED",
                rssi = "-48 dBm (Excellent)",
                direction = RelayDirection.BY_THIS_DEVICE
            )
        }

        _manualPackets.update { listOf(packet) + it }
        _filterParams.update {
            it.copy(
                selectedPacket = packet,
                actionFeedback = if (direction == RelayDirection.THROUGH_THIS_DEVICE) {
                    "Simulated message relayed through this device!"
                } else {
                    "Simulated broadcast relayed by this device!"
                }
            )
        }
    }

    private fun SosAlert.toRelayedPacket(): MeshRelayedPacket {
        val thisDevice = Build.MODEL ?: "Android Device"
        val isThrough = this.isMeshRelayed
        val direction = if (isThrough) RelayDirection.THROUGH_THIS_DEVICE else RelayDirection.BY_THIS_DEVICE
        val senderName = if (isThrough) "Mesh Node (${this.userId.takeLast(4)})" else "Local Device User"
        val senderDevice = if (isThrough) "Peer Node (${this.userId.takeLast(4)})" else "$thisDevice (This Device)"
        val targetName = if (isThrough) "Operations Command Hub" else "All Mesh Peers (Sector Broadcast)"
        val path = if (isThrough) {
            listOf(senderDevice, "$thisDevice (Transit Relay)", targetName)
        } else {
            listOf(senderDevice, "Nearby Mesh Peer (Cluster)")
        }

        return MeshRelayedPacket(
            id = this.id,
            senderId = this.userId,
            senderName = senderName,
            senderPhone = "+91 98*** ****",
            senderDeviceName = senderDevice,
            targetDeviceId = "hub-ops-central",
            targetDeviceName = targetName,
            customMessage = this.message,
            severity = this.severity,
            latitude = this.latitude,
            longitude = this.longitude,
            accuracyMeters = 10.0,
            bloodGroup = "O+",
            emergencyContact = "+91 91234 56789",
            medicalNotes = "Relayed via ResQLink on-device mesh protocol.",
            createdAtEpochMillis = this.createdAtEpochMillis,
            hopCount = if (this.hopCount > 0) this.hopCount else (if (isThrough) 2 else 1),
            relayPath = path,
            transport = "BLE_MESH",
            deliveryStatus = if (this.status.name == "PENDING_SYNC") "RELAYING" else "DELIVERED",
            rssi = if (isThrough) "-58 dBm (Strong)" else "-46 dBm (Direct Broadcast)",
            direction = direction
        )
    }
}
