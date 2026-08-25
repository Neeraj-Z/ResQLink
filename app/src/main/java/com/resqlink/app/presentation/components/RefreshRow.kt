package com.resqlink.app.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resqlink.app.presentation.theme.Spacing

/**
 * A quiet "Synced from Firestore" row with a manual refresh action, used
 * below the header on screens whose content is cached-but-refreshable
 * (Hazards, First Aid). Kept separate from the header itself so the header's
 * icon can stay a pure module-identity glyph (Warning, LocalHospital, ...)
 * rather than overloading it with an unrelated refresh action -- consistency
 * of what that icon slot means matters more than saving one row of UI.
 */
@Composable
fun RefreshRow(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Synced from Firestore"
) {
    Row(
        modifier = modifier.padding(horizontal = Spacing.screenPadding, vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(Spacing.xs))
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
        } else {
            IconButton(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        }
    }
}
