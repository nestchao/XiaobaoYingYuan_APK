package com.xiaobaotv.app.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xiaobaotv.app.ui.theme.Motion

/**
 * Synopsis paragraph that expands in place. The toggle only appears when the
 * text actually overflows the collapsed line count.
 */
@Composable
fun ExpandableSynopsis(
    text: String,
    modifier: Modifier = Modifier,
    collapsedLines: Int = 4,
) {
    var expanded by remember(text) { mutableStateOf(false) }
    var hasOverflow by remember(text) { mutableStateOf(false) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(Motion.Medium.toInt()),
        label = "chevronRotation"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                if (!expanded) hasOverflow = result.hasVisualOverflow
            },
            modifier = Modifier.animateContentSize(
                animationSpec = tween(Motion.Medium.toInt())
            )
        )
        if (hasOverflow || expanded) {
            Spacer(Modifier.height(2.dp))
            val interaction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = if (expanded) "收起" else "展开全部",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(chevronRotation)
                )
            }
        }
    }
}
