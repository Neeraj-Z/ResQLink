package com.resqlink.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.resqlink.app.presentation.navigation.ResQLinkNavGraph
import com.resqlink.app.presentation.theme.ResQLinkTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. All screens are Composable destinations
 * managed by [ResQLinkNavGraph] (Navigation Compose).
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        )

        setContent {
            // Drives the compact (phone) vs. medium/expanded (foldable, tablet)
            // layout switch inside ResQLinkNavGraph -- computed once here so
            // every screen downstream can stay a plain, testable composable.
            val windowSizeClass = calculateWindowSizeClass(this)

            ResQLinkTheme {
                ResQLinkNavGraph(windowSizeClass = windowSizeClass)
            }
        }
    }
}
