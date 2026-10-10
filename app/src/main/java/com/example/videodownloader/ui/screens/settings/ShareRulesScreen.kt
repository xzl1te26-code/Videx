package com.example.videodownloader.ui.screens.settings

import com.example.videodownloader.performAppHaptic
import com.example.videodownloader.HapticType
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.PlatformDownloadRule
import com.example.videodownloader.SettingsManager
import com.example.videodownloader.ShareActionGlobal
import com.example.videodownloader.ui.components.M3SegmentedControl

import androidx.compose.ui.res.stringResource
import com.example.videodownloader.R

data class OptSheetItem(
    val key: String,
    val name: String,
    val desc: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShareRulesScreen(onBack: () -> Unit) {
    val shareGlobalAction by SettingsManager.shareGlobalAction.collectAsState()
    val silentShareDownload by SettingsManager.silentShareDownload.collectAsState()
    val ruleYoutube by SettingsManager.ruleYoutube.collectAsState()
    val ruleTiktok by SettingsManager.ruleTiktok.collectAsState()
    val ruleVk by SettingsManager.ruleVk.collectAsState()
    val ruleInstagram by SettingsManager.ruleInstagram.collectAsState()
    val rulePinterest by SettingsManager.rulePinterest.collectAsState()
    val ruleOther by SettingsManager.ruleOther.collectAsState()

    val transitEnabled by SettingsManager.transitModeEnabled.collectAsState()
    val transitAutoOnly by SettingsManager.transitModeAutopilotOnly.collectAsState()
    val transitMinSize by SettingsManager.transitModeMinSizeMb.collectAsState()
    val transitPlatforms by SettingsManager.transitModePlatforms.collectAsState()

    val transitOptimizeSize by SettingsManager.transitOptimizeSize.collectAsState()
    val transitMaxQuality by SettingsManager.transitMaxQuality.collectAsState()
    val transitCodec by SettingsManager.transitCodec.collectAsState()
    val transitMinSizeThreshold by SettingsManager.transitMinSizeThreshold.collectAsState()
    val transitPhotoQuality by SettingsManager.transitPhotoQuality.collectAsState()

    val view = LocalView.current
    var activePlatformId by remember { mutableStateOf<String?>(null) }
    var activeOptSheet by remember { mutableStateOf<String?>(null) }

    if (activePlatformId != null) {
        val platformInfo = when (activePlatformId) {
            "yt" -> Triple("YouTube", Icons.Default.PlayCircle, ruleYoutube)
            "tt" -> Triple("TikTok", Icons.Default.MusicVideo, ruleTiktok)
            "vk" -> Triple("VK Видео", Icons.Default.SlowMotionVideo, ruleVk)
            "ig" -> Triple("Instagram", Icons.Default.CameraAlt, ruleInstagram)
            "pin" -> Triple("Pinterest", Icons.Default.PushPin, rulePinterest)
            else -> Triple(stringResource(R.string.all_other_sites), Icons.Default.Language, ruleOther)
        }

        PlatformRuleBottomSheet(
            title = platformInfo.first,
            icon = platformInfo.second,
            currentRule = platformInfo.third,
            isSilentMode = silentShareDownload,
            allowAudio = activePlatformId != "pin",
            onRuleSelect = { rule ->
                applyRule(activePlatformId!!, rule)
                activePlatformId = null
            },
            onDismiss = { activePlatformId = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_autopilot_title), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_description)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background, scrolledContainerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                M3SegmentedControl(selectedAction = shareGlobalAction, onSelect = { SettingsManager.setShareGlobalAction(it) })
            }
            item {
                AnimatedContent(targetState = shareGlobalAction, transitionSpec = { (slideInVertically { it / 3 } + fadeIn()).togetherWith(slideOutVertically { -it / 3 } + fadeOut()) }, label = "ModeContentSwitch") { mode ->
                    if (mode == ShareActionGlobal.PREVIEW) {
                        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)), border = com.example.videodownloader.ui.theme.getAppCardBorder()) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp)) }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(stringResource(R.string.how_it_works_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                }
                                Text(text = stringResource(R.string.share_mode_preview_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)), border = com.example.videodownloader.ui.theme.getAppCardBorder()) {
                                Column {
                                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { Icon(Icons.Default.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(stringResource(R.string.stay_in_social_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                            Text(stringResource(R.string.stay_in_social_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(checked = silentShareDownload, onCheckedChange = { SettingsManager.setSilentShareDownload(it) }, colors = customSwitchColors())
                                    }
                                    
                                    val hasConflict = listOf(ruleYoutube, ruleTiktok, ruleVk, ruleInstagram, rulePinterest, ruleOther).any { it == PlatformDownloadRule.PREVIEW_ONLY }
                                    
                                    AnimatedVisibility(
                                        visible = silentShareDownload && hasConflict,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column {
                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = stringResource(R.string.silent_mode_conflict_note),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Text(stringResource(R.string.smart_cleanup_header), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp, modifier = Modifier.padding(start = 6.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                                border = com.example.videodownloader.ui.theme.getAppCardBorder()
                            ) {
                                Column {
                                    Row(modifier = Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.AutoDelete, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(22.dp))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(stringResource(R.string.transit_mode_title), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                                            Text(stringResource(R.string.transit_mode_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(checked = transitEnabled, onCheckedChange = { SettingsManager.setTransitModeEnabled(it) }, colors = customSwitchColors())
                                    }

                                    AnimatedVisibility(
                                        visible = transitEnabled,
                                        enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                                            shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(20.dp),
                                                verticalArrangement = Arrangement.spacedBy(24.dp)
                                            ) {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Text(
                                                        text = stringResource(R.string.transit_mode_header),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                                        SegmentedButton(
                                                            selected = transitAutoOnly,
                                                            onClick = { SettingsManager.setTransitModeAutopilotOnly(true) },
                                                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                                            icon = { SegmentedButtonDefaults.Icon(active = transitAutoOnly) { Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(16.dp)) } }
                                                        ) { Text(stringResource(R.string.transit_mode_smart), fontWeight = FontWeight.Bold) }
                                                        SegmentedButton(
                                                            selected = !transitAutoOnly,
                                                            onClick = { SettingsManager.setTransitModeAutopilotOnly(false) },
                                                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                                            icon = { SegmentedButtonDefaults.Icon(active = !transitAutoOnly) { Icon(Icons.Default.Public, null, modifier = Modifier.size(16.dp)) } }
                                                        ) { Text(stringResource(R.string.transit_mode_all), fontWeight = FontWeight.Bold) }
                                                    }
                                                    
                                                    AnimatedContent(
                                                        targetState = transitAutoOnly,
                                                        transitionSpec = { (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut()) },
                                                        label = "ModeDescAnim"
                                                    ) { isAuto ->
                                                        Text(
                                                            text = if (isAuto) stringResource(R.string.transit_mode_smart_desc) else stringResource(R.string.transit_mode_all_desc),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                                                        )
                                                    }
                                                }

                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = stringResource(R.string.size_threshold_header),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Black,
                                                            color = MaterialTheme.colorScheme.primary,
                                                            letterSpacing = 0.5.sp
                                                        )
                                                        Spacer(modifier = Modifier.weight(1f))
                                                        
                                                        val badgeScale by animateFloatAsState(
                                                            targetValue = 1f,
                                                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                                            label = "BadgeScale"
                                                        )
                                                        
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.primaryContainer,
                                                            shape = CircleShape,
                                                            modifier = Modifier.scale(badgeScale)
                                                        ) {
                                                            Text(
                                                                text = if (transitMinSize == 0) stringResource(R.string.size_any) else stringResource(R.string.size_more_than_mb, transitMinSize),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Slider(
                                                        value = when(transitMinSize) {
                                                            0 -> 0f; 10 -> 1f; 50 -> 2f; 100 -> 3f; 500 -> 4f; else -> 0f
                                                        },
                                                        onValueChange = { 
                                                            val s = when(it.toInt()) { 0 -> 0; 1 -> 10; 2 -> 50; 3 -> 100; 4 -> 500; else -> 0 }
                                                            SettingsManager.setTransitModeMinSizeMb(s)
                                                        },
                                                        steps = 3,
                                                        valueRange = 0f..4f
                                                    )
                                                }

                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Text(
                                                        text = stringResource(R.string.active_services_header),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        letterSpacing = 0.5.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    
                                                    FlowRow(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                                        maxItemsInEachRow = 2
                                                    ) {
                                                        val platforms = listOf(
                                                            "YouTube" to Icons.Default.PlayCircle,
                                                            "TikTok" to Icons.Default.MusicVideo,
                                                            "Instagram" to Icons.Default.CameraAlt,
                                                            "VK Видео" to Icons.Default.SlowMotionVideo
                                                        )
                                                        platforms.forEach { (name, icon) ->
                                                            val isSelected = transitPlatforms.contains(name)
                                                            
                                                            val bgColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, label = "btnBg")
                                                            val contentColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, label = "btnContent")
                                                            val scale by animateFloatAsState(if (isSelected) 1.03f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow), label = "btnScale")

                                                            Surface(
                                                                onClick = {
                                                                    view.performAppHaptic(HapticType.CLICK)
                                                                    SettingsManager.toggleTransitPlatform(name)
                                                                },
                                                                modifier = Modifier.weight(1f).height(52.dp).graphicsLayer(scaleX = scale, scaleY = scale),
                                                                shape = RoundedCornerShape(16.dp),
                                                                color = bgColor,
                                                                border = if (!isSelected) com.example.videodownloader.ui.theme.getAppCardBorder() else null,
                                                                tonalElevation = if (isSelected) 4.dp else 0.dp
                                                            ) {
                                                                Row(
                                                                    modifier = Modifier.padding(horizontal = 12.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.Center
                                                                ) {
                                                                    Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
                                                                    Spacer(modifier = Modifier.width(10.dp))
                                                                    Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = contentColor)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                                border = com.example.videodownloader.ui.theme.getAppCardBorder()
                            ) {
                                Column {
                                    SettingsSwitchRow(
                                        title = "Быстрая отправка",
                                        subtitle = "Оптимизация файлов для Telegram",
                                        icon = Icons.Default.Speed,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        stateFlow = SettingsManager.transitOptimizeSize,
                                        onCheckedChange = { SettingsManager.setTransitOptimizeSize(it) }
                                    )

                                    AnimatedVisibility(
                                        visible = transitOptimizeSize,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                            OptRowItem(
                                                title = "Разрешение",
                                                subtitle = "Лимит качества видео",
                                                badgeText = when (transitMaxQuality) { "720" -> "720p HD"; "480" -> "480p SD"; else -> "1080p HD" },
                                                icon = Icons.Default.Hd,
                                                onClick = { activeOptSheet = "quality" }
                                            )

                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                            OptRowItem(
                                                title = "Видеокодек",
                                                subtitle = "Совместимость с чатами",
                                                badgeText = if (transitCodec == "h265") "H.265 (HEVC)" else "H.264 (MP4)",
                                                icon = Icons.Default.MovieFilter,
                                                onClick = { activeOptSheet = "codec" }
                                            )

                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                            OptRowItem(
                                                title = "Сжатие фото",
                                                subtitle = "Уменьшение веса снимков",
                                                badgeText = when (transitPhotoQuality) { 75 -> "75% Сильное"; 100 -> "Без сжатия"; else -> "88% Баланс" },
                                                icon = Icons.Default.PhotoSizeSelectLarge,
                                                onClick = { activeOptSheet = "photo" }
                                            )

                                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                            OptRowItem(
                                                title = "Порог размера",
                                                subtitle = "Когда запускать сжатие",
                                                badgeText = when (transitMinSizeThreshold) { 10 -> "> 10 МБ"; 25 -> "> 25 МБ"; 50 -> "> 50 МБ"; 100 -> "> 100 МБ"; 200 -> "> 200 МБ"; else -> "Всегда" },
                                                icon = Icons.Default.FilterAlt,
                                                onClick = { activeOptSheet = "threshold" }
                                            )
                                        }
                                    }
                                }
                            }

                            Text(stringResource(R.string.platform_rules_header), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp, modifier = Modifier.padding(start = 6.dp))
                            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)), border = com.example.videodownloader.ui.theme.getAppCardBorder()) {
                                Column {
                                    PlatformRowItem(name = "YouTube", icon = Icons.Default.PlayCircle, iconColor = Color(0xFFFF0000), iconBg = Color(0xFFFF0000).copy(alpha = 0.15f), rule = ruleYoutube, isConflict = silentShareDownload && ruleYoutube == PlatformDownloadRule.PREVIEW_ONLY, onClick = { activePlatformId = "yt" })
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    PlatformRowItem(name = "TikTok", icon = Icons.Default.MusicVideo, iconColor = Color(0xFF00F2FE), iconBg = Color(0xFF00F2FE).copy(alpha = 0.15f), rule = ruleTiktok, isConflict = silentShareDownload && ruleTiktok == PlatformDownloadRule.PREVIEW_ONLY, onClick = { activePlatformId = "tt" })
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    PlatformRowItem(name = "VK Видео", icon = Icons.Default.SlowMotionVideo, iconColor = Color(0xFF0077FF), iconBg = Color(0xFF0077FF).copy(alpha = 0.15f), rule = ruleVk, isConflict = silentShareDownload && ruleVk == PlatformDownloadRule.PREVIEW_ONLY, onClick = { activePlatformId = "vk" })
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    PlatformRowItem(name = "Instagram", icon = Icons.Default.CameraAlt, iconColor = Color(0xFFE1306C), iconBg = Color(0xFFE1306C).copy(alpha = 0.15f), rule = ruleInstagram, isConflict = silentShareDownload && ruleInstagram == PlatformDownloadRule.PREVIEW_ONLY, onClick = { activePlatformId = "ig" })
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    PlatformRowItem(name = "Pinterest", icon = Icons.Default.PushPin, iconColor = Color(0xFFE60023), iconBg = Color(0xFFE60023).copy(alpha = 0.15f), rule = rulePinterest, isConflict = silentShareDownload && rulePinterest == PlatformDownloadRule.PREVIEW_ONLY, onClick = { activePlatformId = "pin" })
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    PlatformRowItem(name = stringResource(R.string.all_other_sites), icon = Icons.Default.Language, iconColor = MaterialTheme.colorScheme.primary, iconBg = MaterialTheme.colorScheme.primaryContainer, rule = ruleOther, isConflict = silentShareDownload && ruleOther == PlatformDownloadRule.PREVIEW_ONLY, onClick = { activePlatformId = "other" })
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    if (activeOptSheet != null) {
        val sheetTitle = when (activeOptSheet) {
            "quality" -> "Максимальное разрешение"
            "codec" -> "Формат и видеокодек"
            "photo" -> "Сжатие фотографий"
            "threshold" -> "Порог размера для активации"
            else -> ""
        }

        val sheetHeaderIcon = when (activeOptSheet) {
            "quality" -> Icons.Default.Hd
            "codec" -> Icons.Default.MovieFilter
            "photo" -> Icons.Default.PhotoSizeSelectLarge
            "threshold" -> Icons.Default.FilterAlt
            else -> Icons.Default.Tune
        }

        val options = when (activeOptSheet) {
            "quality" -> listOf(
                OptSheetItem("1080", "1080p Full HD", "Оптимальный баланс четкости и размера (~15–25 МБ)", Icons.Default.HighQuality),
                OptSheetItem("720", "720p HD", "Ультра-быстрая отправка в Telegram (~10–12 МБ)", Icons.Default.Speed),
                OptSheetItem("480", "480p SD", "Максимальная экономия трафика при слабом 3G", Icons.Default.DataSaverOn)
            )
            "codec" -> listOf(
                OptSheetItem("h264", "H.264 (AVC) • Совместимый", "Проигрывается прямо внутри чатов Telegram, WhatsApp и VK", Icons.Default.PlayCircleOutline),
                OptSheetItem("h265", "H.265 (HEVC) • Ультра-сжатие", "Сжимает файл на 40% сильнее (может отправляться документом)", Icons.Default.VideoSettings)
            )
            "photo" -> listOf(
                OptSheetItem("88", "88% • Баланс сжатия", "Минус 70% веса файла без ощутимой потери деталей", Icons.Default.AutoFixHigh),
                OptSheetItem("75", "75% • Сильное сжатие", "Максимальная компактность для каруселей из 20+ снимков", Icons.Default.PhotoSizeSelectSmall),
                OptSheetItem("100", "100% • Без сжатия", "Сохранять оригинальные несжатые изображения", Icons.Default.Image)
            )
            "threshold" -> listOf(
                OptSheetItem("0", "Всегда", "Применять оптимизацию ко всем скачиваемым медиафайлам", Icons.Default.FlashOn),
                OptSheetItem("10", "Больше 10 МБ", "Сжимать только если файл весит больше 10 МБ", Icons.Default.Filter1),
                OptSheetItem("25", "Больше 25 МБ", "Сжимать только если файл весит больше 25 МБ", Icons.Default.Filter2),
                OptSheetItem("50", "Больше 50 МБ", "Сжимать файлы весом больше 50 МБ (лимит обычного Telegram)", Icons.Default.Filter3),
                OptSheetItem("100", "Больше 100 МБ", "Сжимать только если файл весит больше 100 МБ", Icons.Default.Filter4),
                OptSheetItem("200", "Больше 200 МБ", "Сжимать только гигантские файлы весом больше 200 МБ", Icons.Default.Filter5)
            )
            else -> emptyList()
        }

        val currentSelectedKey = when (activeOptSheet) {
            "quality" -> transitMaxQuality
            "codec" -> transitCodec
            "photo" -> transitPhotoQuality.toString()
            "threshold" -> transitMinSizeThreshold.toString()
            else -> ""
        }

        ModalBottomSheet(
            onDismissRequest = { activeOptSheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 18.dp, start = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            sheetHeaderIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = sheetTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                options.forEach { opt ->
                    val (key, name, desc, icon) = opt
                    val isSelected = currentSelectedKey == key
                    Surface(
                        onClick = {
                            view.performAppHaptic(HapticType.CLICK)
                            when (activeOptSheet) {
                                "quality" -> SettingsManager.setTransitMaxQuality(key)
                                "codec" -> SettingsManager.setTransitCodec(key)
                                "photo" -> SettingsManager.setTransitPhotoQuality(key.toInt())
                                "threshold" -> SettingsManager.setTransitMinSizeThreshold(key.toInt())
                            }
                            activeOptSheet = null
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptRowItem(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlatformRuleBottomSheet(
    title: String,
    icon: ImageVector,
    currentRule: PlatformDownloadRule,
    isSilentMode: Boolean,
    allowAudio: Boolean,
    onRuleSelect: (PlatformDownloadRule) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val view = LocalView.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            // Предупреждение о конфликте параметров
            if (isSilentMode && currentRule == PlatformDownloadRule.PREVIEW_ONLY) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            stringResource(R.string.analysis_rule_conflict_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SelectableRuleOption(
                icon = Icons.Default.Videocam,
                title = stringResource(R.string.rule_video_photo_title),
                subtitle = stringResource(R.string.rule_video_photo_subtitle),
                selected = currentRule == PlatformDownloadRule.VIDEO,
                onClick = { 
                    view.performAppHaptic(HapticType.CLICK)
                    onRuleSelect(PlatformDownloadRule.VIDEO) 
                }
            )

            if (allowAudio) {
                SelectableRuleOption(
                    icon = Icons.Default.MusicNote,
                    title = stringResource(R.string.rule_audio_only_title),
                    subtitle = stringResource(R.string.rule_audio_only_subtitle),
                    selected = currentRule == PlatformDownloadRule.AUDIO,
                    onClick = { 
                        view.performAppHaptic(HapticType.CLICK)
                        onRuleSelect(PlatformDownloadRule.AUDIO) 
                    }
                )
            }

            SelectableRuleOption(
                icon = Icons.Default.Visibility,
                title = stringResource(R.string.rule_preview_title),
                subtitle = stringResource(R.string.rule_preview_subtitle),
                selected = currentRule == PlatformDownloadRule.PREVIEW_ONLY,
                onClick = { 
                    view.performAppHaptic(HapticType.CLICK)
                    onRuleSelect(PlatformDownloadRule.PREVIEW_ONLY) 
                }
            )
        }
    }
}

fun applyRule(id: String, rule: PlatformDownloadRule) {
    when (id) {
        "yt" -> SettingsManager.setRuleYoutube(rule)
        "tt" -> SettingsManager.setRuleTiktok(rule)
        "vk" -> SettingsManager.setRuleVk(rule)
        "ig" -> SettingsManager.setRuleInstagram(rule)
        "pin" -> SettingsManager.setRulePinterest(rule)
        "other" -> SettingsManager.setRuleOther(rule)
    }
}
