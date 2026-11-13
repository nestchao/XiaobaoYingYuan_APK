package com.xiaobaotv.app.ui.player

import android.content.Context
import android.media.AudioManager
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.xiaobaotv.app.domain.model.VideoSource
import com.xiaobaotv.app.ui.components.EpisodeChip
import com.xiaobaotv.app.ui.components.GlassPillButton
import com.xiaobaotv.app.ui.components.GradientPillButton
import com.xiaobaotv.app.ui.components.ProgressRing
import com.xiaobaotv.app.ui.components.SelectChip
import com.xiaobaotv.app.ui.components.TintedIconButton
import com.xiaobaotv.app.ui.components.WindowSize
import com.xiaobaotv.app.ui.components.XiaobaoSpinner
import com.xiaobaotv.app.ui.components.pressScale
import com.xiaobaotv.app.ui.components.rememberWindowSize
import com.xiaobaotv.app.ui.navigation.LocalFullScreenState
import com.xiaobaotv.app.ui.theme.LocalXiaobaoColors
import com.xiaobaotv.app.ui.theme.Motion
import com.xiaobaotv.app.ui.theme.Radii
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// Player — an immersive, edge-to-edge theatre.
//
// All playback / gesture / lifecycle behaviour is unchanged from the previous
// implementation. Only the chrome was rebuilt: frosted floating panels, a
// gradient morphing play button, a hand-drawn scrub bar with buffered progress,
// animated brightness/volume medallions, a double-tap ripple, a glass episode
// sheet and a countdown ring for auto-next.
// ─────────────────────────────────────────────────────────────────────────────

