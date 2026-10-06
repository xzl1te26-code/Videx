package com.example.videodownloader.ui.screens

import android.content.ClipboardManager
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration.Companion.milliseconds
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import com.example.videodownloader.performAppHaptic
import com.example.videodownloader.HapticType
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.WorkInfo
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.videodownloader.*
import com.example.videodownloader.logic.AnalysisManager
import com.example.videodownloader.logic.DownloadManager
import com.example.videodownloader.logic.DownloadTaskStatus
import com.example.videodownloader.ui.components.*
import com.example.videodownloader.ui.components.showInstantSnackbar
import com.example.videodownloader.ui.models.CleanQualityOption
import com.example.videodownloader.utils.detectPlatformName
import com.example.videodownloader.utils.isValidUrl
import com.example.videodownloader.utils.extractUrlFromText
import com.example.videodownloader.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(snackbarHostState: SnackbarHostState, viewModel: MainViewModel) {
    val context = LocalContext.current
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val url by AnalysisManager.url.collectAsState()
    val isAnalyzing by AnalysisManager.isAnalyzing.collectAsState()
    val resultTitle by AnalysisManager.resultTitle.collectAsState()
    val resultThumbnail by AnalysisManager.resultThumbnail.collectAsState()
    val isPhotoPost by AnalysisManager.isPhotoPost.collectAsState()
    val errorMessage by AnalysisManager.errorMessage.collectAsState()
    val availableQualities by AnalysisManager.availableQualities.collectAsState()
    val isPlaylist by AnalysisManager.isPlaylist.collectAsState()
    val playlistEntries by AnalysisManager.playlistEntries.collectAsState()

    val activeTasks by DownloadManager.activeTasks.collectAsState()
    val isDownloading by DownloadManager.isDownloading.collectAsState()
    val queueCount by DownloadManager.queueCount.collectAsState()

    val showThumbnails by SettingsManager.showThumbnails.collectAsState()
    val showQualitySelector by SettingsManager.showQualitySelector.collectAsState()
    val checkDuplicates by SettingsManager.checkDuplicates.collectAsState()
    val useClipboardBubble by SettingsManager.useClipboardBubble.collectAsState()
    val useClipboardPreview by SettingsManager.useClipboardPreview.collectAsState()
    val useClipboardHistory by SettingsManager.useClipboardHistory.collectAsState()
    val recentClipboardUrls by SettingsManager.recentClipboardUrls.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    var clipboardUrl by remember { mutableStateOf<String?>(null) }
    var clipboardMetadata by remember { mutableStateOf<VideoMetadata?>(null) }
    var showRecentHistorySheet by remember { mutableStateOf(false) }

    LaunchedEffect(clipboardUrl, useClipboardPreview) {
        val currentClip = clipboardUrl
        if (useClipboardPreview && !currentClip.isNullOrBlank()) {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val passportFile = CacheManager.getPassportFile(context, currentClip)
                    if (passportFile.exists()) {
                        val jsonStr = passportFile.readText()
                        val json = org.json.JSONObject(jsonStr)
                        val title = json.optString("title", "")
                        val thumb = json.optString("thumbnail", "")
                        if (title.isNotBlank()) {
                            clipboardMetadata = VideoMetadata(title = title, thumbnailUrl = thumb)
                            return@withContext
                        }
                    }

                    // 🧠 Оптимизация: Для смарт-превью делаем легкий HTTP-запрос (без запуска тяжелого yt-dlp)
                    // Используем timeout 3 секунды. Если не вышло — просто ничего не показываем
                    val lightMeta = fetchLightMeta(currentClip)
                    clipboardMetadata = lightMeta
                } catch (_: Exception) {
                    clipboardMetadata = null
                }
            }
        } else {
            clipboardMetadata = null
        }
    }

    fun updateClipboardStatus() {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
            if (!clipboard.hasPrimaryClip()) {
                clipboardUrl = null
                return
            }

            val clipData = clipboard.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val item = clipData.getItemAt(0)
                val clipText = item?.text?.toString() ?: item?.uri?.toString() ?: ""

                val extractedUrl = extractUrlFromText(clipText)
                if (extractedUrl.isNotBlank() && isValidUrl(extractedUrl)) {
                    if (extractedUrl != viewModel.dismissedClipboardUrl && extractedUrl != clipboardUrl) {
                        viewModel.dismissedClipboardUrl = null
                    }
                    clipboardUrl = extractedUrl
                    SettingsManager.addRecentClipboardUrl(extractedUrl)
                } else {
                    clipboardUrl = null
                }
            } else {
                clipboardUrl = null
            }
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.WARN, "Smart clipboard check suppressed: ${e.message}")
        }
    }

    // Умная проверка буфера обмена с задержкой для получения фокуса окна (Android 10+)
    LaunchedEffect(lifecycleOwner) {
        updateClipboardStatus()
        delay(250.milliseconds)
        updateClipboardStatus()
        delay(350.milliseconds)
        updateClipboardStatus()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    updateClipboardStatus()
                    delay(300.milliseconds)
                    updateClipboardStatus()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(checkDuplicates) {
        viewModel.checkDuplicate(context, checkDuplicates)
    }

    val platformName = remember(url) { detectPlatformName(url) }
    val isCurrentUrlValid = remember(url) { isValidUrl(url) }

    if (viewModel.showQualityDialog) {
        val qualityList = remember(availableQualities) {
            val list = mutableListOf(
                CleanQualityOption(
                    id = "best",
                    tag = "AUTO",
                    title = "Авто (Лучшее)",
                    subtitle = "Оригинальное качество источника"
                )
            )
            val descMap = mapOf(
                "2160" to ("4K" to ("4K Ultra HD" to "Предельная детализация")),
                "1440" to ("2K" to ("2K Quad HD" to "Повышенная четкость")),
                "1080" to ("1080p" to ("1080p Full HD" to "Оптимально для экранов смартфонов")),
                "720"  to ("720p" to ("720p HD" to "Баланс четкости и размера файла")),
                "480"  to ("480p" to ("480p SD" to "Экономия памяти устройства")),
                "360"  to ("360p" to ("360p" to "Минимальный вес файла")),
                "240"  to ("240p" to ("240p" to "Для медленного интернета")),
                "144"  to ("144p" to ("144p" to "Экономия мобильного трафика"))
            )
            for (q in availableQualities) {
                val (tag, info) = descMap[q] ?: ("${q}p" to ("${q}p" to "Стандартное качество"))
                val (title, subtitle) = info
                list.add(CleanQualityOption(q, tag, title, subtitle))
            }
            list
        }

        AlertDialog(
            onDismissRequest = { viewModel.showQualityDialog = false },
            title = {
                Text(
                    text = "Качество видео",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(scrollState)
                ) {
                    qualityList.forEach { option ->
                        val isSelected = viewModel.selectedVideoQuality == option.id
                        val itemBg by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            label = "itemBg"
                        )
                        val itemBorder by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            label = "itemBorder"
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = itemBg,
                            border = BorderStroke(1.dp, itemBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    viewModel.updateVideoQuality(option.id)
                                    viewModel.showQualityDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = option.tag,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = option.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(text = option.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                RadioButton(selected = isSelected, onClick = { viewModel.updateVideoQuality(option.id); viewModel.showQualityDialog = false })
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { viewModel.showQualityDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Закрыть") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 115.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val showDynamicTips by SettingsManager.showDynamicTips.collectAsState()
            val tipFrames by SettingsManager.tipFrames.collectAsState()
            val tipAnimationSpeedMs by SettingsManager.tipAnimationSpeedMs.collectAsState()
            val tipPlaybackMode by SettingsManager.tipPlaybackMode.collectAsState()
            val tipConfigVersion by SettingsManager.tipConfigVersion.collectAsState()

            var phraseIndex by remember { mutableIntStateOf(0) }
            var lastPlayedVersion by remember { mutableIntStateOf(-1) }

            LaunchedEffect(showDynamicTips, tipFrames, tipAnimationSpeedMs, tipPlaybackMode, tipConfigVersion) {
                if (lastPlayedVersion != tipConfigVersion) {
                    viewModel.hasTipAnimationPlayedThisSession = false
                    lastPlayedVersion = tipConfigVersion
                }

                val shouldAnimate = showDynamicTips && tipFrames.isNotEmpty() && (
                    tipPlaybackMode == com.example.videodownloader.TipPlaybackMode.EVERY_NAVIGATION || !viewModel.hasTipAnimationPlayedThisSession
                )

                if (shouldAnimate) {
                    viewModel.hasTipAnimationPlayedThisSession = true
                    phraseIndex = 0
                    for (i in 1 until tipFrames.size) {
                        delay(tipAnimationSpeedMs)
                        phraseIndex = i
                    }
                } else {
                    phraseIndex = (tipFrames.size - 1).coerceAtLeast(0)
                }
            }

            val currentFrame = remember(tipFrames, phraseIndex, showDynamicTips) {
                if (showDynamicTips && tipFrames.isNotEmpty()) {
                    tipFrames[phraseIndex.coerceIn(0, tipFrames.size - 1)]
                } else {
                    tipFrames.lastOrNull() ?: TipFrameData("bolt", "Вставьте ссылку — остальное сделает Videx")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Videx",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                AnimatedContent(
                    targetState = currentFrame,
                    transitionSpec = {
                        (slideInVertically(animationSpec = tween(380, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)))
                            .togetherWith(
                                slideOutVertically(animationSpec = tween(380, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(200))
                            )
                    },
                    label = "HeroPhraseTransition"
                ) { frame ->
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = com.example.videodownloader.ui.screens.settings.getIconForFrameKey(frame.iconKey),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = frame.text,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = com.example.videodownloader.ui.theme.getAppCardBorder()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { AnalysisManager.setUrl(it) },
                        placeholder = { Text("Вставьте ссылку...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        leadingIcon = {
                            AnimatedContent(
                                targetState = platformName,
                                transitionSpec = {
                                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                                },
                                label = "PlatformIconAnim"
                            ) { platform ->
                                Icon(
                                    imageVector = when {
                                        platform.contains("YouTube", ignoreCase = true) -> Icons.Default.PlayArrow
                                        platform.contains("TikTok", ignoreCase = true) -> Icons.Default.MusicNote
                                        platform.contains("Instagram", ignoreCase = true) -> Icons.Default.PhotoLibrary
                                        else -> Icons.Default.Link
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        trailingIcon = {
                            AnimatedContent(
                                targetState = when {
                                    url.isNotEmpty() -> "clear"
                                    clipboardUrl != null -> "paste_active"
                                    else -> "paste_inactive"
                                },
                                transitionSpec = {
                                    (fadeIn() + scaleIn()).togetherWith(fadeOut() + scaleOut())
                                },
                                label = "TrailingIconAnim"
                            ) { state ->
                                when (state) {
                                    "clear" -> {
                                        IconButton(onClick = { AnalysisManager.clear(); focusManager.clearFocus() }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Очистить")
                                        }
                                    }
                                    "paste_active" -> {
                                        IconButton(onClick = {
                                            clipboardUrl?.let {
                                                AnalysisManager.setUrl(it)
                                                coroutineScope.launch { snackbarHostState.showInstantSnackbar("Ссылка вставлена") }
                                                focusManager.clearFocus()
                                            }
                                        }) {
                                            Icon(Icons.Default.ContentPaste, contentDescription = "Вставить", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                    "paste_inactive" -> {
                                        IconButton(onClick = {
                                            coroutineScope.launch { snackbarHostState.showInstantSnackbar("В буфере нет ссылки") }
                                        }) {
                                            Icon(
                                                Icons.Default.ContentPasteOff,
                                                contentDescription = "Буфер пуст",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        singleLine = true,
                        enabled = !isAnalyzing,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        // 💎 Смарт-детектор: Полностью переработанный премиальный дизайн (Dynamic Glass & Fluid Layout)
        item {
            val showBubble = useClipboardBubble && clipboardUrl != null && clipboardUrl != url && clipboardUrl != viewModel.dismissedClipboardUrl
            
            AnimatedVisibility(
                visible = showBubble,
                enter = fadeIn(animationSpec = tween(400)) + expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
            ) {
                clipboardUrl?.let { clipUrl ->
                    val clipPlatform = remember(clipUrl) { detectPlatformName(clipUrl) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        // 💎 PREMIUM SMART DETECTOR (Google Material You Concept)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            border = BorderStroke(
                                1.dp, 
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            view.performAppHaptic(HapticType.CLICK)
                                            AnalysisManager.setUrl(clipUrl)
                                            focusManager.clearFocus()
                                        }
                                        .padding(16.dp)
                                ) {
                                    // Шапка карточки: Иконка платформы + Название + Бейдж
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Сама иконка платформы в красивом круглом Material-контейнере без пульсации
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primary
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = when {
                                                        clipPlatform.contains("YouTube", ignoreCase = true) -> Icons.Default.PlayArrow
                                                        clipPlatform.contains("TikTok", ignoreCase = true) -> Icons.Default.MusicNote
                                                        clipPlatform.contains("Instagram", ignoreCase = true) -> Icons.Default.PhotoLibrary
                                                        else -> Icons.Default.Link
                                                    },
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = clipPlatform,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                ) {
                                                    Text(
                                                        text = "БУФЕР",
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.onPrimary,
                                                        fontSize = 9.sp
                                                    )
                                                }

                                                if (useClipboardHistory && recentClipboardUrls.size > 1) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        onClick = {
                                                            view.performAppHaptic(HapticType.CLICK)
                                                            showRecentHistorySheet = true
                                                        },
                                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                                        shape = CircleShape,
                                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                Icons.Default.History,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(11.dp),
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = "${recentClipboardUrls.size}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                fontSize = 9.5.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "Смарт-детектор обнаружил ссылку",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }

                                        // Пространство под кнопку закрытия
                                        Spacer(modifier = Modifier.width(36.dp))
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    if (useClipboardPreview && clipboardMetadata != null) {
                                        val meta = clipboardMetadata!!
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                                    RoundedCornerShape(16.dp)
                                                )
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (meta.thumbnailUrl.isNotBlank()) {
                                                AsyncImage(
                                                    model = meta.thumbnailUrl,
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(width = 80.dp, height = 56.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = meta.title,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = clipUrl,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                        letterSpacing = 0.1.sp
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Link,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = clipUrl,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    letterSpacing = 0.1.sp
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Сочная фирменная Material You кнопка призыва к действию (Action Button)
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(48.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        tonalElevation = 2.dp,
                                        shadowElevation = 2.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoFixHigh,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "АНАЛИЗИРОВАТЬ ССЫЛКУ",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }

                                // Идеально центрированная парящая кнопка закрытия (согласована по высоте с логотипом платформы)
                                Surface(
                                    onClick = {
                                        view.performAppHaptic(HapticType.CLICK)
                                        viewModel.dismissedClipboardUrl = clipUrl
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 22.dp, end = 16.dp)
                                        .size(28.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Скрыть",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = errorMessage.isNotEmpty(),
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ошибка ссылки",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                                lineHeight = 16.sp
                            )
                        }
                        IconButton(onClick = { AnalysisManager.clearError() }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Скрыть",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }

        item {
            AnimatedVisibility(
                visible = errorMessage.isBlank() && (isCurrentUrlValid || isDownloading),
                enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn() + scaleIn(initialScale = 0.95f),
                exit = shrinkVertically() + fadeOut() + scaleOut(targetScale = 0.95f)
            ) {
                Card(modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Crossfade(targetState = isAnalyzing, animationSpec = tween(350), label = "SkeletonCrossfade") { analyzing ->
                            if (analyzing) {
                                MediaCardSkeleton(showThumbnails = showThumbnails)
                            } else if (resultTitle.isNotBlank() || isDownloading) {
                                Column {
                                    AnimatedVisibility(visible = showThumbnails && resultThumbnail.isNotBlank(), enter = expandVertically() + fadeIn(animationSpec = tween(400)), exit = shrinkVertically() + fadeOut()) {
                                        Column {
                                            Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))) {
                                                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(resultThumbnail).crossfade(600).build(), contentDescription = "Обложка", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)), startY = 80f)))
                                                Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.65f)) { Text(text = if (isPhotoPost) "ФОТО-КАРУСЕЛЬ" else if (isPlaylist) "ПЛЕЙЛИСТ" else "HD ВИДЕО", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), letterSpacing = 0.5.sp) }
                                                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary) { Text(text = platformName, color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                        if (!showThumbnails || resultThumbnail.isBlank()) {
                                            Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(if (isPlaylist) Icons.AutoMirrored.Filled.List else if (isPhotoPost) Icons.Default.PhotoLibrary else Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(26.dp)) }
                                            Spacer(modifier = Modifier.width(12.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = platformName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = resultTitle.ifEmpty { "Медиафайл готов к скачиванию" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    if (!isPhotoPost && !isPlaylist && !isDownloading && showQualitySelector) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { viewModel.showQualityDialog = true }) {
                                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(text = "Качество видео", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                                }
                                                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        val displayQualityText = when (viewModel.selectedVideoQuality) {
                                                            "best" -> "Авто"
                                                            "2160" -> "4K UHD"
                                                            "1440" -> "2K QHD"
                                                            "1080" -> "1080p HD"
                                                            "720"  -> "720p HD"
                                                            "480"  -> "480p"
                                                            "360"  -> "360p"
                                                            "240"  -> "240p"
                                                            "144"  -> "144p"
                                                            else -> "${viewModel.selectedVideoQuality}p"
                                                        }
                                                        Text(text = displayQualityText, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, maxLines = 1, softWrap = false)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (viewModel.isDuplicateDetected && !isDownloading) {
                                        Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)), modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(text = "Этот файл уже сохранен в Истории. Вы можете скачать его повторно.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer, lineHeight = 16.sp)
                                            }
                                        }
                                    }
                                    
                                    val sortedTasks by remember { derivedStateOf { activeTasks.sortedBy { it.slot } } }
                                    val isCurrentDownloadActive = remember(url, resultTitle, activeTasks, isDownloading) {
                                        if (url.isBlank() && resultTitle.isBlank()) false
                                        else DownloadManager.isAlreadyDownloading(url, resultTitle) || (isDownloading && sortedTasks.isNotEmpty())
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    AnimatedContent(
                                        targetState = isCurrentDownloadActive,
                                        transitionSpec = {
                                            (fadeIn(animationSpec = tween(300)) + expandVertically()) togetherWith (fadeOut(animationSpec = tween(200)) + shrinkVertically())
                                        },
                                        label = "DownloadCardVsButtonsTransition"
                                    ) { isDownloadingActive ->
                                        if (isDownloadingActive) {
                                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                if (sortedTasks.isNotEmpty()) {
                                                    val heroTask = sortedTasks.first()
                                                    HeroDownloadCard(
                                                        task = heroTask,
                                                        onCancel = { DownloadManager.cancelTask(context, heroTask.id) }
                                                    )

                                                    val secondaryTasks = sortedTasks.drop(1)
                                                    if (secondaryTasks.isNotEmpty()) {
                                                        secondaryTasks.forEach { task ->
                                                            key(task.id) {
                                                                CompactDownloadCard(
                                                                    task = task,
                                                                    onCancel = { DownloadManager.cancelTask(context, task.id) }
                                                                )
                                                            }
                                                        }
                                                    }

                                                    val hiddenInQueue = queueCount - sortedTasks.size
                                                    if (hiddenInQueue > 0) {
                                                        Surface(
                                                            shape = RoundedCornerShape(12.dp),
                                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                            modifier = Modifier.align(Alignment.CenterHorizontally)
                                                        ) {
                                                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                                Icon(Icons.Default.Queue, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                                Spacer(modifier = Modifier.width(8.dp))
                                                                Text(text = "В очереди еще: $hiddenInQueue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    Box(modifier = Modifier.fillMaxWidth().height(90.dp), contentAlignment = Alignment.Center) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                                                            Spacer(modifier = Modifier.width(12.dp))
                                                            Text("Подготовка к скачиванию...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Button(
                                                    onClick = {
                                                        if (isPlaylist) {
                                                            viewModel.showPlaylistSheet = true
                                                        } else {
                                                            DownloadManager.startDownload(
                                                                context = context,
                                                                url = url,
                                                                title = resultTitle,
                                                                isAudio = false,
                                                                quality = viewModel.selectedVideoQuality
                                                            )
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth().height(52.dp),
                                                    shape = RoundedCornerShape(18.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Icon(if (isPlaylist) Icons.AutoMirrored.Filled.List else if (isPhotoPost) Icons.Default.PhotoLibrary else Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(20.dp))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    val buttonText = when { isPlaylist -> "Открыть плейлист"; isPhotoPost -> "Скачать все фото"; resultTitle.isNotEmpty() -> "Скачать видео"; else -> "Скачать публикацию" }
                                                    Text(text = buttonText, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                                AnimatedVisibility(visible = !isPlaylist && !isPhotoPost, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                                                    FilledTonalButton(
                                                        onClick = {
                                                            DownloadManager.startDownload(
                                                                context = context,
                                                                url = url,
                                                                title = resultTitle,
                                                                isAudio = true,
                                                                quality = viewModel.selectedAudioQuality
                                                            )
                                                        },
                                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                                        shape = RoundedCornerShape(18.dp)
                                                    ) {
                                                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(text = "Извлечь Аудиодорожку", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Состояние, когда анализ не выполнен (например, после закрытия ошибки)
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.SavedSearch, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(28.dp))
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Ссылка готова к проверке",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Нажмите кнопку ниже, чтобы загрузить информацию о медиафайле и выбрать качество.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Button(
                                        onClick = {
                                            view.performAppHaptic(HapticType.CLICK)
                                            AnalysisManager.onIncomingUrl(url, autoAnalyze = true)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Анализировать ссылку", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    if (viewModel.showPlaylistSheet && isPlaylist) {
        PlaylistSelectionBottomSheet(
            playlistTitle = resultTitle,
            entries = playlistEntries,
            selectedQuality = viewModel.selectedVideoQuality,
            onDismiss = { viewModel.showPlaylistSheet = false },
            onDownloadSelected = { items: List<PlaylistItem>, isAudio: Boolean ->
                viewModel.showPlaylistSheet = false
                DownloadManager.startBatchDownload(context = context, items = items, isAudio = isAudio, quality = viewModel.selectedVideoQuality)
                val typeStr = if (isAudio) "аудио" else "видео"
                coroutineScope.launch { snackbarHostState.showInstantSnackbar("⚡ В очередь добавлено: ${items.size} ($typeStr)") }
            }
        )
    }

    if (showRecentHistorySheet) {
        ModalBottomSheet(
            onDismissRequest = { showRecentHistorySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Недавно скопированные ссылки",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Выберите ссылку для мгновенного анализа",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (recentClipboardUrls.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                SettingsManager.clearRecentClipboardUrls()
                                showRecentHistorySheet = false
                            }
                        ) {
                            Text("Очистить", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                AnimatedContent(
                    targetState = recentClipboardUrls.isEmpty(),
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(300)) + expandVertically()).togetherWith(fadeOut(animationSpec = tween(200)) + shrinkVertically())
                    },
                    label = "RecentUrlsEmptyStateAnim"
                ) { isEmpty ->
                    if (isEmpty) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.HistoryToggleOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "История скопированных ссылок пуста",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 290.dp)
                        ) {
                            items(recentClipboardUrls, key = { it }) { itemUrl ->
                                val itemPlatform = remember(itemUrl) { detectPlatformName(itemUrl) }
                                Surface(
                                    onClick = {
                                        view.performAppHaptic(HapticType.CLICK)
                                        AnalysisManager.setUrl(itemUrl)
                                        focusManager.clearFocus()
                                        showRecentHistorySheet = false
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .animateItem(
                                            fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(32.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = when {
                                                        itemPlatform.contains("YouTube", ignoreCase = true) -> Icons.Default.PlayArrow
                                                        itemPlatform.contains("TikTok", ignoreCase = true) -> Icons.Default.MusicNote
                                                        itemPlatform.contains("Instagram", ignoreCase = true) -> Icons.Default.PhotoLibrary
                                                        else -> Icons.Default.Link
                                                    },
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = itemPlatform,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = itemUrl,
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                view.performAppHaptic(HapticType.CLICK)
                                                SettingsManager.removeRecentClipboardUrl(itemUrl)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Удалить",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { showRecentHistorySheet = false },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Закрыть", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// 🧠 Облегченный экстрактор метаданных для мини-превью (Работает без yt-dlp)
private val lightHttpClient = OkHttpClient.Builder()
    .connectTimeout(3, TimeUnit.SECONDS)
    .readTimeout(3, TimeUnit.SECONDS)
    .build()

private fun fetchLightMeta(url: String): VideoMetadata? {
    try {
        // Если это TikTok — пробуем получить oEmbed API
        val reqUrl = if (url.contains("tiktok.com")) {
            "https://www.tiktok.com/oembed?url=$url"
        } else {
            url
        }
        
        val request = Request.Builder()
            .url(reqUrl)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()
            
        lightHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val body = response.body?.string() ?: return null
            
            if (reqUrl.contains("oembed")) {
                val json = org.json.JSONObject(body)
                val title = json.optString("title", "")
                val thumb = json.optString("thumbnail_url", "")
                if (title.isNotBlank()) return VideoMetadata(title = title, thumbnailUrl = thumb)
            } else {
                // Извлекаем title и og:image через регулярки
                val titleRegex = "<title>(.*?)</title>".toRegex(RegexOption.IGNORE_CASE)
                val thumbRegex = "<meta\\s+property=\"og:image\"\\s+content=\"(.*?)\"".toRegex(RegexOption.IGNORE_CASE)
                
                var title = titleRegex.find(body)?.groupValues?.get(1) ?: ""
                val thumb = thumbRegex.find(body)?.groupValues?.get(1) ?: ""
                
                title = title.replace("&#39;", "'").replace("&quot;", "\"")
                if (title.isNotBlank()) {
                    return VideoMetadata(title = title, thumbnailUrl = thumb)
                }
            }
        }
    } catch (_: Exception) {}
    return null
}

@Composable
fun HeroDownloadCard(task: DownloadTaskStatus, onCancel: () -> Unit) {
    val animatedProgress by animateFloatAsState(
        targetValue = task.progress / 100f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "heroProgress"
    )

    // Премиальный контейнер с двойным слоем заливки и мягким размытием границ
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        border = BorderStroke(
            1.dp, 
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                )
            )
        ),
        shadowElevation = 4.dp
    ) {
        // Уникальный визуальный эффект: Прогресс плавно заполняет саму карточку сзади, как жидкость
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = animatedProgress)
                    .matchParentSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)
                              )
                        )
                    )
            )

            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        // Сочный, неоновый хай-тек контейнер для главной иконки скачивания
                        Surface(
                            modifier = Modifier.size(42.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(14.dp))
                        
                        Column {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Ультра-премиальный статус-чип с выверенной геометрией и микро-отступами
                            Surface(
                                color = if (task.state == WorkInfo.State.RUNNING) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    0.5.dp,
                                    if (task.state == WorkInfo.State.RUNNING) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (task.state == WorkInfo.State.RUNNING) Icons.Default.FlashOn else Icons.Default.HourglassEmpty,
                                        contentDescription = null,
                                        tint = if (task.state == WorkInfo.State.RUNNING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (task.state == WorkInfo.State.RUNNING) "СКАЧИВАНИЕ" else "В ОЧЕРЕДИ",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 0.8.sp
                                        ),
                                        fontWeight = FontWeight.Black,
                                        color = if (task.state == WorkInfo.State.RUNNING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }
                    
                    // Круглая кнопка закрытия/отмены в стиле iOS Player Controls
                    Surface(
                        onClick = onCancel,
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = "Отмена", 
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), 
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(18.dp))
                
                // Элегантная тонкая неоновая линия прогресса + высокоточный индикатор справа
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    )
                    
                    Spacer(modifier = Modifier.width(14.dp))
                    
                    RollingPercentageBadge(
                        progress = task.progress,
                        modifier = Modifier.wrapContentSize()
                    )
                }
            }
        }
    }
}

@Composable
fun CompactDownloadCard(task: DownloadTaskStatus, onCancel: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Тонкое неоновое кольцо статуса потока
            Box(
                modifier = Modifier.size(26.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { task.progress / 100f },
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title, 
                    style = MaterialTheme.typography.bodyMedium, 
                    fontWeight = FontWeight.Bold, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(1.dp))
                val progressText = if (task.progress == 0) "Ожидание" else "${task.progress}%"
                Text(
                    text = "Поток ${task.slot} • $progressText", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Surface(
                onClick = onCancel,
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = "Отмена", 
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), 
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}
