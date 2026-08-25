package com.resqlink.app.presentation.home

import com.resqlink.app.domain.model.HazardZone

data class HomeUiState(
    val isLoading: Boolean = false,
    val userName: String = "Guest",
    val nearbyHazards: List<HazardZone> = emptyList(),
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)
