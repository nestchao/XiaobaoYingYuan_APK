package com.xiaobaotv.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.xiaobaotv.app.domain.model.VodContent
import com.xiaobaotv.app.domain.model.WatchHistoryItem
import com.xiaobaotv.app.ui.components.AuroraBackground
import com.xiaobaotv.app.ui.components.GlassIconButton
import com.xiaobaotv.app.ui.components.GradientBadge
import com.xiaobaotv.app.ui.components.GradientPillButton
import com.xiaobaotv.app.ui.components.GlassPillButton
import com.xiaobaotv.app.ui.components.LayoutMetrics
import com.xiaobaotv.app.ui.components.LandscapeCardSkeleton
import com.xiaobaotv.app.ui.components.LandscapeVodCard
import com.xiaobaotv.app.ui.components.PosterCardSkeleton
import com.xiaobaotv.app.ui.components.RatingBadge
import com.xiaobaotv.app.ui.components.SectionHeader
import com.xiaobaotv.app.ui.components.ShimmerBox
import com.xiaobaotv.app.ui.components.VodPosterCard
import com.xiaobaotv.app.ui.components.plainSynopsis
import com.xiaobaotv.app.ui.components.railPosterWidth
import com.xiaobaotv.app.ui.components.railWideCardWidth
import com.xiaobaotv.app.ui.components.rememberWindowSize
import com.xiaobaotv.app.ui.components.screenGutter
import com.xiaobaotv.app.ui.components.WindowSize
import com.xiaobaotv.app.ui.navigation.GradientWordmark
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors
import com.xiaobaotv.app.ui.theme.Radii
import kotlinx.coroutines.delay
import java.time.LocalTime

