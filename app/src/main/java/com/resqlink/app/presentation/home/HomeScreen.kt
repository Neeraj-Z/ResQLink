package com.resqlink.app.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.resqlink.app.presentation.components.GradientHeader
import com.resqlink.app.presentation.components.RoundedInfoCard
import com.resqlink.app.presentation.theme.Ember
import com.resqlink.app.presentation.theme.EyebrowStyle
import com.resqlink.app.presentation.theme.Moss
import com.resqlink.app.presentation.theme.Spacing

/**
 * The Home Dashboard: entry point of the app. Surfaces safety-critical
 * modules with one-tap access, fully functional offline.
 */
@Composable
fun HomeScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToFirstAid: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            GradientHeader(
                title = "ResQLink",
                subtitle = if (state.isOffline) "Offline mode -- mesh relay active" else "Stay safe. Stay informed.",
                icon = Icons.Filled.Home
            )
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.screenPadding, vertical = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Text("QUICK ACTIONS", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(Spacing.xxs))

                RoundedInfoCard(
                    title = "Emergency SOS",
                    subtitle = "Alert responders & mesh peers instantly",
                    icon = Icons.Filled.Sos,
                    accentColor = Ember,
                    onClick = onNavigateToSos
                )
                RoundedInfoCard(
                    title = "Mesh Packet Analytics",
                    subtitle = "Inspect relayed packets & connected nodes",
                    icon = Icons.Filled.Analytics,
                    accentColor = MaterialTheme.colorScheme.primary,
                    onClick = onNavigateToAnalytics
                )
                RoundedInfoCard(
                    title = "Interactive Map",
                    subtitle = "Shelters, safe zones, and live GPS location",
                    icon = Icons.Filled.Map,
                    onClick = onNavigateToMap
                )
                RoundedInfoCard(
                    title = "First Aid Guide",
                    subtitle = "Step-by-step offline emergency procedures",
                    icon = Icons.Filled.LocalHospital,
                    accentColor = Moss,
                    onClick = onNavigateToFirstAid
                )
            }
        }

        item { Spacer(Modifier.height(Spacing.lg)) }
    }
}
