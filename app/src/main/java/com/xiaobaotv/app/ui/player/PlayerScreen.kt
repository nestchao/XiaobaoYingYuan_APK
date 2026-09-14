package com.xiaobaotv.app.ui.player

import android.view.View
import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.VolumeOff
import kotlin.math.roundToInt
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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
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
    val window = (context as? ComponentActivity)?.window
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var adjustment by remember { mutableStateOf<String?>(null) }
    var adjustmentLevel by remember { mutableFloatStateOf(0f) }
    var adjusting by remember { mutableStateOf(false) }
    var adjustmentRevision by remember { mutableIntStateOf(0) }

    // Brightness is local to this player window; restore the prior override on exit.
    DisposableEffect(window) {
        val previousBrightness = window?.attributes?.screenBrightness
        onDispose {
            if (window != null && previousBrightness != null) {
                window.attributes = window.attributes.apply { screenBrightness = previousBrightness }
            }
        }
    }
    LaunchedEffect(adjusting, adjustmentRevision) {
        if (!adjusting) {
            delay(1200)
            adjustment = null
        }
    }

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
    var isHorizontalSeeking by remember { mutableStateOf(false) }
    var showSeekPreview by remember { mutableStateOf(false) }
    var seekPreviewMs by remember { mutableLongStateOf(0L) }
    var seekStartMs by remember { mutableLongStateOf(0L) }

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
    LaunchedEffect(showControls, isPlaying, isSpeedMenuExpanded, showEpisodes, scrubFraction, controlInteraction, adjusting, isHorizontalSeeking) {
        if (showControls && isPlaying && !isSpeedMenuExpanded && !showEpisodes && scrubFraction == null && !adjusting && !isHorizontalSeeking) {
            delay(5000L)
            showControls = false
        }
    }

    LaunchedEffect(isHorizontalSeeking) {
        if (!isHorizontalSeeking && showSeekPreview) {
            delay(700L)
            showSeekPreview = false
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
            modifier = Modifier.fillMaxSize()
        )

        // Compose owns gestures above the native video surface. Controls above
        // this layer keep their own touch handling, including the seek slider.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(window, audioManager) {
                    var brightnessGesture = false
                    var level = 0f
                    var maxVolume = 1
                    var minVolume = 0
                    detectVerticalDragGestures(
                        onDragStart = { start ->
                            brightnessGesture = start.x < size.width / 2f
                            adjusting = true
                            adjustmentRevision++
                            if (brightnessGesture) {
                                val systemBrightness = Settings.System.getInt(
                                    context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128
                                ) / 255f
                                level = (window?.attributes?.screenBrightness?.takeIf { it >= 0f }
                                    ?: systemBrightness).coerceIn(0.02f, 1f)
                                adjustment = "亮度"
                            } else {
                                maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                minVolume = if (android.os.Build.VERSION.SDK_INT >= 28)
                                    audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC) else 0
                                level = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
                                adjustment = if (audioManager.isVolumeFixed) "音量由设备控制" else "音量"
                            }
                            adjustmentLevel = level
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            level = (level - dragAmount / (size.height * 0.65f).coerceAtLeast(1f))
                                .coerceIn(if (brightnessGesture) 0.02f else minVolume.toFloat() / maxVolume, 1f)
                            if (brightnessGesture) {
                                window?.let { it.attributes = it.attributes.apply { screenBrightness = level } }
                                adjustmentLevel = level
                            } else {
                                if (!audioManager.isVolumeFixed) {
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                                        (level * maxVolume).roundToInt().coerceIn(minVolume, maxVolume), 0)
                                }
                                adjustmentLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
                            }
                        },
                        onDragEnd = { adjusting = false; controlInteraction++ },
                        onDragCancel = { adjusting = false; controlInteraction++ }
                    )
                }
                .pointerInput(exoPlayer, totalDurationMs) {
                    var draggedMs = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            if (!uiState.isLoading && totalDurationMs > 0L && exoPlayer.isCurrentMediaItemSeekable) {
                                seekStartMs = exoPlayer.currentPosition.coerceIn(0L, totalDurationMs)
                                seekPreviewMs = seekStartMs
                                draggedMs = 0f
                                isHorizontalSeeking = true
                                showSeekPreview = true
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            if (!isHorizontalSeeking) return@detectHorizontalDragGestures
                            change.consume()
                            // A full-width swipe now moves up to three minutes, making
                            // long videos quicker to navigate while retaining precision
                            // for shorter, slower drags.
                            draggedMs += dragAmount / size.width.coerceAtLeast(1) * 180_000f
                            seekPreviewMs = (seekStartMs + draggedMs.toLong()).coerceIn(0L, totalDurationMs)
                        },
                        onDragEnd = {
                            if (isHorizontalSeeking) {
                                exoPlayer.seekTo(seekPreviewMs)
                                isHorizontalSeeking = false
                                controlInteraction++
                            }
                        },
                        onDragCancel = { isHorizontalSeeking = false }
                    )
                }
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
                }) { Text("重试", color = Color.White) }
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
            visible = showControls && !adjusting,
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
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "暂停" else "播放",
                            tint = MaterialTheme.colorScheme.onPrimary,
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
                        .displayCutoutPadding()
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
                        Text("左侧滑动调亮度 · 右侧滑动调音量",
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.weight(1f), maxLines = 2)
                        TextButton(onClick = { showEpisodes = true }, enabled = uiState.sources.isNotEmpty()) {
                            Text("选集", color = Color.White)
                        }
                        Spacer(Modifier.width(8.dp))
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

        adjustment?.let { label ->
            Surface(
                modifier = Modifier.align(Alignment.Center).width(200.dp),
                color = Color(0xEE182133), contentColor = Color.White,
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        if (label == "亮度") Icons.Default.Brightness6
                        else if (adjustmentLevel == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = null, modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(label + " " + (adjustmentLevel * 100).roundToInt() + "%",
                        style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(progress = { adjustmentLevel }, modifier = Modifier.fillMaxWidth(),
                        color = Color.White, trackColor = Color.White.copy(alpha = 0.2f))
                }
            }
        }

        if (showSeekPreview && totalDurationMs > 0L) {
            val isForward = seekPreviewMs >= seekStartMs
            Surface(
                modifier = Modifier.align(Alignment.Center),
                color = Color.Black.copy(alpha = 0.78f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isForward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                        contentDescription = if (isForward) "快进" else "快退",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = (if (isForward) "快进 " else "快退 ") + formatTime(seekPreviewMs),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = formatTime(seekPreviewMs) + " / " + formatTime(totalDurationMs),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                    LinearProgressIndicator(
                        progress = { (seekPreviewMs.toFloat() / totalDurationMs).coerceIn(0f, 1f) },
                        modifier = Modifier.width(180.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
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
                                Text(if (index == uiState.currentEpisodeIndex) "正在播放 · ${episode.name}" else episode.name,
                                    fontWeight = if (index == uiState.currentEpisodeIndex) FontWeight.Bold else FontWeight.Normal)
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
                        Text("取消", color = Color.White, fontSize = 14.sp)
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