private val SpeedOptions = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
private const val AutoNextTotal = 5

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    vodId: Int,
    episodeIndex: Int = 0,
    onBackClick: () -> Unit = {},
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val fullScreenState = LocalFullScreenState.current
    val window = activity?.window
    val decorView = LocalView.current
    val windowSize = rememberWindowSize()
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    // ── Brightness / volume gesture feedback ────────────────────────────────
    var adjustment by remember { mutableStateOf<String?>(null) }
    var adjustmentLevel by remember { mutableFloatStateOf(0f) }
    var adjusting by remember { mutableStateOf(false) }
    var adjustmentRevision by remember { mutableIntStateOf(0) }
    var lastAdjustmentLabel by remember { mutableStateOf("亮度") }

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
    LaunchedEffect(adjustment) {
        adjustment?.let { lastAdjustmentLabel = it }
    }

    // ── Custom Compose overlay states ───────────────────────────────────────
    var showControls by remember { mutableStateOf(true) }
    var skipText by remember { mutableStateOf<String?>(null) }
    var skipRevision by remember { mutableIntStateOf(0) }
    var lastSkipText by remember { mutableStateOf("") }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPlaybackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedPicker by remember { mutableStateOf(false) }

    var showEpisodes by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    var controlInteraction by remember { mutableIntStateOf(0) }
    var isHorizontalSeeking by remember { mutableStateOf(false) }
    var showSeekPreview by remember { mutableStateOf(false) }
    var seekPreviewMs by remember { mutableLongStateOf(0L) }
    var seekStartMs by remember { mutableLongStateOf(0L) }

    // Dynamic position values for the scrub bar and timestamps.
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(0L) }
    var bufferedPercent by remember { mutableIntStateOf(0) }

    // Thread-safe refs — updated whenever uiState changes.
    val hasNextRef = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val hasPrevRef = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val autoNextEnabledRef = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    LaunchedEffect(uiState) {
        hasNextRef.set(uiState.hasNextEpisode)
        hasPrevRef.set(uiState.hasPreviousEpisode)
        autoNextEnabledRef.set(uiState.autoNextEnabled)
    }

    // Auto-hide controls after inactivity.
    LaunchedEffect(
        showControls,
        isPlaying,
        showSpeedPicker,
        showEpisodes,
        scrubFraction,
        controlInteraction,
        adjusting,
        isHorizontalSeeking,
        uiState.showAutoNextOverlay
    ) {
        if (showControls && isPlaying && !showSpeedPicker && !showEpisodes &&
            scrubFraction == null && !adjusting && !isHorizontalSeeking &&
            !uiState.showAutoNextOverlay
        ) {
            delay(4500L)
            showControls = false
        }
    }

    LaunchedEffect(isHorizontalSeeking) {
        if (!isHorizontalSeeking && showSeekPreview) {
            delay(700L)
            showSeekPreview = false
        }
    }

    // Reset the double-tap indicator after a short delay.
    LaunchedEffect(skipText, skipRevision) {
        if (skipText != null) {
            lastSkipText = skipText.orEmpty()
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

        // Wrap to expose next/prev commands.
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

    // Listen for playback state changes.
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
                if (playbackState == Player.STATE_ENDED &&
                    hasNextRef.get() &&
                    autoNextEnabledRef.get()
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

    // Position polling — update the scrub bar and feed position to the ViewModel.
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosMs = exoPlayer.currentPosition
            totalDurationMs = exoPlayer.duration.coerceAtLeast(0L)
            bufferedPercent = exoPlayer.bufferedPercentage
            if (exoPlayer.isPlaying) {
                viewModel.updatePlaybackState(currentPosMs, totalDurationMs)
            }
            delay(500L)
        }
    }

    // Lifecycle-aware save on pause/stop.
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

    // Full-screen immersive mode (modern WindowInsetsControllerCompat).
    LaunchedEffect(Unit) {
        fullScreenState.isActive = true
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    DisposableEffect(decorView, window) {
        val insetsController = window?.let { WindowCompat.getInsetsController(it, decorView) }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())
        onDispose {
            fullScreenState.isActive = false
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
            insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }

    // ── Derived display values ──────────────────────────────────────────────
    val visibleError = uiState.error ?: playbackError
    val controlsVisible = showControls && !adjusting
    val episodes = uiState.sources.getOrNull(uiState.currentSourceIndex)?.episodes.orEmpty()
    val sourceName = uiState.sources.getOrNull(uiState.currentSourceIndex)?.name
    val currentEpisodeTitle = episodes.getOrNull(uiState.currentEpisodeIndex)?.name ?: "正在播放"
    val nextEpisodeName = episodes.getOrNull(uiState.currentEpisodeIndex + 1)?.name
    val episodeLabel = if (episodes.isEmpty()) null else {
        "${(uiState.currentEpisodeIndex + 1).coerceIn(1, episodes.size)} / ${episodes.size}"
    }
    val skipForward = lastSkipText.startsWith("快进")
    val autoNextBottomPadding by animateDpAsState(
        targetValue = if (controlsVisible) 148.dp else 28.dp,
        animationSpec = tween(Motion.Medium.toInt()),
        label = "autoNextBottomPadding"
    )
    val gestureHint = if (windowSize == WindowSize.Compact) null else "左侧调亮度 · 右侧调音量"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Video native renderer — XML controls disabled, Compose overlay owns everything.
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    // Avoid a white flash while the first frame is decoded.
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Compose owns gestures above the native video surface. Controls above
        // this layer keep their own touch handling, including the scrub bar.
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
                                    context.contentResolver,
                                    Settings.System.SCREEN_BRIGHTNESS,
                                    128
                                ) / 255f
                                level = (window?.attributes?.screenBrightness?.takeIf { it >= 0f }
                                    ?: systemBrightness).coerceIn(0.02f, 1f)
                                adjustment = "亮度"
                            } else {
                                maxVolume = audioManager
                                    .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    .coerceAtLeast(1)
                                minVolume = if (android.os.Build.VERSION.SDK_INT >= 28) {
                                    audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
                                } else {
                                    0
                                }
                                level = audioManager
                                    .getStreamVolume(AudioManager.STREAM_MUSIC)
                                    .toFloat() / maxVolume
                                adjustment =
                                    if (audioManager.isVolumeFixed) "音量由设备控制" else "音量"
                            }
                            adjustmentLevel = level
                        },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            level = (level - dragAmount / (size.height * 0.65f).coerceAtLeast(1f))
                                .coerceIn(
                                    if (brightnessGesture) 0.02f else minVolume.toFloat() / maxVolume,
                                    1f
                                )
                            if (brightnessGesture) {
                                window?.let {
                                    it.attributes = it.attributes.apply { screenBrightness = level }
                                }
                                adjustmentLevel = level
                            } else {
                                if (!audioManager.isVolumeFixed) {
                                    audioManager.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        (level * maxVolume).roundToInt()
                                            .coerceIn(minVolume, maxVolume),
                                        0
                                    )
                                }
                                adjustmentLevel = audioManager
                                    .getStreamVolume(AudioManager.STREAM_MUSIC)
                                    .toFloat() / maxVolume
                            }
                        },
                        onDragEnd = {
                            adjusting = false
                            controlInteraction++
                        },
                        onDragCancel = {
                            adjusting = false
                            controlInteraction++
                        }
                    )
                }
                .pointerInput(exoPlayer, totalDurationMs) {
                    var draggedMs = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            if (!uiState.isLoading &&
                                totalDurationMs > 0L &&
                                exoPlayer.isCurrentMediaItemSeekable
                            ) {
                                seekStartMs =
                                    exoPlayer.currentPosition.coerceIn(0L, totalDurationMs)
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
                            seekPreviewMs = (seekStartMs + draggedMs.toLong())
                                .coerceIn(0L, totalDurationMs)
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
                            if (uiState.isLoading ||
                                !exoPlayer.isCurrentMediaItemSeekable ||
                                totalDurationMs <= 0
                            ) {
                                return@onDoubleTap
                            }
                            val seekAmount = if (offset.x < size.width / 2f) -10000L else 10000L
                            val target = (exoPlayer.currentPosition + seekAmount)
                                .coerceIn(0L, totalDurationMs.coerceAtLeast(0))
                            exoPlayer.seekTo(target)
                            skipText = if (seekAmount > 0) "快进 10s" else "快退 10s"
                            skipRevision++
                        },
                        onTap = {
                            showControls = !showControls
                            if (!showControls) showSpeedPicker = false
                        }
                    )
                }
        )

        // Buffering / initial loading.
        if (uiState.isLoading || isBuffering) {
            BufferingIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Playback error.
        if (visibleError != null) {
            PlayerErrorCard(
                message = visibleError,
                onRetry = {
                    playbackError = null
                    if (uiState.error != null) {
                        viewModel.retryPlayback()
                    } else {
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            )
        }

        // Double-tap skip feedback.
        AnimatedVisibility(
            visible = skipText != null,
            modifier = Modifier
                .align(if (skipForward) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 36.dp),
            enter = fadeIn(tween(110)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(dampingRatio = 0.55f, stiffness = 520f)
            ),
            exit = fadeOut(tween(180)) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(180)
            )
        ) {
            SkipIndicator(
                text = lastSkipText,
                isForward = skipForward,
                revision = skipRevision
            )
        }

        // ── Top chrome ─────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.TopCenter),
            enter = fadeIn(tween(200)) + slideInVertically(
                animationSpec = tween(Motion.Medium.toInt(), easing = Motion.EmphasizedDecelerate)
            ) { -it / 2 },
            exit = fadeOut(tween(170)) + slideOutVertically(
                animationSpec = tween(Motion.Fast.toInt(), easing = Motion.EmphasizedAccelerate)
            ) { -it / 2 }
        ) {
            PlayerTopBar(
                title = currentEpisodeTitle,
                sourceName = sourceName,
                episodeLabel = episodeLabel,
                onBackClick = onBackClick
            )
        }

        // ── Center transport ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible && !isHorizontalSeeking &&
                visibleError == null && !uiState.isLoading && !isBuffering,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(170)) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(Motion.Fast.toInt())
            ),
            exit = fadeOut(tween(130)) + scaleOut(
                targetScale = 0.94f,
                animationSpec = tween(130)
            )
        ) {
            PlayerCenterControls(
                isPlaying = isPlaying,
                hasPrevious = uiState.hasPreviousEpisode,
                hasNext = uiState.hasNextEpisode,
                onPlayPause = {
                    controlInteraction++
                    if (isPlaying) {
                        exoPlayer.pause()
                    } else {
                        if (exoPlayer.playbackState == Player.STATE_ENDED) exoPlayer.seekTo(0)
                        exoPlayer.play()
                    }
                },
                onPrevious = { exoPlayer.seekToPreviousMediaItem() },
                onNext = { exoPlayer.seekToNextMediaItem() }
            )
        }

        // ── Bottom chrome ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn(tween(200)) + slideInVertically(
                animationSpec = tween(Motion.Medium.toInt(), easing = Motion.EmphasizedDecelerate)
            ) { it / 2 },
            exit = fadeOut(tween(170)) + slideOutVertically(
                animationSpec = tween(Motion.Fast.toInt(), easing = Motion.EmphasizedAccelerate)
            ) { it / 2 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .displayCutoutPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = showSpeedPicker,
                    enter = fadeIn(tween(160)) + slideInVertically(
                        animationSpec = tween(Motion.Fast.toInt())
                    ) { it / 2 },
                    exit = fadeOut(tween(120)) + slideOutVertically(
                        animationSpec = tween(Motion.Fast.toInt())
                    ) { it / 2 }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SpeedPicker(
                            currentSpeed = currentPlaybackSpeed,
                            onSelect = { speed ->
                                currentPlaybackSpeed = speed
                                exoPlayer.setPlaybackSpeed(speed)
                                showSpeedPicker = false
                                controlInteraction++
                            }
                        )
                    }
                }

                PlayerBottomPanel(
                    positionMs = currentPosMs,
                    durationMs = totalDurationMs,
                    bufferedPercent = bufferedPercent,
                    scrubFraction = scrubFraction,
                    seekEnabled = totalDurationMs > 0L &&
                        exoPlayer.isCurrentMediaItemSeekable &&
                        !uiState.isLoading,
                    speed = currentPlaybackSpeed,
                    gestureHint = gestureHint,
                    onScrubStart = {
                        scrubFraction = if (totalDurationMs > 0L) {
                            (currentPosMs.toFloat() / totalDurationMs).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                    },
                    onScrub = { fraction -> scrubFraction = fraction },
                    onScrubFinished = {
                        scrubFraction?.let {
                            exoPlayer.seekTo((it * totalDurationMs).toLong())
                        }
                        scrubFraction = null
                        controlInteraction++
                    },
                    onEpisodesClick = {
                        showEpisodes = true
                        controlInteraction++
                    },
                    onSpeedClick = {
                        showSpeedPicker = !showSpeedPicker
                        controlInteraction++
                    },
                    modifier = Modifier
                        .widthIn(max = 980.dp)
                        .fillMaxWidth()
                )
            }
        }

        // ── Brightness / volume medallion ──────────────────────────────────
        AnimatedVisibility(
            visible = adjustment != null,
            modifier = Modifier.align(Alignment.Center),
            enter = fadeIn(tween(140)) + scaleIn(
                initialScale = 0.86f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 520f)
            ),
            exit = fadeOut(tween(200)) + scaleOut(
                targetScale = 0.9f,
                animationSpec = tween(200)
            )
        ) {
            AdjustIndicator(
                label = lastAdjustmentLabel,
                level = adjustmentLevel
            )
        }

        // ── Horizontal seek preview ────────────────────────────────────────
        AnimatedVisibility(
            visible = showSeekPreview && totalDurationMs > 0L,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .displayCutoutPadding()
                .padding(top = 96.dp),
            enter = fadeIn(tween(120)) + slideInVertically(
                animationSpec = tween(Motion.Fast.toInt())
            ) { -it / 3 },
            exit = fadeOut(tween(180))
        ) {
            SeekPreviewCard(
                previewMs = seekPreviewMs,
                durationMs = totalDurationMs,
                isForward = seekPreviewMs >= seekStartMs
            )
        }

        // ── Auto-next countdown ────────────────────────────────────────────
        AnimatedVisibility(
            visible = uiState.showAutoNextOverlay,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .displayCutoutPadding()
                .navigationBarsPadding()
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = autoNextBottomPadding
                ),
            enter = fadeIn(tween(200)) + slideInVertically(
                animationSpec = tween(Motion.Medium.toInt(), easing = Motion.EmphasizedDecelerate)
            ) { it / 2 },
            exit = fadeOut(tween(160)) + slideOutVertically(
                animationSpec = tween(Motion.Fast.toInt())
            ) { it / 2 }
        ) {
            AutoNextCard(
                countdown = uiState.autoNextCountdown,
                nextEpisodeName = nextEpisodeName,
                onCancel = { viewModel.cancelAutoNext() }
            )
        }

        // ── Episode sheet ──────────────────────────────────────────────────
        if (showEpisodes) {
            EpisodeSheet(
                sources = uiState.sources,
                currentSourceIndex = uiState.currentSourceIndex,
                currentEpisodeIndex = uiState.currentEpisodeIndex,
                isLoading = uiState.isLoading,
                onSelectEpisode = { index ->
                    viewModel.selectEpisode(index)
                    showEpisodes = false
                    controlInteraction++
                },
                onSelectSource = { index ->
                    viewModel.selectSource(index)
                    controlInteraction++
                },
                onDismiss = {
                    showEpisodes = false
                    controlInteraction++
                }
            )
        }
    }
}

