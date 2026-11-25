package com.xiaobaotv.app.ui.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.xiaobaotv.app.domain.model.SearchHistoryItem
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
import com.xiaobaotv.app.ui.components.VodPosterCard
import com.xiaobaotv.app.ui.components.WindowSize
import com.xiaobaotv.app.ui.components.XiaobaoSpinner
import com.xiaobaotv.app.ui.components.posterGridColumns
import com.xiaobaotv.app.ui.components.pressScale
import com.xiaobaotv.app.ui.components.rememberWindowSize
import com.xiaobaotv.app.ui.components.screenGutter
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors
import com.xiaobaotv.app.ui.theme.Motion
import com.xiaobaotv.app.ui.theme.Radii
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HotTags = listOf(
    "庆余年", "墨雨云间", "玫瑰的故事", "三体", "繁花", "赘婿", "狂飙", "漫长的季节"
)

private val SearchTips: List<Pair<ImageVector, String>> = listOf(
    Icons.Rounded.Movie to "直接输入片名，例如「庆余年」「三体」",
    Icons.Rounded.LocalFireDepartment to "搜索演员或导演的名字也能找到作品",
    Icons.Rounded.AutoAwesome to "关键词越短，搜到的结果越丰富"
)

/**
 * Search: a frosted, focus-reactive search field stays pinned at the top while
 * the body crossfades between the recent/hot discovery panels and an adaptive
 * results grid.
 *
 * Behaviour preserved from the previous implementation: debounced querying via
 * [SearchViewModel.onQueryChange], explicit submit for the IME action, recent
 * history with per-item delete and clear-all, and incremental "load more" with
 * retry through [SearchViewModel.loadNextPage] / [SearchViewModel.retryLoadNextPage].
 */
