package com.resqlink.app.presentation.sos

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
 *   navigates away -- this screen only observes relay status via
 *   [MeshViewModel], it doesn't stop the service on dispose.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SosScreen(
    onBack: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    viewModel: SosViewModel = hiltViewModel(),
    meshViewModel: MeshViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    val meshState by meshViewModel.relayState.collectAsState()

    var showPeersDialog by remember { mutableStateOf(false) }

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
                actions = {
                    IconButton(onClick = onNavigateToAnalytics) {
                        Icon(Icons.Filled.Analytics, contentDescription = "Analytics")
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
                .verticalScroll(rememberScrollState())
                .padding(Spacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (state.sendState) {
                SosSendState.SENT_LOCALLY, SosSendState.SYNCED -> {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Moss,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        "Your SOS has been recorded",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "It will sync automatically once you have connectivity, and is actively being relayed to nearby devices in the mesh.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(Spacing.lg))
                    Button(
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Filled.Analytics, contentDescription = null)
                        Spacer(Modifier.width(Spacing.xs))
                        Text("View in Analytics Dashboard")
                    }

                    Spacer(Modifier.height(Spacing.sm))
                    OutlinedButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(Spacing.xs))
                        Text("Send Another SOS Alert")
                    }
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

                    Spacer(Modifier.height(Spacing.lg))
                    OutlinedTextField(
                        value = state.message,
                        onValueChange = viewModel::updateMessage,
                        label = { Text("Add a short message (optional)") },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            LocationStatusRow(
                status = state.locationStatus,
                latitude = state.latitude,
                longitude = state.longitude,
                accuracyMeters = state.locationAccuracyMeters,
                onRetry = viewModel::refreshLocation
            )

            Spacer(Modifier.height(Spacing.md))
            MeshStatusRow(
                permissionsGranted = meshPermissionsState.allPermissionsGranted,
                meshState = meshState,
                onClick = { showPeersDialog = true }
            )
        }
    }

    if (showPeersDialog) {
        ConnectedPeersDialog(
            meshState = meshState,
            onDismiss = { showPeersDialog = false }
        )
    }
}

/**
 * Dialog displaying all connected mesh peer devices when the user taps on the connected count.
 */
@Composable
private fun ConnectedPeersDialog(
    meshState: MeshRelayState,
    onDismiss: () -> Unit
) {
    val peers = (meshState as? MeshRelayState.Ready)?.peers ?: emptyList()
    val count = (meshState as? MeshRelayState.Ready)?.connectedPeerCount ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Devices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Connected Devices ($count)", style = MaterialTheme.typography.titleMedium)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Nearby devices in mesh cluster transmitting and relaying emergency traffic without internet:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (peers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.md),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Scanning for Bluetooth and Wi-Fi Direct nodes...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    peers.forEach { peer ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(Spacing.sm)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        peer.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    StatusPill(
                                        text = peer.status,
                                        color = Moss
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        if (peer.transport.contains("Wi-Fi", ignoreCase = true)) Icons.Filled.Wifi else Icons.Filled.Router,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        peer.transport,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (peer.signalOrAddress.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Endpoint: ${peer.signalOrAddress}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Status line reflecting GPS fix state.
 */
@Composable
private fun LocationStatusRow(
    status: LocationStatus,
    latitude: Double?,
    longitude: Double?,
    accuracyMeters: Float?,
    onRetry: () -> Unit = {}
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
                text = "Location unavailable \u2014 tap to retry",
                color = Amber,
                modifier = Modifier.clip(CircleShape).clickable { onRetry() }
            )
            LocationStatus.PERMISSION_DENIED -> StatusPill(
                text = "Location permission needed for accuracy",
                color = Amber
            )
        }
    }
}

/**
 * Status line explaining what the mesh relay is doing, clickable to inspect connected peers.
 */
@Composable
private fun MeshStatusRow(
    permissionsGranted: Boolean,
    meshState: MeshRelayState,
    onClick: () -> Unit
) {
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
                    color = Slate,
                    modifier = Modifier.clip(CircleShape).clickable { onClick() }
                )
                is MeshRelayState.Ready -> {
                    val count = meshState.connectedPeerCount
                    val text = if (count > 0)
                        "$count device${if (count == 1) "" else "s"} connected \u2022 Tap to view"
                    else
                        "No nearby devices yet \u2022 Tap to view"
                    StatusPill(
                        text = text,
                        color = if (count > 0) Moss else Slate,
                        modifier = Modifier.clip(CircleShape).clickable { onClick() }
                    )
                }
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
