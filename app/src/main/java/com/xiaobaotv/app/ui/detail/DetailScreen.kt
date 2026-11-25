package com.xiaobaotv.app.ui.detail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.xiaobaotv.app.domain.model.Episode
import com.xiaobaotv.app.domain.model.VideoSource
import com.xiaobaotv.app.domain.model.VodContent
import com.xiaobaotv.app.domain.model.WatchHistoryItem
import com.xiaobaotv.app.ui.components.ContentContainer
import com.xiaobaotv.app.ui.components.EpisodeChip
import com.xiaobaotv.app.ui.components.ErrorState
import com.xiaobaotv.app.ui.components.ExpandableSynopsis
import com.xiaobaotv.app.ui.components.GlassPillButton
import com.xiaobaotv.app.ui.components.GlassSurface
import com.xiaobaotv.app.ui.components.GradientPillButton
import com.xiaobaotv.app.ui.components.MetaChip
import com.xiaobaotv.app.ui.components.ScrimPill
import com.xiaobaotv.app.ui.components.SelectChip
import com.xiaobaotv.app.ui.components.ShimmerBox
import com.xiaobaotv.app.ui.components.TintedIconButton
import com.xiaobaotv.app.ui.components.WindowSize
import com.xiaobaotv.app.ui.components.plainSynopsis
import com.xiaobaotv.app.ui.components.rememberWindowSize
import com.xiaobaotv.app.ui.components.screenGutter
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors
import com.xiaobaotv.app.ui.theme.Motion
import com.xiaobaotv.app.ui.theme.Radii

// ─────────────────────────────────────────────────────────────────────────────
// Detail — cinematic hero over an aurora canvas.
//
// Phone   : full-bleed parallax backdrop, poster/title overlapping the artwork,
//           glass metadata chips, gradient CTA, frosted synopsis card and a
//           lazily rendered episode grid.
// Tablet  : the same backdrop at a shorter height, then a refined two-pane
//           layout — artwork + actions on the left, story + episodes on the
//           right — that scrolls as a single page.
//
// The backdrop is drawn behind the status bar and parallaxes in the draw phase
// only, so scrolling never triggers recomposition of the hero.
// ─────────────────────────────────────────────────────────────────────────────

private const val EPISODE_GROUP_SIZE = 100

private val PhoneBackdropHeight = 384.dp
private val TabletBackdropHeight = 344.dp

/** Extra artwork height used to hide the gap created by the parallax shift. */
private val BackdropParallaxOverhang = 220.dp

private enum class DetailPhase { Loading, Error, Content }

@Composable
fun DetailScreen(
    vodId: Int,
    onPlayClick: (Int, Int) -> Unit,
    onBackClick: () -> Unit,
    viewModel: DetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val windowSize = rememberWindowSize()

    LaunchedEffect(vodId) {
        viewModel.loadDetail(vodId)
    }

    val phase = when {
        uiState.vod != null -> DetailPhase.Content
        uiState.error != null -> DetailPhase.Error
        else -> DetailPhase.Loading
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Crossfade(
            targetState = phase,
            modifier = Modifier.fillMaxSize(),
            animationSpec = tween(Motion.Medium.toInt()),
            label = "detailPhase"
        ) { current ->
            when (current) {
                DetailPhase.Content -> {
                    val vod = uiState.vod
                    if (vod != null) {
                        if (windowSize == WindowSize.Compact) {
                            PhoneDetailContent(
                                vod = vod,
                                uiState = uiState,
                                onPlayClick = onPlayClick,
                                onRetrySources = { viewModel.loadDetail(vodId) }
                            )
                        } else {
                            TabletDetailContent(
                                vod = vod,
                                uiState = uiState,
                                onPlayClick = onPlayClick,
                                onRetrySources = { viewModel.loadDetail(vodId) }
                            )
                        }
                    }
                }

                DetailPhase.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorState(
                            message = uiState.error.orEmpty(),
                            onRetry = { viewModel.loadDetail(vodId) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                DetailPhase.Loading -> DetailSkeleton(
                    backdropHeight = if (windowSize == WindowSize.Compact) {
                        PhoneBackdropHeight
                    } else {
                        TabletBackdropHeight
                    }
                )
            }
        }

        // Floating chrome — always reachable, even while loading or on error.
        TintedIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "返回",
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 14.dp, top = 8.dp)
        )
    }
}