// ─── Player chrome surface ──────────────────────────────────────────────────

/**
 * Dark smoked glass used by every floating player control. Deliberately
 * theme-independent so white control glyphs stay legible over any video frame,
 * in both light and dark app themes.
 */
@Composable
private fun PlayerGlass(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    @Suppress("UNUSED_PARAMETER") strong: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = if (strong) 0.64f else 0.50f),
                        Color.Black.copy(alpha = if (strong) 0.48f else 0.38f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape),
        content = content
    )
}

// ─── Top bar ────────────────────────────────────────────────────────────────

@Composable
private fun PlayerTopBar(
    title: String,
    sourceName: String?,
    episodeLabel: String?,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.74f), Color.Transparent)
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .displayCutoutPadding()
                .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TintedIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "返回",
                onClick = onBackClick,
                size = 44.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!sourceName.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "线路 · $sourceName",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (episodeLabel != null) {
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.42f))
                        .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = episodeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ─── Center transport ───────────────────────────────────────────────────────

@Composable
private fun PlayerCenterControls(
    isPlaying: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(34.dp)
    ) {
        ControlCircleButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = "上一集",
            enabled = hasPrevious,
            onClick = onPrevious,
            size = 56.dp
        )
        PlayPauseButton(isPlaying = isPlaying, onClick = onPlayPause)
        ControlCircleButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = "下一集",
            enabled = hasNext,
            onClick = onNext,
            size = 56.dp
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = LocalXiaobaoColors.current
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(88.dp)
            .pressScale(interaction)
            .clip(CircleShape)
            .background(Brush.linearGradient(extras.brandGradient))
            .border(1.dp, Color.White.copy(alpha = 0.24f), CircleShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isPlaying,
            transitionSpec = {
                (fadeIn(tween(170)) + scaleIn(
                    initialScale = 0.55f,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 480f)
                )) togetherWith
                    (fadeOut(tween(110)) + scaleOut(
                        targetScale = 0.55f,
                        animationSpec = tween(110)
                    ))
            },
            contentAlignment = Alignment.Center,
            label = "playPauseIcon"
        ) { playing ->
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playing) "暂停" else "播放",
                tint = Color.White,
                modifier = Modifier.size(46.dp)
            )
        }
    }
}

