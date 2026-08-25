package com.resqlink.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resqlink.app.presentation.theme.DataReadoutStyle
import com.resqlink.app.presentation.theme.Spacing

/**
 * Small rounded status badge with a leading indicator dot -- e.g. "● 3 peers
 * connected", "● 28.61390, 77.20900" -- one shared component so status
 * language reads consistently everywhere it appears (SOS, Map, Analytics).
 * Text renders in the monospace data-readout style since most pill content
 * is technical (coordinates, counts, hop numbers) rather than prose.
 */
@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.3f), CircleShape)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(Spacing.xs))
        Text(text, style = DataReadoutStyle, color = color)
    }
}
