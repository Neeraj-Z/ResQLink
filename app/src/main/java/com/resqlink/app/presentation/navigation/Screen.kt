package com.resqlink.app.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/** Sealed navigation graph. Adding a screen means adding one entry here + one composable. */
sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    data object Home : Screen("home", "Home", Icons.Filled.Home)
    data object Map : Screen("map", "Map", Icons.Filled.Map)
    data object Sos : Screen("sos", "SOS")
    data object FirstAid : Screen("first_aid", "First Aid", Icons.Filled.LocalHospital)
    data object Hazards : Screen("hazards", "Hazards", Icons.Filled.Warning)
    data object Analytics : Screen("analytics", "Analytics", Icons.Filled.Analytics)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings)

    companion object {
        /** Destinations shown in the bottom navigation bar, in display order. SOS is handled as a FAB, not a tab. */
        val bottomBarItems = listOf(Home, Map, FirstAid, Analytics)
    }
}
