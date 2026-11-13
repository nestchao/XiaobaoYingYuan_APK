package com.xiaobaotv.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.xiaobaotv.app.domain.model.VodContent
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors
import com.xiaobaotv.app.ui.theme.Motion
import com.xiaobaotv.app.ui.theme.Radii

/**
 * The workhorse poster tile. Artwork dominates, metadata is quiet, and the
 * whole card springs inward on touch.
 */
@Composable
fun VodPosterCard(
    vod: VodContent,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    showMeta: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val extras = LocalXiaobaoColors.current
    val meta = remember(vod) {
        buildList {
            vod.year?.takeIf { it.isNotBlank() }?.let { add(it.take(4)) }
            vod.typeName?.takeIf { it.isNotBlank() }?.let { add(it) }
        }.joinToString(" · ")
    }

    Column(
        modifier = modifier
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null) { onClick(vod.id) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(Radii.poster)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, extras.cardBorder, Radii.poster)
        ) {
            AsyncImage(
                model = vod.pic,
                contentDescription = vod.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Bottom scrim keeps the remark chip legible over busy artwork.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = extras.posterScrim,
                            startY = 420f
                        )
                    )
            )

            if (!vod.score.isNullOrBlank() && vod.score != "0.0") {
                RatingBadge(
                    score = vod.score,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                )
            }

            if (!vod.remarks.isNullOrBlank()) {
                ScrimPill(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(7.dp),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Text(
                        text = vod.remarks,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = vod.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (showMeta && meta.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Wide 16:9 card used by "continue watching" rails and editorial rows. The
 * title sits on the artwork so the row stays a single scannable line.
 */
@Composable
fun LandscapeVodCard(
    title: String,
    pic: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    episodeLabel: String? = null,
    subtitle: String? = null,
    progress: Float? = null,
    badge: String? = null,
    aspectRatio: Float = 16f / 9f,
) {
    val interaction = remember { MutableInteractionSource() }
    val extras = LocalXiaobaoColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = tween(Motion.Slow.toInt()),
        label = "wideCardProgress"
    )

    Column(
        modifier = modifier
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, extras.cardBorder, RoundedCornerShape(18.dp))
        ) {
            AsyncImage(
                model = pic,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.05f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.78f)
                            )
                        )
                    )
            )

            // Play affordance
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.34f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (badge != null) {
                BadgePill(
                    text = badge,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(9.dp),
                    container = Color.Black.copy(alpha = 0.5f),
                    contentColor = Color.White
                )
            } else if (episodeLabel != null) {
                ScrimPill(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(9.dp),
                    color = Color.Black.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = episodeLabel,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (progress != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .background(Brush.horizontalGradient(extras.brandGradientSoft))
                    )
                }
            }
        }
    }
}

/** Skeleton matching [VodPosterCard], used while content loads. */
@Composable
fun PosterCardSkeleton(modifier: Modifier = Modifier) {
    val shape = Radii.poster
    Column(modifier = modifier) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            shape = shape
        )
        Spacer(Modifier.height(9.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .height(12.dp),
            shape = RoundedCornerShape(6.dp)
        )
        Spacer(Modifier.height(6.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .height(10.dp),
            shape = RoundedCornerShape(5.dp)
        )
    }
}

/** Skeleton matching [LandscapeVodCard]. */
@Composable
fun LandscapeCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f),
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(8.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(11.dp),
            shape = RoundedCornerShape(6.dp)
        )
    }
}
