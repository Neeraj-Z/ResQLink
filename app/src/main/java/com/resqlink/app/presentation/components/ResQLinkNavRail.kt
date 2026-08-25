package com.resqlink.app.presentation.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.resqlink.app.presentation.navigation.Screen
import com.resqlink.app.presentation.theme.Signal
import com.resqlink.app.presentation.theme.Spacing

/**
 * Navigation rail for medium/expanded-width layouts (foldables opened flat,
 * tablets). Mirrors [ResQLinkBottomBar]'s items, glow-ring SOS treatment,
 * and cyan selected-state accent so the two chrome modes feel like the same
 * app, not two different products.
 */
@Composable
fun ResQLinkNavRail(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    onSosClick: () -> Unit
) {
    val items = Screen.bottomBarItems
    val midpoint = items.size / 2

    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        Spacer(Modifier.height(Spacing.md))
        items.forEachIndexed { index, screen ->
            if (index == midpoint) {
                SosNavItem(onClick = onSosClick)
                Spacer(Modifier.height(Spacing.sm))
            }
            NavigationRailItem(
                selected = currentRoute == screen.route,
                onClick = { onNavigate(screen) },
                icon = { Icon(screen.icon!!, contentDescription = screen.label) },
                label = { Text(screen.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = Signal,
                    selectedTextColor = Signal,
                    indicatorColor = Signal.copy(alpha = 0.14f)
                )
            )
        }
    }
}
