package com.resqlink.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = Signal,
    onPrimary = Void,
    primaryContainer = SignalDeep,
    onPrimaryContainer = Signal,
    secondary = Ember,
    onSecondary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Slate,
    outline = Slate,
    outlineVariant = HairlineDark,
    error = Ember
)

private val LightColors = lightColorScheme(
    primary = SignalDim,
    onPrimary = Color.White,
    primaryContainer = LightSurfaceVariant,
    onPrimaryContainer = SignalDim,
    secondary = EmberDeep,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = SlateLight,
    outline = SlateLight,
    outlineVariant = Hairline,
    error = EmberDeep
)

/**
 * App-wide Material 3 theme.
 *
 * ResQLink is designed dark-first ("ops console" aesthetic: deep space
 * background, one cyan signal accent, ember reserved strictly for SOS/
 * extreme alerts) -- the light theme exists for accessibility/preference
 * but the dark theme is the primary intended experience, unusually for a
 * consumer app but consistent with the "control room" visual language this
 * project targets.
 *
 * @param dynamicColor Defaults to **false**: the palette is a deliberate
 *   design choice, so Android 12+ wallpaper-derived dynamic color is
 *   intentionally not used here.
 */
@Composable
fun ResQLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ResQLinkTypography,
        shapes = ResQLinkShapes,
        content = content
    )
}
