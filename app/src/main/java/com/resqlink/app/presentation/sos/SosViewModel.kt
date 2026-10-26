package com.resqlink.app.presentation.sos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resqlink.app.domain.model.LocationResult
import com.resqlink.app.domain.repository.LocationRepository
import com.resqlink.app.domain.usecase.TriggerSosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the SOS flow. Kept intentionally simple: pressing SOS must never be
 * blocked by validation, spinners, or network waits -- state moves to
 * SENT_LOCALLY the instant the use case returns, which happens as soon as the
 * alert is safely in Room (see [TriggerSosUseCase] / SosRepositoryImpl).
 *
 * Location follows the same non-blocking principle: [refreshLocation] runs as
 * soon as permission is granted so a fix is usually already warm by the time
 * the person presses the button, but [sendSos] never waits on it indefinitely --
 * a missing or stale location is far better than a missing SOS.
 */
@HiltViewModel
class SosViewModel @Inject constructor(
    private val triggerSos: TriggerSosUseCase,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SosUiState())
    val uiState: StateFlow<SosUiState> = _uiState.asStateFlow()

    fun updateMessage(message: String) {
        _uiState.update { it.copy(message = message) }
    }

    /**
     * Kicks off a best-effort location fetch. Safe to call multiple times
     * (e.g. re-entering the screen) -- each call just refreshes the fix.
     */
    fun refreshLocation() {
        _uiState.update { it.copy(locationStatus = LocationStatus.LOCATING) }
        viewModelScope.launch {
            when (val result = locationRepository.getCurrentLocation(timeoutMillis = 10_000L)) {
                is LocationResult.Success -> _uiState.update {
                    it.copy(
                        latitude = result.location.latitude,
                        longitude = result.location.longitude,
                        locationAccuracyMeters = result.location.accuracyMeters,
                        locationStatus = LocationStatus.AVAILABLE
                    )
                }
                is LocationResult.PermissionDenied -> _uiState.update {
                    it.copy(locationStatus = LocationStatus.PERMISSION_DENIED)
                }
                is LocationResult.Unavailable -> _uiState.update {
                    it.copy(locationStatus = LocationStatus.UNAVAILABLE)
                }
            }
        }
    }

    fun sendSos(userId: String = "local_user") {
        viewModelScope.launch {
            _uiState.update { it.copy(sendState = SosSendState.SENDING) }

            // If a location isn't already warm, make one last short attempt --
            // capped well below anything that would feel like the button "hung".
            var state = _uiState.value
            if (state.latitude == null) {
                val quickFix = locationRepository.getCurrentLocation(timeoutMillis = 2_000L)
                if (quickFix is LocationResult.Success) {
                    _uiState.update {
                        it.copy(
                            latitude = quickFix.location.latitude,
                            longitude = quickFix.location.longitude,
                            locationAccuracyMeters = quickFix.location.accuracyMeters,
                            locationStatus = LocationStatus.AVAILABLE
                        )
                    }
                    state = _uiState.value
                }
            }

            runCatching {
                triggerSos(
                    userId = userId,
                    latitude = state.latitude ?: 0.0,
                    longitude = state.longitude ?: 0.0,
                    message = state.message.ifBlank { "Emergency assistance needed" }
                )
            }.onSuccess {
                _uiState.update { it.copy(sendState = SosSendState.SENT_LOCALLY) }
            }.onFailure {
                _uiState.update { it.copy(sendState = SosSendState.FAILED) }
            }
        }
    }

    fun reset() {
        _uiState.update { it.copy(sendState = SosSendState.IDLE, message = "") }
    }
}
