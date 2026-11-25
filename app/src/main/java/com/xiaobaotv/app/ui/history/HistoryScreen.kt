package com.xiaobaotv.app.ui.history

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.xiaobaotv.app.domain.model.WatchHistoryItem
import com.xiaobaotv.app.ui.components.AuroraBackground
import com.xiaobaotv.app.ui.components.ContentContainer
import com.xiaobaotv.app.ui.components.EmptyState
import com.xiaobaotv.app.ui.components.ErrorState
import com.xiaobaotv.app.ui.components.GlassIconButton
import com.xiaobaotv.app.ui.components.GlassSurface
import com.xiaobaotv.app.ui.components.LayoutMetrics
import com.xiaobaotv.app.ui.components.MetaChip
import com.xiaobaotv.app.ui.components.ShimmerBox
import com.xiaobaotv.app.ui.components.WindowSize
import com.xiaobaotv.app.ui.components.XiaobaoTopBar
import com.xiaobaotv.app.ui.components.pressScale
import com.xiaobaotv.app.ui.components.rememberWindowSize
import com.xiaobaotv.app.ui.components.screenGutter
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors
import com.xiaobaotv.app.ui.theme.Motion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// 观看历史
//
// A quiet, editorial archive of everything the viewer has started. Entries are
// grouped by relative age, presented as frosted "continue watching" cards and
// can be removed with a right-to-left swipe (or the explicit delete button for
// accessibility). Removal is undoable through a glass snackbar.
//
// Motion notes:
//  - rows fade + slide up in a 22ms stagger on first compose only
//  - every card springs inward while pressed
//  - progress bars animate from zero once, then stay put
//  - deletion runs through LazyList's item animator for a smooth collapse
// ─────────────────────────────────────────────────────────────────────────────

private val HistoryMaxWidth = 1100.dp
private val HistoryCardShape = RoundedCornerShape(22.dp)
private val HistoryThumbShape = RoundedCornerShape(14.dp)
private val HistoryHorizontalMinWidth = 356.dp

/** Delay used by the first-load stagger, capped so long lists never feel slow. */
private fun staggerDelay(index: Int): Long = (index * 22L).coerceAtMost(240L)

@Composable
fun HistoryScreen(
    onVodClick: (Int) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSize = rememberWindowSize()
    val gutter = screenGutter()
    val groups = remember(uiState.items) { groupHistory(uiState.items) }
    val revealedVodIds = remember { mutableSetOf<Int>() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showClearAllDialog by rememberSaveable { mutableStateOf(false) }
    val hasItems = uiState.items.isNotEmpty()

    // Delete immediately, then offer a glass snackbar with an undo action. The
    // full item (including its lastWatchedAt) is kept so the entry reappears in
    // its original group when restored.
    val handleDelete: (WatchHistoryItem) -> Unit = remember(viewModel, snackbarHostState, scope) {
        { item ->
            viewModel.deleteItem(item.vodId)
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "已删除",
                    actionLabel = "撤销",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restoreItem(item)
                }
            }
        }
    }

    AuroraBackground(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                ContentContainer(maxWidth = HistoryMaxWidth) {
                    XiaobaoTopBar(
                        title = "观看历史",
                        subtitle = if (hasItems) "共 ${uiState.items.size} 部内容" else null,
                        actions = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (hasItems) {
                                    ClearAllTextButton(onClick = { showClearAllDialog = true })
                                }
                                GlassIconButton(
                                    icon = Icons.Rounded.Settings,
                                    contentDescription = "设置与外观",
                                    onClick = onSettingsClick,
                                    size = 42.dp,
                                    iconSize = 20.dp
                                )
                            }
                        }
                    )
                }

                ContentContainer(
                    modifier = Modifier.weight(1f),
                    maxWidth = HistoryMaxWidth
                ) {
                    when {
                        uiState.isLoading && !hasItems -> HistorySkeleton()

                        !hasItems && uiState.error != null -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            ErrorState(
                                message = uiState.error.orEmpty(),
                                onRetry = null,
                                modifier = Modifier.padding(bottom = 56.dp)
                            )
                        }

                        !hasItems -> Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = if (windowSize == WindowSize.Compact) 72.dp else 0.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyState(
                                icon = Icons.Rounded.History,
                                title = "暂无观看记录",
                                description = "看过的内容会自动出现在这里，随时继续播放"
                            )
                        }

                        windowSize == WindowSize.Compact -> HistoryList(
                            groups = groups,
                            revealed = revealedVodIds,
                            onVodClick = onVodClick,
                            onDelete = handleDelete
                        )

                        else -> HistoryGrid(
                            groups = groups,
                            revealed = revealedVodIds,
                            onVodClick = onVodClick,
                            onDelete = handleDelete
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(
                        start = gutter,
                        end = gutter,
                        bottom = if (windowSize == WindowSize.Compact) {
                            LayoutMetrics.BottomContentClearance - 40.dp
                        } else {
                            20.dp
                        }
                    )
            ) { data ->
                HistorySnackbar(data)
            }
        }
    }

    if (showClearAllDialog) {
        ClearHistoryDialog(
            onConfirm = {
                viewModel.clearAll()
                showClearAllDialog = false
            },
            onDismiss = { showClearAllDialog = false }
        )
    }
}

