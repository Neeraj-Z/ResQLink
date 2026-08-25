package com.resqlink.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.resqlink.app.presentation.components.GradientHeader
import com.resqlink.app.presentation.components.RoundedInfoCard
import com.resqlink.app.presentation.theme.Spacing

/** Settings: theme, language, and role/mode toggles (Volunteer/Rescue-team mode is future-ready). */
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        GradientHeader(eyebrow = "Module", title = "Settings", icon = Icons.Filled.Settings)

        Column(
            modifier = Modifier.padding(horizontal = Spacing.screenPadding, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            RoundedInfoCard(
                title = "Dark theme",
                subtitle = "Follow system, or force light/dark",
                trailing = {
                    Switch(checked = state.isDarkTheme == true, onCheckedChange = { viewModel.setDarkTheme(it) })
                }
            )
            RoundedInfoCard(title = "Language", subtitle = "Current: ${state.language.uppercase()}")
            RoundedInfoCard(
                title = "Volunteer / rescue mode",
                subtitle = "Unlocks responder tools",
                trailing = {
                    Switch(checked = state.isVolunteerMode, onCheckedChange = { viewModel.setVolunteerMode(it) })
                }
            )
        }
    }
}
