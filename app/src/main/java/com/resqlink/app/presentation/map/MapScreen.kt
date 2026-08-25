package com.resqlink.app.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.resqlink.app.domain.model.HazardSeverity
import com.resqlink.app.domain.model.HazardZone
import com.resqlink.app.presentation.theme.Amber
import com.resqlink.app.presentation.theme.Ember
import com.resqlink.app.presentation.theme.EyebrowStyle
import com.resqlink.app.presentation.theme.Moss
import com.resqlink.app.presentation.theme.Spacing

/**
 * Interactive Map module (Google Maps SDK via maps-compose).
 *
 * Renders two layers of offline-cached data on top of the base map, both
 * sourced from Room via [MapViewModel] -- exactly the same repositories
 * every other screen uses, so hazard/SOS pins still render with no
 * connectivity, only the map *tiles* themselves require a network fetch:
 * - Hazard zones as a coloured circle (radius-accurate) + marker, colour
 *   coded by severity to match HazardScreen's palette.
 * - Active (non-resolved) SOS alerts as ember markers.
 *
 * TODO (future-ready): swap the tile provider to a bundled offline tile set
 * (e.g. MapLibre + MBTiles) when no connectivity is present, gated behind an
 * `isOffline` flag from ConnectivityObserver -- the composable boundary here
 * is intentionally kept thin so that swap doesn't ripple into other screens.
 */
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    val defaultLocation = LatLng(28.6139, 77.2090) // Fallback: New Delhi
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 11f)
    }

    var hasCenteredOnUser by remember { mutableStateOf(false) }
    LaunchedEffect(state.userLatitude, state.userLongitude) {
        val lat = state.userLatitude
        val lng = state.userLongitude
        if (lat != null && lng != null && !hasCenteredOnUser) {
            cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 13f)
            hasCenteredOnUser = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            state.hazards.forEach { hazard ->
                val color = hazardColor(hazard.severity)
                val center = LatLng(hazard.latitude, hazard.longitude)

                Circle(
                    center = center,
                    radius = hazard.radiusMeters,
                    strokeColor = color,
                    strokeWidth = 3f,
                    fillColor = color.copy(alpha = 0.18f)
                )
                MarkerInfoWindow(
                    state = rememberMarkerState(position = center),
                    title = hazard.title,
                    snippet = "${hazard.severity.name} \u00b7 ${hazard.type.name}",
                    icon = BitmapDescriptorFactory.defaultMarker(hazardMarkerHue(hazard.severity))
                )
            }

            state.activeSosAlerts.forEach { alert ->
                Marker(
                    state = rememberMarkerState(position = LatLng(alert.latitude, alert.longitude)),
                    title = "SOS Alert",
                    snippet = alert.message.ifBlank { "Emergency assistance needed" },
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }

            val userLat = state.userLatitude
            val userLng = state.userLongitude
            if (userLat != null && userLng != null) {
                Marker(
                    state = rememberMarkerState(position = LatLng(userLat, userLng)),
                    title = "Your location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(Spacing.md)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
        ) {
            Text("LIVE MAP", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        MapLegend(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(Spacing.md)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SmallFloatingActionButton(onClick = { viewModel.refresh() }) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh hazards")
                }
            }
            Spacer(Modifier.height(Spacing.xs))
            FloatingActionButton(onClick = { viewModel.locateUser() }) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Find my location")
            }
        }
    }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
            .padding(Spacing.sm)
    ) {
        Text("SEVERITY", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xxs))
        LegendRow("Extreme / High", Ember)
        LegendRow("Moderate", Amber)
        LegendRow("Low", Moss)
    }
}

@Composable
private fun LegendRow(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

private fun hazardColor(severity: HazardSeverity): Color = when (severity) {
    HazardSeverity.EXTREME, HazardSeverity.HIGH -> Ember
    HazardSeverity.MODERATE -> Amber
    HazardSeverity.LOW -> Moss
}

/** Maps our severity enum to the closest stock Google Maps marker hue (Google Maps markers only ship a fixed hue palette, not arbitrary RGB). */
private fun hazardMarkerHue(severity: HazardSeverity): Float = when (severity) {
    HazardSeverity.EXTREME, HazardSeverity.HIGH -> BitmapDescriptorFactory.HUE_RED
    HazardSeverity.MODERATE -> BitmapDescriptorFactory.HUE_ORANGE
    HazardSeverity.LOW -> BitmapDescriptorFactory.HUE_GREEN
}
