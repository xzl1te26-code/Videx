package com.example.videodownloader.ui.screens.settings

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
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chaquo.python.Python
import com.example.videodownloader.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val ytdlpVersion = remember {
        try {
            val py = Python.getInstance()
            py.getModule("yt_dlp.version")["__version__"]?.toString() ?: "2024.04.09"
        } catch (_: Exception) {
            "Встроенное ядро"
        }
    }

    var showFeaturesSheet by remember { mutableStateOf(false) }
    var showPlatformsSheet by remember { mutableStateOf(false) }
    var showTechSpecsSheet by remember { mutableStateOf(false) }

    // Шторка возможностей
    if (showFeaturesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFeaturesSheet = false },
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Возможности и особенности",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Ключевой функционал Videx v1.1.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                AboutFeatureRow(
                    icon = Icons.Default.HighQuality,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    title = stringResource(R.string.feature_quality_title),
                    description = stringResource(R.string.feature_quality_desc)
                )

                AboutFeatureRow(
                    icon = Icons.Default.PhotoLibrary,
                    iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                    iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    title = stringResource(R.string.feature_tiktok_photos_title),
                    description = stringResource(R.string.feature_tiktok_photos_desc)
                )

                AboutFeatureRow(
                    icon = Icons.Default.MusicNote,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer,
                    iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    title = stringResource(R.string.feature_audio_title),
                    description = stringResource(R.string.feature_audio_desc)
                )

                AboutFeatureRow(
                    icon = Icons.Default.Sync,
                    iconBg = MaterialTheme.colorScheme.surfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = stringResource(R.string.feature_ota_title),
                    description = stringResource(R.string.feature_ota_desc)
                )

                AboutFeatureRow(
                    icon = Icons.Default.Checklist,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.feature_batch_title),
                    description = stringResource(R.string.feature_batch_desc)
                )

                Button(
                    onClick = { showFeaturesSheet = false },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Понятно", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Шторка поддерживаемых платформ
    if (showPlatformsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPlatformsSheet = false },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Public,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Поддерживаемые платформы",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Загрузка с 1000+ веб-сайтов по всему миру",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                Text(
                    text = stringResource(R.string.about_platforms_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlatformChip("YouTube / Shorts")
                    PlatformChip("TikTok No-WM")
                    PlatformChip("VK Видео / Клипы")
                    PlatformChip("Instagram Reels")
                    PlatformChip("X (Twitter)")
                    PlatformChip("Pinterest")
                    PlatformChip("Reddit")
                    PlatformChip("SoundCloud")
                    PlatformChip("Vimeo")
                }

                Button(
                    onClick = { showPlatformsSheet = false },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Закрыть", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Шторка технических сведений
    if (showTechSpecsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTechSpecsSheet = false },
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Системные сведения",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Архитектура ядра и среды выполнения",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                TechSpecCardItem(
                    icon = Icons.Default.Terminal,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    label = stringResource(R.string.tech_spec_parser_label),
                    value = "yt-dlp v$ytdlpVersion",
                    badgeText = stringResource(R.string.tech_spec_active_badge),
                    badgeBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    badgeColor = MaterialTheme.colorScheme.primary
                )

                TechSpecCardItem(
                    icon = Icons.Default.Code,
                    iconBg = MaterialTheme.colorScheme.secondaryContainer,
                    iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    label = stringResource(R.string.tech_spec_python_label),
                    value = "CPython 3.13 • Chaquopy",
                    badgeText = stringResource(R.string.tech_spec_ndk_badge),
                    badgeBg = MaterialTheme.colorScheme.secondaryContainer,
                    badgeColor = MaterialTheme.colorScheme.onSecondaryContainer
                )

                TechSpecCardItem(
                    icon = Icons.Default.Palette,
                    iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                    iconColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    label = stringResource(R.string.tech_spec_ui_label),
                    value = "Jetpack Compose • Material 3",
                    badgeText = stringResource(R.string.tech_spec_material_you_badge),
                    badgeBg = MaterialTheme.colorScheme.tertiaryContainer,
                    badgeColor = MaterialTheme.colorScheme.onTertiaryContainer
                )

                TechSpecCardItem(
                    icon = Icons.Default.Memory,
                    iconBg = MaterialTheme.colorScheme.surfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    label = stringResource(R.string.tech_spec_memory_label),
                    value = "16 KB Page Alignment",
                    badgeText = stringResource(R.string.tech_spec_android15_badge),
                    badgeBg = MaterialTheme.colorScheme.primaryContainer,
                    badgeColor = MaterialTheme.colorScheme.primary
                )

                TechSpecCardItem(
                    icon = Icons.Default.DeveloperBoard,
                    iconBg = MaterialTheme.colorScheme.surfaceVariant,
                    iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    label = stringResource(R.string.tech_spec_cpu_label),
                    value = "ARM64-v8a • x86_64",
                    badgeText = stringResource(R.string.tech_spec_64bit_badge),
                    badgeBg = MaterialTheme.colorScheme.surfaceVariant,
                    badgeColor = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = { showTechSpecsSheet = false },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Закрыть", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_description))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. ШАПКА БРЕНДА
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Videx",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = stringResource(R.string.app_version_info),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 2. ОФИЦИАЛЬНОЕ СООБЩЕСТВО VIDEX
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Forum, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "ОФИЦИАЛЬНОЕ СООБЩЕСТВО",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "Videx в Telegram",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Text(
                            text = "Подписывайтесь на официальный канал новостей Videx и вступайте в чат сообщества для обсуждений и техподдержки.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { uriHandler.openUri("https://t.me/videx_app") },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Канал", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            FilledTonalButton(
                                onClick = { uriHandler.openUri("https://t.me/videx_chat") },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Чат группы", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. ПАРТНЁР ПРОЕКТА
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Groups, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "ПАРТНЁР ПРОЕКТА",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.2.sp
                                )
                                Text(
                                    text = "ProxyGuide Team",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Text(
                            text = "Приложение создано при поддержке ProxyGuide — экспертов в области приватности и безопасности. Лучшие VPN-решения для вашего комфорта.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                        
                        Spacer(modifier = Modifier.height(18.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { uriHandler.openUri("https://t.me/ProxyGuide_Community") },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Сообщество", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            FilledTonalButton(
                                onClick = { uriHandler.openUri("https://t.me/ProxyGuidebot") },
                                modifier = Modifier.weight(1f).height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Бот", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 4. СТРУКТУРИРОВАННОЕ ПОДМЕНЮ СВЕДЕНИЙ
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column {
                        com.example.videodownloader.ui.screens.settings.SettingsNavigationRow(
                            title = "Возможности и особенности",
                            subtitle = "4K HD, MP3, фото-карусели, OTA и очереди",
                            icon = Icons.Default.AutoAwesome,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = { showFeaturesSheet = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        com.example.videodownloader.ui.screens.settings.SettingsNavigationRow(
                            title = "Поддерживаемые платформы (1000+)",
                            subtitle = "YouTube, TikTok, Instagram, VK, Pinterest и др.",
                            icon = Icons.Default.Public,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = { showPlatformsSheet = true }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        com.example.videodownloader.ui.screens.settings.SettingsNavigationRow(
                            title = "Системные сведения",
                            subtitle = "yt-dlp v$ytdlpVersion, CPython 3.13, NDK, Compose",
                            icon = Icons.Default.Terminal,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = { showTechSpecsSheet = true }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