/**
 * Home: a cinematic hero carousel over a field of horizontal content rails.
 *
 * Motion notes:
 *  - the hero auto-advances on a slow timer, pausing whenever the user touches it
 *  - artwork inside the hero parallaxes at a different rate than the pager
 *  - each rail scrolls natively; cards spring in on press
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onVodClick: (Int) -> Unit,
    onPlayClick: (Int) -> Unit,
    onResumeClick: (Int, Int) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSize = rememberWindowSize()
    val extras = LocalXiaobaoColors.current
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val refreshing = uiState.isLoading && uiState.heroItems.isNotEmpty()
    val hasContent = uiState.heroItems.isNotEmpty() || uiState.hotMovies.isNotEmpty()

    AuroraBackground(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { viewModel.loadHomeContent() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = statusTop + 8.dp,
                    bottom = LayoutMetrics.BottomContentClearance
                )
            ) {
                item(key = "header", contentType = "header") {
                    HomeHeader(
                        windowSize = windowSize,
                        onSettingsClick = onSettingsClick
                    )
                }

                when {
                    !hasContent && uiState.isLoading -> {
                        item(key = "heroSkeleton") { HeroSkeleton() }
                        item(key = "railSkeleton1") { RailSkeleton() }
                        item(key = "railSkeleton2") { RailSkeleton() }
                    }

                    !hasContent && uiState.error != null -> {
                        item(key = "error") {
                            com.xiaobaotv.app.ui.components.ErrorState(
                                message = uiState.error.orEmpty(),
                                onRetry = { viewModel.loadHomeContent() },
                                modifier = Modifier.padding(vertical = 48.dp)
                            )
                        }
                    }

                    else -> {
                        if (uiState.heroItems.isNotEmpty()) {
                            item(key = "hero", contentType = "hero") {
                                HeroCarousel(
                                    items = uiState.heroItems,
                                    onVodClick = onVodClick,
                                    onPlayClick = onPlayClick
                                )
                            }
                        }

                        if (uiState.continueWatchingList.isNotEmpty()) {
                            item(key = "continueHeader", contentType = "header") {
                                SectionHeader(
                                    title = "继续观看",
                                    subtitle = "一键回到上次的进度"
                                )
                            }
                            item(key = "continueRail", contentType = "rail") {
                                ContinueWatchingRail(
                                    items = uiState.continueWatchingList,
                                    onResumeClick = onResumeClick
                                )
                            }
                        }

                        if (uiState.hotMovies.isNotEmpty()) {
                            item(key = "movieHeader", contentType = "header") {
                                SectionHeader(
                                    title = "热门电影",
                                    subtitle = "本周大家都在看"
                                )
                            }
                            item(key = "movieRail", contentType = "rail") {
                                VodRail(items = uiState.hotMovies, onVodClick = onVodClick)
                            }
                        }

                        if (uiState.hotTvSeries.isNotEmpty()) {
                            item(key = "tvHeader", contentType = "header") {
                                SectionHeader(
                                    title = "热门剧集",
                                    subtitle = "一口气追完的好剧"
                                )
                            }
                            item(key = "tvRail", contentType = "rail") {
                                VodRail(items = uiState.hotTvSeries, onVodClick = onVodClick)
                            }
                        }

                        if (uiState.hotAnime.isNotEmpty()) {
                            item(key = "animeHeader", contentType = "header") {
                                SectionHeader(
                                    title = "热门动漫",
                                    subtitle = "新番与经典一网打尽"
                                )
                            }
                            item(key = "animeRail", contentType = "rail") {
                                VodRail(items = uiState.hotAnime, onVodClick = onVodClick)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Header ─────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(
    windowSize: WindowSize,
    onSettingsClick: () -> Unit,
) {
    val gutter = screenGutter()
    val greeting = rememberGreeting()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = gutter, end = gutter, top = 6.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            GradientWordmark(
                style = if (windowSize == WindowSize.Compact) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.headlineLarge
                }
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$greeting，今天想看点什么？",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        GlassIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "设置与外观",
            onClick = onSettingsClick,
            size = 44.dp
        )
    }
}

@Composable
private fun rememberGreeting(): String = remember {
    when (LocalTime.now().hour) {
        in 0..5 -> "凌晨好"
        in 6..11 -> "早上好"
        in 12..13 -> "中午好"
        in 14..17 -> "下午好"
        else -> "晚上好"
    }
}

// ─── Hero carousel ─────────────────────────────────────────────────────────

@Composable
private fun HeroCarousel(
    items: List<VodContent>,
    onVodClick: (Int) -> Unit,
    onPlayClick: (Int) -> Unit,
) {
    val gutter = screenGutter()
    val windowSize = rememberWindowSize()
    val extras = LocalXiaobaoColors.current
    val heroHeight = when (windowSize) {
        WindowSize.Compact -> LayoutMetrics.CompactHeroHeight
        WindowSize.Medium -> LayoutMetrics.MediumHeroHeight
        WindowSize.Expanded -> LayoutMetrics.ExpandedHeroHeight
    }
    val startPage = remember(items.size) {
        val middle = Int.MAX_VALUE / 2
        middle - (middle % items.size)
    }
    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { Int.MAX_VALUE }
    )

    // Auto-advance unless the user is currently dragging.
    LaunchedEffect(pagerState, items.size) {
        while (true) {
            delay(5600L)
            if (!pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = gutter)
            .height(heroHeight)
            .clip(Radii.hero)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val vod = items[page % items.size]
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction

            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = vod.pic,
                    contentDescription = vod.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = pageOffset * size.width * 0.22f
                            scaleX = 1.12f
                            scaleY = 1.12f
                        }
                )

                // Scrims: bottom for text, soft top for the status area.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(extras.heroScrim))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.36f), Color.Transparent)
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    GradientBadge(text = "全网热播")
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = vod.name,
                        style = if (windowSize == WindowSize.Compact) {
                            MaterialTheme.typography.displaySmall
                        } else {
                            MaterialTheme.typography.displayMedium
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.graphicsLayer { alpha = (1f - kotlin.math.abs(pageOffset) * 0.9f).coerceIn(0f, 1f) }
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!vod.score.isNullOrBlank() && vod.score != "0.0") {
                            RatingBadge(
                                score = vod.score,
                                container = Color.White.copy(alpha = 0.16f)
                            )
                        }
                        val meta = listOfNotNull(
                            vod.year?.take(4)?.takeIf { it.isNotBlank() },
                            vod.area?.takeIf { it.isNotBlank() },
                            vod.typeName?.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        if (meta.isNotBlank()) {
                            Text(
                                text = meta,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (!vod.content.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = remember(vod.content) { plainSynopsis(vod.content) },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.graphicsLayer {
                                alpha = (1f - kotlin.math.abs(pageOffset)).coerceIn(0f, 1f)
                            }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GradientPillButton(
                            text = "立即播放",
                            icon = Icons.Rounded.PlayArrow,
                            onClick = { onPlayClick(vod.id) },
                            height = 48.dp
                        )
                        GlassPillButton(
                            text = "详情",
                            onClick = { onVodClick(vod.id) },
                            height = 48.dp,
                            contentColor = Color.White,
                            container = Color.Black.copy(alpha = 0.34f),
                            borderColor = Color.White.copy(alpha = 0.22f)
                        )
                    }
                }

                // Slide counter
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "${(pagerState.currentPage % items.size) + 1} / ${items.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Page indicator bars
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 22.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.26f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val selected = pagerState.currentPage % items.size
            repeat(items.size) { index ->
                val isSelected = index == selected
                Box(
                    modifier = Modifier
                        .height(3.5.dp)
                        .width(if (isSelected) 20.dp else 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSelected) Color.White
                            else Color.White.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
}

// ─── Rails ──────────────────────────────────────────────────────────────────

@Composable
private fun VodRail(
    items: List<VodContent>,
    onVodClick: (Int) -> Unit,
) {
    val gutter = screenGutter()
    val cardWidth = railPosterWidth()
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = gutter),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(
            items = items,
            key = { it.id },
            contentType = { "vod" }
        ) { vod ->
            VodPosterCard(
                vod = vod,
                onClick = onVodClick,
                modifier = Modifier.width(cardWidth)
            )
        }
    }
}

@Composable
private fun ContinueWatchingRail(
    items: List<WatchHistoryItem>,
    onResumeClick: (Int, Int) -> Unit,
) {
    val gutter = screenGutter()
    val cardWidth = railWideCardWidth()
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = gutter),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(items = items, key = { it.vodId }, contentType = { "history" }) { history ->
            LandscapeVodCard(
                title = history.name,
                pic = history.pic,
                onClick = { onResumeClick(history.vodId, history.episodeIndex + 1) },
                modifier = Modifier.width(cardWidth),
                episodeLabel = history.episodeName,
                subtitle = if (history.durationMs > 0) {
                    "已看 ${(history.positionMs * 100 / history.durationMs).toInt().coerceIn(0, 100)}%"
                } else {
                    "继续观看"
                },
                progress = if (history.durationMs > 0) {
                    history.positionMs.toFloat() / history.durationMs
                } else {
                    null
                }
            )
        }
    }
}

// ─── Skeletons ──────────────────────────────────────────────────────────────

@Composable
private fun HeroSkeleton() {
    val gutter = screenGutter()
    val windowSize = rememberWindowSize()
    val heroHeight = when (windowSize) {
        WindowSize.Compact -> LayoutMetrics.CompactHeroHeight
        WindowSize.Medium -> LayoutMetrics.MediumHeroHeight
        WindowSize.Expanded -> LayoutMetrics.ExpandedHeroHeight
    }
    Column(modifier = Modifier.padding(horizontal = gutter)) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(heroHeight),
            shape = Radii.hero
        )
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun RailSkeleton() {
    val gutter = screenGutter()
    val cardWidth = railPosterWidth()
    Column {
        ShimmerBox(
            modifier = Modifier
                .padding(horizontal = gutter)
                .width(120.dp)
                .height(20.dp),
            shape = RoundedCornerShape(6.dp)
        )
        Spacer(Modifier.height(14.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(5) {
                PosterCardSkeleton(modifier = Modifier.width(cardWidth))
            }
        }
        Spacer(Modifier.height(22.dp))
    }
}

@Composable
private fun ContinueSkeleton() {
    val gutter = screenGutter()
    val cardWidth = railWideCardWidth()
    LazyRow(
        contentPadding = PaddingValues(horizontal = gutter),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(3) {
            LandscapeCardSkeleton(modifier = Modifier.width(cardWidth))
        }
    }
}
