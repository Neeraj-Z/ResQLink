package com.resqlink.app.presentation.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.resqlink.app.presentation.components.GradientHeader
import com.resqlink.app.presentation.theme.EyebrowStyle
import com.resqlink.app.presentation.theme.Signal
import com.resqlink.app.presentation.theme.Spacing

/** Analytics Dashboard module: at-a-glance operational stats. Chart wiring (Vico) attaches here in v2. */
@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader(
            eyebrow = "Module",
            title = "Analytics",
            subtitle = "On-device safety statistics",
            icon = Icons.Filled.Analytics
        )

        Column(modifier = Modifier.padding(Spacing.screenPadding)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatCard("Total Alerts", state.totalAlerts.toString(), Modifier.weight(1f))
                StatCard("Pending Sync", state.pendingSync.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                StatCard("Mesh Relayed", state.meshRelayed.toString(), Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = Signal
            )
            Spacer(Modifier.height(Spacing.xxs))
            Text(label.uppercase(), style = EyebrowStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