@Composable
private fun ControlCircleButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val contentAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.32f,
        animationSpec = tween(Motion.Fast.toInt()),
        label = "controlAlpha"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { alpha = contentAlpha }
            .pressScale(interaction)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.38f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(size * 0.42f)
        )
    }
}

// ─── Bottom control panel ───────────────────────────────────────────────────

@Composable
private fun PlayerBottomPanel(
    positionMs: Long,
    durationMs: Long,
    bufferedPercent: Int,
    scrubFraction: Float?,
    seekEnabled: Boolean,
    speed: Float,
    gestureHint: String?,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    onEpisodesClick: () -> Unit,
    onSpeedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerGlass(
        modifier = modifier.capturePointerInput(),
        shape = RoundedCornerShape(26.dp),
        strong = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTime(
                        scrubFraction?.let { (it * durationMs).toLong() } ?: positionMs
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.widthIn(min = 44.dp)
                )
                AuroraSeekBar(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    bufferedPercent = bufferedPercent,
                    scrubFraction = scrubFraction,
                    enabled = seekEnabled,
                    onScrubStart = onScrubStart,
                    onScrub = onScrub,
                    onScrubFinished = onScrubFinished,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                )
                Text(
                    text = formatTime(durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ControlTextButton(
                    icon = Icons.Rounded.GridView,
                    text = "选集",
                    onClick = onEpisodesClick
                )
                ControlTextButton(
                    icon = Icons.Rounded.Speed,
                    text = "倍速 ${formatSpeed(speed)}",
                    onClick = onSpeedClick
                )
                Spacer(Modifier.weight(1f))
                if (gestureHint != null) {
                    Text(
                        text = gestureHint,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.42f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlTextButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .height(44.dp)
            .pressScale(interaction, pressedScale = 0.94f)
            .clip(Radii.chip)
            .background(Color.White.copy(alpha = 0.09f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), Radii.chip)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(17.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            maxLines = 1
        )
    }
}

/**
 * Hand-drawn scrub bar: buffered track, gradient played track and a thumb that
 * swells while dragging. Dragging only previews; the seek is committed on
 * release, mirroring the previous slider semantics.
 */
@Composable
private fun AuroraSeekBar(
    positionMs: Long,
    durationMs: Long,
    bufferedPercent: Int,
    scrubFraction: Float?,
    enabled: Boolean,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extras = LocalXiaobaoColors.current
    var isDragging by remember { mutableStateOf(false) }
    var widthPx by remember { mutableFloatStateOf(1f) }

    val baseProgress = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    val rawProgress = (scrubFraction ?: baseProgress).coerceIn(0f, 1f)
    val smoothProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(420, easing = LinearEasing),
        label = "seekSmoothProgress"
    )
    val progress = if (isDragging || scrubFraction != null) rawProgress else smoothProgress
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.45f else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 620f),
        label = "seekThumbScale"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(enabled, durationMs) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        onScrubStart()
                        onScrub((offset.x / widthPx).coerceIn(0f, 1f))
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onScrub((change.position.x / widthPx).coerceIn(0f, 1f))
                    },
                    onDragEnd = {
                        isDragging = false
                        onScrubFinished()
                    },
                    onDragCancel = {
                        isDragging = false
                        onScrubFinished()
                    }
                )
            }
            .pointerInput(enabled, durationMs) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    onScrubStart()
                    onScrub((offset.x / widthPx).coerceIn(0f, 1f))
                    onScrubFinished()
                }
            }
    ) {
        val trackHeight = 4.dp.toPx()
        val radius = trackHeight / 2f
        val centerY = size.height / 2f
        val trackTop = centerY - radius

        drawRoundRect(
            color = Color.White.copy(alpha = 0.18f),
            topLeft = Offset(0f, trackTop),
            size = Size(size.width, trackHeight),
            cornerRadius = CornerRadius(radius, radius)
        )

        val bufferFraction = (bufferedPercent / 100f).coerceIn(0f, 1f)
        if (bufferFraction > 0f) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.30f),
                topLeft = Offset(0f, trackTop),
                size = Size(size.width * bufferFraction, trackHeight),
                cornerRadius = CornerRadius(radius, radius)
            )
        }

        val playedWidth = size.width * progress
        if (playedWidth > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(extras.brandGradient),
                topLeft = Offset(0f, trackTop),
                size = Size(playedWidth, trackHeight),
                cornerRadius = CornerRadius(radius, radius)
            )
        }

        val thumbX = (size.width * progress).coerceIn(0f, size.width)
        val thumbRadius = 6.dp.toPx() * thumbScale
        drawCircle(
            color = Color.White.copy(alpha = 0.16f),
            radius = thumbRadius * 2.1f,
            center = Offset(thumbX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = thumbRadius,
            center = Offset(thumbX, centerY)
        )
        drawCircle(
            color = extras.brandGradient.last(),
            radius = thumbRadius * 0.5f,
            center = Offset(thumbX, centerY)
        )
    }
}

