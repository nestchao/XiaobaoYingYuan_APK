package com.xiaobaotv.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Rounded "pebble" geometry: generously rounded corners on big containers,
 * tighter radii on small chips. Radii grow with the size of the element so the
 * UI reads as a family of softly related surfaces.
 */
val XiaobaoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

object Radii {
    val chip = RoundedCornerShape(50)
    val badge = RoundedCornerShape(9.dp)
    val poster = RoundedCornerShape(18.dp)
    val hero = RoundedCornerShape(30.dp)
    val card = RoundedCornerShape(24.dp)
    val sheet = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    val navBar = RoundedCornerShape(28.dp)
    val navItem = RoundedCornerShape(22.dp)
    val button = RoundedCornerShape(50)
}
