package com.resqlink.app.presentation.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.resqlink.app.domain.model.MeshRelayedPacket
import com.resqlink.app.domain.model.RelayDirection
import com.resqlink.app.domain.model.SosSeverity
import com.resqlink.app.presentation.components.GradientHeader
import com.resqlink.app.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Void)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            GradientHeader(
                eyebrow = "TELEMETRY & MESH RELAY",
                title = "Analytics",
                subtitle = "Real-time messages relayed through & by this device",
                icon = Icons.Filled.Analytics
            )

            // Feedback Banner
            state.actionFeedback?.let { feedback ->
                Spacer(Modifier.height(Spacing.xs))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.screenPadding)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Signal.copy(alpha = 0.12f))
                        .border(1.dp, Signal, RoundedCornerShape(8.dp))
                        .padding(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Signal,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = feedback,
                        color = Signal,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss",
                        tint = Slate,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { viewModel.dismissFeedback() }
                    )
                }
            }

            Column(
                modifier = Modifier.padding(Spacing.screenPadding)
            ) {
                // Top Simulation Testing Bar
                SimulationBar(
                    deviceName = state.thisDeviceName,
                    hasPackets = state.packets.isNotEmpty(),
                    onSimulateThrough = { viewModel.simulateRelay(RelayDirection.THROUGH_THIS_DEVICE) },
                    onSimulateBy = { viewModel.simulateRelay(RelayDirection.BY_THIS_DEVICE) },
                    onClear = { viewModel.clearRelayedPackets() }
                )

                Spacer(Modifier.height(Spacing.sm))

                // KPI Stat Cards: Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    StatCard(
                        label = "TOTAL RELAYED MESSAGES",
                        sub = "Traversed this node",
                        value = state.totalAlerts.toString(),
                        icon = Icons.Filled.DeviceHub,
                        accentColor = Signal,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "ACTIVE MESH PEERS",
                        sub = "Direct radio range",
                        value = state.connectedPeerCount.toString(),
                        icon = Icons.Filled.SignalCellularAlt,
                        accentColor = Signal,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(Spacing.sm))

                // KPI Stat Cards: Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    StatCard(
                        label = "RELAYED THROUGH DEVICE",
                        sub = "Transit hops forwarded",
                        value = state.relayedThroughCount.toString(),
                        icon = Icons.Filled.SwapHoriz,
                        accentColor = Signal,
                        isActive = state.activeFilter == "THROUGH",
                        onClick = {
                            viewModel.setFilter(if (state.activeFilter == "THROUGH") "ALL" else "THROUGH")
                        },
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "RELAYED BY THIS DEVICE",
                        sub = "Broadcast from node",
                        value = state.relayedByCount.toString(),
                        icon = Icons.Filled.Radio,
                        accentColor = Amber,
                        isActive = state.activeFilter == "BY",
                        onClick = {
                            viewModel.setFilter(if (state.activeFilter == "BY") "ALL" else "BY")
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(Spacing.lg))

                // Main Section: Relayed Messages Log
                Text(
                    text = "DEVICE RELAY TELEMETRY LOG",
                    style = EyebrowStyle,
                    color = Signal
                )
                Text(
                    text = "Click any message to inspect full content, identity & routing data",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                    modifier = Modifier.padding(top = 2.dp, bottom = Spacing.sm)
                )

                // Filter Tabs & Search Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, HairlineDark)
                ) {
                    Column(modifier = Modifier.padding(Spacing.sm)) {
                        // Segmented Filter Tabs
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterTabButton(
                                title = "All (${state.packets.size})",
                                isSelected = state.activeFilter == "ALL",
                                dotColor = null,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setFilter("ALL") }
                            )
                            FilterTabButton(
                                title = "Through (${state.relayedThroughCount})",
                                isSelected = state.activeFilter == "THROUGH",
                                dotColor = Signal,
                                modifier = Modifier.weight(1.1f),
                                onClick = { viewModel.setFilter("THROUGH") }
                            )
                            FilterTabButton(
                                title = "By (${state.relayedByCount})",
                                isSelected = state.activeFilter == "BY",
                                dotColor = Amber,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.setFilter("BY") }
                            )
                        }

                        Spacer(Modifier.height(Spacing.xs))

                        // Search Field
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = {
                                Text(
                                    "Search by sender, device, or message...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Slate
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Search, contentDescription = null, tint = Slate, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = Slate, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Void,
                                unfocusedContainerColor = Void,
                                focusedBorderColor = Signal,
                                unfocusedBorderColor = HairlineDark,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.sm))

                // Relayed Message Cards or Empty State
                if (state.filteredPackets.isEmpty()) {
                    EmptyRelayedState(searchQuery = state.searchQuery)
                } else {
                    state.filteredPackets.forEach { packet ->
                        RelayedMessageCard(
                            packet = packet,
                            thisDeviceName = state.thisDeviceName,
                            onClick = { viewModel.selectPacket(packet) }
                        )
                        Spacer(Modifier.height(Spacing.sm))
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                // Connected Mesh Peers Breakdown Card
                ConnectedPeersCard(peerCount = state.connectedPeerCount)

                Spacer(Modifier.height(Spacing.md))

                // Hazard Severity Breakdown Card
                HazardSeverityCard(
                    extreme = state.extremeHazards,
                    moderate = state.moderateHazards,
                    low = state.lowHazards
                )

                Spacer(Modifier.height(Spacing.xxl))
            }
        }

        // Full Message & Telemetry Inspector Dialog
        state.selectedPacket?.let { packet ->
            PacketInspectorDialog(
                packet = packet,
                thisDeviceName = state.thisDeviceName,
                onDismiss = { viewModel.selectPacket(null) }
            )
        }
    }
}

