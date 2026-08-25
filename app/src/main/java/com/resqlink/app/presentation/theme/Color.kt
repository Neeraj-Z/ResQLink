package com.resqlink.app.presentation.theme

import androidx.compose.ui.graphics.Color

// ---- ResQLink design tokens (v2: "near-future ops console") -----------
// A dark, deep-space base with one cyan signal accent -- the visual
// language of a control-room display, not a consumer social app. Ember
// stays reserved for exactly one thing (SOS / extreme severity) so it
// still reads as genuine alarm, not just "the app's red".

// Core neutrals -- deep space, not pure black (pure black reads cheap/flat under glow)
val Void = Color(0xFF06090F)          // deepest background
val Ink = Color(0xFF0B0F17)           // primary dark surface
val InkRaised = Color(0xFF121826)     // elevated dark surface (cards)
val InkRaisedHigh = Color(0xFF1A2233) // highest elevation (sheets, dialogs)
val Paper = Color(0xFFF4F6F9)         // light-theme background (cool, not cream)
val PaperSurface = Color(0xFFFFFFFF)
val PaperRaised = Color(0xFFEDF0F5)

val Slate = Color(0xFF7C8AA0)         // secondary text, dark theme
val SlateLight = Color(0xFF4C5872)    // secondary text, light theme
val Hairline = Color(0xFFDDE2EA)      // light-mode borders
val HairlineDark = Color(0xFF232B3D)  // dark-mode borders
val HairlineGlow = Color(0xFF2A5A6B)  // dark-mode borders on active/focused elements

// Signal accent -- the app's one "technology" color, used for structure,
// active states, data readouts, and mesh/connectivity indicators.
val Signal = Color(0xFF35E0C8)        // cyan-teal, the core accent
val SignalDim = Color(0xFF1E8A7A)     // muted variant for secondary UI
val SignalDeep = Color(0xFF0E4A48)    // deep variant for containers/fills

// Reserved strictly for SOS and EXTREME/HIGH hazard severity -- never used decoratively.
val Ember = Color(0xFFFF4F3E)
val EmberDeep = Color(0xFFC22E22)
val EmberGlow = Color(0xFF4A1712)     // low-alpha-equivalent solid for glow backgrounds

val Moss = Color(0xFF3DDC97)          // resolved / safe / LOW severity
val Amber = Color(0xFFFFB23E)         // caution / MODERATE severity

// Light theme surfaces
val LightBackground = Paper
val LightSurface = PaperSurface
val LightSurfaceVariant = PaperRaised
val LightOnSurface = Color(0xFF10141C)

// Dark theme surfaces (primary theme -- this app is designed dark-first)
val DarkBackground = Void
val DarkSurface = Ink
val DarkSurfaceVariant = InkRaised
val DarkOnSurface = Color(0xFFE7ECF3)
