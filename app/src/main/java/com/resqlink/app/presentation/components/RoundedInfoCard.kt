package com.resqlink.app.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resqlink.app.presentation.theme.IconContainer
import com.resqlink.app.presentation.theme.Spacing

/**
 * The app's standard row card: flat surface, hairline border, no drop shadow.
 * Elevation-as-depth reads as generic Material; a quiet 1dp border reads as
 * deliberate and calm, which fits an app people reach for under stress. The
 * icon container carries a faint matching-color ring -- a small, cheap
 * detail that reads as "powered/active" rather than a plain flat tint.
 * Icon container size is always [IconContainer.md] so cards line up across
 * every screen regardless of which icon is used.
 *
 * @param trailing optional right-aligned control (e.g. a Switch) for
 *   settings-style rows -- kept separate from [content] so a toggle sits
 *   vertically centered next to the text instead of stacked beneath it.
 */
@Composable
fun RoundedInfoCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(IconContainer.md)
                        .clip(MaterialTheme.shapes.medium)
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(Spacing.sm))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (subtitle != null) {
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                content?.invoke()
            }
            if (trailing != null) {
                Spacer(Modifier.width(Spacing.sm))
                trailing()
            }
        }
    }
}
