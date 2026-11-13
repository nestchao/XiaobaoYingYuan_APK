package com.xiaobaotv.app.ui.category

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.xiaobaotv.app.domain.model.VodContent
import com.xiaobaotv.app.ui.components.AuroraBackground
import com.xiaobaotv.app.ui.components.ContentContainer
import com.xiaobaotv.app.ui.components.EmptyState
import com.xiaobaotv.app.ui.components.ErrorState
import com.xiaobaotv.app.ui.components.GlassIconButton
import com.xiaobaotv.app.ui.components.GlassPillButton
import com.xiaobaotv.app.ui.components.LayoutMetrics
import com.xiaobaotv.app.ui.components.PosterCardSkeleton
import com.xiaobaotv.app.ui.components.ScreenHeader
import com.xiaobaotv.app.ui.components.SelectChip
import com.xiaobaotv.app.ui.components.VodPosterCard
import com.xiaobaotv.app.ui.components.WindowSize
import com.xiaobaotv.app.ui.components.XiaobaoSpinner
import com.xiaobaotv.app.ui.components.focusScale
import com.xiaobaotv.app.ui.components.posterGridColumns
import com.xiaobaotv.app.ui.components.rememberWindowSize
import com.xiaobaotv.app.ui.components.screenGutter
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors
import com.xiaobaotv.app.ui.theme.Motion
import com.xiaobaotv.app.ui.theme.Radii
import kotlinx.coroutines.delay

/**
 * Category: an editorial header plus a pinned row of animated filter pills over
 * an adaptive poster grid.
 *
 * Behaviour preserved from the previous implementation: Paging 3 powered grid
 * for the five top level categories, refresh / retry through the paging items
 * and an age-appropriate empty / error state.
 *
 * Motion notes:
 *  - the filter pills reveal in a short stagger the first time the screen opens
 *  - the selected pill gets a springy lift on top of the shared colour crossfade
 *  - the content area crossfades between skeleton / grid / empty / error
 *  - new pages animate in through [androidx.compose.foundation.lazy.grid.LazyGridItemScope]
 */
