package com.resqlink.app.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.resqlink.app.presentation.settings.SettingsUiState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "resqlink_settings")

/**
 * Persists user-facing preferences (theme, language, volunteer mode) via
 * Jetpack DataStore -- lightweight, offline, and available before the Room
 * database or any network call completes.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob())

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme_override_set")
        val DARK_THEME_VALUE = booleanPreferencesKey("dark_theme_value")
        val LANGUAGE = stringPreferencesKey("language")
        val VOLUNTEER_MODE = booleanPreferencesKey("volunteer_mode")
    }

    val settingsFlow: StateFlow<SettingsUiState> = context.dataStore.data
        .map { prefs ->
            SettingsUiState(
                isDarkTheme = if (prefs[Keys.DARK_THEME] == true) prefs[Keys.DARK_THEME_VALUE] else null,
                language = prefs[Keys.LANGUAGE] ?: "en",
                isVolunteerMode = prefs[Keys.VOLUNTEER_MODE] ?: false
            )
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun setDarkTheme(enabled: Boolean?) {
        scope.launch {
            context.dataStore.edit { prefs ->
                if (enabled == null) {
                    prefs[Keys.DARK_THEME] = false
                } else {
                    prefs[Keys.DARK_THEME] = true
                    prefs[Keys.DARK_THEME_VALUE] = enabled
                }
            }
        }
    }

    fun setLanguage(code: String) {
        scope.launch { context.dataStore.edit { it[Keys.LANGUAGE] = code } }
    }

    fun setVolunteerMode(enabled: Boolean) {
        scope.launch { context.dataStore.edit { it[Keys.VOLUNTEER_MODE] = enabled } }
    }
}
