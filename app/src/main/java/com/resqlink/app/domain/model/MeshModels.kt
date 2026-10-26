package com.resqlink.app.domain.model

data class MeshPeerInfo(
    val id: String,
    val name: String,
    val transport: String,
    val status: String = "Connected",
    val signalOrAddress: String = ""
)

/**
 * State of the on-device mesh relay engine (see [com.resqlink.app.domain.repository.MeshRepository]).
 * Kept separate from the SOS domain model since mesh is a transport concern,
 * not an emergency-data concern -- SOS alerts don't know or care how they travel.
 */
sealed class MeshRelayState {
    data object Idle : MeshRelayState()
    data object Starting : MeshRelayState()
    data class Ready(
        val connectedPeerCount: Int,
        val peers: List<MeshPeerInfo> = emptyList()
    ) : MeshRelayState()
    data class Error(val message: String) : MeshRelayState()
}

