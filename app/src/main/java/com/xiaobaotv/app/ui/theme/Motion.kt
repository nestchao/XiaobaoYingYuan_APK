package com.xiaobaotv.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset

/**
 * Central motion vocabulary for the app.
 *
 * Everything that moves should pick a token from here so the whole product
 * feels like the same physical system: quick fades, confident eased slides and
 * springy, slightly overshooting presses.
 */
object Motion {

    // Durations (ms)
    const val Instant = 90L
    const val Fast = 160L
    const val Medium = 260L
    const val Slow = 380L
    const val Lazy = 520L

    // Easings
    /** Quick out, long settle — the default for entrances. */
    val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Fast start, soft landing — emphasized entrance. */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Drag-off screens and exits. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Friendly overshoot for playful accents. */
    val Overshoot: Easing = CubicBezierEasing(0.34f, 1.26f, 0.64f, 1f)

    // Spring specs
    fun <T> smoothSpring(stiffness: Float = 380f, dampingRatio: Float = 0.9f): SpringSpec<T> =
        spring(dampingRatio = dampingRatio, stiffness = stiffness)

    fun <T> bouncySpring(stiffness: Float = 420f, dampingRatio: Float = 0.6f): SpringSpec<T> =
        spring(dampingRatio = dampingRatio, stiffness = stiffness)

    fun <T> gentleSpring(stiffness: Float = 220f, dampingRatio: Float = 0.95f): SpringSpec<T> =
        spring(dampingRatio = dampingRatio, stiffness = stiffness)

    // Common specs
    fun <T> fadeInSpec(): FiniteAnimationSpec<T> = tween(Fast.toInt(), easing = Standard)
    fun <T> fadeOutSpec(): FiniteAnimationSpec<T> = tween(Instant.toInt(), easing = Standard)

    fun slideInSpec(): FiniteAnimationSpec<IntOffset> =
        tween(Medium.toInt(), easing = EmphasizedDecelerate)

    fun slideOutSpec(): FiniteAnimationSpec<IntOffset> =
        tween(Fast.toInt(), easing = EmphasizedAccelerate)

    /** Scale used when a card is pressed. */
    const val PressedScale = 0.955f

    /** Scale used for "container transform" style bored transitions. */
    const val DetailEnterScale = 0.94f
}
