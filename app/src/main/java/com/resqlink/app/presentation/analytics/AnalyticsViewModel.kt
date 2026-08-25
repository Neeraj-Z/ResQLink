package com.resqlink.app.presentation.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resqlink.app.domain.repository.SosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class AnalyticsUiState(
    val totalAlerts: Int = 0,
    val pendingSync: Int = 0,
    val meshRelayed: Int = 0
)

/**
 * Analytics Dashboard: local, on-device statistics derived from the SOS log
 * and (in the future) volunteer/resource data. This intentionally computes
 * from Room-backed data so analytics remain available offline; a
 * server-aggregated "regional" view is a natural v2 extension via a new
 * repository method, without touching this ViewModel's shape.
 */
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    sosRepository: SosRepository
) : ViewModel() {

    val uiState: StateFlow<AnalyticsUiState> = sosRepository.observeAlerts()
        .map { alerts ->
            AnalyticsUiState(
                totalAlerts = alerts.size,
                pendingSync = alerts.count { it.status.name == "PENDING_SYNC" },
                meshRelayed = alerts.count { it.isMeshRelayed }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())
}
