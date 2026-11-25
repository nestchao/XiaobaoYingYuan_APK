package com.xiaobaotv.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors

/**
 * The app's signature backdrop: a deep canvas with slowly drifting aurora
 * glows. It is intentionally extremely subtle — the content should feel like it
 * floats on a living surface, not like the background is competing with it.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    glowStrength: Float = 1f,
    animated: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val extras = LocalXiaobaoColors.current
    val background = MaterialTheme.colorScheme.background
    val isDark = background.luminance() < 0.4f
    val alphaScale = (if (isDark) 1f else 0.62f) * glowStrength

    val primary = extras.glowPrimary.copy(alpha = 0.20f * alphaScale)
    val secondary = extras.glowSecondary.copy(alpha = 0.16f * alphaScale)
    val tertiary = extras.glowTertiary.copy(alpha = 0.10f * alphaScale)

    val transition = rememberInfiniteTransition(label = "aurora")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraDrift"
    )
    val drift2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 37_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auroraDrift2"
    )

    Box(modifier = modifier.background(background)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (!animated) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(primary, Color.Transparent),
                        center = Offset(size.width * 0.2f, size.height * 0.02f),
                        radius = size.maxDimension * 0.72f
                    ),
                    radius = size.maxDimension * 0.72f,
                    center = Offset(size.width * 0.2f, size.height * 0.02f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(secondary, Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.42f),
                        radius = size.maxDimension * 0.62f
                    ),
                    radius = size.maxDimension * 0.62f,
                    center = Offset(size.width * 0.95f, size.height * 0.42f)
                )
                return@Canvas
            }

            val first = Offset(
                x = size.width * (0.08f + 0.20f * drift),
                y = size.height * (-0.06f + 0.16f * drift2)
            )
            val second = Offset(
                x = size.width * (1.02f - 0.18f * drift2),
                y = size.height * (0.30f + 0.22f * drift)
            )
            val third = Offset(
                x = size.width * (0.55f + 0.12f * drift2),
                y = size.height * (0.92f - 0.10f * drift)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(primary, Color.Transparent),
                    center = first,
                    radius = size.maxDimension * 0.74f
                ),
                radius = size.maxDimension * 0.74f,
                center = first
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(secondary, Color.Transparent),
                    center = second,
                    radius = size.maxDimension * 0.66f
                ),
                radius = size.maxDimension * 0.66f,
                center = second
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tertiary, Color.Transparent),
                    center = third,
                    radius = size.maxDimension * 0.52f
                ),
                radius = size.maxDimension * 0.52f,
                center = third
            )
        }
        content()
    }
}