// ─── Lists ──────────────────────────────────────────────────────────────────

@Composable
private fun HistoryList(
    groups: List<HistoryGroup>,
    revealed: MutableSet<Int>,
    onVodClick: (Int) -> Unit,
    onDelete: (WatchHistoryItem) -> Unit,
) {
    val gutter = screenGutter()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = gutter,
            end = gutter,
            top = 2.dp,
            bottom = LayoutMetrics.BottomContentClearance
        ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        var runningIndex = 0
        groups.forEachIndexed { groupIndex, group ->
            item(key = "section_${group.age.name}", contentType = "section") {
                HistorySectionLabel(
                    label = group.age.label,
                    count = group.items.size,
                    modifier = Modifier
                        .padding(top = if (groupIndex == 0) 8.dp else 18.dp)
                        .animateItem()
                )
            }
            group.items.forEachIndexed { indexInGroup, entry ->
                val slot = runningIndex + indexInGroup
                item(key = entry.vodId, contentType = "card") {
                    RevealedHistoryCard(
                        item = entry,
                        delayMs = staggerDelay(slot),
                        revealed = revealed,
                        onClick = { onVodClick(entry.vodId) },
                        onDelete = { onDelete(entry) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
            runningIndex += group.items.size
        }
    }
}

@Composable
private fun HistoryGrid(
    groups: List<HistoryGroup>,
    revealed: MutableSet<Int>,
    onVodClick: (Int) -> Unit,
    onDelete: (WatchHistoryItem) -> Unit,
) {
    val gutter = screenGutter()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = gutter,
            end = gutter,
            top = 4.dp,
            bottom = bottomInset + 32.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        var runningIndex = 0
        groups.forEachIndexed { groupIndex, group ->
            item(
                key = "section_${group.age.name}",
                span = { GridItemSpan(maxLineSpan) },
                contentType = "section"
            ) {
                HistorySectionLabel(
                    label = group.age.label,
                    count = group.items.size,
                    modifier = Modifier
                        .padding(top = if (groupIndex == 0) 10.dp else 20.dp)
                        .animateItem()
                )
            }
            group.items.forEachIndexed { indexInGroup, entry ->
                val slot = runningIndex + indexInGroup
                item(key = entry.vodId, contentType = "card") {
                    RevealedHistoryCard(
                        item = entry,
                        delayMs = staggerDelay(slot),
                        revealed = revealed,
                        onClick = { onVodClick(entry.vodId) },
                        onDelete = { onDelete(entry) },
                        modifier = Modifier.animateItem(),
                        gridMode = true
                    )
                }
            }
            runningIndex += group.items.size
        }
    }
}

@Composable
private fun HistorySectionLabel(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val extras = LocalXiaobaoColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(extras.brandGradient))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(extras.cardBorder, Color.Transparent))
                )
        )
    }
}

// ─── Cards ──────────────────────────────────────────────────────────────────

/**
 * Fade + slide wrapper used for the first-load stagger. A shared set of already
 * revealed ids prevents the animation from replaying when a row is recycled by
 * the lazy list.
 */
