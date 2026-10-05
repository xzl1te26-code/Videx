package com.example.videodownloader.ui.screens.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.CacheManager
import com.example.videodownloader.FileNameTemplate
import com.example.videodownloader.MinDiskSpace
import com.example.videodownloader.R
import com.example.videodownloader.SettingsManager
import com.example.videodownloader.ui.components.showInstantSnackbar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var cacheSizeText by remember { mutableStateOf("0 КБ") }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showDiskSpaceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val sizeBytes = CacheManager.getCacheSizeBytes(context)
        cacheSizeText = CacheManager.formatSize(sizeBytes)
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            SettingsManager.setCustomDir(context, uri)
            coroutineScope.launch { snackbarHostState.showInstantSnackbar(context.getString(R.string.save_folder_changed_toast)) }
        }
    }

    if (showTemplateDialog) {
        val fileNameTemplate by SettingsManager.fileNameTemplate.collectAsState()
        val templateOptions = listOf(
            Triple(FileNameTemplate.TITLE_PLATFORM, "Название и сервис", "Видео - YouTube.mp4"),
            Triple(FileNameTemplate.TITLE_ONLY, "Только название", "Видео.mp4"),
            Triple(FileNameTemplate.PLATFORM_TITLE, "Сервис и название", "[YouTube] Видео.mp4"),
            Triple(FileNameTemplate.DATE_TITLE, "Дата и название", "20261005_Видео.mp4"),
            Triple(FileNameTemplate.DATE_TIME_TITLE, "Дата, время и название", "20261005_1830_Видео.mp4")
        )

        ModalBottomSheet(
            onDismissRequest = { showTemplateDialog = false },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            Icons.Default.TextFields,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.filename_template_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Выберите формат сохранения медиафайлов",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 4.dp))

                templateOptions.forEach { (tmpl, title, subtitle) ->
                    val isSelected = fileNameTemplate == tmpl
                    val icon = when (tmpl) {
                        FileNameTemplate.TITLE_PLATFORM -> Icons.Default.TextFields
                        FileNameTemplate.TITLE_ONLY -> Icons.Default.Subtitles
                        FileNameTemplate.PLATFORM_TITLE -> Icons.Default.Label
                        FileNameTemplate.DATE_TITLE -> Icons.Default.Event
                        FileNameTemplate.DATE_TIME_TITLE -> Icons.Default.Schedule
                    }

                    Surface(
                        onClick = {
                            SettingsManager.setFileNameTemplate(tmpl)
                            showTemplateDialog = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    SettingsManager.setFileNameTemplate(tmpl)
                                    showTemplateDialog = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { showTemplateDialog = false },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Готово", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showDiskSpaceDialog) {
        val minDiskSpace by SettingsManager.minDiskSpace.collectAsState()
        val spaceOptions = listOf(
            Triple(MinDiskSpace.NONE, "Без ограничений", "Скачивать до полного заполнения устройства"),
            Triple(MinDiskSpace.MB500, "Минимум 500 МБ", "Защита от переполнения внутренней памяти"),
            Triple(MinDiskSpace.GB1, "Минимум 1 ГБ", "Оптимальный запас места для системы")
        )

        ModalBottomSheet(
            onDismissRequest = { showDiskSpaceDialog = false },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SdStorage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.disk_space_limit_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Порог остановки загрузок при нехватке памяти",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 4.dp))

                spaceOptions.forEach { (limit, title, subtitle) ->
                    val isSelected = minDiskSpace == limit
                    val icon = when (limit) {
                        MinDiskSpace.NONE -> Icons.Default.AllInclusive
                        MinDiskSpace.MB500 -> Icons.Default.SdCardAlert
                        MinDiskSpace.GB1 -> Icons.Default.DiscFull
                    }

                    Surface(
                        onClick = {
                            SettingsManager.setMinDiskSpace(limit)
                            showDiskSpaceDialog = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    SettingsManager.setMinDiskSpace(limit)
                                    showDiskSpaceDialog = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { showDiskSpaceDialog = false },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Готово", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Хранилище и файлы", fontWeight = FontWeight.Bold) },
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
                    text = "КАТАЛОГИ И ГАЛЕРЕЯ",
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
                        DownloadFolderRow { folderPickerLauncher.launch(null) }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.save_to_gallery_title),
                            subtitle = stringResource(R.string.save_to_gallery_desc),
                            icon = Icons.Default.PhotoLibrary,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            stateFlow = SettingsManager.saveToGallery,
                            onCheckedChange = { SettingsManager.setSaveToGallery(it) }
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier
                                .clickable {
                                    coroutineScope.launch {
                                        val currentBytes = CacheManager.getCacheSizeBytes(context)
                                        if (currentBytes <= 1024L) {
                                            snackbarHostState.showInstantSnackbar(context.getString(R.string.cache_already_empty_toast))
                                        } else {
                                            val success = CacheManager.clearCache(context)
                                            if (success) {
                                                cacheSizeText = "0 КБ"
                                                snackbarHostState.showInstantSnackbar(context.getString(R.string.cache_cleared_toast))
                                            }
                                        }
                                    }
                                }
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.clear_cache_title), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                                Text(stringResource(R.string.clear_cache_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                                Text(text = cacheSizeText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "ИМЕНА И ЛИМИТЫ",
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
                        FileNameTemplateRow { showTemplateDialog = true }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        DiskSpaceControlRow { showDiskSpaceDialog = true }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        SettingsSwitchRow(
                            title = stringResource(R.string.check_duplicates_title),
                            subtitle = stringResource(R.string.check_duplicates_desc),
                            icon = Icons.Default.CopyAll,
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            stateFlow = SettingsManager.checkDuplicates,
                            onCheckedChange = { SettingsManager.setCheckDuplicates(it) }
                        )
                    }
                }
            }
        }
    }
}
