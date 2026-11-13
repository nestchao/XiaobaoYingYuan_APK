package com.xiaobaotv.app.ui.components

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.xiaobaotv.app.ui.theme.Motion

/**
 * Scales a composable down slightly while pressed using a springy curve.
 *
 * Pair with `clickable(interactionSource = source, indication = null)` to get a
 * tactile, native-feeling press without the harsh default ripple.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = Motion.PressedScale,
    animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = 0.55f,
        stiffness = 620f,
        visibilityThreshold = 0.001f
    ),
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = animationSpec,
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Animates a subtle lift/scale for cards that are in a "selected" or "focused"
 * state (e.g. the current photo in a carousel).
 */
@Composable
fun Modifier.focusScale(
    focused: Boolean,
    focusedScale: Float = 1.04f,
): Modifier {
    val scale by animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = 260f),
        label = "focusScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