@Composable
private fun RevealedHistoryCard(
    item: WatchHistoryItem,
    delayMs: Long,
    revealed: MutableSet<Int>,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    gridMode: Boolean = false,
) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(item.vodId) {
        if (revealed.add(item.vodId)) {
            if (delayMs > 0L) delay(delayMs)
            reveal.animateTo(
                targetValue = 1f,
                animationSpec = tween(Motion.Slow.toInt(), easing = Motion.EmphasizedDecelerate)
            )
        } else {
            reveal.snapTo(1f)
        }
    }
    HistorySwipeCard(
        item = item,
        onClick = onClick,
        onDelete = onDelete,
        gridMode = gridMode,
        modifier = modifier.graphicsLayer {
            alpha = reveal.value
            translationY = (1f - reveal.value) * 26.dp.toPx()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistorySwipeCard(
    item: WatchHistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    gridMode: Boolean = false,
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                currentOnDelete()
                true
            } else {
                false
            }
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // Material3's SwipeToDismissBoxState.progress is 1f while settled
            // (distance between current and closest anchor is zero), so gate it
            // on the drag direction or the delete background would be visible
            // through the translucent card at rest.
            val swipeProgress = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                dismissState.progress
            } else {
                0f
            }
            SwipeDeleteBackground(progress = swipeProgress)
        }
    ) {
        HistoryCard(
            item = item,
            onClick = onClick,
            onDeleteClick = onDelete,
            gridMode = gridMode
        )
    }
}

@Composable
private fun SwipeDeleteBackground(progress: Float) {
    val alpha = progress.coerceIn(0f, 1f)
    val error = MaterialTheme.colorScheme.error
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(HistoryCardShape)
            .background(error.copy(alpha = 0.16f * alpha))
            .border(1.dp, error.copy(alpha = 0.32f * alpha), HistoryCardShape)
            .padding(end = 22.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = error.copy(alpha = alpha),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "删除",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = error.copy(alpha = alpha)
            )
        }
    }
}

@Composable
private fun HistoryCard(
    item: WatchHistoryItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
    gridMode: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // Capture the width before entering nested layout scopes: the layout
        // scope marker hides the BoxWithConstraints receiver inside GlassSurface.
        val cardWidth = maxWidth
        // Grid cards get a vertical poster-style layout until the column is wide
        // enough for a comfortable landscape row.
        val horizontal = !gridMode || cardWidth >= HistoryHorizontalMinWidth
        GlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(interaction)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClickLabel = "继续观看",
                    role = Role.Button,
                    onClick = onClick
                ),
            shape = HistoryCardShape
        ) {
            if (horizontal) {
                val thumbWidth = if (gridMode) {
                    (cardWidth * 0.36f).coerceIn(124.dp, 150.dp)
                } else {
                    136.dp
                }
                HorizontalHistoryContent(
                    item = item,
                    thumbWidth = thumbWidth,
                    onDeleteClick = onDeleteClick
                )
            } else {
                VerticalHistoryContent(
                    item = item,
                    onDeleteClick = onDeleteClick
                )
            }
        }
    }
}

@Composable
private fun HorizontalHistoryContent(
    item: WatchHistoryItem,
    thumbWidth: Dp,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HistoryThumbnail(
            item = item,
            modifier = Modifier
                .width(thumbWidth)
                .aspectRatio(16f / 9f)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            HistoryName(item.name)
            Spacer(Modifier.height(6.dp))
            EpisodeAndTimeRow(
                item = item,
                timeLabel = relativeTime(item.lastWatchedAt)
            )
            if (item.durationMs > 0L) {
                Spacer(Modifier.height(8.dp))
                HistoryProgressRow(item = item)
            }
        }
        Spacer(Modifier.width(6.dp))
        SubtleDeleteButton(
            contentDescription = "删除 ${item.name}",
            onClick = onDeleteClick
        )
    }
}

@Composable
private fun VerticalHistoryContent(
    item: WatchHistoryItem,
    onDeleteClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        HistoryThumbnail(
            item = item,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                HistoryName(item.name)
                Spacer(Modifier.height(6.dp))
                EpisodeAndTimeRow(
                    item = item,
                    timeLabel = relativeTime(item.lastWatchedAt)
                )
                if (item.durationMs > 0L) {
                    Spacer(Modifier.height(8.dp))
                    HistoryProgressRow(item = item)
                }
            }
            Spacer(Modifier.width(2.dp))
            SubtleDeleteButton(
                contentDescription = "删除 ${item.name}",
                onClick = onDeleteClick
            )
        }
    }
}

@Composable
private fun HistoryName(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun EpisodeAndTimeRow(
    item: WatchHistoryItem,
    timeLabel: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetaChip(
            text = episodeLabel(item),
            modifier = Modifier.widthIn(max = 96.dp),
            container = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            contentColor = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = timeLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HistoryProgressRow(item: WatchHistoryItem) {
    val extras = LocalXiaobaoColors.current
    val progress = (item.positionMs.toFloat() / item.durationMs.toFloat()).coerceIn(0f, 1f)
    val percent = (progress * 100f).roundToInt().coerceIn(0, 100)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(Motion.Slow.toInt(), easing = Motion.EmphasizedDecelerate),
        label = "historyProgress"
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(extras.progressTrack)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(extras.brandGradientSoft))
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

@Composable
private fun HistoryThumbnail(
    item: WatchHistoryItem,
    modifier: Modifier = Modifier,
) {
    val extras = LocalXiaobaoColors.current
    Box(
        modifier = modifier
            .clip(HistoryThumbShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        AsyncImage(
            model = item.pic,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.32f))
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(30.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.34f))
                .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, extras.cardBorder, HistoryThumbShape)
        )
    }
}

