package com.example.videodownloader.ui.screens

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import kotlinx.coroutines.isActive
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.videodownloader.DownloadedFileItem
import com.example.videodownloader.HistoryManager
import com.example.videodownloader.MediaType
import com.example.videodownloader.MainActivity
import com.example.videodownloader.R
import java.io.File
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun PlayerScreen(
    item: DownloadedFileItem,
    onBack: () -> Unit,
    isInPip: Boolean = false,
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val mainActivity = context as? MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val view = LocalView.current

    val videoViewBounds = remember { android.graphics.Rect() }

    // 🎬 Загружаем плейлист видео из Истории
    val historyList by HistoryManager.getHistoryFlow(context).collectAsState(initial = emptyList())
    val videoList = remember(historyList, item) {
        val filtered = historyList.filter { it.mediaType == MediaType.VIDEO && !it.isMissing }
        if (filtered.none { it.id == item.id }) filtered + item else filtered
    }

    val initialIndex = remember(videoList, item) {
        videoList.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
    }

    var currentTrackId by remember { mutableStateOf(item.id) }
    val currentItem = remember(videoList, currentTrackId) {
        videoList.find { it.id == currentTrackId } ?: item
    }

    // 🎬 ExoPlayer setup (Hardware Composer Surface + Auto-Pause on Unplug + Audio Focus)
    val exoPlayer = remember {
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true) // 🎧 Авто-управление аудиофокусом
            .setHandleAudioBecomingNoisy(true) // 🎧 Авто-пауза при отключении наушников/Bluetooth
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
                setSeekParameters(androidx.media3.exoplayer.SeekParameters.DEFAULT)
                val mediaItems = videoList.map { videoItem ->
                    val uri = if (videoItem.pathOrUri.startsWith("content://")) {
                        videoItem.pathOrUri.toUri()
                    } else {
                        Uri.fromFile(File(videoItem.pathOrUri))
                    }
                    MediaItem.Builder()
                        .setMediaId(videoItem.id)
                        .setUri(uri)
                        .build()
                }
                setMediaItems(mediaItems, initialIndex, 0L)
                prepare()
                playWhenReady = true
            }
    }

    // ⭐️ ДИНАМИЧЕСКОЕ ОБНОВЛЕНИЕ ПЛЕЙЛИСТА ПРИ ЗАГРУЗКЕ
    LaunchedEffect(videoList) {
        if (videoList.isNotEmpty()) {
            val currentIndex = videoList.indexOfFirst { it.id == currentTrackId }.coerceAtLeast(0)
            val mediaItems = videoList.map { videoItem ->
                val uri = if (videoItem.pathOrUri.startsWith("content://")) {
                    videoItem.pathOrUri.toUri()
                } else {
                    Uri.fromFile(File(videoItem.pathOrUri))
                }
                MediaItem.Builder()
                    .setMediaId(videoItem.id)
                    .setUri(uri)
                    .build()
            }
            val currentPos = exoPlayer.currentPosition
            val isPlayingNow = exoPlayer.isPlaying
            exoPlayer.setMediaItems(mediaItems, currentIndex, currentPos)
            exoPlayer.prepare()
            if (isPlayingNow) exoPlayer.play()
        }
    }

    var isPlaying by remember { mutableStateOf(value = true) }
    var showControls by remember { mutableStateOf(value = true) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(value = false) }
    var isGestureInteracting by remember { mutableStateOf(value = false) }

    // Resize Mode
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var resizeIndicatorText by remember { mutableStateOf("") }
    var showResizeIndicator by remember { mutableStateOf(false) }
    var wasPlayingBeforeDrag by remember { mutableStateOf(false) }

    // Speed Control
    var speedIndex by remember { mutableIntStateOf(0) }
    val speedOptions = remember { listOf(1.0f, 1.25f, 1.5f, 2.0f) }

    // Dialogs
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isTrackSwitching by remember { mutableStateOf(false) }

    // Gesture states
    var gestureType by remember { mutableStateOf<GestureType?>(null) }
    var gestureValue by remember { mutableFloatStateOf(0f) } // 0.0 to 1.0
    var showGestureIndicator by remember { mutableStateOf(value = false) }

    // Seek animation state
    var seekAction by remember { mutableStateOf<SeekAction?>(null) }

    // Gesture & Seek Timer management
    var gestureJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var seekJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    var controlsResetTrigger by remember { mutableLongStateOf(0L) }

    fun resetControlsTimer() {
        controlsResetTrigger = System.currentTimeMillis()
    }

    // Immersive Mode handling
    LaunchedEffect(showControls, isInPip) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (showControls && !isInPip) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Auto-hide controls: 🧠 Адаптивный умный таймер (сбрасывается при любой активности)
    LaunchedEffect(showControls, isPlaying, isInPip, isDragging, isGestureInteracting, controlsResetTrigger) {
        if (isInPip) {
            showControls = false
            return@LaunchedEffect
        }
        // Скрываем только если: плеер играет, интерфейс показан и пользователь НИЧЕГО не трогает
        if (showControls && isPlaying && !isDragging && !isGestureInteracting) {
            delay(4000.milliseconds)
            showControls = false
        }
    }

    // Progress update: 🧠 Непрерывное обновление 200 мс даже на паузе
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            if (!isDragging) {
                playbackPosition = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(200.milliseconds)
        }
    }

    LaunchedEffect(exoPlayer) {
        mainActivity?.pipCommands?.collect { type ->
            when (type) {
                MainActivity.CONTROL_TYPE_PLAY_PAUSE -> {
                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                }
                MainActivity.CONTROL_TYPE_BACK -> exoPlayer.seekTo(exoPlayer.currentPosition - 10000)
                MainActivity.CONTROL_TYPE_FORWARD -> exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
            }
        }
    }

    val currentIsInPip by rememberUpdatedState(isInPip)

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (playing) {
                    isTrackSwitching = false
                }
                mainActivity?.updatePipPlaybackState(playing)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED) {
                    isTrackSwitching = false
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if ((videoSize.width > 0) && (videoSize.height > 0)) {
                    mainActivity?.updatePipAspectRatio(videoSize.width, videoSize.height)
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.let { id ->
                    if (id.isNotBlank()) currentTrackId = id
                }
                isTrackSwitching = true
            }
        }
        exoPlayer.addListener(listener)

        var wasPlayingBeforePause = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    wasPlayingBeforePause = exoPlayer.isPlaying
                    val isActivityInPip = activity?.isInPictureInPictureMode ?: false
                    val isLeavingForPip = mainActivity?.isLeavingForPip ?: false
                    if (!isActivityInPip && !currentIsInPip && !isLeavingForPip) {
                        exoPlayer.pause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (wasPlayingBeforePause) {
                        exoPlayer.play()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            // Reset immersive mode
            activity?.window?.let { window ->
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    BackHandler {
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onBack()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить видео?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Text("Видеофайл «${currentItem.name}» будет полностью удален из памяти устройства.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            coroutineScope.launch {
                                HistoryManager.deleteFile(context, currentItem)
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Удалить", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showDeleteDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            dismissButton = {}
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!isInPip) {
                            showControls = !showControls
                            if (showControls) resetControlsTimer()
                        }
                    },
                    onDoubleTap = { offset ->
                        if (isInPip) return@detectTapGestures
                        resetControlsTimer()
                        val isRight = offset.x > size.width / 2
                        val seekTime = 10000L
                        if (isRight) {
                            exoPlayer.seekTo(exoPlayer.currentPosition + seekTime)
                            seekAction = SeekAction.Forward
                        } else {
                            exoPlayer.seekTo(exoPlayer.currentPosition - seekTime)
                            seekAction = SeekAction.Backward
                        }
                        seekJob?.cancel()
                        seekJob = coroutineScope.launch {
                            delay(650.milliseconds)
                            seekAction = null
                        }
                    }
                )
            }
            .pointerInput(showControls, isInPip) {
                if (isInPip) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val dragEvent = event.changes.firstOrNull() ?: continue

                        // ✋ Ignore gestures starting in control zones if they are visible
                        if (showControls) {
                            val topZone = size.height * 0.15f
                            val bottomZone = size.height * 0.75f
                            if ((dragEvent.position.y < topZone) || (dragEvent.position.y > bottomZone)) {
                                continue
                            }
                        }

                        if (dragEvent.pressed && !dragEvent.isConsumed) {
                            isGestureInteracting = true
                            val isLeft = dragEvent.position.x < size.width / 2
                            val type = if (isLeft) GestureType.Brightness else GestureType.Volume

                            val currentValue = if (type == GestureType.Brightness) {
                                val current = activity?.window?.attributes?.screenBrightness ?: 0.5f
                                if (current < 0) 0.5f else current
                            } else {
                                val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
                                audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
                            }

                            var totalDragY = 0f
                            var totalDragX = 0f
                            var isGestureStarted = false
                            val touchSlop = viewConfiguration.touchSlop

                            while (true) {
                                val nextEvent = awaitPointerEvent()
                                val nextDrag = nextEvent.changes.firstOrNull() ?: break

                                if (nextDrag.isConsumed) break
                                if (!nextDrag.pressed) break

                                val deltaY = nextDrag.previousPosition.y - nextDrag.position.y
                                val deltaX = nextDrag.previousPosition.x - nextDrag.position.x
                                totalDragY += deltaY
                                totalDragX += abs(deltaX)

                                if (!isGestureStarted && abs(totalDragY) > touchSlop) {
                                    if (abs(totalDragY) > totalDragX) {
                                        isGestureStarted = true
                                    } else if (totalDragX > touchSlop) {
                                        break
                                    }
                                }

                                if (isGestureStarted) {
                                    val newValue = (currentValue + totalDragY / size.height).coerceIn(0f, 1f)

                                    gestureType = type
                                    gestureValue = newValue
                                    showGestureIndicator = true

                                    if (type == GestureType.Brightness) {
                                        val lp = activity?.window?.attributes
                                        lp?.screenBrightness = newValue
                                        activity?.window?.attributes = lp
                                    } else {
                                        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (newValue * max).toInt(), 0)
                                    }
                                    nextDrag.consume()
                                }
                            }

                            if (isGestureStarted) {
                                gestureJob?.cancel()
                                gestureJob = coroutineScope.launch {
                                    delay(1200.milliseconds)
                                    showGestureIndicator = false
                                }
                            }
                            // ✋ Пользователь закончил жест
                            isGestureInteracting = false
                            resetControlsTimer()
                        }
                    }
                }
            }
    ) {
        // 🎞 Video View
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = {
                it.resizeMode = resizeMode
            },
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { layoutCoordinates ->
                    val rect = layoutCoordinates.boundsInWindow()
                    videoViewBounds.set(
                        rect.left.toInt(),
                        rect.top.toInt(),
                        rect.right.toInt(),
                        rect.bottom.toInt()
                    )
                    mainActivity?.updatePipSourceRect(videoViewBounds)
                }
        )

        // 🎬 ПЛАВНЫЙ КРОССФЕЙД ПРЕВЬЮ ПРИ СМЕНЕ ВИДЕО В ПЛЕЙЛИСТЕ
        val thumbData = remember(currentItem) {
            currentItem.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { url ->
                if (url.startsWith("http://") || url.startsWith("https://")) url
                else File(url)
            } ?: if (currentItem.pathOrUri.startsWith("content://")) currentItem.pathOrUri.toUri() else File(currentItem.pathOrUri)
        }

        AnimatedVisibility(
            visible = isTrackSwitching,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(280)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = thumbData,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer(alpha = 0.85f)
                )
            }
        }

        val controlsAlpha by animateFloatAsState(
            targetValue = if (showControls && !isInPip) 1f else 0f,
            animationSpec = tween(durationMillis = 400, easing = LinearOutSlowInEasing),
            label = "ControlsAlpha"
        )

        if (controlsAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.45f * controlsAlpha),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.45f * controlsAlpha)
                            )
                        )
                    )
            )
        }

        // 📐 Всплывающий индикатор Resize Mode
        if (!isInPip) {
            AnimatedVisibility(
                visible = showResizeIndicator,
                enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.92f) + slideInVertically(initialOffsetY = { -it / 3 }),
                exit = fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.92f) + slideOutVertically(targetOffsetY = { -it / 3 }),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    tonalElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AspectRatio, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = resizeIndicatorText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        val topBarOffset by animateDpAsState(
            targetValue = if (showControls) 0.dp else (-120).dp,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            label = "TopBarOffset"
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = topBarOffset)
                .graphicsLayer(alpha = controlsAlpha)
        ) {
            TopPlayerBar(
                item = currentItem,
                onBack = {
                    onBack()
                },
                resizeMode = resizeMode,
                isLandscape = isLandscape,
                onResizeToggle = {
                    resetControlsTimer()
                    resizeMode = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                    resizeIndicatorText = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> "Оригинал (Fit)"
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Заполнение (Zoom)"
                        else -> "Растянуть (Fill)"
                    }
                    showResizeIndicator = true
                    coroutineScope.launch {
                        delay(1500)
                        showResizeIndicator = false
                    }
                },
                onShare = { HistoryManager.shareFile(context, currentItem) },
                onDelete = { showDeleteDialog = true }
            )
        }

        val bottomBarOffset by animateDpAsState(
            targetValue = if (showControls) 0.dp else 140.dp,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            label = "BottomBarOffset"
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = bottomBarOffset)
                .graphicsLayer(alpha = controlsAlpha)
        ) {
            BottomPlayerControls(
                exoPlayer = exoPlayer,
                isPlaying = isPlaying,
                position = playbackPosition,
                duration = duration,
                isDragging = isDragging,
                isLandscape = isLandscape,
                speedIndex = speedIndex,
                onSpeedChange = {
                    resetControlsTimer()
                    speedIndex = (speedIndex + 1) % speedOptions.size
                    exoPlayer.setPlaybackSpeed(speedOptions[speedIndex])
                },
                onNext = {
                    resetControlsTimer()
                    if (exoPlayer.hasNextMediaItem()) exoPlayer.seekToNextMediaItem() else exoPlayer.seekTo(0, 0L)
                },
                onPrev = {
                    resetControlsTimer()
                    val currentPos = exoPlayer.currentPosition
                    if (currentPos > 3000L) {
                        exoPlayer.seekTo(0L)
                    } else {
                        if (exoPlayer.hasPreviousMediaItem()) exoPlayer.seekToPreviousMediaItem() else exoPlayer.seekTo(0L)
                    }
                },
                onDragStart = {
                    isDragging = true
                    wasPlayingBeforeDrag = exoPlayer.isPlaying
                    if (wasPlayingBeforeDrag) exoPlayer.pause()
                    resetControlsTimer()
                },
                onDragEnd = { targetValue ->
                    val targetMs = (targetValue * duration).toLong().coerceIn(0L, duration.coerceAtLeast(1L))
                    playbackPosition = targetMs
                    exoPlayer.seekTo(targetMs)
                    if (wasPlayingBeforeDrag) exoPlayer.play()
                    coroutineScope.launch {
                        delay(200.milliseconds)
                        isDragging = false
                    }
                    resetControlsTimer()
                },
                onInteraction = { resetControlsTimer() }
            )
        }

        val centerScale by animateFloatAsState(
            targetValue = if (showControls) 1f else 0.8f,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "CenterScale"
        )

        if (controlsAlpha > 0.5f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer(scaleX = centerScale, scaleY = centerScale, alpha = (controlsAlpha - 0.5f) * 2f)
            ) {
                val buttonSize = if (isLandscape) 72.dp else 88.dp
                val iconSize = if (isLandscape) 40.dp else 48.dp

                Surface(
                    onClick = {
                        resetControlsTimer()
                        if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.size(buttonSize)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedContent(
                            targetState = isPlaying,
                            transitionSpec = {
                                scaleIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) togetherWith
                                scaleOut()
                            },
                            label = "PlayPause"
                        ) { playing ->
                            Icon(
                                if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                    }
                }
            }
        }

        if (!isInPip) {
            AnimatedVisibility(
                visible = showGestureIndicator,
                enter = fadeIn(animationSpec = tween(150)) + scaleIn(initialScale = 0.92f) + slideInVertically(initialOffsetY = { -it / 3 }),
                exit = fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.92f) + slideOutVertically(targetOffsetY = { -it / 3 }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = if (isLandscape) 64.dp else 96.dp)
            ) {
                GestureIndicator(type = gestureType, value = gestureValue)
            }
            SeekVisualFeedback(seekAction = seekAction, isLandscape = isLandscape)
        }
    }
}

