package org.dgeek.sumgrid.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * SumGrid shape scale.
 *
 * Grid cells use small (4.dp) rounded corners.
 * Cards and dialogs use medium (12.dp) rounded corners.
 * Bottom sheets and large surfaces use large (16.dp) rounded corners.
 */
val SumGridShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