// ---------------- SUB-COMPONENTS ----------------

@Composable
private fun SimulationBar(
    deviceName: String,
    hasPackets: Boolean,
    onSimulateThrough: () -> Unit,
    onSimulateBy: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = InkRaised),
        border = BorderStroke(1.dp, HairlineDark)
    ) {
        Column(modifier = Modifier.padding(Spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Signal)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "NODE: ${deviceName.uppercase()}",
                        style = EyebrowStyle,
                        color = Signal,
                        fontSize = 10.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // + Relay Through Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSimulateThrough() },
                        color = Signal,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = Color(0xFF0B0F17), modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("+ Through", color = Color(0xFF0B0F17), style = EyebrowStyle, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // + Relay By Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSimulateBy() },
                        color = Amber.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Amber),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Radio, contentDescription = null, tint = Amber, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("+ By", color = Amber, style = EyebrowStyle, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (hasPackets) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { onClear() },
                            color = Void,
                            border = BorderStroke(1.dp, HairlineDark),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Clear", tint = Slate, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(2.dp))
                                Text("Clear", color = Slate, style = EyebrowStyle, fontSize = 9.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    sub: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .let { if (onClick != null) it.clickable { onClick() } else it },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) accentColor.copy(alpha = 0.08f) else DarkSurface
        ),
        border = BorderStroke(1.dp, if (isActive) accentColor else HairlineDark)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                    color = accentColor,
                    fontSize = 24.sp
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                }
            }
            Spacer(Modifier.height(Spacing.xxs))
            Text(text = label, style = EyebrowStyle, fontSize = 9.sp, color = DarkOnSurface)
            Text(text = sub, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = Slate)
        }
    }
}

