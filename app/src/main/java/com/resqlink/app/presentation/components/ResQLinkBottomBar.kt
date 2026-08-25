package com.resqlink.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.resqlink.app.presentation.navigation.Screen
import com.resqlink.app.presentation.theme.Ember
import com.resqlink.app.presentation.theme.Signal

/**
 * Bottom navigation for compact-width (phone) layouts. Dark, hairline-topped
 * bar with a cyan indicator on the selected tab. SOS sits as a raised
 * circular action with a soft outer glow ring -- deliberately the one
 * saturated element on the bar, rather than a regular tab.
 */
@Composable
fun ResQLinkBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    onSosClick: () -> Unit
) {
    val items = Screen.bottomBarItems
    val midpoint = items.size / 2

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            items.forEachIndexed { index, screen ->
                if (index == midpoint) {
                    SosNavItem(onClick = onSosClick)
                }
                NavigationBarItem(
                    selected = currentRoute == screen.route,
                    onClick = { onNavigate(screen) },
                    icon = { Icon(screen.icon!!, contentDescription = screen.label) },
                    label = { Text(screen.label, style = MaterialTheme.typography.labelMedium) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Signal,
                        selectedTextColor = Signal,
                        indicatorColor = Signal.copy(alpha = 0.14f)
                    )
                )
            }
        }
    }
}

@Composable
internal fun SosNavItem(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .offset(y = (-12).dp)
            .size(64.dp)
            .clip(CircleShape)
            .background(Ember.copy(alpha = 0.18f)), // outer glow ring
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Ember),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick) {
                Icon(Icons.Filled.Sos, contentDescription = "SOS", tint = Color.White)
            }
        }
    }
}