@Composable
private fun SubtleDeleteButton(
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(44.dp)
            .pressScale(interaction, pressedScale = 0.88f)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = "删除记录",
                role = Role.Button,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.size(19.dp)
        )
    }
}

// ─── Loading / chrome ───────────────────────────────────────────────────────

@Composable
private fun HistorySkeleton() {
    val gutter = screenGutter()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = gutter, end = gutter, top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShimmerBox(
            modifier = Modifier
                .width(92.dp)
                .height(13.dp),
            shape = RoundedCornerShape(6.dp)
        )
        repeat(4) {
            HistoryCardSkeleton()
        }
    }
}

@Composable
private fun HistoryCardSkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(
            modifier = Modifier
                .width(136.dp)
                .aspectRatio(16f / 9f),
            shape = HistoryThumbShape
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(14.dp),
                shape = RoundedCornerShape(6.dp)
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(10.dp),
                shape = RoundedCornerShape(5.dp)
            )
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(4.dp),
                shape = RoundedCornerShape(2.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        ShimmerBox(
            modifier = Modifier.size(40.dp),
            shape = CircleShape
        )
    }
}

@Composable
private fun ClearAllTextButton(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.error
        ),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "清除全部",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun HistorySnackbar(data: SnackbarData) {
    val extras = LocalXiaobaoColors.current
    Snackbar(
        snackbarData = data,
        shape = RoundedCornerShape(16.dp),
        containerColor = extras.glassFillStrong,
        contentColor = MaterialTheme.colorScheme.onSurface,
        actionColor = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "清除全部观看记录？",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "此操作无法撤销。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "清除",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = "取消",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        },
        shape = MaterialTheme.shapes.large,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    )
}

// ─── Data ───────────────────────────────────────────────────────────────────

private enum class HistoryAge(val label: String) {
    TODAY("今天"),
    YESTERDAY("昨天"),
    THIS_WEEK("本周"),
    EARLIER("更早")
}

private data class HistoryGroup(
    val age: HistoryAge,
    val items: List<WatchHistoryItem>
)

/**
 * Buckets the repository list (already sorted newest-first) into the four
 * relative-age sections. Future timestamps are folded into "今天" so a device
 * clock change can never hide an entry.
 */
private fun groupHistory(items: List<WatchHistoryItem>): List<HistoryGroup> {
    if (items.isEmpty()) return emptyList()

    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val buckets = LinkedHashMap<HistoryAge, MutableList<WatchHistoryItem>>()

    items.sortedByDescending { it.lastWatchedAt }.forEach { item ->
        val date = Instant.ofEpochMilli(item.lastWatchedAt).atZone(zone).toLocalDate()
        val age = when {
            !date.isBefore(today) -> HistoryAge.TODAY
            date == today.minusDays(1) -> HistoryAge.YESTERDAY
            date.isAfter(today.minusDays(7)) -> HistoryAge.THIS_WEEK
            else -> HistoryAge.EARLIER
        }
        buckets.getOrPut(age) { mutableListOf() }.add(item)
    }

    return HistoryAge.entries.mapNotNull { age ->
        buckets[age]?.let { HistoryGroup(age = age, items = it) }
    }
}

private fun episodeLabel(item: WatchHistoryItem): String {
    val name = item.episodeName.trim()
    return if (name.isNotEmpty()) name else "第 ${item.episodeIndex + 1} 集"
}

private fun relativeTime(timestampMs: Long): String {
    val diff = (System.currentTimeMillis() - timestampMs).coerceAtLeast(0L)
    return when {
        diff < 60_000L -> "刚刚"
        diff < 3_600_000L -> "${diff / 60_000L} 分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000L} 小时前"
        diff < 7 * 86_400_000L -> "${diff / 86_400_000L} 天前"
        else -> {
            val date = Instant.ofEpochMilli(timestampMs).atZone(ZoneId.systemDefault()).toLocalDate()
            "${date.monthValue} 月 ${date.dayOfMonth} 日"
        }
    }
}