// ─── Speed picker ───────────────────────────────────────────────────────────

@Composable
private fun SpeedPicker(
    currentSpeed: Float,
    onSelect: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerGlass(
        modifier = modifier.capturePointerInput(),
        shape = Radii.card,
        strong = true
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpeedOptions.forEach { speed ->
                SpeedChip(
                    text = formatSpeed(speed),
                    selected = abs(currentSpeed - speed) < 0.001f,
                    onClick = { onSelect(speed) }
                )
            }
        }
    }
}

@Composable
private fun SpeedChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val container by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.White.copy(alpha = 0.08f)
        },
        animationSpec = tween(Motion.Fast.toInt()),
        label = "speedChipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            Color.White
        },
        animationSpec = tween(Motion.Fast.toInt()),
        label = "speedChipContent"
    )
    Box(
        modifier = Modifier
            .height(44.dp)
            .pressScale(interaction, pressedScale = 0.94f)
            .clip(Radii.chip)
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = content,
            maxLines = 1
        )
    }
}

// ─── Gesture feedback ───────────────────────────────────────────────────────

@Composable
private fun AdjustIndicator(
    label: String,
    level: Float,
    modifier: Modifier = Modifier,
) {
    val extras = LocalXiaobaoColors.current
    val safeLevel = level.coerceIn(0f, 1f)
    val isVolume = label.startsWith("音量")
    val icon = when {
        !isVolume -> Icons.Rounded.Brightness6
        safeLevel <= 0.001f -> Icons.AutoMirrored.Rounded.VolumeOff
        else -> Icons.AutoMirrored.Rounded.VolumeUp
    }

    PlayerGlass(
        modifier = modifier.width(136.dp),
        shape = Radii.card,
        strong = true
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(108.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.16f))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .fillMaxHeight(safeLevel)
                        .clip(RoundedCornerShape(50))
                        .background(Brush.verticalGradient(extras.brandGradient))
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "${(safeLevel * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SeekPreviewCard(
    previewMs: Long,
    durationMs: Long,
    isForward: Boolean,
    modifier: Modifier = Modifier,
) {
    val extras = LocalXiaobaoColors.current
    val fraction = if (durationMs > 0L) {
        (previewMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    PlayerGlass(
        modifier = modifier.widthIn(min = 200.dp),
        shape = Radii.card,
        strong = true
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (isForward) {
                        Icons.Rounded.FastForward
                    } else {
                        Icons.Rounded.FastRewind
                    },
                    contentDescription = if (isForward) "快进" else "快退",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = formatTime(previewMs),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "/ ${formatTime(durationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .width(196.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .background(Brush.horizontalGradient(extras.brandGradient))
                )
            }
        }
    }
}

