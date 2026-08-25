package com.resqlink.app.presentation.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.resqlink.app.presentation.analytics.AnalyticsScreen
import com.resqlink.app.presentation.components.ResQLinkBottomBar
import com.resqlink.app.presentation.components.ResQLinkNavRail
import com.resqlink.app.presentation.firstaid.FirstAidScreen
import com.resqlink.app.presentation.hazard.HazardScreen
import com.resqlink.app.presentation.home.HomeScreen
import com.resqlink.app.presentation.map.MapScreen
import com.resqlink.app.presentation.settings.SettingsScreen
import com.resqlink.app.presentation.sos.SosScreen

/**
 * Root composable: hosts navigation chrome + NavHost together so the
 * selected item always mirrors the current back-stack destination.
 *
 * Responsive behavior: [windowSizeClass] decides the chrome shape --
 * a bottom bar on phones (compact width), a side [ResQLinkNavRail] on
 * foldables/tablets (medium and expanded width), where a bottom bar would
 * waste horizontal space and sit awkwardly far from the thumb.
 */
@Composable
fun ResQLinkNavGraph(
    navController: NavHostController = rememberNavController(),
    windowSizeClass: WindowSizeClass
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    val onNavigate: (Screen) -> Unit = { screen ->
        navController.navigate(screen.route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val onSosClick: () -> Unit = { navController.navigate(Screen.Sos.route) }

    val hostGraph: @Composable (Modifier) -> Unit = { modifier ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = modifier
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToMap = { navController.navigate(Screen.Map.route) },
                    onNavigateToSos = onSosClick,
                    onNavigateToHazards = { navController.navigate(Screen.Hazards.route) },
                    onNavigateToFirstAid = { navController.navigate(Screen.FirstAid.route) }
                )
            }
            composable(Screen.Map.route) { MapScreen() }
            composable(Screen.Sos.route) { SosScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.FirstAid.route) { FirstAidScreen() }
            composable(Screen.Hazards.route) { HazardScreen() }
            composable(Screen.Analytics.route) { AnalyticsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }

    if (useNavRail) {
        Row(modifier = Modifier.fillMaxSize()) {
            ResQLinkNavRail(currentRoute = currentRoute, onNavigate = onNavigate, onSosClick = onSosClick)
            hostGraph(Modifier.fillMaxSize())
        }
    } else {
        Scaffold(
            bottomBar = { ResQLinkBottomBar(currentRoute = currentRoute, onNavigate = onNavigate, onSosClick = onSosClick) }
        ) { innerPadding ->
            hostGraph(Modifier.padding(innerPadding))
        }
    }
}