@Composable
private fun SeekVisualFeedback(seekAction: SeekAction?, isLandscape: Boolean) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = seekAction != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(
                if (seekAction == SeekAction.Forward) Alignment.CenterEnd else Alignment.CenterStart
            ).padding(horizontal = if (isLandscape) 80.dp else 48.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (seekAction == SeekAction.Forward) Icons.Default.Forward10 else Icons.Default.Replay10,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(if (isLandscape) 56.dp else 64.dp)
                )
                Text(
                    text = if (seekAction == SeekAction.Forward) "+10s" else "-10s",
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GestureIndicator(type: GestureType?, value: Float) {
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "GestureValueSmoothAnim"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(12.dp).copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (type) {
                    GestureType.Volume -> if (animatedValue > 0.5f) Icons.AutoMirrored.Filled.VolumeUp else if (animatedValue > 0f) Icons.AutoMirrored.Filled.VolumeDown else Icons.AutoMirrored.Filled.VolumeMute
                    GestureType.Brightness -> Icons.Default.Brightness6
                    null -> Icons.Default.Info
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            LinearProgressIndicator(
                progress = { animatedValue },
                modifier = Modifier.width(110.dp).height(6.dp).clip(CircleShape),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "${kotlin.math.round(animatedValue * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(36.dp)
            )
        }
    }
}

