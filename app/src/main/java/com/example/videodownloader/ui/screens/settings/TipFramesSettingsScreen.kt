package com.example.videodownloader.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.SettingsManager
import com.example.videodownloader.TipFrameData
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipFramesSettingsScreen(
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val tipFrames by SettingsManager.tipFrames.collectAsState()

    val availableIcons = remember {
        listOf(
            "bolt" to ("Молния" to Icons.Default.Bolt),
            "link" to ("Ссылка" to Icons.Default.Link),
            "quality" to ("Качество" to Icons.Default.HighQuality),
            "rocket" to ("Ракета" to Icons.Default.RocketLaunch),
            "fire" to ("Огонь" to Icons.Default.Whatshot),
            "star" to ("Звезда" to Icons.Default.Star),
            "shield" to ("Защита" to Icons.Default.Security),
            "speed" to ("Скорость" to Icons.Default.Speed),
            "magic" to ("Магия" to Icons.Default.AutoAwesome),
            "music" to ("Музыка" to Icons.Default.MusicNote),
            "video" to ("Видео" to Icons.Default.PlayCircle),
            "photo" to ("Фото" to Icons.Default.PhotoLibrary),
            "download" to ("Загрузка" to Icons.Default.Download),
            "check" to ("Галочка" to Icons.Default.CheckCircle),
            "heart" to ("Сердце" to Icons.Default.Favorite),
            "bulb" to ("Идея" to Icons.Default.Lightbulb)
        )
    }

    // Предпросмотр кадров
    var previewIndex by remember { mutableIntStateOf(0) }
    var isPreviewPlaying by remember { mutableStateOf(true) }

    LaunchedEffect(tipFrames, isPreviewPlaying) {
        if (isPreviewPlaying && tipFrames.isNotEmpty()) {
            while (true) {
                for (i in tipFrames.indices) {
                    previewIndex = i
                    delay(2200)
                }
            }
        }
    }

    val currentPreviewFrame = remember(tipFrames, previewIndex) {
        if (tipFrames.isNotEmpty()) tipFrames[previewIndex.coerceIn(0, tipFrames.size - 1)]
        else TipFrameData("bolt", "Превью кадра")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Кадры приветствия", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    TextButton(onClick = { SettingsManager.resetTipFrames() }) {
                        Text("Сброс", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 💎 ИНТЕРАКТИВНЫЙ HERO БЛОК ПРЕДПРОСМОТРА
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    border = com.example.videodownloader.ui.theme.getAppCardBorder()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Предпросмотр",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Демонстрация на Главном экране",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val playScale by animateFloatAsState(
                                targetValue = if (isPreviewPlaying) 1.05f else 1.0f,
                                animationSpec = spring(stiffness = Spring.StiffnessLow),
                                label = "PlayScale"
                            )

                            IconButton(
                                onClick = { isPreviewPlaying = !isPreviewPlaying },
                                modifier = Modifier
                                    .size(36.dp)
                                    .graphicsLayer { scaleX = playScale; scaleY = playScale }
                            ) {
                                Icon(
                                    imageVector = if (isPreviewPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = "Пауза/Плей",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // ПЛАВАЮЩАЯ КАПСУЛА ПРЕВЬЮ
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        ) {
                            AnimatedContent(
                                targetState = currentPreviewFrame,
                                transitionSpec = {
                                    (slideInVertically(animationSpec = tween(380, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(250)))
                                        .togetherWith(
                                            slideOutVertically(animationSpec = tween(380, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(200))
                                        )
                                },
                                label = "PreviewCapsuleAnim"
                            ) { frame ->
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = getIconForFrameKey(frame.iconKey),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = frame.text,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 🔢 ВЫБОР КОЛИЧЕСТВА КАДРОВ
            item {
                Text(
                    text = "КОЛИЧЕСТВО КАДРОВ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    border = com.example.videodownloader.ui.theme.getAppCardBorder()
                ) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .padding(10.dp)
                            .fillMaxWidth()
                    ) {
                        for (count in 1..5) {
                            SegmentedButton(
                                selected = tipFrames.size == count,
                                onClick = {
                                    val newFrames = if (tipFrames.size < count) {
                                        tipFrames + List(count - tipFrames.size) { i ->
                                            TipFrameData("bolt", "Кадр ${tipFrames.size + i + 1}")
                                        }
                                    } else {
                                        tipFrames.take(count)
                                    }
                                    SettingsManager.setTipFrames(newFrames)
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = count - 1, count = 5)
                            ) {
                                Text("$count", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Редактирование кадров
            item {
                Text(
                    text = "НАСТРОЙКА СОДЕРЖИМОГО",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            items(
                count = tipFrames.size,
                key = { index -> "frame_$index" }
            ) { index ->
                val frame = tipFrames[index]
                val isFinalFrame = index == tipFrames.size - 1

                val frameBadgeColor by animateColorAsState(
                    targetValue = if (isFinalFrame) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(220),
                    label = "badgeColor"
                )
                val frameTextColor by animateColorAsState(
                    targetValue = if (isFinalFrame) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(220),
                    label = "textColor"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem(
                            fadeInSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                            fadeOutSpec = tween(durationMillis = 250, easing = FastOutLinearInEasing),
                            placementSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    border = com.example.videodownloader.ui.theme.getAppCardBorder()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = frameBadgeColor,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = frameTextColor
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isFinalFrame) "Кадр ${index + 1} (Финал замирает)" else "Кадр ${index + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFinalFrame) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (isFinalFrame) "Этот кадр остаётся висеть на экране" else "Анимированный промежуточный кадр",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Выберите иконку:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // ИКОНКИ (КАРУСЕЛЬ КРУГЛЫХ ЧИПОВ С ПЛАВНЫМИ GPU-МИКРОАНИМАЦИЯМИ)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            availableIcons.forEach { (key, info) ->
                                val (iconLabel, icon) = info
                                val isSelected = frame.iconKey == key

                                val chipScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.05f else 1.0f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                                    label = "chipScale"
                                )
                                val chipBgColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    animationSpec = tween(180),
                                    label = "chipBgColor"
                                )
                                val chipBorderColor by animateColorAsState(
                                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                    animationSpec = tween(180),
                                    label = "chipBorderColor"
                                )

                                Surface(
                                    onClick = {
                                        val updated = tipFrames.toMutableList()
                                        updated[index] = frame.copy(iconKey = key)
                                        SettingsManager.setTipFrames(updated)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    color = chipBgColor,
                                    border = BorderStroke(1.dp, chipBorderColor),
                                    modifier = Modifier.graphicsLayer { scaleX = chipScale; scaleY = chipScale }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = iconLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ТЕКСТ КАДРА
                        OutlinedTextField(
                            value = frame.text,
                            onValueChange = { newText ->
                                val updated = tipFrames.toMutableList()
                                updated[index] = frame.copy(text = newText)
                                SettingsManager.setTipFrames(updated)
                            },
                            label = { Text("Текст кадра ${index + 1}") },
                            placeholder = { Text("Введите фразу...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