// ─── Phone layout ───────────────────────────────────────────────────────────

@Composable
private fun PhoneDetailContent(
    vod: VodContent,
    uiState: DetailUiState,
    onPlayClick: (Int, Int) -> Unit,
    onRetrySources: () -> Unit,
) {
    val gutter = screenGutter()
    val listState = rememberLazyListState()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val selection = rememberEpisodeSelection(uiState.sources)
    val episodes = uiState.sources.getOrNull(selection.sourceIndex)?.episodes.orEmpty()
    val groups = remember(episodes) { episodes.chunked(EPISODE_GROUP_SIZE) }
    val visibleEpisodes = groups.getOrNull(selection.groupIndex).orEmpty()
    val columns = 4
    val rows = remember(visibleEpisodes, columns) { visibleEpisodes.chunked(columns) }
    val currentEpisodeNumber = uiState.historyItem?.episodeIndex?.plus(1)

    val showEpisodes = uiState.sources.isNotEmpty() ||
        uiState.sourcesError != null ||
        uiState.isLoading

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = navBottom + 36.dp)
    ) {
        item(key = "hero", contentType = "hero") {
            PhoneHero(
                vod = vod,
                uiState = uiState,
                listState = listState,
                onPlayClick = onPlayClick
            )
        }

        item(key = "synopsis", contentType = "synopsis") {
            SynopsisCard(
                vod = vod,
                modifier = Modifier.padding(start = gutter, end = gutter, top = 22.dp)
            )
        }

        if (showEpisodes) {
            item(key = "episodeHeader", contentType = "episodeHeader") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 26.dp)
                ) {
                    SectionLabel(
                        title = "选集播放",
                        subtitle = episodeSubtitle(
                            totalCount = episodes.size,
                            history = uiState.historyItem,
                            sourceIndex = selection.sourceIndex
                        ),
                        modifier = Modifier.padding(horizontal = gutter)
                    )
                    Spacer(Modifier.height(12.dp))

                    if (uiState.sources.size > 1) {
                        Text(
                            text = "播放线路",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = gutter)
                        )
                        Spacer(Modifier.height(7.dp))
                        SourceChipsRow(
                            sources = uiState.sources,
                            selectedIndex = selection.sourceIndex,
                            onSelect = { selection.selectSource(it) },
                            contentPadding = PaddingValues(horizontal = gutter)
                        )
                        Spacer(Modifier.height(11.dp))
                    }

                    if (groups.size > 1) {
                        GroupChipsRow(
                            groups = groups,
                            selectedIndex = selection.groupIndex,
                            onSelect = { selection.selectGroup(it) },
                            contentPadding = PaddingValues(horizontal = gutter)
                        )
                        Spacer(Modifier.height(11.dp))
                    }

                    when {
                        uiState.sources.isEmpty() && uiState.sourcesError != null -> {
                            SourcesErrorCard(
                                message = uiState.sourcesError.orEmpty(),
                                onRetry = onRetrySources,
                                modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp)
                            )
                        }

                        uiState.sources.isEmpty() && uiState.isLoading -> {
                            EpisodeChipsSkeleton(
                                gutter = gutter,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        uiState.sources.isEmpty() -> {
                            Text(
                                text = "暂无可用播放源",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = gutter, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            items(
                items = rows,
                key = { row -> row.firstOrNull()?.index ?: 0 },
                contentType = { "episodeRow" }
            ) { row ->
                EpisodeGridRow(
                    episodes = row,
                    columns = columns,
                    currentEpisodeNumber = currentEpisodeNumber,
                    onEpisodeClick = { episode -> onPlayClick(vod.id, episode.index) },
                    modifier = Modifier.padding(horizontal = gutter, vertical = 4.dp)
                )
            }

            item(key = "episodeBottom", contentType = "spacer") {
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun PhoneHero(
    vod: VodContent,
    uiState: DetailUiState,
    listState: LazyListState,
    onPlayClick: (Int, Int) -> Unit,
) {
    val gutter = screenGutter()
    val overlap = 96.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        HeroBackdrop(
            pic = vod.pic,
            height = PhoneBackdropHeight,
            contentDescription = null,
            scrollOffset = { listState.firstVisibleItemScrollOffset.toFloat() }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = gutter,
                    end = gutter,
                    top = PhoneBackdropHeight - overlap
                )
        ) {
            Row(verticalAlignment = Alignment.Top) {
                PosterArtwork(
                    vod = vod,
                    modifier = Modifier.width(112.dp)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vod.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(9.dp))
                    MetaChips(vod = vod)
                }
            }

            if (!vod.actor.isNullOrBlank() || !vod.director.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                CreditLines(vod = vod)
            }

            Spacer(Modifier.height(18.dp))

            PlayActions(
                uiState = uiState,
                vodId = vod.id,
                onPlayClick = onPlayClick
            )
        }
    }
}

// ─── Tablet layout ──────────────────────────────────────────────────────────

@Composable
private fun TabletDetailContent(
    vod: VodContent,
    uiState: DetailUiState,
    onPlayClick: (Int, Int) -> Unit,
    onRetrySources: () -> Unit,
) {
    val gutter = screenGutter()
    val scrollState = rememberScrollState()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val selection = rememberEpisodeSelection(uiState.sources)
    val episodes = uiState.sources.getOrNull(selection.sourceIndex)?.episodes.orEmpty()
    val groups = remember(episodes) { episodes.chunked(EPISODE_GROUP_SIZE) }
    val visibleEpisodes = groups.getOrNull(selection.groupIndex).orEmpty()
    val currentEpisodeNumber = uiState.historyItem?.episodeIndex?.plus(1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            HeroBackdrop(
                pic = vod.pic,
                height = TabletBackdropHeight,
                contentDescription = null,
                scrollOffset = { scrollState.value.toFloat() }
            )

            ContentContainer(modifier = Modifier.fillMaxWidth()) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = gutter,
                            end = gutter,
                            top = TabletBackdropHeight - 118.dp
                        )
                ) {
                    val available = maxWidth
                    val posterWidth = when {
                        available < 560.dp -> 168.dp
                        available < 860.dp -> 214.dp
                        else -> 284.dp
                    }
                    val paneGap = if (available < 720.dp) 22.dp else 34.dp
                    val rightWidth = (available - posterWidth - paneGap).coerceAtLeast(180.dp)
                    val columns = when {
                        rightWidth < 300.dp -> 3
                        rightWidth < 380.dp -> 4
                        rightWidth < 480.dp -> 5
                        rightWidth < 640.dp -> 6
                        else -> 8
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(paneGap)
                    ) {
                        Column(modifier = Modifier.width(posterWidth)) {
                            PosterArtwork(
                                vod = vod,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(16.dp))
                            MetaChips(vod = vod)
                        }

                        Column(modifier = Modifier.width(rightWidth)) {
                            Text(
                                text = vod.name,
                                style = if (rightWidth < 430.dp) {
                                    MaterialTheme.typography.headlineMedium
                                } else {
                                    MaterialTheme.typography.displaySmall
                                },
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            CreditLines(vod = vod)
                            Spacer(Modifier.height(16.dp))
                            // Keep the primary CTA above the fold on short
                            // landscape tablets: it belongs next to the title.
                            PlayActions(
                                uiState = uiState,
                                vodId = vod.id,
                                onPlayClick = onPlayClick,
                                stacked = true
                            )
                            Spacer(Modifier.height(20.dp))
                            SynopsisCard(
                                vod = vod,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(Modifier.height(24.dp))
                            EpisodeSectionInline(
                                sources = uiState.sources,
                                sourcesError = uiState.sourcesError,
                                isLoading = uiState.isLoading,
                                selection = selection,
                                groups = groups,
                                visibleEpisodes = visibleEpisodes,
                                totalCount = episodes.size,
                                currentEpisodeNumber = currentEpisodeNumber,
                                columns = columns,
                                history = uiState.historyItem,
                                onRetrySources = onRetrySources,
                                onEpisodeClick = { episode -> onPlayClick(vod.id, episode.index) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(navBottom + 44.dp))
    }
}

// ─── Shared building blocks ─────────────────────────────────────────────────

@Composable
private fun HeroBackdrop(
    pic: String,
    height: Dp,
    contentDescription: String?,
    scrollOffset: () -> Float,
    modifier: Modifier = Modifier,
) {
    val extras = LocalXiaobaoColors.current
    val density = LocalDensity.current
    val heightPx = with(density) { height.toPx() }
    val overhangPx = with(density) { BackdropParallaxOverhang.toPx() }
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.4f
    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
    ) {
        AsyncImage(
            model = pic,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                // requiredHeight ignores the parent's max height, so the
                // parallax overhang is not squashed (which previously cropped
                // the artwork to its top third).
                .requiredHeight(height + BackdropParallaxOverhang)
                .graphicsLayer {
                    // Draw-phase parallax: the artwork drifts at ~42% of the
                    // scroll speed while the overhang keeps the frame filled.
                    val offset = scrollOffset().coerceIn(0f, heightPx)
                    translationY = -overhangPx + offset * 0.42f
                    scaleX = 1.03f
                    scaleY = 1.03f
                }
        )

        // Top scrim so status icons and the floating back button stay legible.
        // In the light theme the system renders dark status icons, so the scrim
        // flips to a soft porcelain wash instead of black.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .background(
                    Brush.verticalGradient(
                        if (isDarkTheme) {
                            listOf(Color.Black.copy(alpha = 0.58f), Color.Transparent)
                        } else {
                            listOf(Color.White.copy(alpha = 0.62f), Color.Transparent)
                        }
                    )
                )
        )

        // Cinematic fade into the page background. The fade starts late so the
        // artwork stays vivid; light theme fades to the porcelain canvas, dark
        // theme to the midnight canvas.
        val fadeStartPx = heightPx * (if (isDarkTheme) 0.30f else 0.42f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, background),
                        startY = fadeStartPx,
                        endY = heightPx
                    )
                )
        )

        // Extra darkening as the artwork scrolls away.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val fraction = (scrollOffset() / heightPx).coerceIn(0f, 1f)
                    if (fraction > 0.001f) {
                        drawRect(Color.Black.copy(alpha = 0.42f * fraction))
                    }
                }
        )
    }
}

