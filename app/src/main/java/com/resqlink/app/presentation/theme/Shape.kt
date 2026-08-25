package com.resqlink.app.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radius scale tuned tighter than the previous "soft minimal" pass --
 * a control-panel aesthetic reads more precise with less roundness, without
 * going all the way to sharp corners (which would feel cold/hostile for an
 * emergency app). Used consistently across cards, sheets, and containers.
 */
val ResQLinkShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)