@Composable
private fun FilterTabButton(
    title: String,
    isSelected: Boolean,
    dotColor: Color?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = if (isSelected) SignalDeep else InkRaised,
        border = BorderStroke(1.dp, if (isSelected) Signal else Color.Transparent),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = title,
                color = if (isSelected) Color.White else Slate,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyRelayedState(searchQuery: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, HairlineDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Radio, contentDescription = null, tint = Slate, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = if (searchQuery.isNotEmpty()) "No Matching Messages" else "No Relayed Messages Yet",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) {
                    "No messages matching \"$searchQuery\" were found."
                } else {
                    "Messages will appear here in real time as they are broadcast by this device or relayed through nearby mesh peers."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Slate,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun RelayedMessageCard(
    packet: MeshRelayedPacket,
    thisDeviceName: String,
    onClick: () -> Unit
) {
    val isThrough = packet.direction == RelayDirection.THROUGH_THIS_DEVICE
    val accentColor = if (isThrough) Signal else Amber

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, HairlineDark)
    ) {
        // Colored side-indicator line via Row
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor)
            )

            Column(modifier = Modifier.padding(Spacing.md)) {
                // Header: Direction Badge + Severity + Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Direction pill
                    Surface(
                        color = accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isThrough) Icons.Filled.SwapHoriz else Icons.Filled.Radio,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = if (isThrough) "RELAYED THROUGH THIS DEVICE" else "RELAYED BY THIS DEVICE",
                                color = accentColor,
                                style = EyebrowStyle,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Severity + Time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val sevColor = when (packet.severity) {
                            SosSeverity.CRITICAL -> Ember
                            SosSeverity.HIGH -> Amber
                            else -> Signal
                        }
                        Surface(
                            color = sevColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, sevColor),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = packet.severity.name,
                                color = sevColor,
                                style = EyebrowStyle,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }

                        Text(
                            text = formatTimeAgo(packet.createdAtEpochMillis),
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 10.sp,
                            color = Slate
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.xs))

                // Sender & Target Identity Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = InkRaised)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = packet.senderName,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = packet.senderDeviceName,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = Slate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Slate,
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(14.dp)
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(text = "DESTINATION", style = EyebrowStyle, fontSize = 7.sp, color = Slate)
                            Text(
                                text = packet.targetDeviceName,
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 11.sp,
                                color = Signal,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xs))

                // Multi-Device Route Hops
                Text(
                    text = "RELAY ROUTE HOPS (${packet.relayPath.size} NODES):",
                    style = EyebrowStyle,
                    fontSize = 8.sp,
                    color = Slate
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    packet.relayPath.forEachIndexed { idx, node ->
                        val isThisNode = node.contains("This Device", ignoreCase = true) || node.contains(thisDeviceName, ignoreCase = true)

                        Surface(
                            color = if (isThisNode) SignalDeep else Void,
                            border = BorderStroke(1.dp, if (isThisNode) Signal else HairlineDark),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isThisNode) {
                                    Icon(Icons.Filled.Shield, contentDescription = null, tint = Signal, modifier = Modifier.size(9.dp))
                                    Spacer(Modifier.width(2.dp))
                                }
                                Text(
                                    text = if (isThisNode) "★ $node" else node,
                                    color = if (isThisNode) Color.White else Slate,
                                    fontSize = 9.sp,
                                    fontWeight = if (isThisNode) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        if (idx < packet.relayPath.size - 1) {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = Slate,
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(11.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xs))

                // Message Excerpt Highlight Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Void),
                    border = BorderStroke(1.dp, HairlineDark)
                ) {
                    Column(modifier = Modifier.padding(Spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Chat, contentDescription = null, tint = Signal, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("RELAYED MESSAGE PAYLOAD", style = EyebrowStyle, fontSize = 8.sp, color = Signal)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("TAP TO EXPAND", style = EyebrowStyle, fontSize = 7.sp, color = Signal, fontWeight = FontWeight.Bold)
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Signal, modifier = Modifier.size(11.dp))
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = "\"${packet.customMessage}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkOnSurface,
                            fontStyle = FontStyle.Italic,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.xs))

                // Bottom Strip: Telemetry Tags
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            BorderStroke(1.dp, HairlineDark),
                            shape = RoundedCornerShape(0.dp)
                        )
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Slate, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = "%.3f°, %.3f°".format(packet.latitude, packet.longitude),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                fontSize = 9.sp,
                                color = Slate
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Wifi, contentDescription = null, tint = Slate, modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(2.dp))
                            Text(text = packet.transport, fontSize = 9.sp, color = Slate)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Inspect Data", fontSize = 10.sp, color = Signal, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.Filled.OpenInNew, contentDescription = null, tint = Signal, modifier = Modifier.size(11.dp))
                    }
                }
            }
        }
    }
}

// ---------------- INSPECTION DIALOG ----------------

