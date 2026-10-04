package com.example.videodownloader.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.*
import com.example.videodownloader.R
import com.example.videodownloader.logic.OtaManager
import com.example.videodownloader.ui.components.showInstantSnackbar
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsMainScreen(
    snackbarHostState: SnackbarHostState,
    onOpenLogs: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenShareRules: () -> Unit,
    onOpenSpeedSettings: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenNotifSettings: () -> Unit,
    onOpenPlayerSettings: () -> Unit,
    onOpenAppearanceSettings: () -> Unit,
    onOpenStorageSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
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
                    text = stringResource(R.string.main_features_header),
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
                        AutopilotRuleRow(onOpenShareRules)

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsNavigationRow(
                            title = stringResource(R.string.login_services_title),
                            subtitle = stringResource(R.string.login_services_desc),
                            icon = Icons.Default.LockOpen,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = onOpenAccounts
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsNavigationRow(
                            title = stringResource(R.string.speed_and_threads_title),
                            subtitle = stringResource(R.string.speed_and_threads_desc),
                            icon = Icons.Default.RocketLaunch,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            subtitleColor = MaterialTheme.colorScheme.primary,
                            onClick = onOpenSpeedSettings
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsNavigationRow(
                            title = stringResource(R.string.notif_settings_title),
                            subtitle = stringResource(R.string.notif_settings_desc),
                            icon = Icons.Default.Notifications,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = onOpenNotifSettings
                        )
                    }
                }
            }

            item {
                Text(
                    text = "ПЛЕЕР И ФИЛЬТРЫ",
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
                        val useInternalPlayer by SettingsManager.useInternalPlayer.collectAsState()
                        SettingsNavigationRow(
                            title = stringResource(R.string.player_selection_header),
                            subtitle = if (useInternalPlayer) stringResource(R.string.internal_player_title) else stringResource(R.string.external_player_title),
                            icon = Icons.Default.PlayCircle,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = onOpenPlayerSettings
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.sponsorblock_title),
                            subtitle = stringResource(R.string.sponsorblock_desc),
                            icon = Icons.Default.SubtitlesOff,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            stateFlow = SettingsManager.useSponsorBlock,
                            onCheckedChange = { SettingsManager.setUseSponsorBlock(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.anti_block_title),
                            subtitle = stringResource(R.string.anti_block_desc),
                            icon = Icons.Default.Security,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            stateFlow = SettingsManager.useImpersonate,
                            onCheckedChange = { SettingsManager.setUseImpersonate(it) }
                        )
                    }
                }
            }

            item {
                Text(
                    text = "ОФОРМЛЕНИЕ И ХРАНИЛИЩЕ",
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
                        SettingsNavigationRow(
                            title = "Интерфейс и внешний вид",
                            subtitle = "Цвета Material You, селектор качества, превью, виджеты",
                            icon = Icons.Default.Palette,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = onOpenAppearanceSettings
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsNavigationRow(
                            title = "Хранилище и файлы",
                            subtitle = "Папка сохранения, очистка кэша, шаблоны имён, лимиты памяти",
                            icon = Icons.Default.FolderOpen,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = onOpenStorageSettings
                        )
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.system_and_core_header),
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
                        AppUpdateRow(snackbarHostState)

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        OtaUpdateRow(snackbarHostState)

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsNavigationRow(
                            title = stringResource(R.string.system_logs_title),
                            subtitle = stringResource(R.string.system_logs_desc),
                            icon = Icons.AutoMirrored.Filled.List,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = onOpenLogs
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsNavigationRow(
                            title = stringResource(R.string.about_title),
                            subtitle = stringResource(R.string.about_app_desc),
                            icon = Icons.Default.Info,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = onOpenAbout
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AutopilotRuleRow(onOpenShareRules: () -> Unit) {
    val shareGlobalAction by SettingsManager.shareGlobalAction.collectAsState()
    SettingsNavigationRow(
        title = stringResource(R.string.share_autopilot_title),
        subtitle = if (shareGlobalAction == ShareActionGlobal.AUTOPILOT) stringResource(R.string.share_mode_autopilot_desc) else stringResource(R.string.share_mode_preview_short_desc),
        icon = Icons.Default.AutoAwesome,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        onClick = onOpenShareRules
    )
}

@Composable
fun DownloadFolderRow(onClick: () -> Unit) {
    val customDirName by SettingsManager.customDirName.collectAsState()
    SettingsNavigationRow(
        title = stringResource(R.string.download_folder_title),
        subtitle = customDirName,
        icon = Icons.Default.FolderOpen,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        subtitleColor = MaterialTheme.colorScheme.primary,
        onClick = onClick
    )
}

@Composable
fun FileNameTemplateRow(onClick: () -> Unit) {
    val fileNameTemplate by SettingsManager.fileNameTemplate.collectAsState()
    val tmplText = when(fileNameTemplate) {
        FileNameTemplate.TITLE_PLATFORM -> "Название - Платформа"
        FileNameTemplate.DATE_TITLE -> "Дата_Название"
    }
    SettingsNavigationRow(
        title = stringResource(R.string.filename_template_title),
        subtitle = tmplText,
        icon = Icons.Default.TextFields,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        subtitleColor = MaterialTheme.colorScheme.primary,
        showBadge = false,
        onClick = onClick,
        trailingIcon = Icons.Default.ExpandMore
    )
}

@Composable
fun DiskSpaceControlRow(onClick: () -> Unit) {
    val minDiskSpace by SettingsManager.minDiskSpace.collectAsState()
    val limitText = when(minDiskSpace) {
        MinDiskSpace.NONE -> stringResource(R.string.disabled_status)
        MinDiskSpace.MB500 -> stringResource(R.string.stop_at_500mb)
        MinDiskSpace.GB1 -> stringResource(R.string.stop_at_1gb)
    }
    SettingsNavigationRow(
        title = stringResource(R.string.disk_space_control_title),
        subtitle = limitText,
        icon = Icons.Default.SdStorage,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        subtitleColor = MaterialTheme.colorScheme.primary,
        showBadge = false,
        onClick = onClick,
        trailingIcon = Icons.Default.ExpandMore
    )
}

@Composable
fun AppUpdateRow(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isChecking by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.example.videodownloader.logic.AppUpdateInfo?>(null) }

    if (updateInfo != null && updateInfo!!.hasUpdate) {
        com.example.videodownloader.ui.components.AppUpdateDialog(
            updateInfo = updateInfo!!,
            onDismiss = { updateInfo = null }
        )
    }

    Row(
        modifier = Modifier
            .clickable {
                if (!isChecking) {
                    isChecking = true
                    coroutineScope.launch {
                        val result = com.example.videodownloader.logic.AppUpdateChecker.checkForUpdates()
                        isChecking = false
                        if (result.isSuccess) {
                            val info = result.getOrThrow()
                            if (info.hasUpdate) {
                                updateInfo = info
                            } else {
                                snackbarHostState.showInstantSnackbar("У вас установлена последняя версия Videx (v${info.latestVersion})")
                            }
                        } else {
                            snackbarHostState.showInstantSnackbar("Не удалось проверить обновления")
                        }
                    }
                }
            }
            .padding(16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.GetApp, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Обновление приложения", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Проверить новые версии Videx на GitHub", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isChecking) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun OtaUpdateRow(snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isUpdating by OtaManager.isUpdating.collectAsState()

    Row(
        modifier = Modifier
            .clickable {
                if (!isUpdating) {
                    coroutineScope.launch { snackbarHostState.showInstantSnackbar(context.getString(R.string.checking_ota_version_toast)) }
                    OtaManager.startUpdate(context) { message -> coroutineScope.launch { snackbarHostState.showInstantSnackbar(message) } }
                }
            }
            .padding(16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.update_core_ota_title), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.update_core_ota_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (isUpdating) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showBadge: Boolean = false,
    onClick: () -> Unit,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.ChevronRight
) {
    Row(
        modifier = Modifier
            .clickable { onClick() }
            .padding(16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(containerColor), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(trailingIcon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    stateFlow: StateFlow<Boolean>,
    onCheckedChange: (Boolean) -> Unit
) {
    val checked by stateFlow.collectAsState()
    Row(
        modifier = Modifier.padding(16.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(containerColor), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = contentColor)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = customSwitchColors()
        )
    }
}
