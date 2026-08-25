package com.resqlink.app.presentation.sos

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.resqlink.app.domain.model.MeshRelayState
import com.resqlink.app.presentation.components.StatusPill
import com.resqlink.app.presentation.theme.Amber
import com.resqlink.app.presentation.theme.Ember
import com.resqlink.app.presentation.theme.EyebrowStyle
import com.resqlink.app.presentation.theme.Moss
import com.resqlink.app.presentation.theme.Slate
import com.resqlink.app.presentation.theme.Spacing
import com.resqlink.app.service.SosForegroundService

/**
 * The SOS screen: one large, unmissable button. Deliberately minimal --
 * emergency UI should not require reading or decision-making under stress.
 *
 * Owns two independent lifecycles, each gated by its own permission request
 * so declining one never blocks the other:
 * - Location: requested and fetched as soon as the screen opens, so a fix is
 *   usually already warm by the time the person presses SOS (see SosViewModel).
 * - Mesh relay: once the required Bluetooth/Wi-Fi permissions are granted,
 *   this screen starts [SosForegroundService], which owns the actual
 *   advertising/discovery lifecycle and keeps running after the person
 *   navigates away -- this screen only *observes* relay status via
 *   [MeshViewModel], it doesn't stop the service on dispose.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SosScreen(
    onBack: () -> Unit,
    viewModel: SosViewModel = hiltViewModel(),
    meshViewModel: MeshViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val meshState by meshViewModel.relayState.collectAsState()

    // Requested independently from meshPermissionsState below, even though
    // ACCESS_FINE_LOCATION also appears in MeshPermissions.required -- Android
    // permission grants are idempotent, so this overlap is harmless, and it
    // keeps "can I get a location fix" fully decoupled from "can mesh relay run".
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    val meshPermissionsState = rememberMultiplePermissionsState(MeshPermissions.required)

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
        if (!meshPermissionsState.allPermissionsGranted) {
            meshPermissionsState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) viewModel.refreshLocation()
    }

    LaunchedEffect(meshPermissionsState.allPermissionsGranted) {
        if (meshPermissionsState.allPermissionsGranted) SosForegroundService.start(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency SOS") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (state.sendState) {
                SosSendState.SENT_LOCALLY, SosSendState.SYNCED -> {
                    Text(
                        "Your SOS has been recorded",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "It will sync automatically once you have connectivity, and is being relayed to nearby devices in the meantime.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                else -> {
                    PulsingSosButton(
                        isSending = state.sendState == SosSendState.SENDING,
                        onClick = { viewModel.sendSos() }
                    )
                    Spacer(Modifier.height(Spacing.lg))
                    Text(
                        "Press in a real emergency.\nYour location is shared automatically.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            OutlinedTextField(
                value = state.message,
                onValueChange = viewModel::updateMessage,
                label = { Text("Add a short message (optional)") },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Spacing.lg))
            LocationStatusRow(
                status = state.locationStatus,
                latitude = state.latitude,
                longitude = state.longitude,
                accuracyMeters = state.locationAccuracyMeters
            )
            Spacer(Modifier.height(Spacing.md))
            MeshStatusRow(
                permissionsGranted = meshPermissionsState.allPermissionsGranted,
                meshState = meshState
            )
        }
    }
}

/**
 * Quiet status line reflecting GPS fix state. Purely informational: the SOS
 * button above is never disabled by this, since a stale or missing location
 * must never stop someone from sending an alert (see SosViewModel.sendSos).
 * Shows the actual coordinates once available -- not just "locked" -- since
 * the numbers themselves are useful to confirm at a glance before an alert
 * goes out.
 */
@Composable
private fun LocationStatusRow(
    status: LocationStatus,
    latitude: Double?,
    longitude: Double?,
    accuracyMeters: Float?
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("LOCATION", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))

        when (status) {
            LocationStatus.IDLE, LocationStatus.LOCATING -> StatusPill(
                text = "Getting your location\u2026",
                color = Slate
            )
            LocationStatus.AVAILABLE -> StatusPill(
                text = if (latitude != null && longitude != null) {
                    val coords = "%.5f, %.5f".format(latitude, longitude)
                    if (accuracyMeters != null) "$coords (\u00b1${accuracyMeters.toInt()}m)" else coords
                } else "Location locked",
                color = Moss
            )
            LocationStatus.UNAVAILABLE -> StatusPill(
                text = "Location unavailable \u2014 SOS will still send",
                color = Amber
            )
            LocationStatus.PERMISSION_DENIED -> StatusPill(
                text = "Location permission needed for accuracy",
                color = Amber
            )
        }
    }
}

/**
 * Small, quiet status line explaining what the mesh relay is doing right now.
 * Kept out of the way of the primary SOS action -- this is supporting
 * information, not something the user needs to act on.
 */
@Composable
private fun MeshStatusRow(permissionsGranted: Boolean, meshState: MeshRelayState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("MESH RELAY", style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.xs))

        when {
            !permissionsGranted -> StatusPill(
                text = "Nearby permissions needed",
                color = Amber
            )
            else -> when (meshState) {
                is MeshRelayState.Idle, is MeshRelayState.Starting -> StatusPill(
                    text = "Searching for nearby devices\u2026",
                    color = Slate
                )
                is MeshRelayState.Ready -> StatusPill(
                    text = if (meshState.connectedPeerCount > 0)
                        "${meshState.connectedPeerCount} nearby device${if (meshState.connectedPeerCount == 1) "" else "s"} connected"
                    else
                        "No nearby devices yet",
                    color = if (meshState.connectedPeerCount > 0) Moss else Slate
                )
                is MeshRelayState.Error -> StatusPill(
                    text = "Mesh unavailable",
                    color = Ember
                )
            }
        }
    }
}

@Composable
private fun PulsingSosButton(isSending: Boolean, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "sos_scale"
    )
    // Radar-style expanding ring behind the button -- a cheap, high-impact
    // "active transmitter" signal that reads as genuine future-tech rather
    // than a plain static circle.
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "sos_ring_scale"
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "sos_ring_alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        if (!isSending) {
            Box(
                modifier = Modifier
                    .size(196.dp)
                    .scale(ringScale)
                    .clip(CircleShape)
                    .border(2.dp, Ember.copy(alpha = ringAlpha), CircleShape)
            )
        }
        Box(
            modifier = Modifier
                .size(196.dp)
                .scale(if (isSending) 1f else scale)
                .clip(CircleShape)
                .background(Ember),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick, enabled = !isSending) {
                if (isSending) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Icon(Icons.Filled.Sos, contentDescription = "Send SOS", tint = Color.White, modifier = Modifier.size(60.dp))
                }
            }
        }
    }
}