@Composable
private fun PacketInspectorDialog(
    packet: MeshRelayedPacket,
    thisDeviceName: String,
    onDismiss: () -> Unit
) {
    val isThrough = packet.direction == RelayDirection.THROUGH_THIS_DEVICE
    val accentColor = if (isThrough) Signal else Amber

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Void.copy(alpha = 0.9f))
                .padding(Spacing.sm),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = InkRaised),
                border = BorderStroke(1.dp, HairlineDark)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Spacing.md)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("RELAYED PACKET TELEMETRY", style = EyebrowStyle, color = Signal, fontSize = 10.sp)
                            Text("Message & Hop Inspector", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Void)
                                .border(1.dp, HairlineDark, CircleShape)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(Modifier.height(Spacing.sm))

                    // Direction Banner
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = accentColor.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, accentColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isThrough) Icons.Filled.SwapHoriz else Icons.Filled.Radio,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = if (isThrough) "RELAYED THROUGH THIS DEVICE" else "RELAYED BY THIS DEVICE",
                                    color = accentColor,
                                    style = EyebrowStyle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isThrough) {
                                        "Received from mesh peer and forwarded across cluster via BLE."
                                    } else {
                                        "Originated locally by $thisDeviceName and broadcast across mesh."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = DarkOnSurface
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(Spacing.sm))

                    // Full Message Card
                    SectionCard(title = "FULL RELAYED MESSAGE CONTENT", icon = Icons.Filled.Chat) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Void)
                                .border(1.dp, HairlineDark, RoundedCornerShape(8.dp))
                                .padding(Spacing.md)
                        ) {
                            Text(
                                text = "\"${packet.customMessage}\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(Modifier.height(Spacing.xs))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "SEVERITY: ${packet.severity.name}",
                                style = EyebrowStyle,
                                fontSize = 9.sp,
                                color = when (packet.severity) {
                                    SosSeverity.CRITICAL -> Ember
                                    SosSeverity.HIGH -> Amber
                                    else -> Signal
                                }
                            )
                            Text(
                                text = "STATUS: ${packet.deliveryStatus}",
                                style = EyebrowStyle,
                                fontSize = 9.sp,
                                color = Moss
                            )
                            Text(
                                text = formatExactTime(packet.createdAtEpochMillis),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                fontSize = 9.sp,
                                color = Slate
                            )
                        }
                    }

                    Spacer(Modifier.height(Spacing.sm))

                    // Multi-Hop Route Timeline
                    SectionCard(title = "RELAY ROUTE & MESH NODES CHAIN", icon = Icons.Filled.DeviceHub) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            packet.relayPath.forEachIndexed { idx, node ->
                                val isFirst = idx == 0
                                val isLast = idx == packet.relayPath.size - 1
                                val isThisDevice = node.contains("This Device", ignoreCase = true) || node.contains(thisDeviceName, ignoreCase = true)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(24.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isThisDevice) Signal else if (isFirst) Amber else Slate
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isFirst) Icons.Filled.Radio else if (isThisDevice) Icons.Filled.Shield else Icons.Filled.LocationOn,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                        if (!isLast) {
                                            Box(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .height(28.dp)
                                                    .background(HairlineDark)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(8.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = node,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (isThisDevice) Signal else Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = if (isFirst) "ORIGIN" else if (isThisDevice) "THIS NODE (TRANSIT)" else if (isLast) "DESTINATION" else "HOP $idx",
                                                style = EyebrowStyle,
                                                fontSize = 7.sp,
                                                color = Slate
                                            )
                                        }
                                        Text(
                                            text = if (isFirst) {
                                                "Created and broadcasted by ${packet.senderName}"
                                            } else if (isThisDevice) {
                                                "Received via mesh, payload verified, and routed onward"
                                            } else {
                                                "Destination responder or downstream cluster peer"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 10.sp,
                                            color = Slate
                                        )
                                        Spacer(Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(Spacing.sm))

                    // Identification Grid
                    SectionCard(title = "DEVICE & SENDER IDENTIFICATION", icon = Icons.Filled.Badge) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                InfoGridItem("SENDER NAME", packet.senderName, Modifier.weight(1f))
                                Spacer(Modifier.width(6.dp))
                                InfoGridItem("SENDER PHONE", packet.senderPhone ?: "N/A", Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                InfoGridItem("SENDER DEVICE", packet.senderDeviceName, Modifier.weight(1f))
                                Spacer(Modifier.width(6.dp))
                                InfoGridItem("TARGET DESTINATION", packet.targetDeviceName, Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(Modifier.height(Spacing.sm))

                    // Location & Triage Data
                    SectionCard(title = "RELAYED TRIAGE & LOCATION DATA", icon = Icons.Filled.LocalHospital) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                InfoGridItem("GPS COORDINATES", "%.5f°, %.5f°".format(packet.latitude, packet.longitude), Modifier.weight(1f), isMono = true)
                                Spacer(Modifier.width(6.dp))
                                InfoGridItem("LOCATION ACCURACY", "±%.0fm".format(packet.accuracyMeters), Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                InfoGridItem("BLOOD GROUP", packet.bloodGroup ?: "Not Specified", Modifier.weight(1f), valueColor = Ember)
                                Spacer(Modifier.width(6.dp))
                                InfoGridItem("EMERGENCY CONTACT", packet.emergencyContact ?: "None", Modifier.weight(1f))
                            }
                            InfoGridItem("MEDICAL / RESCUE NOTES", packet.medicalNotes ?: "None", Modifier.fillMaxWidth())
                        }
                    }

                    Spacer(Modifier.height(Spacing.sm))

                    // Radio Diagnostics
                    SectionCard(title = "RADIO & TRANSPORT DIAGNOSTICS", icon = Icons.Filled.WifiTethering) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            InfoGridItem("TRANSPORT", packet.transport, Modifier.weight(1f))
                            Spacer(Modifier.width(6.dp))
                            InfoGridItem("SIGNAL RSSI", packet.rssi, Modifier.weight(1f), valueColor = Signal)
                        }
                    }

                    Spacer(Modifier.height(Spacing.md))

                    // Modal Close Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SignalDeep),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Close Inspector", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(Spacing.md))
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, HairlineDark)
    ) {
        Column(modifier = Modifier.padding(Spacing.sm)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Signal, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(text = title, style = EyebrowStyle, color = Signal, fontSize = 9.sp)
            }
            content()
        }
    }
}

