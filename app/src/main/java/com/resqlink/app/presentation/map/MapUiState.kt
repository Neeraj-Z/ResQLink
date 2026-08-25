package com.resqlink.app.presentation.map

import com.resqlink.app.domain.model.HazardZone
import com.resqlink.app.domain.model.SosAlert

data class MapUiState(
    val hazards: List<HazardZone> = emptyList(),
    val activeSosAlerts: List<SosAlert> = emptyList(),
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val isRefreshing: Boolean = false
)
