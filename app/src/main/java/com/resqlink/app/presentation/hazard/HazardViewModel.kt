package com.resqlink.app.presentation.hazard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resqlink.app.domain.model.HazardZone
import com.resqlink.app.domain.repository.HazardRepository
import com.resqlink.app.domain.usecase.ObserveHazardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HazardViewModel @Inject constructor(
    observeHazards: ObserveHazardsUseCase,
    private val hazardRepository: HazardRepository
) : ViewModel() {

    val hazards: StateFlow<List<HazardZone>> = observeHazards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Auto-refresh once when the screen first opens -- this is what makes
        // hazards you add manually in the Firebase Console (see README section
        // 8, step 6) show up without needing a pull-to-refresh gesture first.
        // `lat`/`lng` are placeholders until MapScreen/location wiring supplies
        // a real position; HazardFirestoreDataSource currently does a full
        // fetch regardless (see that class's doc comment), so this still works.
        refresh(lat = 0.0, lng = 0.0)
    }

    /** Pull-to-refresh: attempts a live update; silently keeps the cache on failure. */
    fun refresh(lat: Double, lng: Double) {
        viewModelScope.launch {
            _isRefreshing.value = true
            hazardRepository.refreshHazards(lat, lng)
            _isRefreshing.value = false
        }
    }
}
