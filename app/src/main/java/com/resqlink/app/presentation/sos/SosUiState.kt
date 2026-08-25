package com.resqlink.app.presentation.sos

enum class SosSendState { IDLE, SENDING, SENT_LOCALLY, SYNCED, FAILED }

/** Purely informational -- never gates the SOS button itself. See SosViewModel.sendSos(). */
enum class LocationStatus { IDLE, LOCATING, AVAILABLE, UNAVAILABLE, PERMISSION_DENIED }

data class SosUiState(
    val sendState: SosSendState = SosSendState.IDLE,
    val message: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAccuracyMeters: Float? = null,
    val locationStatus: LocationStatus = LocationStatus.IDLE
)
