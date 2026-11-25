package com.xiaobaotv.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors

/**
 * A moving sheen used for skeleton placeholders.
 *
 * The gradient translation is driven in the draw phase, so the animation is
 * cheap: it never triggers recomposition of the skeleton hierarchy.
 */
@Composable
fun Modifier.shimmer(shape: Shape = RoundedCornerShape(12.dp)): Modifier {
    val extras = LocalXiaobaoColors.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    val base = extras.shimmerBase
    val highlight = extras.shimmerHighlight
    return this
        .clip(shape)
        .drawWithCache {
            val width = size.width.coerceAtLeast(1f)
            val brush = Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(-width, 0f),
                end = Offset(width, 0f)
            )
            onDrawBehind {
                translate(left = progress * width) {
                    drawRect(brush = brush)
                }
            }
        }
}

/** Convenience skeleton block. */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    Box(modifier = modifier.shimmer(shape))
}