@Composable
private fun SkipIndicator(
    text: String,
    isForward: Boolean,
    revision: Int,
    modifier: Modifier = Modifier,
) {
    val rippleScale = remember { Animatable(0.55f) }
    val rippleAlpha = remember { Animatable(0.55f) }
    LaunchedEffect(revision) {
        rippleScale.snapTo(0.55f)
        rippleAlpha.snapTo(0.55f)
        rippleScale.animateTo(
            targetValue = 1.25f,
            animationSpec = tween(430, easing = Motion.EmphasizedDecelerate)
        )
        rippleAlpha.animateTo(targetValue = 0f, animationSpec = tween(430))
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.22f * rippleAlpha.value),
                            Color.Transparent
                        )
                    ),
                    radius = size.minDimension / 2f * rippleScale.value
                )
            }
            Icon(
                imageVector = if (isForward) {
                    Icons.Rounded.FastForward
                } else {
                    Icons.Rounded.FastRewind
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// ─── Status surfaces ────────────────────────────────────────────────────────

@Composable
private fun BufferingIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center
    ) {
        XiaobaoSpinner(size = 40.dp, strokeWidth = 3.dp)
    }
}

@Composable
private fun PlayerErrorCard(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerGlass(
        modifier = modifier.widthIn(max = 420.dp),
        shape = Radii.card,
        strong = true
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "播放失败",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.66f),
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(18.dp))
            GradientPillButton(
                text = "重试",
                onClick = onRetry,
                icon = Icons.Rounded.Refresh,
                height = 46.dp
            )
        }
    }
}

