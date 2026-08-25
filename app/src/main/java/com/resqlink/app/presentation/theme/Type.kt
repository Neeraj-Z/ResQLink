package com.resqlink.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale with deliberate, non-default tracking: large headlines pull
 * slightly tight (calm authority, not shouty), small labels push slightly
 * wide and uppercase in usage (see `Eyebrow` text style below), the way
 * dispatch/signage typography reads. This is what keeps the UI from
 * feeling like stock Material defaults.
 */
val ResQLinkTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 25.sp, lineHeight = 31.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.1).sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.1.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.2.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.6.sp)
)

/** Small uppercase section eyebrow (e.g. "NEARBY HAZARDS") -- reuses labelSmall's wide tracking. */
val EyebrowStyle = ResQLinkTypography.labelSmall

/**
 * Monospace "data readout" style -- coordinates, hop counts, timestamps,
 * IDs. This one deliberate typographic switch (proportional -> mono for
 * numeric/technical values only) is what reads as "instrument panel"
 * rather than just a dark-mode reskin. Use sparingly: only for values a
 * technician would actually want character-aligned, never for prose.
 */
val DataReadoutStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.2.sp
)
