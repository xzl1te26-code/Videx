package com.example.videodownloader.ui.screens.settings

import androidx.compose.animation.*
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.AppTheme
import com.example.videodownloader.R
import com.example.videodownloader.SettingsManager
import com.example.videodownloader.TipFrameData

fun getIconForFrameKey(key: String): ImageVector {
    return when (key) {
        "link" -> Icons.Default.Link
        "quality" -> Icons.Default.HighQuality
        "bolt" -> Icons.Default.Bolt
        "rocket" -> Icons.Default.RocketLaunch
        "fire" -> Icons.Default.Whatshot
        "star" -> Icons.Default.Star
        "shield" -> Icons.Default.Security
        "speed" -> Icons.Default.Speed
        "magic" -> Icons.Default.AutoAwesome
        "music" -> Icons.Default.MusicNote
        "video" -> Icons.Default.PlayCircle
        "photo" -> Icons.Default.PhotoLibrary
        "download" -> Icons.Default.Download
        "check" -> Icons.Default.CheckCircle
        "heart" -> Icons.Default.Favorite
        "bulb" -> Icons.Default.Lightbulb
        else -> Icons.Default.Bolt
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    onOpenTipFramesSettings: () -> Unit,
    onOpenHapticSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Интерфейс и внешний вид", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
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
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "ЦВЕТОВАЯ ТЕМА",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    border = com.example.videodownloader.ui.theme.getAppCardBorder()
                ) {
                    Column {
                        val currentAppTheme by SettingsManager.appTheme.collectAsState()

                        ThemeSelectionRow(
                            selectedTheme = currentAppTheme,
                            onThemeSelect = { SettingsManager.setAppTheme(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.dynamic_colors_title),
                            subtitle = stringResource(R.string.dynamic_colors_desc),
                            icon = Icons.Default.Palette,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            stateFlow = SettingsManager.useDynamicColors,
                            onCheckedChange = { SettingsManager.setUseDynamicColors(it) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "ТАКТИЛЬНЫЙ ОТКЛИК",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    border = com.example.videodownloader.ui.theme.getAppCardBorder()
                ) {
                    Column {
                        val hapticEnabled by SettingsManager.hapticEnabled.collectAsState()
                        val hapticIntensity by SettingsManager.hapticIntensity.collectAsState()
                        val intensityText = when(hapticIntensity) {
                            com.example.videodownloader.HapticIntensity.SOFT -> "Мягкая сила"
                            com.example.videodownloader.HapticIntensity.STANDARD -> "Стандартная сила"
                            com.example.videodownloader.HapticIntensity.STRONG -> "Сильная отдача"
                        }

                        SettingsNavigationRow(
                            title = "Тактильный отклик (Вибрация)",
                            subtitle = if (hapticEnabled) "Включен • $intensityText" else "Выключен",
                            icon = Icons.Default.Vibration,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = onOpenHapticSettings
                        )
                    }
                }
            }

            item {
                Text(
                    text = "ЭЛЕМЕНТЫ И МОДУЛИ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    border = com.example.videodownloader.ui.theme.getAppCardBorder()
                ) {
                    Column {
                        val showDynamicTips by SettingsManager.showDynamicTips.collectAsState()

                        SettingsSwitchRow(
                            title = "Анимация приветствия",
                            subtitle = "Живые динамические подсказки при открытии Главного экрана",
                            icon = Icons.Default.AutoAwesome,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            stateFlow = SettingsManager.showDynamicTips,
                            onCheckedChange = { SettingsManager.setShowDynamicTips(it) }
                        )

                        AnimatedVisibility(visible = showDynamicTips) {
                            Column {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                SettingsNavigationRow(
                                    title = "Кадры приветствия",
                                    subtitle = "Текст, иконки и количество кадров",
                                    icon = Icons.Default.Slideshow,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    onClick = onOpenTipFramesSettings
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.quality_selector_title),
                            subtitle = stringResource(R.string.quality_selector_desc),
                            icon = Icons.Default.Tune,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            stateFlow = SettingsManager.showQualitySelector,
                            onCheckedChange = { SettingsManager.setShowQualitySelector(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.thumbnails_before_download_title),
                            subtitle = stringResource(R.string.thumbnails_before_download_desc),
                            icon = Icons.Default.Image,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            stateFlow = SettingsManager.showThumbnails,
                            onCheckedChange = { SettingsManager.setShowThumbnails(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.history_thumbnails_title),
                            subtitle = stringResource(R.string.history_thumbnails_desc),
                            icon = Icons.Default.VideoLibrary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            stateFlow = SettingsManager.showHistoryThumbnails,
                            onCheckedChange = { SettingsManager.setShowHistoryThumbnails(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsSwitchRow(
                            title = "Сортировка истории",
                            subtitle = "Удобные фильтры по дате, размеру файла и длительности видео",
                            icon = Icons.Default.Sort,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            stateFlow = SettingsManager.showHistorySort,
                            onCheckedChange = { SettingsManager.setShowHistorySort(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.storage_stats_title),
                            subtitle = stringResource(R.string.storage_stats_desc),
                            icon = Icons.Default.PieChart,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            stateFlow = SettingsManager.showStorageStats,
                            onCheckedChange = { SettingsManager.setShowStorageStats(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.clipboard_bubble_title),
                            subtitle = stringResource(R.string.clipboard_bubble_desc),
                            icon = Icons.Default.ContentPaste,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            stateFlow = SettingsManager.useClipboardBubble,
                            onCheckedChange = { SettingsManager.setUseClipboardBubble(it) }
                        )

                        val isBubbleEnabled by SettingsManager.useClipboardBubble.collectAsState()

                        androidx.compose.animation.AnimatedVisibility(
                            visible = isBubbleEnabled,
                            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
                        ) {
                            Column {
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                SettingsSwitchRow(
                                    title = "Превью ссылок",
                                    subtitle = "Загружать обложку и название ролика прямо в плашке смарт-буфера",
                                    icon = Icons.Default.Preview,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    stateFlow = SettingsManager.useClipboardPreview,
                                    onCheckedChange = { SettingsManager.setUseClipboardPreview(it) }
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                SettingsSwitchRow(
                                    title = "История буфера",
                                    subtitle = "Запоминать последние 5 ссылок в истории смарт-буфера для быстрого доступа",
                                    icon = Icons.Default.History,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    stateFlow = SettingsManager.useClipboardHistory,
                                    onCheckedChange = { SettingsManager.setUseClipboardHistory(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelectionRow(
    selectedTheme: AppTheme,
    onThemeSelect: (AppTheme) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = when (selectedTheme) {
                            AppTheme.SYSTEM -> Icons.Default.SettingsSuggest
                            AppTheme.DARK -> Icons.Default.DarkMode
                            AppTheme.LIGHT -> Icons.Default.LightMode
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Тема оформления",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (selectedTheme) {
                        AppTheme.SYSTEM -> "Системная (по умолчанию)"
                        AppTheme.DARK -> "Тёмная тема"
                        AppTheme.LIGHT -> "Светлая тема"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val themes = listOf(
                AppTheme.SYSTEM to "Системная",
                AppTheme.DARK to "Тёмная",
                AppTheme.LIGHT to "Светлая"
            )

            for ((theme, label) in themes) {
                val isSelected = selectedTheme == theme
                Surface(
                    onClick = { onThemeSelect(theme) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.weight(1f).height(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


