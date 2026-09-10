package com.xiaobaotv.app.ui.player

import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.style.TextOverflow
import androidx.media3.common.PlaybackException
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.xiaobaotv.app.ui.navigation.LocalFullScreenState
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    vodId: Int,
    episodeIndex: Int = 0,
    onBackClick: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val fullScreenState = LocalFullScreenState.current

    // Custom Compose overlay states
    var showControls by remember { mutableStateOf(true) }
    var skipText by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackSpeed by remember { mutableStateOf(1.0f) }
    var isSpeedMenuExpanded by remember { mutableStateOf(false) }

    var showEpisodes by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    var controlInteraction by remember { mutableIntStateOf(0) }

    // Dynamic position values for slider and timestamps
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }

    // Thread-safe refs — updated whenever uiState changes
    val hasNextRef = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val hasPrevRef = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val autoNextEnabledRef = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    LaunchedEffect(uiState) {
        hasNextRef.set(uiState.hasNextEpisode)
        hasPrevRef.set(uiState.hasPreviousEpisode)
        autoNextEnabledRef.set(uiState.autoNextEnabled)
    }

    // Auto-hide controls after inactivity
    LaunchedEffect(showControls, isPlaying, isSpeedMenuExpanded, showEpisodes, scrubFraction, controlInteraction) {
        if (showControls && isPlaying && !isSpeedMenuExpanded && !showEpisodes && scrubFraction == null) {
            delay(5000L)
            showControls = false
        }
    }

    // Reset skip indicator after a short delay
    LaunchedEffect(skipText) {
        if (skipText != null) {
            delay(800L)
            skipText = null
        }
    }

    val exoPlayer = remember(vodId) {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(mapOf("Referer" to "https://www.xiaobaotv.tv/"))
        val cacheDataSourceFactory = androidx.media3.datasource.cache.CacheDataSource.Factory()
            .setCache(viewModel.exoPlayerCache)
            .setUpstreamDataSourceFactory(dataSourceFactory)
        val basePlayer = ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .build()

        // Wrap to expose next/prev commands
        object : ForwardingPlayer(basePlayer) {
            override fun isCommandAvailable(command: Int): Boolean {
                return getAvailableCommands().contains(command)
            }

            override fun getAvailableCommands(): Player.Commands {
                val commands = super.getAvailableCommands()
                var builder = commands.buildUpon()
                if (hasNextRef.get()) {
                    builder = builder.add(Player.COMMAND_SEEK_TO_NEXT)
                }
                if (hasPrevRef.get()) {
                    builder = builder.add(Player.COMMAND_SEEK_TO_PREVIOUS)
                }
                return builder.build()
            }

            override fun seekToNextMediaItem() {
                val nextIndex = viewModel.uiState.value.currentEpisodeIndex + 1
                viewModel.selectEpisode(nextIndex)
            }

            override fun seekToPreviousMediaItem() {
                val prevIndex = viewModel.uiState.value.currentEpisodeIndex - 1
                viewModel.selectEpisode(prevIndex)
            }

            override fun seekToNext() = seekToNextMediaItem()
            override fun seekToPrevious() = seekToPreviousMediaItem()
        }
    }

    // Listen for playback state changes
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing) showControls = true
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.localizedMessage ?: "Playback failed"
                showControls = true
                isBuffering = false
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED
                    && hasNextRef.get()
                    && autoNextEnabledRef.get()
                ) {
                    viewModel.triggerAutoNextCountdown()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(vodId) {
        viewModel.loadVideoInfo(vodId, episodeIndex)
    }

    LaunchedEffect(uiState.playbackUrl) {
        playbackError = null
        scrubFraction = null
        if (uiState.playbackUrl == null) exoPlayer.pause()
        uiState.playbackUrl?.let { url ->
            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            if (uiState.savedPositionMs > 0L) {
                exoPlayer.seekTo(uiState.savedPositionMs)
            }
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    // Position polling — update slider values and feed position to ViewModel
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosMs = exoPlayer.currentPosition
            totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
            if (exoPlayer.isPlaying) {
                viewModel.updatePlaybackState(currentPosMs, totalDurationMs)
            }
            delay(500L)
        }
    }

    // Lifecycle-aware save on pause/stop
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                viewModel.saveOnPause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(vodId) {
        onDispose {
            viewModel.cancelAutoNext()
            exoPlayer.release()
        }
    }

    // Full-screen immersive mode toggle
    val activity = LocalContext.current as? ComponentActivity
    LaunchedEffect(Unit) {
        fullScreenState.isActive = true
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        activity?.window?.decorView?.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    DisposableEffect(Unit) {
        onDispose {
            fullScreenState.isActive = false
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            @Suppress("DEPRECATION")
            activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video native renderer — XML controls disabled, Compose overlay handles everything
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(exoPlayer) {
                    detectTapGestures(
                        onDoubleTap = onDoubleTap@{ offset ->
                            if (uiState.isLoading || !exoPlayer.isCurrentMediaItemSeekable || totalDurationMs <= 0) return@onDoubleTap
                            val seekAmount = if (offset.x < size.width / 2f) -10000L else 10000L
                            val target = (exoPlayer.currentPosition + seekAmount)
                                .coerceIn(0L, totalDurationMs.coerceAtLeast(0))
                            exoPlayer.seekTo(target)
                            skipText = if (seekAmount > 0) "快进 10s" else "快退 10s"
                        },
                        onTap = {
                            showControls = !showControls
                        }
                    )
                }
        )

        // Loading indicator
        if (uiState.isLoading || isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.primary
            )
        }

        val visibleError = uiState.error ?: playbackError
        if (visibleError != null) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(16.dp)).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(visibleError, color = Color.White)
                TextButton(onClick = {
                    playbackError = null
                    if (uiState.error != null) viewModel.retryPlayback()
                    else { exoPlayer.prepare(); exoPlayer.play() }
                }) { Text("重试") }
            }
        }

        // Skip indicator overlay
        skipText?.let { text ->
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = text, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Full Compose overlay controls
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top gradient + header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                            )
                        )
                        .align(Alignment.TopCenter)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .displayCutoutPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.sources
                            .getOrNull(uiState.currentSourceIndex)
                            ?.episodes
                            ?.getOrNull(uiState.currentEpisodeIndex)
                            ?.name ?: "正在播放",
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Center play/pause + prev/next
                if (!uiState.isLoading && !isBuffering && visibleError == null) Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { exoPlayer.seekToPreviousMediaItem() },
                        enabled = uiState.hasPreviousEpisode,
                        colors = IconButtonDefaults.iconButtonColors(
                            disabledContentColor = Color.Gray.copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.SkipPrevious,
                            contentDescription = "上一集",
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            controlInteraction++
                            if (isPlaying) exoPlayer.pause() else {
                                if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0)
                                exoPlayer.play()
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    IconButton(
                        onClick = { exoPlayer.seekToNextMediaItem() },
                        enabled = uiState.hasNextEpisode,
                        colors = IconButtonDefaults.iconButtonColors(
                            disabledContentColor = Color.Gray.copy(alpha = 0.5f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            Icons.Filled.SkipNext,
                            contentDescription = "下一集",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Bottom gradient + timeline + speed
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                        .align(Alignment.BottomCenter)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // Timeline slider with timestamps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(scrubFraction?.let { (it * totalDurationMs).toLong() } ?: currentPosMs),
                            color = Color.White,
                            fontSize = 12.sp
                        )

                        Slider(
                            value = scrubFraction ?: if (totalDurationMs > 0) {
                                (currentPosMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                            } else 0f,
                            onValueChange = { percent ->
                                scrubFraction = percent
                            },
                            onValueChangeFinished = {
                                scrubFraction?.let { exoPlayer.seekTo((it * totalDurationMs).toLong()) }
                                scrubFraction = null
                                controlInteraction++
                            },
                            enabled = totalDurationMs > 0 && exoPlayer.isCurrentMediaItemSeekable && !uiState.isLoading,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.Gray.copy(alpha = 0.5f)
                            )
                        )

                        Text(
                            text = formatTime(totalDurationMs),
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    // Speed selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEpisodes = true }, enabled = uiState.sources.isNotEmpty()) {
                            Text("选集", color = Color.White)
                        }
                        Spacer(Modifier.weight(1f))
                        Box {
                            TextButton(onClick = { isSpeedMenuExpanded = true }) {
                                Icon(
                                    Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${currentPlaybackSpeed}x",
                                    color = Color.White
                                )
                            }

                            DropdownMenu(
                                expanded = isSpeedMenuExpanded,
                                onDismissRequest = { isSpeedMenuExpanded = false }
                            ) {
                                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text("${speed}x") },
                                        onClick = {
                                            currentPlaybackSpeed = speed
                                            exoPlayer.setPlaybackSpeed(speed)
                                            isSpeedMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showEpisodes) {
            AlertDialog(
                onDismissRequest = { showEpisodes = false },
                title = { Text("选集") },
                text = {
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                        itemsIndexed(uiState.sources.getOrNull(uiState.currentSourceIndex)?.episodes.orEmpty()) { index, episode ->
                            TextButton(
                                onClick = { viewModel.selectEpisode(index); showEpisodes = false; controlInteraction++ },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isLoading
                            ) {
                                Text(episode.name, fontWeight = if (index == uiState.currentEpisodeIndex) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showEpisodes = false }) { Text("关闭") } }
            )
        }

        // Auto-next countdown overlay
        if (uiState.showAutoNextOverlay) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(16.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "下一集即将播放 ${uiState.autoNextCountdown}s",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { viewModel.cancelAutoNext() }) {
                        Text("取消", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}
