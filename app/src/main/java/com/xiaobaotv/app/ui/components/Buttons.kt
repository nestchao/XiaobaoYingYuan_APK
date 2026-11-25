package com.xiaobaotv.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors
import com.xiaobaotv.app.ui.theme.Radii

/**
 * The primary call-to-action: a pill with the brand gradient, a light inner
 * highlight and a springy press.
 */
@Composable
fun GradientPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    contentPaddingHorizontal: Dp = 24.dp,
) {
    val extras = LocalXiaobaoColors.current
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .height(height)
            .alpha(if (enabled) 1f else 0.45f)
            .pressScale(interaction)
            .clip(Radii.button)
            .background(Brush.horizontalGradient(extras.brandGradient))
            .border(1.dp, Color.White.copy(alpha = 0.18f), Radii.button)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = contentPaddingHorizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1
        )
    }
}

/**
 * Secondary action rendered as a glass pill — used next to the primary CTA and
 * in empty states.
 */
@Composable
fun GlassPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 52.dp,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    container: Color? = null,
    borderColor: Color? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val extras = LocalXiaobaoColors.current
    Row(
        modifier = modifier
            .height(height)
            .alpha(if (enabled) 1f else 0.45f)
            .pressScale(interaction)
            .clip(Radii.button)
            .background(container ?: extras.glassFillStrong)
            .border(1.dp, borderColor ?: extras.glassBorder, Radii.button)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
            maxLines = 1
        )
    }
}

/** Compact text button with a chevron, used for "查看全部" style links. */
@Composable
fun LinkPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.94f)
            .clip(Radii.chip)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .widthIn(min = 44.dp)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (icon != null) {
            Spacer(Modifier.width(3.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}
