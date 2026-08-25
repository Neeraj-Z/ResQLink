package com.resqlink.app.presentation.theme

import androidx.compose.ui.unit.dp

/**
 * A single 4pt spacing scale used everywhere instead of ad-hoc padding
 * values. If a screen needs a gap that isn't here, that's a signal to
 * reconsider the layout rather than reach for a one-off dp value --
 * consistency at this level is most of what makes an app feel "aligned".
 */
object Spacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 48.dp
    val xxxl = 64.dp

    /** Standard horizontal screen margin -- matches the width breakpoints in WindowSize.kt. */
    val screenPadding = md
}

/** Icon-container sizing tokens, kept consistent across cards, list rows, and headers. */
object IconContainer {
    val sm = 32.dp
    val md = 40.dp
    val lg = 56.dp
}