@UnstableApi
@Composable
fun TopPlayerBar(
    item: DownloadedFileItem,
    onBack: () -> Unit,
    resizeMode: Int,
    isLandscape: Boolean,
    onResizeToggle: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val background = if (isLandscape) {
        Brush.verticalGradient(
            0.0f to Color.Black.copy(alpha = 0.8f),
            0.5f to Color.Black.copy(alpha = 0.4f),
            1.0f to Color.Transparent
        )
    } else {
        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isLandscape) Modifier.background(background) else Modifier)
    ) {
        val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        var stableTopPadding by remember { mutableStateOf(value = 32.dp) }
        LaunchedEffect(statusBarHeight) {
            if (statusBarHeight > 0.dp) stableTopPadding = statusBarHeight
        }

        Spacer(Modifier.height(stableTopPadding))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = if (isLandscape) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLandscape) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            AnimatedContent(
                targetState = item,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + slideInVertically { it / 2 })
                        .togetherWith(fadeOut(animationSpec = tween(180)) + slideOutVertically { -it / 2 })
                },
                label = "VideoTitleTransition",
                modifier = Modifier.weight(1f)
            ) { currentVideo ->
                Column {
                    Text(
                        text = currentVideo.name,
                        style = if (isLandscape) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        color = if (isLandscape) Color.White else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val formatTag = currentVideo.name.substringAfterLast('.', "MP4").uppercase()
                    Text(
                        text = "${currentVideo.sizeFormatted} • $formatTag",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLandscape) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onShare,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Поделиться", tint = if (isLandscape) Color.White else MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onResizeToggle,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    imageVector = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> Icons.Default.FitScreen
                        AspectRatioFrameLayout.RESIZE_MODE_FILL -> Icons.Default.AspectRatio
                        else -> Icons.Default.ZoomOutMap
                    },
                    contentDescription = "Resize Mode",
                    tint = if (isLandscape) Color.White else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun BottomPlayerControls(
    exoPlayer: ExoPlayer,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    isDragging: Boolean,
    isLandscape: Boolean,
    speedIndex: Int,
    onSpeedChange: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onDragStart: () -> Unit,
    onDragEnd: (Float) -> Unit,
    onInteraction: () -> Unit = {}
) {
    var localSliderValue by remember {
        mutableFloatStateOf(if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f)
    }

    LaunchedEffect(position, duration, isDragging) {
        if (!isDragging && duration > 0) {
            localSliderValue = (position.toFloat() / duration).coerceIn(0f, 1f)
        }
    }

    if (isLandscape) {
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        var stableBottomPadding by remember { mutableStateOf(value = 16.dp) }
        LaunchedEffect(navBarPadding) {
            if (navBarPadding > 0.dp) stableBottomPadding = navBarPadding
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.4f to Color.Black.copy(alpha = 0.4f),
                        1.0f to Color.Black.copy(alpha = 0.85f)
                    )
                )
                .padding(bottom = stableBottomPadding)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentPos = if (isDragging) (localSliderValue * duration).toLong() else position
                    Text(
                        text = formatTime(currentPos),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(55.dp),
                        textAlign = TextAlign.Start
                    )

                    Slider(
                        value = localSliderValue,
                        onValueChange = {
                            localSliderValue = it
                            if (!isDragging) onDragStart()
                        },
                        onValueChangeFinished = {
                            onDragEnd(localSliderValue)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.weight(1f).height(32.dp).padding(horizontal = 8.dp)
                    )

                    Text(
                        text = formatTime(duration),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(55.dp),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ⚡️ Переключатель скорости
                    Surface(
                        onClick = onSpeedChange,
                        shape = RoundedCornerShape(12.dp),
                        color = if (speedIndex > 0) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        val speedValue = listOf(1.0f, 1.25f, 1.5f, 2.0f)[speedIndex]
                        Text(
                            text = "${speedValue}x",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (speedIndex > 0) MaterialTheme.colorScheme.onPrimary else Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // ⏭️ Предыдущий трек
                    IconButton(
                        onClick = onPrev,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Предыдущее", tint = Color.White, modifier = Modifier.size(26.dp))
                    }

                    // ⏪ Назад на 10 секунд
                    IconButton(onClick = {
                        onInteraction()
                        exoPlayer.seekTo(exoPlayer.currentPosition - 10000)
                    }) {
                        Icon(Icons.Default.Replay10, contentDescription = null, tint = Color.White)
                    }

                    Surface(
                        onClick = {
                            onInteraction()
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // ⏩ Вперед на 10 секунд
                    IconButton(onClick = {
                        onInteraction()
                        exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
                    }) {
                        Icon(Icons.Default.Forward10, contentDescription = null, tint = Color.White)
                    }

                    // ⏭️ Следующий трек
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Следующее", tint = Color.White, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
    } else {
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        var stableBottomPadding by remember { mutableStateOf(value = 24.dp) }
        LaunchedEffect(navBarPadding) {
            if (navBarPadding > 0.dp) stableBottomPadding = navBarPadding
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = stableBottomPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentPos = if (isDragging) (localSliderValue * duration).toLong() else position
                    Text(
                        text = formatTime(currentPos),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(45.dp),
                        textAlign = TextAlign.Start
                    )

                    Slider(
                        value = localSliderValue,
                        onValueChange = {
                            localSliderValue = it
                            if (!isDragging) onDragStart()
                        },
                        onValueChangeFinished = {
                            onDragEnd(localSliderValue)
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.weight(1f).height(24.dp).padding(horizontal = 8.dp)
                    )

                    Text(
                        text = formatTime(duration),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(45.dp),
                        textAlign = TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ⚡️ Переключатель скорости
                    Surface(
                        onClick = onSpeedChange,
                        shape = RoundedCornerShape(12.dp),
                        color = if (speedIndex > 0) MaterialTheme.colorScheme.primary else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    ) {
                        val speedValue = listOf(1.0f, 1.25f, 1.5f, 2.0f)[speedIndex]
                        Text(
                            text = "${speedValue}x",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (speedIndex > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // ⏭️ Предыдущий трек
                    IconButton(
                        onClick = onPrev,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Предыдущее", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    }

                    // ⏪ Назад на 10 секунд
                    IconButton(onClick = {
                        onInteraction()
                        exoPlayer.seekTo(exoPlayer.currentPosition - 10000)
                    }) {
                        Icon(Icons.Default.Replay10, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    Surface(
                        onClick = {
                            onInteraction()
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp),
                        shadowElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // ⏩ Вперед на 10 секунд
                    IconButton(onClick = {
                        onInteraction()
                        exoPlayer.seekTo(exoPlayer.currentPosition + 10000)
                    }) {
                        Icon(Icons.Default.Forward10, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }

                    // ⏭️ Следующий трек
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Следующее", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    }
                }
            }
        }
    }
}
private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return java.util.Locale.getDefault().let { locale ->
        if (hours > 0) {
            String.format(locale, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(locale, "%02d:%02d", minutes, seconds)
        }
    }
}

private enum class GestureType { Volume, Brightness }
private enum class SeekAction { Forward, Backward }