@Composable
fun SearchScreen(
    onVodClick: (Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val windowSize = rememberWindowSize()
    val isCompact = windowSize == WindowSize.Compact
    val useTwoColumnIdle = windowSize == WindowSize.Expanded
    val gutter = screenGutter()
    val bottomClearance = if (isCompact) LayoutMetrics.BottomContentClearance else 56.dp
    val onVodClickRemembered = remember(onVodClick) { { id: Int -> onVodClick(id) } }

    val submitSearch: (String) -> Unit = { raw ->
        viewModel.submitSearch(raw)
        keyboard?.hide()
        focusManager.clearFocus()
    }
    val resetSearch: () -> Unit = {
        viewModel.clearSearch()
        keyboard?.hide()
        focusManager.clearFocus()
    }

    val phase = when {
        uiState.isLoading -> SearchPhase.Loading
        uiState.error != null -> SearchPhase.Error
        uiState.query.isBlank() -> SearchPhase.Idle
        uiState.results.isNotEmpty() -> SearchPhase.Results
        else -> SearchPhase.Empty
    }

    AuroraBackground(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
        ) {
            ContentContainer(maxWidth = 1500.dp) {
                Column {
                    ScreenHeader(
                        title = "发现精彩",
                        subtitle = "搜索电影、剧集与动漫"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = gutter, end = gutter, bottom = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AuroraSearchField(
                            query = uiState.query,
                            onQueryChange = { viewModel.onQueryChange(it) },
                            onSubmit = submitSearch,
                            onClear = { viewModel.clearSearch() },
                            modifier = Modifier
                                .widthIn(max = 680.dp)
                                .fillMaxWidth()
                        )
                    }
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
                    label = "searchPhase"
                ) { target ->
                    ContentContainer(modifier = Modifier.fillMaxSize(), maxWidth = 1500.dp) {
                        when (target) {
                            SearchPhase.Idle -> SearchIdleContent(
                                recentSearches = uiState.recentSearches,
                                hotTags = HotTags,
                                twoColumn = useTwoColumnIdle,
                                bottomClearance = bottomClearance,
                                onSearch = submitSearch,
                                onDeleteRecent = viewModel::deleteRecentSearch,
                                onClearRecent = viewModel::clearRecentSearches
                            )

                            SearchPhase.Loading -> SearchSkeletonGrid(
                                bottomClearance = bottomClearance
                            )

                            SearchPhase.Error -> SearchErrorPane(
                                onRetry = { submitSearch(uiState.query) }
                            )

                            SearchPhase.Empty -> SearchEmptyPane(onClear = resetSearch)

                            SearchPhase.Results -> SearchResultsGrid(
                                query = uiState.query,
                                results = uiState.results,
                                hasMorePages = uiState.hasMorePages,
                                isLoadingMore = uiState.isLoadingMore,
                                loadMoreError = uiState.loadMoreError,
                                currentPage = uiState.currentPage,
                                bottomClearance = bottomClearance,
                                onVodClick = onVodClickRemembered,
                                onClear = resetSearch,
                                onLoadMore = viewModel::loadNextPage,
                                onRetryLoadMore = viewModel::retryLoadNextPage
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class SearchPhase { Idle, Loading, Results, Empty, Error }

// ─── Search field ───────────────────────────────────────────────────────────

@Composable
private fun AuroraSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extras = LocalXiaobaoColors.current
    val primary = MaterialTheme.colorScheme.primary
    val onVariant = MaterialTheme.colorScheme.onSurfaceVariant

    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    // Kept as State (not delegated) so the halo animation invalidates the draw
    // phase only instead of recomposing the whole field every frame.
    val focusAmountState = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(Motion.Medium.toInt(), easing = Motion.Standard),
        label = "searchFocus"
    )
    val containerColor by animateColorAsState(
        targetValue = if (focused) extras.glassFillStrong else extras.glassFill,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "searchContainer"
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) primary.copy(alpha = 0.55f) else extras.glassBorder,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "searchBorder"
    )
    val leadingTint by animateColorAsState(
        targetValue = if (focused) primary else onVariant,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "searchLeading"
    )
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(containerColor)
            .drawBehind {
                val amount = focusAmountState.value
                if (amount > 0.01f) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                primary.copy(alpha = 0.16f * amount),
                                extras.glowSecondary.copy(alpha = 0.12f * amount)
                            )
                        )
                    )
                }
            }
            .border(1.5.dp, borderColor, shape)
            .pointerInput(Unit) {
                detectTapGestures { focusRequester.requestFocus() }
            }
            .padding(start = 16.dp, end = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = leadingTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { state -> focused = state.isFocused },
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            singleLine = true,
            cursorBrush = SolidColor(primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit(query) }),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = "搜索你想看的",
                            style = MaterialTheme.typography.bodyLarge,
                            color = onVariant.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                    innerTextField()
                }
            }
        )
        AnimatedVisibility(
            visible = query.isNotEmpty(),
            enter = fadeIn(tween(Motion.Fast.toInt())) +
                scaleIn(
                    animationSpec = tween(Motion.Fast.toInt()),
                    initialScale = 0.6f
                ) +
                expandHorizontally(
                    animationSpec = tween(Motion.Fast.toInt()),
                    expandFrom = Alignment.End
                ),
            exit = fadeOut(tween(Motion.Instant.toInt())) +
                scaleOut(
                    animationSpec = tween(Motion.Instant.toInt()),
                    targetScale = 0.6f
                ) +
                shrinkHorizontally(
                    animationSpec = tween(Motion.Fast.toInt()),
                    shrinkTowards = Alignment.End
                )
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClear
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "清除搜索内容",
                    tint = onVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        SearchSubmitButton(
            enabled = query.isNotBlank(),
            onClick = { onSubmit(query) }
        )
    }
}

