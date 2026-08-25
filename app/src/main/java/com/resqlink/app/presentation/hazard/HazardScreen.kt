package com.resqlink.app.presentation.hazard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.resqlink.app.domain.model.HazardSeverity
import com.resqlink.app.presentation.components.GradientHeader
import com.resqlink.app.presentation.components.RefreshRow
import com.resqlink.app.presentation.components.RoundedInfoCard
import com.resqlink.app.presentation.theme.Amber
import com.resqlink.app.presentation.theme.Ember
import com.resqlink.app.presentation.theme.Moss
import com.resqlink.app.presentation.theme.Spacing

/**
 * Hazard Information module: cached disaster/hazard zones, color-coded by
 * severity. Refreshes automatically once on open (see HazardViewModel's
 * init block) and via [RefreshRow] -- both pull fresh data from Firestore
 * (see README section 8) into the local Room cache.
 */
@Composable
fun HazardScreen(viewModel: HazardViewModel = hiltViewModel()) {
    val hazards by viewModel.hazards.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            GradientHeader(
                eyebrow = "Module",
                title = "Hazard Information",
                subtitle = "Cached for offline access",
                icon = Icons.Filled.Warning
            )
        }
        item {
            RefreshRow(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.refresh(lat = 0.0, lng = 0.0) },
                label = "Synced from Firestore"
            )
        }
        if (hazards.isEmpty()) {
            item {
                Text(
                    "No active hazards in your cached area.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xl)
                )
            }
        }
        items(hazards, key = { it.id }) { hazard ->
            val color = when (hazard.severity) {
                HazardSeverity.EXTREME, HazardSeverity.HIGH -> Ember
                HazardSeverity.MODERATE -> Amber
                HazardSeverity.LOW -> Moss
            }
            RoundedInfoCard(
                title = hazard.title,
                subtitle = "${hazard.type.name.lowercase().replaceFirstChar { it.uppercase() }} · ${hazard.severity.name.lowercase().replaceFirstChar { it.uppercase() }} · ${hazard.description}",
                icon = Icons.Filled.Warning,
                accentColor = color,
                modifier = Modifier.padding(horizontal = Spacing.screenPadding, vertical = Spacing.xxs)
            )
        }
        item { Spacer(Modifier.height(Spacing.lg)) }
    }
}
