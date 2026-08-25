package com.resqlink.app.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resqlink.app.domain.model.LocationResult
import com.resqlink.app.domain.model.SosStatus
import com.resqlink.app.domain.repository.HazardRepository
import com.resqlink.app.domain.repository.LocationRepository
import com.resqlink.app.domain.repository.SosRepository
import com.resqlink.app.domain.usecase.ObserveHazardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Interactive Map: overlays cached hazard zones and unresolved
 * SOS alerts (both from Room, so both render even fully offline) on top of
 * the user's live location. Hazard/SOS data comes from the same
 * offline-first repositories every other screen uses -- the map is not a
 * separate data path, just a different rendering of the same local state.
 */
@HiltViewModel
class MapViewModel @Inject constructor(
    observeHazards: ObserveHazardsUseCase,
    sosRepository: SosRepository,
    private val hazardRepository: HazardRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        combine(
            observeHazards(),
            sosRepository.observeAlerts().map { alerts -> alerts.filter { it.status != SosStatus.RESOLVED } }
        ) { hazards, activeAlerts ->
            hazards to activeAlerts
        }.onEach { (hazards, activeAlerts) ->
            _uiState.update { it.copy(hazards = hazards, activeSosAlerts = activeAlerts) }
        }.launchIn(viewModelScope)

        locateUser()
    }

    /** Best-effort: the map still renders hazards/SOS pins even if location is denied or unavailable. */
    fun locateUser() {
        viewModelScope.launch {
            val result = locationRepository.getCurrentLocation(timeoutMillis = 6_000L)
            if (result is LocationResult.Success) {
                _uiState.update {
                    it.copy(userLatitude = result.location.latitude, userLongitude = result.location.longitude)
                }
                refresh()
            }
        }
    }

    fun refresh() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            hazardRepository.refreshHazards(
                lat = state.userLatitude ?: 0.0,
                lng = state.userLongitude ?: 0.0
            )
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }
}