@Composable
private fun SearchSubmitButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val extras = LocalXiaobaoColors.current
    val interaction = remember { MutableInteractionSource() }
    val tintAlphaState = animateFloatAsState(
        targetValue = if (enabled) 1f else 0.5f,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "submitAlpha"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer { alpha = tintAlphaState.value }
            .pressScale(interaction, pressedScale = 0.9f)
            .clip(CircleShape)
            .background(Brush.linearGradient(extras.brandGradient))
            .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.ArrowUpward,
            contentDescription = "搜索",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

// ─── Idle: recent / hot discovery ───────────────────────────────────────────

@Composable
private fun SearchIdleContent(
    recentSearches: List<SearchHistoryItem>,
    hotTags: List<String>,
    twoColumn: Boolean,
    bottomClearance: Dp,
    onSearch: (String) -> Unit,
    onDeleteRecent: (String) -> Unit,
    onClearRecent: () -> Unit
) {
    val gutter = screenGutter()
    val scrollState = rememberScrollState()
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(90L)
        revealed = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = gutter,
                end = gutter,
                top = 4.dp,
                bottom = bottomClearance
            )
    ) {
        if (twoColumn) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (recentSearches.isNotEmpty()) {
                        SearchGlassPanel {
                            RecentSearchSection(
                                recentSearches = recentSearches,
                                onSearch = onSearch,
                                onDeleteRecent = onDeleteRecent,
                                onClearRecent = onClearRecent
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    SearchGlassPanel {
                        HotSearchSection(
                            hotTags = hotTags,
                            revealed = revealed,
                            onSearch = onSearch
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    SearchGlassPanel {
                        SearchTipsPanel()
                    }
                }
            }
        } else {
            if (recentSearches.isNotEmpty()) {
                RecentSearchSection(
                    recentSearches = recentSearches,
                    onSearch = onSearch,
                    onDeleteRecent = onDeleteRecent,
                    onClearRecent = onClearRecent
                )
                Spacer(Modifier.height(28.dp))
            }
            HotSearchSection(
                hotTags = hotTags,
                revealed = revealed,
                onSearch = onSearch
            )
        }
    }
}

@Composable
private fun SearchGlassPanel(content: @Composable () -> Unit) {
    val extras = LocalXiaobaoColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radii.card)
            .background(extras.glassFill)
            .border(1.dp, extras.glassBorder, Radii.card)
            .padding(horizontal = 18.dp, vertical = 17.dp)
    ) {
        content()
    }
}

