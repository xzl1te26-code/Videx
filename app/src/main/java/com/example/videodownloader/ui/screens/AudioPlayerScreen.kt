package com.example.videodownloader.ui.screens

import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.app.NotificationManager
import android.app.NotificationChannel
import android.app.Notification
import android.net.Uri
import com.example.videodownloader.performAppHaptic
import com.example.videodownloader.HapticType
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import coil.compose.AsyncImage
import com.example.videodownloader.DownloadedFileItem
import com.example.videodownloader.HistoryManager
import com.example.videodownloader.MediaType
import com.example.videodownloader.AudioControlBus
import com.example.videodownloader.NotificationReceiver
import com.example.videodownloader.MainActivity
import com.example.videodownloader.AsyncLogger
import com.example.videodownloader.LogLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
@Composable
fun AudioPlayerScreen(
    item: DownloadedFileItem,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()

    val activity = LocalContext.current as? ComponentActivity
    DisposableEffect(Unit) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
        onDispose {}
    }

    // 🎵 Загружаем полный плейлист аудиозаписей из Истории
    val historyList by HistoryManager.getHistoryFlow(context).collectAsState(initial = emptyList())
    val audioList = remember(historyList, item) {
        val filtered = historyList.filter { it.mediaType == MediaType.AUDIO && !it.isMissing }
        if (filtered.none { it.id == item.id }) filtered + item else filtered
    }

    val initialIndex = remember(audioList, item) {
        audioList.indexOfFirst { it.id == item.id }.coerceAtLeast(0)
    }

    var currentTrackId by remember { mutableStateOf(item.id) }
    val currentItem = remember(audioList, currentTrackId) {
        audioList.find { it.id == currentTrackId } ?: item
    }

    val exoPlayer = remember {
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true) // 🎧 Авто-управление аудиофокусом
            .setHandleAudioBecomingNoisy(true) // 🎧 Авто-пауза при отключении наушников/Bluetooth
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
                setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)

                val mediaItems = audioList.map { audioItem ->
                    val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(audioItem.name)
                        .setArtist("Videx")
                        .setArtworkUri(
                            audioItem.thumbnailUrl?.takeIf { it.isNotBlank() }?.let {
                                if (it.startsWith("http://") || it.startsWith("https://")) Uri.parse(it)
                                else Uri.fromFile(File(it))
                            } ?: if (audioItem.pathOrUri.startsWith("content://")) audioItem.pathOrUri.toUri() else Uri.fromFile(File(audioItem.pathOrUri))
                        )
                        .build()

                    val uri = if (audioItem.pathOrUri.startsWith("content://")) {
                        audioItem.pathOrUri.toUri()
                    } else {
                        Uri.fromFile(File(audioItem.pathOrUri))
                    }

                    MediaItem.Builder()
                        .setMediaId(audioItem.id)
                        .setUri(uri)
                        .setMediaMetadata(mediaMetadata)
                        .build()
                }

                setMediaItems(mediaItems, initialIndex, 0L)
                prepare()
                playWhenReady = true
            }
    }

    val audioIds = remember(audioList) { audioList.map { it.id } }

    // ⭐️ ДИНАМИЧЕСКОЕ ОБНОВЛЕНИЕ ПЛЕЙЛИСТА EXOPLAYER ПРИ ИЗМЕНЕНИИ СОСТАВА ТРЕКОВ ИЗ ИСТОРИИ
    LaunchedEffect(audioIds) {
        if (audioList.isNotEmpty()) {
            val currentIndex = audioList.indexOfFirst { it.id == currentTrackId }.coerceAtLeast(0)
            val mediaItems = audioList.map { audioItem ->
                val mediaMetadata = androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(audioItem.name)
                    .setArtist("Videx")
                    .setArtworkUri(
                        audioItem.thumbnailUrl?.takeIf { it.isNotBlank() }?.let {
                            if (it.startsWith("http://") || it.startsWith("https://")) Uri.parse(it)
                            else Uri.fromFile(File(it))
                        } ?: if (audioItem.pathOrUri.startsWith("content://")) audioItem.pathOrUri.toUri() else Uri.fromFile(File(audioItem.pathOrUri))
                    )
                    .build()

                val uri = if (audioItem.pathOrUri.startsWith("content://")) {
                    audioItem.pathOrUri.toUri()
                } else {
                    Uri.fromFile(File(audioItem.pathOrUri))
                }

                MediaItem.Builder()
                    .setMediaId(audioItem.id)
                    .setUri(uri)
                    .setMediaMetadata(mediaMetadata)
                    .build()
            }

            val currentPos = exoPlayer.currentPosition
            val isPlayingNow = exoPlayer.isPlaying
            exoPlayer.setMediaItems(mediaItems, currentIndex, currentPos)
            exoPlayer.prepare()
            if (isPlayingNow) exoPlayer.play()
        }
    }

    val mediaSession = remember(exoPlayer) {
        MediaSession.Builder(context, exoPlayer)
            .setId("AudioPlayerMediaSession_${item.id}")
            .build()
    }

    var isPlaying by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isDragging by remember { mutableStateOf(false) }
    var localSliderValue by remember { mutableFloatStateOf(0f) }
    var speedIndex by remember { mutableIntStateOf(0) }
    var isRepeatOne by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var lastPrevClickTime by remember { mutableLongStateOf(0L) }

    val speedOptions = remember { listOf(1.0f, 1.25f, 1.5f, 2.0f) }
    val currentSpeed = speedOptions[speedIndex]

    // 🍎 Apple Music / Spotify Двойной тап перемотки / Предыдущий трек
    fun playPreviousOrRestart() {
        view.performAppHaptic(HapticType.CLICK)
        val currentPos = exoPlayer.currentPosition
        val now = System.currentTimeMillis()
        if (currentPos > 3000L && (now - lastPrevClickTime > 2000L)) {
            // 1-й клик при прослушивании > 3 сек: отматываем текущий трек на начало (0мс)
            exoPlayer.seekTo(0L)
        } else {
            // 2-й клик подряд или позиция < 3 сек: переключаем на ПРЕДЫДУЩИЙ трек в плейлисте
            if (exoPlayer.hasPreviousMediaItem()) {
                exoPlayer.seekToPreviousMediaItem()
            } else {
                exoPlayer.seekTo(0L)
            }
        }
        lastPrevClickTime = now
    }

    fun playNextTrack() {
        view.performAppHaptic(HapticType.CLICK)
        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
        } else {
            exoPlayer.seekTo(0, 0L)
        }
    }

    // 🍎 Apple Music / Spotify Интерактивный масштабируемый эффект обложки (1.0f при игрании, 0.88f при паузе)
    val coverScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.0f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "AppleMusicCoverScale"
    )

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            if (!isDragging) {
                position = exoPlayer.currentPosition
                duration = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(500.milliseconds)
        }
    }

    // ⭐️ Синхронизация Slider с позицией ExoPlayer ТОЛЬКО когда пользователь НЕ тащит ползунок
    LaunchedEffect(position, duration, isDragging) {
        if (!isDragging && duration > 0) {
            localSliderValue = (position.toFloat() / duration).coerceIn(0f, 1f)
        }
    }

    DisposableEffect(exoPlayer, mediaSession) {
        AudioControlBus.onPrevTrack = { playPreviousOrRestart() }
        AudioControlBus.onPlayPause = { if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play() }
        AudioControlBus.onNextTrack = { playNextTrack() }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "audio_player_playback_channel"

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Аудиопроигрыватель",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Управление воспроизведением аудио"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 8888, openIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        fun updateSystemMediaNotification(playing: Boolean, activeItem: DownloadedFileItem) {
            try {
                @Suppress("DEPRECATION")
                val token = mediaSession.sessionCompatToken

                val prevIntent = Intent(context, NotificationReceiver::class.java).apply {
                    action = NotificationReceiver.ACTION_AUDIO_PREV
                }
                val prevPendingIntent = PendingIntent.getBroadcast(
                    context, 8001, prevIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val playPauseIntent = Intent(context, NotificationReceiver::class.java).apply {
                    action = NotificationReceiver.ACTION_AUDIO_PLAY_PAUSE
                }
                val playPausePendingIntent = PendingIntent.getBroadcast(
                    context, 8002, playPauseIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val nextIntent = Intent(context, NotificationReceiver::class.java).apply {
                    action = NotificationReceiver.ACTION_AUDIO_NEXT
                }
                val nextPendingIntent = PendingIntent.getBroadcast(
                    context, 8003, nextIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notifBuilder = NotificationCompat.Builder(context, channelId)
                    .setContentTitle(activeItem.name)
                    .setContentText("${activeItem.sizeFormatted} • Videx")
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentIntent(contentPendingIntent)
                    .addAction(android.R.drawable.ic_media_previous, "Предыдущий", prevPendingIntent)
                    .addAction(if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play, if (playing) "Пауза" else "Воспроизведение", playPausePendingIntent)
                    .addAction(android.R.drawable.ic_media_next, "Следующий", nextPendingIntent)
                    .setOngoing(playing)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setStyle(
                        MediaNotificationCompat.MediaStyle()
                            .setMediaSession(token)
                            .setShowActionsInCompactView(0, 1, 2)
                    )

                notificationManager.notify(2002, notifBuilder.build())
            } catch (e: Exception) {
                AsyncLogger.log(LogLevel.WARN, "Media notification error: ${e.message}")
            }
        }

        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                updateSystemMediaNotification(playing, currentItem)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.let { id ->
                    if (id.isNotBlank()) {
                        currentTrackId = id
                    }
                }
                val activeItem = audioList.find { it.id == mediaItem?.mediaId } ?: currentItem
                updateSystemMediaNotification(exoPlayer.isPlaying, activeItem)
            }
        }
        exoPlayer.addListener(listener)

        updateSystemMediaNotification(isPlaying, currentItem)

        onDispose {
            AudioControlBus.onPrevTrack = null
            AudioControlBus.onPlayPause = null
            AudioControlBus.onNextTrack = null
            exoPlayer.removeListener(listener)
            try {
                notificationManager.cancel(2002)
            } catch (_: Exception) {}
            mediaSession.release()
            exoPlayer.release()
        }
    }

    BackHandler {
        onBack()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить трек?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Text("Аудиофайл «${currentItem.name}» будет полностью удален из памяти устройства.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
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

    Box(modifier = Modifier.fillMaxSize()) {
        // 🌀 Размытый темный градиентный фон с кроссфейдом при смене трека
        Crossfade(targetState = currentItem, animationSpec = tween(350), label = "AudioBgBlurCrossfade") { activeItem ->
            AsyncImage(
                model = activeItem.thumbnailUrl ?: (if (activeItem.pathOrUri.startsWith("content://")) activeItem.pathOrUri.toUri() else File(activeItem.pathOrUri)),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(50.dp)
                    .graphicsLayer(alpha = 0.45f),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 🔝 Шапка с заголовком и кнопками быстрого действия
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    view.performAppHaptic(HapticType.CLICK)
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = Color.White)
                }

                Text(
                    text = "Аудиопроигрыватель",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                IconButton(onClick = {
                    view.performAppHaptic(HapticType.CLICK)
                    HistoryManager.shareFile(context, currentItem)
                }) {
                    Icon(Icons.Default.Share, contentDescription = "Поделиться", tint = Color.White.copy(alpha = 0.9f))
                }

                IconButton(onClick = {
                    view.performAppHaptic(HapticType.CLICK)
                    showDeleteDialog = true
                }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // 🍎 Современная обложка в стиле Apple Music / Spotify с анимированным кроссфейдом и скейлом
            AnimatedContent(
                targetState = currentItem,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(250)) + scaleIn(initialScale = 0.92f))
                        .togetherWith(fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.92f))
                },
                label = "AudioCoverCrossfade"
            ) { activeItem ->
                Surface(
                    modifier = Modifier
                        .size(280.dp)
                        .graphicsLayer {
                            scaleX = coverScale
                            scaleY = coverScale
                            shadowElevation = if (isPlaying) 28f else 12f
                        },
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    shadowElevation = if (isPlaying) 20.dp else 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val thumbUrl = activeItem.thumbnailUrl
                        if (!thumbUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = thumbUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier.size(80.dp),
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.15f),
                                    border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(44.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

            // 🎵 Анимированный заголовок трека и Метаданные (Размер • Формат)
            AnimatedContent(
                targetState = currentItem,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(220)) + slideInVertically { it / 2 })
                        .togetherWith(fadeOut(animationSpec = tween(180)) + slideOutVertically { -it / 2 })
                },
                label = "AudioTitleMetadataTransition"
            ) { activeItem ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 28.dp)
                ) {
                    Text(
                        text = activeItem.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 26.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val formatTag = activeItem.name.substringAfterLast('.', "MP3").uppercase()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "${activeItem.sizeFormatted} • $formatTag",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 📊 Динамический эквалайзер
                    AudioEqualizerWaveform(isPlaying = isPlaying)
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            // 🎛️ Элементы управления воспроизведением
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                val currentDisplayPos = if (isDragging) (localSliderValue * duration).toLong() else position
                Slider(
                    value = localSliderValue,
                    onValueChange = { value ->
                        isDragging = true
                        localSliderValue = value
                    },
                    onValueChangeFinished = {
                        val targetPos = (localSliderValue * duration).toLong()
                        exoPlayer.seekTo(targetPos)
                        position = targetPos
                        isDragging = false
                        view.performAppHaptic(HapticType.PLAYER_GESTURE)
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatTime(currentDisplayPos), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(formatTime(duration), color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 🔁 Кнопка цикла (Repeat)
                    IconButton(
                        onClick = {
                            view.performAppHaptic(HapticType.CLICK)
                            isRepeatOne = !isRepeatOne
                            exoPlayer.repeatMode = if (isRepeatOne) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                        }
                    ) {
                        Icon(
                            if (isRepeatOne) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = "Повтор",
                            tint = if (isRepeatOne) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // ⏭️ Предыдущий трек / Сброс на начало (Spotify/Apple Music)
                    IconButton(
                        onClick = { playPreviousOrRestart() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Предыдущий трек", tint = Color.White, modifier = Modifier.size(34.dp))
                    }

                    // ⏯️ Воспроизведение / Пауза
                    Surface(
                        onClick = {
                            view.performAppHaptic(HapticType.CLICK)
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        },
                        shape = CircleShape,
                        color = Color.White,
                        modifier = Modifier.size(68.dp),
                        shadowElevation = 8.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Воспроизвести/Пауза",
                                tint = Color.Black,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                    }

                    // ⏭️ Следующий трек (Spotify/Apple Music)
                    IconButton(
                        onClick = { playNextTrack() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Следующий трек", tint = Color.White, modifier = Modifier.size(34.dp))
                    }

                    // ⚡️ Переключатель скорости (1.0x -> 1.25x -> 1.5x -> 2.0x)
                    Surface(
                        onClick = {
                            view.performAppHaptic(HapticType.CLICK)
                            speedIndex = (speedIndex + 1) % speedOptions.size
                            exoPlayer.setPlaybackSpeed(speedOptions[speedIndex])
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (currentSpeed > 1.0f) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "${currentSpeed}x",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (currentSpeed > 1.0f) MaterialTheme.colorScheme.onPrimary else Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 📊 Живой анимированный эквалайзер
 */
@Composable
private fun AudioEqualizerWaveform(isPlaying: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "EqualizerAnim")

    val barHeights = List(5) { index ->
        val duration = remember(index) { 350 + index * 120 }
        infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "EqualizerBar_$index"
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(18.dp)
    ) {
        barHeights.forEach { heightState ->
            val animatedHeight = if (isPlaying) heightState.value else 0.25f
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .height((18 * animatedHeight).dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return java.util.Locale.getDefault().let {
        String.format(it, "%02d:%02d", minutes, seconds)
    }
}
