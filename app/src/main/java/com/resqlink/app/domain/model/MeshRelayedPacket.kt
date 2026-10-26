package com.resqlink.app.domain.model

enum class RelayDirection {
    BY_THIS_DEVICE,
    THROUGH_THIS_DEVICE
}

data class MeshRelayedPacket(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderPhone: String? = null,
    val senderDeviceName: String,
    val targetDeviceId: String,
    val targetDeviceName: String,
    val customMessage: String,
    val severity: SosSeverity,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double = 10.0,
    val bloodGroup: String? = null,
    val emergencyContact: String? = null,
    val medicalNotes: String? = null,
    val createdAtEpochMillis: Long,
    val hopCount: Int = 1,
    val relayPath: List<String> = emptyList(),
    val transport: String = "BLE_MESH",
    val deliveryStatus: String = "DELIVERED",
    val rssi: String = "-56 dBm (Strong)",
    val direction: RelayDirection = RelayDirection.THROUGH_THIS_DEVICE
)