@Composable
private fun AutoNextCard(
    countdown: Int,
    nextEpisodeName: String?,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlayerGlass(
        modifier = modifier.widthIn(max = 460.dp),
        shape = Radii.card,
        strong = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                ProgressRing(
                    progress = (countdown.toFloat() / AutoNextTotal).coerceIn(0f, 1f),
                    size = 48.dp,
                    strokeWidth = 4.dp,
                    gradient = true
                )
                Text(
                    text = countdown.coerceAtLeast(0).toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "即将播放下一集",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                if (!nextEpisodeName.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = nextEpisodeName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.62f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            GlassPillButton(
                text = "取消",
                onClick = onCancel,
                height = 44.dp,
                contentColor = Color.White,
                container = Color.White.copy(alpha = 0.14f),
                borderColor = Color.White.copy(alpha = 0.20f)
            )
        }
    }
}

// ─── Episode sheet ──────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeSheet(
    sources: List<VideoSource>,
    currentSourceIndex: Int,
    currentEpisodeIndex: Int,
    isLoading: Boolean,
    onSelectEpisode: (Int) -> Unit,
    onSelectSource: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val windowSize = rememberWindowSize()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val episodes = sources.getOrNull(currentSourceIndex)?.episodes.orEmpty()
    val columns = when (windowSize) {
        WindowSize.Compact -> 4
        WindowSize.Medium -> 6
        WindowSize.Expanded -> 8
    }
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val sheetMaxHeight = (screenHeight * 0.72f).coerceIn(320.dp, 640.dp)
    val gridMaxHeight = (sheetMaxHeight - 188.dp).coerceAtLeast(180.dp)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = Radii.sheet,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeight)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "选集",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "共 ${episodes.size} 集",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${(currentEpisodeIndex + 1).coerceAtMost(episodes.size.coerceAtLeast(1))} / ${episodes.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (sources.size > 1) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "播放线路",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(items = sources) { index, source ->
                        SelectChip(
                            text = source.name.ifBlank { "线路 ${index + 1}" },
                            selected = index == currentSourceIndex,
                            onClick = { onSelectSource(index) },
                            modifier = Modifier.heightIn(min = 44.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (episodes.isEmpty()) {
                Text(
                    text = "暂无可用剧集",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = gridMaxHeight),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    itemsIndexed(items = episodes) { index, episode ->
                        EpisodeChip(
                            text = episode.name,
                            selected = index == currentEpisodeIndex,
                            onClick = { if (!isLoading) onSelectEpisode(index) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ─── Small helpers ──────────────────────────────────────────────────────────

/**
 * Marks a floating panel as the top-most pointer target so drags on the panel
 * never leak into the video gesture layer underneath (which would seek or
 * change volume). Child clickables still receive their events first.
 */
private fun Modifier.capturePointerInput(): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent()
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

private fun formatSpeed(speed: Float): String = "${speed}x"
