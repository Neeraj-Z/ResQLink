package com.resqlink.app.presentation.firstaid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resqlink.app.domain.model.FirstAidGuide
import com.resqlink.app.domain.repository.FirstAidRepository
import com.resqlink.app.domain.usecase.GetFirstAidGuidesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FirstAidViewModel @Inject constructor(
    getGuides: GetFirstAidGuidesUseCase,
    private val firstAidRepository: FirstAidRepository
) : ViewModel() {

    private val query = MutableStateFlow("")

    val guides: StateFlow<List<FirstAidGuide>> = query
        .flatMapLatest { getGuides(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Auto-refresh once when the screen first opens -- this is what makes
        // guides you add manually in the Firebase Console (see README section
        // 8, step 6) show up without requiring a manual refresh tap first.
        refresh()
    }

    fun onQueryChanged(text: String) {
        query.value = text
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            firstAidRepository.refreshFromServer()
            _isRefreshing.value = false
        }
    }
}
