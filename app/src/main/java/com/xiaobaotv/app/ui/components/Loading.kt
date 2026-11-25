package com.xiaobaotv.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors

/**
 * Minimal, brand-coloured loading spinner: a soft track with a gradient arc
 * sweeping around it.
 */
@Composable
fun XiaobaoSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    strokeWidth: Dp = 3.dp,
) {
    val primary = MaterialTheme.colorScheme.primary
    val extras = LocalXiaobaoColors.current
    val transition = rememberInfiniteTransition(label = "spinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinnerRotation"
    )
    Canvas(modifier = modifier.size(size)) {
        val stroke = strokeWidth.toPx()
        val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
        val topLeft = Offset(stroke / 2f, stroke / 2f)
        drawArc(
            color = extras.progressTrack,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        rotate(rotation) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(Color.Transparent, primary)
                ),
                startAngle = 0f,
                sweepAngle = 290f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

/** Center-aligned spinner for full-screen loading states. */
@Composable
fun BoxedLoading(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        XiaobaoSpinner(size = 40.dp)
    }
}
