package com.xiaobaotv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors

/**
 * Frosted-glass surface used for floating chrome: the bottom navigation pill,
 * side bars, floating action buttons and chips.
 *
 * Compose cannot sample the backdrop, so we fake the material with a
 * translucent vertical gradient plus a 1dp top-lit hairline border. On OLED
 * dark mode this reads as smoked glass; in light mode as frosted porcelain.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    strong: Boolean = false,
    border: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val extras = LocalXiaobaoColors.current
    val fill = if (strong) extras.glassFillStrong else extras.glassFill
    val hairline = Brush.verticalGradient(
        colors = listOf(extras.glassBorder, extras.glassBorder.copy(alpha = 0.015f))
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        fill,
                        fill.copy(alpha = (fill.alpha * 0.82f).coerceIn(0f, 1f))
                    )
                )
            )
            .then(if (border) Modifier.border(1.dp, hairline, shape) else Modifier),
        content = content
    )
}

/** A circular glass icon button with a springy press animation. */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    strong: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    GlassSurface(
        modifier = modifier
            .size(size)
            .pressScale(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        shape = CircleShape,
        strong = strong
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier
                .align(Alignment.Center)
                .size(iconSize)
        )
    }
}

/**
 * Tinted circular icon button used for primary floating actions on top of
 * imagery (play, back, favourite).
 */
@Composable
fun TintedIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    container: Color = Color.Black.copy(alpha = 0.42f),
    tint: Color = Color.White,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .pressScale(interaction)
            .clip(CircleShape)
            .background(container)
            .border(1.dp, Color.White.copy(alpha = 0.14f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** A small translucent pill used to float metadata above artwork. */
@Composable
fun ScrimPill(
    modifier: Modifier = Modifier,
    color: Color = Color.Black.copy(alpha = 0.52f),
    shape: Shape = CircleShape,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        content()
    }
}
