package com.resqlink.app.presentation.settings

import androidx.lifecycle.ViewModel
import com.resqlink.app.data.local.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class SettingsUiState(
    val isDarkTheme: Boolean? = null, // null = follow system
    val language: String = "en",
    val isVolunteerMode: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: SettingsDataStore
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = dataStore.settingsFlow

    fun setDarkTheme(enabled: Boolean?) = dataStore.setDarkTheme(enabled)
    fun setLanguage(code: String) = dataStore.setLanguage(code)
    fun setVolunteerMode(enabled: Boolean) = dataStore.setVolunteerMode(enabled)
}