@Composable
fun CategoryScreen(
    onVodClick: (Int) -> Unit,
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagingItems = viewModel.pagingDataFlow.collectAsLazyPagingItems()

    val windowSize = rememberWindowSize()
    val isCompact = windowSize == WindowSize.Compact
    val columns = posterGridColumns()
    val bottomClearance =
        if (isCompact) LayoutMetrics.BottomContentClearance else 56.dp
    val onVodClickRemembered = remember(onVodClick) { { id: Int -> onVodClick(id) } }

    val selectedTab = CategoryTabs.firstOrNull { it.typeId == uiState.selectedTypeId }
        ?: CategoryTabs.first()

    // One-shot stagger reveal for the filter pills.
    var pillsRevealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(70L)
        pillsRevealed = true
    }

    val refreshState = pagingItems.loadState.refresh
    val appendState = pagingItems.loadState.append
    val phase = when {
        pagingItems.itemCount > 0 -> CategoryPhase.Grid
        refreshState is LoadState.Loading -> CategoryPhase.Loading
        refreshState is LoadState.Error -> CategoryPhase.Error
        else -> CategoryPhase.Empty
    }

    AuroraBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            ContentContainer {
                Column {
                    ScreenHeader(
                        title = "分类片库",
                        subtitle = "按喜好探索，总有一部适合你",
                        modifier = Modifier.padding(bottom = 2.dp),
                        trailing = {
                            GlassIconButton(
                                icon = Icons.Rounded.Refresh,
                                contentDescription = "刷新当前分类",
                                onClick = { pagingItems.refresh() },
                                size = 44.dp,
                                iconSize = 20.dp
                            )
                        }
                    )

                    CategoryFilterRow(
                        selectedTypeId = selectedTab.typeId,
                        revealed = pillsRevealed,
                        onSelect = { typeId -> viewModel.selectType(typeId) },
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )

                    CategorySelectionCaption(
                        tab = selectedTab,
                        loadedCount = pagingItems.itemCount,
                        endReached = appendState.endOfPaginationReached &&
                            pagingItems.itemCount > 0
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Crossfade(
                    targetState = phase,
                    modifier = Modifier.fillMaxSize(),
                    animationSpec = tween(
                        durationMillis = Motion.Medium.toInt(),
                        easing = Motion.Standard
                    ),
                    label = "categoryPhase"
                ) { target ->
                    ContentContainer(modifier = Modifier.fillMaxSize()) {
                        when (target) {
                            CategoryPhase.Loading -> CategorySkeletonGrid(
                                columns = columns,
                                bottomClearance = bottomClearance
                            )

                            CategoryPhase.Error -> CategoryErrorPane(
                                onRetry = { pagingItems.retry() }
                            )

                            CategoryPhase.Empty -> CategoryEmptyPane(
                                tab = selectedTab,
                                onReload = { pagingItems.refresh() }
                            )

                            CategoryPhase.Grid -> CategoryContentGrid(
                                pagingItems = pagingItems,
                                columns = columns,
                                bottomClearance = bottomClearance,
                                appendState = appendState,
                                onVodClick = onVodClickRemembered
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Header pieces ──────────────────────────────────────────────────────────

private data class CategoryTab(
    val typeId: Int,
    val name: String,
    val icon: ImageVector,
    val tagline: String
)

private val CategoryTabs = listOf(
    CategoryTab(1, "电影", Icons.Rounded.Movie, "院线新片与高分佳作"),
    CategoryTab(2, "电视剧", Icons.Rounded.Tv, "热播剧集一次追个够"),
    CategoryTab(3, "综艺", Icons.Rounded.Mic, "爆笑综艺陪你放松"),
    CategoryTab(4, "动漫", Icons.Rounded.Animation, "新番连载与经典重温"),
    CategoryTab(5, "短剧", Icons.Rounded.Bolt, "高能短剧停不下来")
)

private enum class CategoryPhase { Loading, Error, Empty, Grid }

@Composable
private fun CategoryFilterRow(
    selectedTypeId: Int,
    revealed: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val windowSize = rememberWindowSize()
    val gutter = screenGutter()

    if (windowSize != WindowSize.Expanded) {
        val listState = rememberLazyListState()
        LazyRow(
            state = listState,
            modifier = modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(
                count = CategoryTabs.size,
                key = { index -> CategoryTabs[index].typeId },
                contentType = { "category_pill" }
            ) { index ->
                val tab = CategoryTabs[index]
                CategoryFilterPill(
                    tab = tab,
                    selected = tab.typeId == selectedTypeId,
                    revealIndex = index,
                    revealed = revealed,
                    onSelect = onSelect
                )
            }
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryTabs.forEachIndexed { index, tab ->
                CategoryFilterPill(
                    tab = tab,
                    selected = tab.typeId == selectedTypeId,
                    revealIndex = index,
                    revealed = revealed,
                    onSelect = onSelect
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterPill(
    tab: CategoryTab,
    selected: Boolean,
    revealIndex: Int,
    revealed: Boolean,
    onSelect: (Int) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val revealState = animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(
            durationMillis = 420,
            delayMillis = revealIndex * 55,
            easing = Motion.EmphasizedDecelerate
        ),
        label = "categoryPillReveal"
    )

    SelectChip(
        text = tab.name,
        selected = selected,
        onClick = {
            if (!selected) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onSelect(tab.typeId)
            }
        },
        icon = tab.icon,
        modifier = Modifier
            .graphicsLayer {
                val reveal = revealState.value
                alpha = reveal
                translationY = (1f - reveal) * 16.dp.toPx()
            }
            .focusScale(focused = selected, focusedScale = 1.02f)
    )
}

@Composable
private fun CategorySelectionCaption(
    tab: CategoryTab,
    loadedCount: Int,
    endReached: Boolean
) {
    val extras = LocalXiaobaoColors.current
    val gutter = screenGutter()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = gutter, end = gutter, top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .clip(Radii.chip)
                .background(Brush.verticalGradient(extras.brandGradient))
        )
        Spacer(Modifier.width(9.dp))
        Box(modifier = Modifier.weight(1f)) {
            Crossfade(
                targetState = tab,
                animationSpec = tween(
                    durationMillis = Motion.Fast.toInt(),
                    easing = Motion.Standard
                ),
                label = "categoryCaption"
            ) { current ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = current.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = current.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (endReached) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = "共 $loadedCount 部",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// ─── Grid states ────────────────────────────────────────────────────────────

@Composable
private fun CategorySkeletonGrid(
    columns: Int,
    bottomClearance: Dp
) {
    val gutter = screenGutter()
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = gutter,
            end = gutter,
            top = 8.dp,
            bottom = bottomClearance
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        userScrollEnabled = false
    ) {
        items(count = columns * 4) {
            PosterCardSkeleton(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CategoryEmptyPane(
    tab: CategoryTab,
    onReload: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        EmptyState(
            icon = Icons.Rounded.MovieFilter,
            title = "暂无相关内容",
            description = "「${tab.name}」片库暂时空空的，稍后再来看看吧",
            actionLabel = "重新加载",
            onAction = onReload
        )
    }
}

@Composable
private fun CategoryErrorPane(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ErrorState(
            title = "加载失败",
            message = "网络开小差了，请检查网络后重试",
            onRetry = onRetry
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryContentGrid(
    pagingItems: LazyPagingItems<VodContent>,
    columns: Int,
    bottomClearance: Dp,
    appendState: LoadState,
    onVodClick: (Int) -> Unit
) {
    val gutter = screenGutter()
    val refreshing = pagingItems.loadState.refresh is LoadState.Loading &&
        pagingItems.itemCount > 0

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { pagingItems.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = gutter,
                end = gutter,
                top = 8.dp,
                bottom = bottomClearance
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(
                count = pagingItems.itemCount,
                key = { index ->
                    pagingItems.peek(index)?.id ?: "category_placeholder_$index"
                },
                contentType = { index ->
                    if (pagingItems.peek(index) != null) "vod" else "skeleton"
                }
            ) { index ->
                val vod = pagingItems[index]
                if (vod != null) {
                    VodPosterCard(
                        vod = vod,
                        onClick = onVodClick,
                        modifier = Modifier.animateItem()
                    )
                } else {
                    PosterCardSkeleton(modifier = Modifier.fillMaxWidth())
                }
            }

            item(
                key = "category_footer",
                contentType = "footer",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                CategoryListFooter(
                    appendState = appendState,
                    itemCount = pagingItems.itemCount,
                    onRetry = { pagingItems.retry() }
                )
            }
        }
    }
}

@Composable
private fun CategoryListFooter(
    appendState: LoadState,
    itemCount: Int,
    onRetry: () -> Unit
) {
    when {
        appendState is LoadState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                XiaobaoSpinner(size = 28.dp)
            }
        }

        appendState is LoadState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassPillButton(
                    text = "加载失败，点击重试",
                    onClick = onRetry,
                    height = 44.dp
                )
            }
        }

        appendState.endOfPaginationReached && itemCount > 0 -> {
            CategoryEndCaption(itemCount = itemCount)
        }

        else -> {
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun CategoryEndCaption(itemCount: Int) {
    val extras = LocalXiaobaoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(3.dp)
                .clip(Radii.chip)
                .background(Brush.horizontalGradient(extras.brandGradient))
        )
        Spacer(Modifier.height(9.dp))
        Text(
            text = "已经到底啦 · 共 $itemCount 部",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
