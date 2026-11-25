package com.xiaobaotv.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors
import com.xiaobaotv.app.ui.theme.Radii
import com.xiaobaotv.app.ui.theme.Motion

/**
 * Neutral translucent chip for metadata such as year, region or genre.
 */
@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = LocalXiaobaoColors.current.glassFill,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    val extras = LocalXiaobaoColors.current
    Row(
        modifier = modifier
            .clip(Radii.chip)
            .background(container)
            .border(1.dp, extras.glassBorder, Radii.chip)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(12.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Rating pill with a star, floating over poster art. */
@Composable
fun RatingBadge(
    score: String,
    modifier: Modifier = Modifier,
    container: Color = Color.Black.copy(alpha = 0.5f),
    showStar: Boolean = true,
) {
    val extras = LocalXiaobaoColors.current
    Row(
        modifier = modifier
            .clip(Radii.badge)
            .background(container)
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (showStar) {
            Text(
                text = "★",
                color = extras.rating,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = score,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Generic solid accent badge, e.g. "全网热播" or "更新至 12 集". */
@Composable
fun BadgePill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .clip(Radii.badge)
            .background(container)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(12.dp))
        }
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

/**
 * Selectable pill used by filter rows. Selection is animated with a colour
 * crossfade plus a very small spring so switching categories feels responsive.
 */
@Composable
fun SelectChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val extras = LocalXiaobaoColors.current
    val interaction = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else extras.glassFill,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "chipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "chipContent"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else extras.glassBorder,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "chipBorder"
    )
    Row(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.94f)
            .clip(Radii.chip)
            .background(container)
            .border(1.dp, borderColor, Radii.chip)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .defaultMinSize(minHeight = 38.dp)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(15.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1
        )
    }
}

/**
 * Episode tile with a "currently playing" accent.
 */
@Composable
fun EpisodeChip(
    text: String,
    selected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    height: Dp = 44.dp,
) {
    val extras = LocalXiaobaoColors.current
    val interaction = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.9f) else extras.glassFill,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "episodeContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "episodeContent"
    )
    Box(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.93f)
            .clip(shape)
            .background(container)
            .border(1.dp, if (selected) Color.Transparent else extras.glassBorder, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .defaultMinSize(minHeight = height)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Gradient label used on the hero banner ("全网热播"). */
@Composable
fun GradientBadge(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val extras = LocalXiaobaoColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(extras.brandGradient))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        }
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Small animated dot used to mark the live/current item in lists. */
@Composable
fun StatusDot(
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 6.dp,
    modifier: Modifier = Modifier,
) {
    val animatedSize by animateDpAsState(targetValue = size, animationSpec = tween(Motion.Fast.toInt()), label = "dotSize")
    Box(
        modifier = modifier
            .size(animatedSize)
            .clip(RoundedCornerShape(50))
            .background(color)
    )
}

@Composable
fun SectionSpacer(width: Dp = 8.dp) {
    Spacer(Modifier.width(width))
}