@Composable
private fun InfoGridItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isMono: Boolean = false,
    valueColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Void)
            .border(1.dp, HairlineDark, RoundedCornerShape(6.dp))
            .padding(Spacing.xs)
    ) {
        Column {
            Text(text = label, style = EyebrowStyle, fontSize = 7.sp, color = Slate)
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = if (isMono) MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace) else MaterialTheme.typography.bodySmall,
                color = valueColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ConnectedPeersCard(peerCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, HairlineDark)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CONNECTED MESH PEERS", style = EyebrowStyle, color = Slate)
                Surface(
                    color = SignalDeep,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "$peerCount ACTIVE",
                        color = Signal,
                        style = EyebrowStyle,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(Modifier.height(Spacing.xs))

            if (peerCount == 0) {
                Text(
                    text = "No active peer nodes broadcasting in local radio range. Connecting to nearby ResQLink devices automatically when in proximity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = "Cluster active: $peerCount node(s) connected and ready to relay emergency SOS packets over Bluetooth & Wi-Fi Direct.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Signal,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun HazardSeverityCard(extreme: Int, moderate: Int, low: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, HairlineDark)
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text("HAZARD SEVERITY BREAKDOWN", style = EyebrowStyle, color = Slate)
            Spacer(Modifier.height(Spacing.xs))

            HazardRow(label = "Extreme / High Hazards", count = extreme, dotColor = Ember)
            Spacer(Modifier.height(Spacing.xxs))
            HazardRow(label = "Moderate Severity", count = moderate, dotColor = Amber)
            Spacer(Modifier.height(Spacing.xxs))
            HazardRow(label = "Low / Advisory", count = low, dotColor = Moss)
        }
    }
}

@Composable
private fun HazardRow(label: String, count: Int, dotColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = DarkOnSurface, fontSize = 13.sp)
        }
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = dotColor,
            fontWeight = FontWeight.Bold
        )
    }
}

// Helper time formatters
private fun formatTimeAgo(epoch: Long): String {
    val diff = (System.currentTimeMillis() - epoch).coerceAtLeast(0)
    val mins = diff / 60000
    return when {
        mins < 1 -> "Just now"
        mins < 60 -> "${mins}m ago"
        mins < 1440 -> "${mins / 60}h ago"
        else -> "${mins / 1440}d ago"
    }
}

private fun formatExactTime(epoch: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(epoch))
}
