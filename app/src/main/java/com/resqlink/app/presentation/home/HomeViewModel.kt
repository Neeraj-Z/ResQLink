package com.resqlink.app.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resqlink.app.domain.repository.UserRepository
import com.resqlink.app.domain.usecase.ObserveHazardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Drives the Home Dashboard. Combines cached hazards (Room, always available)
 * with the current user profile so the dashboard renders instantly from local
 * data even before/without any network call.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeHazards: ObserveHazardsUseCase,
    userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        combine(
            observeHazards(),
            userRepository.observeCurrentUser()
        ) { hazards, user ->
            HomeUiState(
                isLoading = false,
                userName = user?.name ?: "Guest",
                nearbyHazards = hazards.take(3)
            )
        }.onEach { _uiState.value = it }
            .launchIn(viewModelScope)
    }
}
