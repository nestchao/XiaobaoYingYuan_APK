package com.xiaobaotv.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.xiaobaotv.app.domain.model.WatchHistoryItem
import com.xiaobaotv.app.ui.components.SectionHeader
import com.xiaobaotv.app.ui.components.plainSynopsis
import com.xiaobaotv.app.ui.components.VodPosterCard
import com.xiaobaotv.app.ui.navigation.isExpandedLayout
import com.xiaobaotv.app.ui.theme.PromoBadge
import com.xiaobaotv.app.ui.theme.ScoreStar

@Composable
fun HomeScreen(
    onVodClick: (Int) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val onVodClickRemembered = remember(onVodClick) { { id: Int -> onVodClick(id) } }
    val isTablet = isExpandedLayout()
    val heroHeight = if (isTablet) 400.dp else 340.dp

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        uiState.error?.let { error ->
            Text(
                text = error,
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.error
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("小宝TV", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("发现好故事，开启好时光", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalIconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "设置与外观")
                }
                }
            }
            // Hero Banner Carousel
            if (uiState.heroItems.isNotEmpty()) {
                item {
                    val pagerState = rememberPagerState(pageCount = { uiState.heroItems.size })
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(heroHeight)
                            .clip(RoundedCornerShape(24.dp))
                    ) {
                        HorizontalPager(state = pagerState) { page ->
                            val vod = uiState.heroItems[page]
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onVodClickRemembered(vod.id) }
                            ) {
                                AsyncImage(
                                    model = vod.pic,
                                    contentDescription = vod.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Gradient overlay
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.15f),
                                                    Color.Black.copy(alpha = 0.85f)
                                                )
                                            )
                                        )
                                )

                                // Content overlay
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(horizontal = 20.dp, vertical = 20.dp)
                                ) {
                                    // Genre badge
                                    Surface(
                                        color = PromoBadge,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "全网热播",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = vod.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    if (!vod.content.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = remember(vod.content) { plainSynopsis(vod.content) },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Play button
                                    Button(
                                        onClick = { onVodClickRemembered(vod.id) },
                                        shape = RoundedCornerShape(24.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color.Black
                                        ),
                                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                                    ) {
                                        Text(
                                            text = "▶",
                                            fontSize = 14.sp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "立即播放",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                        Surface(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                            color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(12.dp)) {
                            Text("${pagerState.currentPage + 1} / ${uiState.heroItems.size}", color = Color.White,
                                style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                }
            }

            // Continue Watching
            if (uiState.continueWatchingList.isNotEmpty()) {
                item { SectionHeader(title = "继续观看") }
                item {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.continueWatchingList,
                            key = { it.vodId },
                            contentType = { "history" }
                        ) { history ->
                            HistoryPosterCard(
                                history = history,
                                onClick = onVodClickRemembered,
                                modifier = Modifier.width(if (isTablet) 140.dp else 120.dp)
                            )
                        }
                    }
                }
            }

            // Latest Movies
            if (uiState.hotMovies.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "热门电影"
                    )
                }
                item {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.hotMovies,
                            key = { it.id },
                            contentType = { "vod" }
                        ) { vod ->
                            VodPosterCard(
                                vod = vod,
                                onClick = onVodClickRemembered,
                                modifier = Modifier.width(if (isTablet) 140.dp else 120.dp)
                            )
                        }
                    }
                }
            }

            // Hot TV Series
            if (uiState.hotTvSeries.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "热门剧集"
                    )
                }
                item {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.hotTvSeries,
                            key = { it.id },
                            contentType = { "vod" }
                        ) { vod ->
                            VodPosterCard(
                                vod = vod,
                                onClick = onVodClickRemembered,
                                modifier = Modifier.width(if (isTablet) 140.dp else 120.dp)
                            )
                        }
                    }
                }
            }

            // Hot Anime
            if (uiState.hotAnime.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "热门动漫"
                    )
                }
                item {
                    LazyRow(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.hotAnime,
                            key = { it.id },
                            contentType = { "vod" }
                        ) { vod ->
                            VodPosterCard(
                                vod = vod,
                                onClick = onVodClickRemembered,
                                modifier = Modifier.width(if (isTablet) 140.dp else 120.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryPosterCard(
    history: WatchHistoryItem,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick(history.vodId) }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(0.75f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = history.pic,
                contentDescription = history.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Episode badge
            Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = RoundedCornerShape(bottomEnd = 8.dp),
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = history.episodeName,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            // Progress bar at bottom
            if (history.durationMs > 0) {
                val progress = history.positionMs.toFloat() / history.durationMs
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Black.copy(alpha = 0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = history.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
