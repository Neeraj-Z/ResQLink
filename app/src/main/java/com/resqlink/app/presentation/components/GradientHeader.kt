package com.resqlink.app.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.resqlink.app.presentation.theme.EyebrowStyle
import com.resqlink.app.presentation.theme.Ink
import com.resqlink.app.presentation.theme.InkRaised
import com.resqlink.app.presentation.theme.Signal
import com.resqlink.app.presentation.theme.SignalDeep
import com.resqlink.app.presentation.theme.Spacing

/**
 * Section header used atop each top-level screen: a dark glass panel with a
 * single cyan hairline seam along the bottom edge, rather than a solid
 * colour block -- this is what reads as "instrument panel" instead of "app
 * banner". Color saturation stays reserved for the SOS button
 * ([ResQLinkBottomBar]) and status indicators; this header itself is quiet.
 *
 * @param eyebrow optional small uppercase label above the title (e.g. "MODULE").
 * @param onIconClick when supplied, makes the icon slot a tappable action.
 * @param iconContentDescription required screen-reader label when [onIconClick] is supplied.
 * @param isIconLoading swaps the icon for a small spinner.
 */
@Composable
fun GradientHeader(
    title: String,
    subtitle: String? = null,
    eyebrow: String? = null,
    icon: ImageVector? = null,
    onIconClick: (() -> Unit)? = null,
    iconContentDescription: String? = null,
    isIconLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(InkRaised, Ink)))
                .padding(horizontal = Spacing.md, vertical = Spacing.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    if (eyebrow != null) {
                        Text(
                            text = eyebrow.uppercase(),
                            color = Signal,
                            style = EyebrowStyle
                        )
                        Spacer(Modifier.height(Spacing.xxs))
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    if (subtitle != null) {
                        Spacer(Modifier.height(Spacing.xxs))
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (icon != null) {
                    Spacer(Modifier.width(Spacing.md))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SignalDeep)
                            .let {
                                if (onIconClick != null) {
                                    it.clickable(
                                        onClick = onIconClick,
                                        role = Role.Button,
                                        enabled = !isIconLoading
                                    )
                                } else it
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isIconLoading) {
                            CircularProgressIndicator(
                                color = Signal,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = icon,
                                contentDescription = if (onIconClick != null) iconContentDescription else null,
                                tint = Signal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
        // The one signature detail: a thin glowing seam marking the boundary
        // between "chrome" and "content" -- cheap to render, reads expensive.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Brush.horizontalGradient(listOf(Signal.copy(alpha = 0f), Signal.copy(alpha = 0.7f), Signal.copy(alpha = 0f))))
        )
    }
}