@Composable
private fun PosterArtwork(
    vod: VodContent,
    modifier: Modifier = Modifier,
) {
    val extras = LocalXiaobaoColors.current
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .shadow(elevation = 20.dp, shape = Radii.poster, clip = false)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(extras.posterScrim))
        )
        if (!vod.remarks.isNullOrBlank()) {
            ScrimPill(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
                color = Color.Black.copy(alpha = 0.56f)
            ) {
                Text(
                    text = vod.remarks.orEmpty().substringBefore("/").trim(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun MetaChips(
    vod: VodContent,
    modifier: Modifier = Modifier,
) {
    val extras = LocalXiaobaoColors.current
    val score = vod.score?.takeIf { it.isNotBlank() && it != "0.0" && it != "0" }
    val year = vod.year?.take(4)?.takeIf { it.isNotBlank() }
    val area = vod.area?.takeIf { it.isNotBlank() }
    val typeName = vod.typeName?.takeIf { it.isNotBlank() }
    // The detail page sometimes reports "更新至第06集/2026-11-14 00:00:34";
    // the chip only needs the human part.
    val remarks = vod.remarks
        ?.substringBefore("/")
        ?.trim()
        ?.takeIf { it.isNotBlank() }

    ChipWrap(modifier = modifier.fillMaxWidth()) {
        if (score != null) {
            MetaChip(
                text = "★ $score",
                contentColor = extras.rating
            )
        }
        if (year != null) {
            MetaChip(text = year)
        }
        if (area != null) {
            MetaChip(text = area)
        }
        if (typeName != null) {
            MetaChip(text = typeName)
        }
        if (remarks != null) {
            MetaChip(
                text = remarks,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun CreditLines(
    vod: VodContent,
    modifier: Modifier = Modifier,
) {
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier.fillMaxWidth()) {
        if (!vod.actor.isNullOrBlank()) {
            Text(
                text = "主演 · ${vod.actor}",
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!vod.director.isNullOrBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = "导演 · ${vod.director}",
                style = MaterialTheme.typography.bodySmall,
                color = onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlayActions(
    uiState: DetailUiState,
    vodId: Int,
    onPlayClick: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
) {
    val history = uiState.historyItem
    val primaryLabel = remember(history) { primaryPlayLabel(history) }
    val targetEpisode = history?.episodeIndex?.plus(1) ?: 1

    if (stacked) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GradientPillButton(
                text = primaryLabel,
                onClick = { onPlayClick(vodId, targetEpisode) },
                icon = Icons.Rounded.PlayArrow,
                modifier = Modifier.fillMaxWidth(),
                contentPaddingHorizontal = 16.dp
            )
            GlassPillButton(
                text = "从第 1 集开始",
                onClick = { onPlayClick(vodId, 1) },
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp
            )
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GradientPillButton(
                text = primaryLabel,
                onClick = { onPlayClick(vodId, targetEpisode) },
                icon = Icons.Rounded.PlayArrow,
                modifier = Modifier.weight(1f),
                contentPaddingHorizontal = 16.dp
            )
            GlassPillButton(
                text = "从第 1 集开始",
                onClick = { onPlayClick(vodId, 1) }
            )
        }
    }
}

@Composable
private fun SynopsisCard(
    vod: VodContent,
    modifier: Modifier = Modifier,
) {
    val extras = LocalXiaobaoColors.current
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = Radii.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(15.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(extras.brandGradient))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "剧情简介",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            ExpandableSynopsis(
                text = remember(vod.content) { plainSynopsis(vod.content) },
                collapsedLines = 5
            )
        }
    }
}

@Composable
private fun SectionLabel(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val extras = LocalXiaobaoColors.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(extras.brandGradient))
        )
        Spacer(Modifier.width(9.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─── Episode selection ──────────────────────────────────────────────────────

/**
 * Small holder for the two levels of episode selection (source → 100-episode
 * group). Kept as a plain state holder so both the phone and the tablet layout
 * share exactly the same selection semantics as the previous implementation.
 */
private class EpisodeSelection {
    var sourceIndex by mutableIntStateOf(0)
    var groupIndex by mutableIntStateOf(0)

    fun selectSource(index: Int) {
        sourceIndex = index
        groupIndex = 0
    }

    fun selectGroup(index: Int) {
        groupIndex = index
    }
}

@Composable
private fun rememberEpisodeSelection(sources: List<VideoSource>): EpisodeSelection =
    remember(sources) { EpisodeSelection() }

@Composable
private fun SourceChipsRow(
    sources: List<VideoSource>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items = sources, key = { index, _ -> "source-$index" }) { index, source ->
            SelectChip(
                text = source.name.ifBlank { "线路 ${index + 1}" },
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.heightIn(min = 44.dp)
            )
        }
    }
}

@Composable
private fun GroupChipsRow(
    groups: List<List<Episode>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    contentPadding: PaddingValues,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(items = groups, key = { index, _ -> "group-$index" }) { index, group ->
            val first = group.firstOrNull()?.index ?: 0
            val last = group.lastOrNull()?.index ?: first
            SelectChip(
                text = if (first == last) "$first" else "$first-$last",
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.heightIn(min = 44.dp)
            )
        }
    }
}

@Composable
private fun EpisodeGridRow(
    episodes: List<Episode>,
    columns: Int,
    currentEpisodeNumber: Int?,
    onEpisodeClick: (Episode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        episodes.forEach { episode ->
            EpisodeChip(
                text = episode.name,
                selected = currentEpisodeNumber != null && episode.index == currentEpisodeNumber,
                onClick = { onEpisodeClick(episode) },
                modifier = Modifier.weight(1f)
            )
        }
        repeat((columns - episodes.size).coerceAtLeast(0)) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun EpisodeSectionInline(
    sources: List<VideoSource>,
    sourcesError: String?,
    isLoading: Boolean,
    selection: EpisodeSelection,
    groups: List<List<Episode>>,
    visibleEpisodes: List<Episode>,
    totalCount: Int,
    currentEpisodeNumber: Int?,
    columns: Int,
    history: WatchHistoryItem?,
    onRetrySources: () -> Unit,
    onEpisodeClick: (Episode) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(
            title = "选集播放",
            subtitle = episodeSubtitle(
                totalCount = totalCount,
                history = history,
                sourceIndex = selection.sourceIndex
            )
        )
        Spacer(Modifier.height(12.dp))

        when {
            sources.isEmpty() && sourcesError != null -> {
                SourcesErrorCard(
                    message = sourcesError,
                    onRetry = onRetrySources,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            sources.isEmpty() && isLoading -> {
                EpisodeChipsSkeleton(gutter = 0.dp)
            }

            sources.isEmpty() -> {
                Text(
                    text = "暂无可用播放源",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            else -> {
                if (sources.size > 1) {
                    Text(
                        text = "播放线路",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(7.dp))
                    SourceChipsRow(
                        sources = sources,
                        selectedIndex = selection.sourceIndex,
                        onSelect = { selection.selectSource(it) },
                        contentPadding = PaddingValues(0.dp)
                    )
                    Spacer(Modifier.height(11.dp))
                }
                if (groups.size > 1) {
                    GroupChipsRow(
                        groups = groups,
                        selectedIndex = selection.groupIndex,
                        onSelect = { selection.selectGroup(it) },
                        contentPadding = PaddingValues(0.dp)
                    )
                    Spacer(Modifier.height(11.dp))
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    visibleEpisodes.chunked(columns).forEach { row ->
                        EpisodeGridRow(
                            episodes = row,
                            columns = columns,
                            currentEpisodeNumber = currentEpisodeNumber,
                            onEpisodeClick = onEpisodeClick,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcesErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = Radii.card
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            GlassPillButton(
                text = "重试",
                onClick = onRetry,
                height = 44.dp
            )
        }
    }
}

@Composable
private fun EpisodeChipsSkeleton(
    gutter: Dp,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = gutter)
    ) {
        repeat(2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) {
                    ShimmerBox(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = MaterialTheme.shapes.small
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Skeleton ───────────────────────────────────────────────────────────────

@Composable
private fun DetailSkeleton(backdropHeight: Dp) {
    val gutter = screenGutter()
    Column(modifier = Modifier.fillMaxSize()) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(backdropHeight),
            shape = RoundedCornerShape(0.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = gutter, end = gutter, top = 20.dp)
        ) {
            Row {
                ShimmerBox(
                    modifier = Modifier
                        .width(112.dp)
                        .aspectRatio(2f / 3f),
                    shape = Radii.poster
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(26.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.58f)
                            .height(26.dp),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(58.dp)
                                .height(22.dp),
                            shape = RoundedCornerShape(50)
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .width(48.dp)
                                .height(22.dp),
                            shape = RoundedCornerShape(50)
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .width(56.dp)
                                .height(22.dp),
                            shape = RoundedCornerShape(50)
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = Radii.button
            )
            Spacer(Modifier.height(22.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp),
                shape = Radii.card
            )
            Spacer(Modifier.height(26.dp))
            ShimmerBox(
                modifier = Modifier
                    .width(132.dp)
                    .height(22.dp),
                shape = RoundedCornerShape(6.dp)
            )
            Spacer(Modifier.height(14.dp))
            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(4) {
                        ShimmerBox(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = MaterialTheme.shapes.small
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────────────────

/**
 * Wraps its children into as many rows as needed. Used for metadata chips so a
 * long region/type name never overflows the poster column on tablets.
 */
@Composable
private fun ChipWrap(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    verticalSpacing: Dp = 6.dp,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val hSpace = horizontalSpacing.roundToPx()
        val vSpace = verticalSpacing.roundToPx()

        val placeables = measurables.map { it.measure(Constraints(maxWidth = maxWidth)) }
        val positions = ArrayList<IntArray>(placeables.size)

        var x = 0
        var y = 0
        var rowHeight = 0
        placeables.forEach { placeable ->
            if (x > 0 && x + placeable.width > maxWidth) {
                x = 0
                y += rowHeight + vSpace
                rowHeight = 0
            }
            positions.add(intArrayOf(x, y))
            x += placeable.width + hSpace
            rowHeight = maxOf(rowHeight, placeable.height)
        }

        val totalHeight = if (placeables.isEmpty()) 0 else y + rowHeight
        layout(maxWidth, totalHeight.coerceAtMost(constraints.maxHeight)) {
            placeables.forEachIndexed { index, placeable ->
                val position = positions[index]
                placeable.place(position[0], position[1])
            }
        }
    }
}

private fun primaryPlayLabel(history: WatchHistoryItem?): String {
    if (history == null) return "立即播放"
    val percent = if (history.durationMs > 0L) {
        (history.positionMs * 100 / history.durationMs).toInt().coerceIn(0, 100)
    } else {
        0
    }
    return if (percent > 0) {
        "继续观看 ${history.episodeName} ($percent%)"
    } else {
        "继续观看 ${history.episodeName}"
    }
}

private fun episodeSubtitle(
    totalCount: Int,
    history: WatchHistoryItem?,
    sourceIndex: Int,
): String? {
    if (totalCount <= 0) return null
    val base = "共 $totalCount 集"
    if (history == null || history.sourceIndex != sourceIndex || history.durationMs <= 0L) {
        return base
    }
    val percent = (history.positionMs * 100 / history.durationMs).toInt().coerceIn(0, 100)
    return "$base · 上次看到 ${history.episodeName} $percent%"
}