@Composable
private fun SearchSectionHeader(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val extras = LocalXiaobaoColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(if (subtitle != null) 26.dp else 18.dp)
                .clip(Radii.chip)
                .background(Brush.verticalGradient(extras.brandGradient))
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecentSearchSection(
    recentSearches: List<SearchHistoryItem>,
    onSearch: (String) -> Unit,
    onDeleteRecent: (String) -> Unit,
    onClearRecent: () -> Unit
) {
    SearchSectionHeader(
        title = "最近搜索",
        subtitle = "点一下继续上次的探索",
        trailing = {
            GlassIconButton(
                icon = Icons.Rounded.DeleteSweep,
                contentDescription = "清空最近搜索",
                onClick = onClearRecent,
                size = 44.dp,
                iconSize = 18.dp
            )
        }
    )
    Spacer(Modifier.height(14.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        recentSearches.forEach { item ->
            key(item.query) {
                AnimatedRecentSearchChip(
                    query = item.query,
                    onSelect = { onSearch(item.query) },
                    onDelete = { onDeleteRecent(item.query) }
                )
            }
        }
    }
}

@Composable
private fun AnimatedRecentSearchChip(
    query: String,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var removing by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = !removing,
        enter = fadeIn(tween(Motion.Fast.toInt())) +
            scaleIn(tween(Motion.Fast.toInt()), initialScale = 0.9f),
        exit = fadeOut(tween(Motion.Fast.toInt())) +
            scaleOut(tween(Motion.Fast.toInt()), targetScale = 0.86f)
    ) {
        RecentSearchChip(
            query = query,
            onSelect = onSelect,
            onDelete = {
                if (!removing) {
                    removing = true
                    scope.launch {
                        delay(Motion.Fast)
                        onDelete()
                    }
                }
            }
        )
    }
}

@Composable
private fun RecentSearchChip(
    query: String,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val extras = LocalXiaobaoColors.current
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .pressScale(interaction, pressedScale = 0.96f)
            .clip(Radii.chip)
            .background(extras.glassFill)
            .border(1.dp, extras.glassBorder, Radii.chip)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onSelect
            )
            .padding(start = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = query,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 180.dp)
        )
        Spacer(Modifier.width(2.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDelete
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "删除搜索记录：$query",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HotSearchSection(
    hotTags: List<String>,
    revealed: Boolean,
    onSearch: (String) -> Unit
) {
    SearchSectionHeader(
        title = "热门搜索",
        subtitle = "大家都在搜"
    )
    Spacer(Modifier.height(14.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        hotTags.forEachIndexed { index, tag ->
            HotSearchChip(
                rank = index + 1,
                tag = tag,
                revealed = revealed,
                onClick = { onSearch(tag) }
            )
        }
    }
}

@Composable
private fun HotSearchChip(
    rank: Int,
    tag: String,
    revealed: Boolean,
    onClick: () -> Unit
) {
    val extras = LocalXiaobaoColors.current
    val interaction = remember { MutableInteractionSource() }
    val revealState = animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(
            durationMillis = 420,
            delayMillis = (rank - 1) * 45,
            easing = Motion.EmphasizedDecelerate
        ),
        label = "hotTagReveal"
    )

    Row(
        modifier = Modifier
            .graphicsLayer {
                val reveal = revealState.value
                alpha = reveal
                translationY = (1f - reveal) * 14.dp.toPx()
            }
            .heightIn(min = 44.dp)
            .pressScale(interaction, pressedScale = 0.95f)
            .clip(Radii.chip)
            .background(extras.glassFill)
            .border(1.dp, extras.glassBorder, Radii.chip)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(extras.brandGradient)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$rank",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics { }
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = tag,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SearchTipsPanel() {
    SearchSectionHeader(
        title = "搜索小贴士",
        subtitle = "更快找到想看的"
    )
    Spacer(Modifier.height(16.dp))
    SearchTips.forEachIndexed { index, tip ->
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LocalXiaobaoColors.current.glassFill),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tip.first,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = tip.second,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
        if (index != SearchTips.lastIndex) {
            Spacer(Modifier.height(14.dp))
        }
    }
}

// ─── Results states ─────────────────────────────────────────────────────────

@Composable
private fun SearchSkeletonGrid(bottomClearance: Dp) {
    val gutter = screenGutter()
    val columns = posterGridColumns()
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = gutter,
            end = gutter,
            top = 6.dp,
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
private fun SearchEmptyPane(onClear: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        EmptyState(
            icon = Icons.Rounded.SearchOff,
            title = "没有找到相关内容",
            description = "换个关键词试试，关键词越短越容易搜到",
            actionLabel = "清空搜索",
            onAction = onClear
        )
    }
}

@Composable
private fun SearchErrorPane(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        ErrorState(
            title = "搜索失败",
            message = "网络开小差了，请检查网络后重试",
            onRetry = onRetry
        )
    }
}

@Composable
private fun SearchResultsGrid(
    query: String,
    results: List<VodContent>,
    hasMorePages: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    currentPage: Int,
    bottomClearance: Dp,
    onVodClick: (Int) -> Unit,
    onClear: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit
) {
    val gutter = screenGutter()
    val columns = posterGridColumns()
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = gutter,
            end = gutter,
            top = 6.dp,
            bottom = bottomClearance
        ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(
            key = "search_caption",
            contentType = "caption",
            span = { GridItemSpan(maxLineSpan) }
        ) {
            SearchResultCaption(
                query = query,
                count = results.size,
                onClear = onClear
            )
        }

        items(
            count = results.size,
            key = { index -> results[index].id },
            contentType = { "vod" }
        ) { index ->
            VodPosterCard(
                vod = results[index],
                onClick = onVodClick,
                modifier = Modifier.animateItem()
            )
        }

        if (hasMorePages || isLoadingMore || loadMoreError != null) {
            item(
                key = "search_footer",
                contentType = "footer",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                SearchLoadMoreFooter(
                    isLoadingMore = isLoadingMore,
                    loadMoreError = loadMoreError,
                    currentPage = currentPage,
                    onLoadMore = onLoadMore,
                    onRetry = onRetryLoadMore
                )
            }
        } else if (results.isNotEmpty()) {
            item(
                key = "search_end",
                contentType = "end",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                SearchEndCaption(count = results.size)
            }
        }
    }
}

@Composable
private fun SearchResultCaption(
    query: String,
    count: Int,
    onClear: () -> Unit
) {
    val extras = LocalXiaobaoColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(Radii.chip)
                .background(Brush.verticalGradient(extras.brandGradient))
        )
        Spacer(Modifier.width(9.dp))
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "“$query”",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "$count 部",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        GlassIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "清空搜索结果",
            onClick = onClear,
            size = 44.dp,
            iconSize = 18.dp
        )
    }
}

@Composable
private fun SearchLoadMoreFooter(
    isLoadingMore: Boolean,
    loadMoreError: String?,
    currentPage: Int,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit
) {
    when {
        isLoadingMore -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                XiaobaoSpinner(size = 28.dp)
            }
        }

        loadMoreError != null -> {
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

        else -> {
            // Sentinel: becomes visible when the user scrolls near the end of
            // the current page, then asks the view model for the next page.
            LaunchedEffect(currentPage) {
                onLoadMore()
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                XiaobaoSpinner(size = 24.dp)
            }
        }
    }
}

@Composable
private fun SearchEndCaption(count: Int) {
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
            text = "已展示全部 $count 部结果",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}
