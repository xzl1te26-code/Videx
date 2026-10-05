package com.example.videodownloader.ui.screens

import android.net.Uri
import com.example.videodownloader.performAppHaptic
import com.example.videodownloader.HapticType
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import androidx.compose.ui.graphics.graphicsLayer
import com.example.videodownloader.*
import com.example.videodownloader.ui.components.RollingItemCounter
import com.example.videodownloader.ui.components.showInstantSnackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CompactStorageWidget(fileList: List<DownloadedFileItem>) {
    val totalBytes = remember(fileList) { fileList.sumOf { it.sizeBytes } }
    val videoBytes = remember(fileList) { fileList.filter { it.mediaType == MediaType.VIDEO }.sumOf { it.sizeBytes } }
    val audioBytes = remember(fileList) { fileList.filter { it.mediaType == MediaType.AUDIO }.sumOf { it.sizeBytes } }
    val photoBytes = remember(fileList) { fileList.filter { it.mediaType == MediaType.PHOTO }.sumOf { it.sizeBytes } }

    var hasAnimated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasAnimated = true
    }

    val fillProgress by animateFloatAsState(
        targetValue = if (hasAnimated) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "StorageCanvasFill"
    )

    val textScale by animateFloatAsState(
        targetValue = if (hasAnimated) 1f else 0.9f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "TotalBytesPulse"
    )

    val videoColor = MaterialTheme.colorScheme.primary
    val audioColor = MaterialTheme.colorScheme.secondary
    val photoColor = MaterialTheme.colorScheme.tertiary
    val trackBgColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = com.example.videodownloader.ui.theme.getAppCardBorder()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Память",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${fileList.size} файлов",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = HistoryManager.formatBytes(totalBytes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.scale(textScale)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
            ) {
                val totalWidth = size.width
                val cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2, size.height / 2)

                drawRoundRect(
                    color = trackBgColor,
                    size = size,
                    cornerRadius = cornerRadius
                )

                if (totalBytes > 0 && fillProgress > 0.001f) {
                    val gapPx = 2.5.dp.toPx()
                    val activeTotalWidth = (totalWidth * fillProgress)

                    val videoWidth = (videoBytes.toFloat() / totalBytes) * activeTotalWidth
                    val audioWidth = (audioBytes.toFloat() / totalBytes) * activeTotalWidth
                    val photoWidth = (photoBytes.toFloat() / totalBytes) * activeTotalWidth

                    var currentX = 0f

                    if (videoWidth > 0f) {
                        val drawWidth = if (audioWidth > 0f || photoWidth > 0f) (videoWidth - gapPx).coerceAtLeast(0f) else videoWidth
                        drawRoundRect(
                            color = videoColor,
                            topLeft = androidx.compose.ui.geometry.Offset(currentX, 0f),
                            size = androidx.compose.ui.geometry.Size(drawWidth, size.height),
                            cornerRadius = cornerRadius
                        )
                        currentX += videoWidth
                    }

                    if (audioWidth > 0f) {
                        val drawWidth = if (photoWidth > 0f) (audioWidth - gapPx).coerceAtLeast(0f) else audioWidth
                        drawRoundRect(
                            color = audioColor,
                            topLeft = androidx.compose.ui.geometry.Offset(currentX, 0f),
                            size = androidx.compose.ui.geometry.Size(drawWidth, size.height),
                            cornerRadius = cornerRadius
                        )
                        currentX += audioWidth
                    }

                    if (photoWidth > 0f) {
                        drawRoundRect(
                            color = photoColor,
                            topLeft = androidx.compose.ui.geometry.Offset(currentX, 0f),
                            size = androidx.compose.ui.geometry.Size(photoWidth, size.height),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StorageLegendItem(color = videoColor, title = "Видео", size = videoBytes)
                StorageLegendItem(color = audioColor, title = "Аудио", size = audioBytes)
                StorageLegendItem(color = photoColor, title = "Фото", size = photoBytes)
            }
        }
    }
}

@Composable
fun StorageLegendItem(color: Color, title: String, size: Long) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 13.sp
            )
            Text(
                text = HistoryManager.formatBytes(size),
                style = MaterialTheme.typography.labelMedium,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 15.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(
    snackbarHostState: SnackbarHostState,
    mainViewModel: com.example.videodownloader.viewmodel.MainViewModel,
    onOpenPlayer: (DownloadedFileItem) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val view = LocalView.current

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    // Получаем состояние истории из ViewModel
    val fileList by mainViewModel.historyState.collectAsState()

    var isScanning by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<DownloadedFileItem?>(null) }
    var fileToPrune by remember { mutableStateOf<DownloadedFileItem?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var showPruneAllDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedItems by remember { mutableStateOf(setOf<String>()) }
    var showMultiDeleteDialog by remember { mutableStateOf(false) }

    // 💾 ИСПОЛЬЗУЕМ СОСТОЯНИЕ ИЗ VIEWMODEL ДЛЯ ПЕРСИСТЕНТНОСТИ (сохраняется при навигации)
    // Больше не используем локальные remember, чтобы при возврате из плеера всё оставалось на месте


    // 📜 УПРАВЛЕНИЕ СКРОЛЛОМ: Запоминаем позицию
    val listState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = mainViewModel.historyScrollIndex,
        initialFirstVisibleItemScrollOffset = mainViewModel.historyScrollOffset
    )

    // Сохраняем позицию скролла в ViewModel при изменении (snapshotFlow для производительности)
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                mainViewModel.historyScrollIndex = index
                mainViewModel.historyScrollOffset = offset
            }
    }

    val showHistoryThumbnails by SettingsManager.showHistoryThumbnails.collectAsState()
    val showHistorySort by SettingsManager.showHistorySort.collectAsState()
    val historySyncMode by SettingsManager.historySyncMode.collectAsState()

    val showStorageStats by SettingsManager.showStorageStats.collectAsState()
    val useInternalPlayer by SettingsManager.useInternalPlayer.collectAsState()

    // Вычисление статистики за один проход
    val stats = remember(fileList) {
        var missing = 0
        var video = 0
        var audio = 0
        var photo = 0
        fileList.forEach {
            if (it.isMissing) missing++
            when (it.mediaType) {
                MediaType.VIDEO -> video++
                MediaType.AUDIO -> audio++
                MediaType.PHOTO -> photo++
            }
        }
        arrayOf(missing, video, audio, photo)
    }

    val missingCount = stats[0]
    val videoCount = stats[1]
    val audioCount = stats[2]
    val photoCount = stats[3]

    val filteredFileList by remember(fileList, mainViewModel.historySelectedTypeFilter, mainViewModel.historySearchQuery, mainViewModel.historySortOrder) {
        derivedStateOf {
            fileList.filter { item ->
                val matchesType = mainViewModel.historySelectedTypeFilter == null || item.mediaType == mainViewModel.historySelectedTypeFilter
                val matchesQuery = mainViewModel.historySearchQuery.isBlank() || item.name.contains(mainViewModel.historySearchQuery.trim(), ignoreCase = true)
                matchesType && matchesQuery
            }.let { filtered ->
                when (mainViewModel.historySortOrder) {
                    com.example.videodownloader.viewmodel.HistorySortOrder.NEWEST -> filtered.sortedByDescending { it.timestamp }
                    com.example.videodownloader.viewmodel.HistorySortOrder.OLDEST -> filtered.sortedBy { it.timestamp }
                    com.example.videodownloader.viewmodel.HistorySortOrder.SIZE_LARGEST -> filtered.sortedByDescending { it.sizeBytes }
                    com.example.videodownloader.viewmodel.HistorySortOrder.SIZE_SMALLEST -> filtered.sortedBy { it.sizeBytes }
                    com.example.videodownloader.viewmodel.HistorySortOrder.DURATION_LONGEST -> filtered.sortedByDescending { it.durationMs }
                }
            }
        }
    }

    BackHandler(enabled = isSelectionMode || mainViewModel.historySearchQuery.isNotEmpty()) {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedItems = emptySet()
        } else if (mainViewModel.historySearchQuery.isNotEmpty()) {
            mainViewModel.historySearchQuery = ""
        }
    }

    val itemToPrune = fileToPrune
    if (itemToPrune != null) {
        AlertDialog(
            onDismissRequest = { fileToPrune = null },
            icon = { Icon(Icons.Default.LinkOff, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Файл не найден", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Text("Файл «${itemToPrune.name}» был удален или перемещен вручную. Удалить запись из истории?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val id = itemToPrune.id
                            fileToPrune = null
                            coroutineScope.launch {
                                val dao = AppDatabase.getDatabase(context).historyDao()
                                dao.deleteById(id)
                                snackbarHostState.showInstantSnackbar("Запись удалена")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Удалить запись", fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { fileToPrune = null }, modifier = Modifier.fillMaxWidth()) { Text("Оставить", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            },
            dismissButton = {}
        )
    }

    if (showPruneAllDialog) {
        AlertDialog(
            onDismissRequest = { showPruneAllDialog = false },
            icon = { Icon(Icons.Default.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Очистить историю?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Text("Будут удалены все записи ($missingCount шт.), файлы которых физически отсутствуют на устройстве.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showPruneAllDialog = false
                            coroutineScope.launch {
                                val dao = AppDatabase.getDatabase(context).historyDao()
                                fileList.filter { it.isMissing }.forEach { dao.deleteById(it.id) }
                                snackbarHostState.showInstantSnackbar("История очищена от мертвых ссылок")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Очистить", fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { showPruneAllDialog = false }, modifier = Modifier.fillMaxWidth()) { Text("Отмена") }
                }
            },
            dismissButton = {}
        )
    }

    val itemToDelete = fileToDelete
    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Удалить файл?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Text("Файл «${itemToDelete.name}» будет удален из памяти и истории.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            fileToDelete = null
                            coroutineScope.launch {
                                val deleted = HistoryManager.deleteFile(context, itemToDelete)
                                if (deleted) {
                                    snackbarHostState.showInstantSnackbar("Файл удален")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Удалить", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { fileToDelete = null },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            },
            dismissButton = {}
        )
    }

    if (showMultiDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showMultiDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Удалить выбранные?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = { Text("Будет удалено файлов: ${selectedItems.size}. Это действие нельзя отменить.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showMultiDeleteDialog = false
                            coroutineScope.launch {
                                val itemsToDelete = fileList.filter { selectedItems.contains(it.id) }
                                var deletedCount = 0
                                itemsToDelete.forEach { item ->
                                    if (HistoryManager.deleteFile(context, item)) deletedCount++
                                }
                                isSelectionMode = false
                                selectedItems = emptySet()
                                snackbarHostState.showInstantSnackbar("Удалено файлов: $deletedCount")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Удалить", fontWeight = FontWeight.Bold) }

                    TextButton(
                        onClick = { showMultiDeleteDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            },
            dismissButton = {}
        )
    }

    if (showSortMenu) {
        ModalBottomSheet(
            onDismissRequest = { showSortMenu = false },
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
                            Icons.AutoMirrored.Filled.Sort,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Сортировка истории",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Выберите порядок отображения файлов",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 4.dp))

                val sortOptions = listOf(
                    com.example.videodownloader.viewmodel.HistorySortOrder.NEWEST to ("Сначала новые" to Icons.Default.Schedule),
                    com.example.videodownloader.viewmodel.HistorySortOrder.OLDEST to ("Сначала старые" to Icons.Default.HistoryToggleOff),
                    com.example.videodownloader.viewmodel.HistorySortOrder.SIZE_LARGEST to ("Сначала тяжелые" to Icons.Default.ArrowDownward),
                    com.example.videodownloader.viewmodel.HistorySortOrder.SIZE_SMALLEST to ("Сначала легкие" to Icons.Default.ArrowUpward),
                    com.example.videodownloader.viewmodel.HistorySortOrder.DURATION_LONGEST to ("Самые длинные (Медиа)" to Icons.Default.PlayCircleOutline)
                )

                sortOptions.forEach { (order, uiData) ->
                    val (title, icon) = uiData
                    val isSelected = mainViewModel.historySortOrder == order
                    
                    Surface(
                        onClick = {
                            view.performAppHaptic(HapticType.CLICK)
                            mainViewModel.historySortOrder = order
                            showSortMenu = false
                        },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else Color.Transparent,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            if (isSelected) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { showSortMenu = false },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Готово", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showClearAllDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp)) },
            title = { Text("Удалить всё?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Text("Все скачанные файлы (${fileList.size} шт.) будут полностью удалены из памяти телефона и истории.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showClearAllDialog = false
                            coroutineScope.launch {
                                val success = HistoryManager.clearAllHistory(context)
                                if (success) {
                                    snackbarHostState.showInstantSnackbar("История и файлы полностью очищены")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Да, удалить всё", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = { showClearAllDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            },
            dismissButton = {},
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically { -it / 2 }) togetherWith (fadeOut(tween(150)) + slideOutVertically { -it / 2 })
                },
                label = "TopBarModeTransition"
            ) { selectionActive ->
                if (selectionActive) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RollingItemCounter(
                                    count = selectedItems.size,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "выбрано",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                view.performAppHaptic(HapticType.CLICK)
                                isSelectionMode = false
                                selectedItems = emptySet()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Отменить выбор")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    view.performAppHaptic(HapticType.CLICK)
                                    HistoryManager.shareMultipleFiles(context, fileList.filter { selectedItems.contains(it.id) })
                                },
                                enabled = selectedItems.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = "Поделиться",
                                    tint = if (selectedItems.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                            IconButton(
                                onClick = {
                                    view.performAppHaptic(HapticType.CLICK)
                                    showMultiDeleteDialog = true
                                },
                                enabled = selectedItems.isNotEmpty()
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Удалить",
                                    tint = if (selectedItems.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                            IconButton(
                                onClick = {
                                    view.performAppHaptic(HapticType.CLICK)
                                    selectedItems = if (selectedItems.size == filteredFileList.size) emptySet() else filteredFileList.map { it.id }.toSet()
                                }
                            ) {
                                Icon(
                                    if (selectedItems.size == filteredFileList.size) Icons.Default.Deselect else Icons.Default.SelectAll,
                                    contentDescription = "Выбрать все"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                } else {
                    TopAppBar(
                        title = { Text("История", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) },
                        actions = {
                            if (fileList.isNotEmpty()) {
                                if (historySyncMode == com.example.videodownloader.HistorySyncMode.BOTH || historySyncMode == com.example.videodownloader.HistorySyncMode.BUTTON_ONLY) {
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            isScanning = true
                                            HistoryManager.syncWithStorage(context)
                                            isScanning = false
                                            snackbarHostState.showInstantSnackbar("Синхронизация завершена")
                                        }
                                    }, enabled = !isScanning) {
                                        if (isScanning) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.Sync, contentDescription = "Синхронизировать с памятью")
                                        }
                                    }
                                }
                                if (missingCount > 0) {
                                    IconButton(onClick = { showPruneAllDialog = true }) {
                                        BadgedBox(badge = { Badge { Text(missingCount.toString()) } }) {
                                            Icon(Icons.Default.LinkOff, contentDescription = "Очистить мертвые ссылки", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }

                                if (showHistorySort) {
                                    IconButton(onClick = { 
                                        view.performAppHaptic(HapticType.CLICK)
                                        showSortMenu = true 
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Сортировка")
                                    }
                                }

                                IconButton(onClick = {
                                    view.performAppHaptic(HapticType.CLICK)
                                    isSelectionMode = true
                                }) {
                                    Icon(Icons.Default.Checklist, contentDescription = "Выбрать")
                                }
                                IconButton(onClick = { showClearAllDialog = true }) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить всё", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.background
                        ),
                        scrollBehavior = scrollBehavior
                    )
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isScanning,
            onRefresh = {
                val allowSwipe = historySyncMode == com.example.videodownloader.HistorySyncMode.BOTH || historySyncMode == com.example.videodownloader.HistorySyncMode.SWIPE_ONLY
                if (allowSwipe && !isSelectionMode && !isScanning) {
                    coroutineScope.launch {
                        isScanning = true
                        HistoryManager.syncWithStorage(context)
                        isScanning = false
                        snackbarHostState.showInstantSnackbar("Синхронизация завершена")
                    }
                }
            },
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            LazyColumn(
                state = listState, // 📜 ПРИВЯЗЫВАЕМ СОСТОЯНИЕ СКРОЛЛА
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 115.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showStorageStats && fileList.isNotEmpty() && !isSelectionMode) {
                    item {
                        CompactStorageWidget(fileList = fileList)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                if (fileList.isNotEmpty()) {
                    stickyHeader {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = !isSelectionMode,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            HistoryHeader(
                                searchQuery = mainViewModel.historySearchQuery,
                                onSearchQueryChange = { mainViewModel.historySearchQuery = it },
                                selectedTypeFilter = mainViewModel.historySelectedTypeFilter,
                                onFilterClick = { mainViewModel.historySelectedTypeFilter = it },
                                totalCount = fileList.size,
                                videoCount = videoCount,
                                audioCount = audioCount,
                                photoCount = photoCount,
                                onClearSearch = {
                                    mainViewModel.historySearchQuery = ""
                                    focusManager.clearFocus()
                                }
                            )
                        }
                    }
                }
                if (fileList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(36.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("История пока пуста", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Все скачанные видео, фото и треки будут отображаться здесь", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isScanning = true
                                            HistoryManager.syncWithStorage(context)
                                            isScanning = false
                                            snackbarHostState.showInstantSnackbar("Память проверена")
                                        }
                                    },
                                    enabled = !isScanning,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Найти файлы в памяти", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (filteredFileList.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillParentMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (mainViewModel.historySearchQuery.isNotBlank()) "Ничего не найдено по запросу «${mainViewModel.historySearchQuery}»" else "Файлов этого типа нет",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    itemsIndexed(items = filteredFileList, key = { _, item -> item.id }) { index, item ->
                        HistoryItemCard(
                            item = item,
                            index = index,
                            modifier = Modifier.animateItem(
                                fadeInSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
                                fadeOutSpec = tween(durationMillis = 280, easing = FastOutLinearInEasing),
                                placementSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                            ),
                            isSelected = selectedItems.contains(item.id),
                            isSelectionMode = isSelectionMode,
                            showHistoryThumbnails = showHistoryThumbnails,
                            onCardClick = {
                                if (isSelectionMode) {
                                    view.performAppHaptic(HapticType.CLICK)
                                    selectedItems = if (selectedItems.contains(item.id)) selectedItems - item.id else selectedItems + item.id
                                } else if (item.isMissing) {
                                    fileToPrune = item
                                } else {
                                    if (useInternalPlayer) {
                                        onOpenPlayer(item)
                                    } else {
                                        HistoryManager.openFile(context, item)
                                    }
                                }
                            },
                            onCardLongClick = {
                                if (!isSelectionMode) {
                                    view.performAppHaptic(HapticType.LONG_PRESS)
                                    isSelectionMode = true
                                    selectedItems = selectedItems + item.id
                                }
                            },
                            onShareClick = { HistoryManager.shareFile(context, item) },
                            onDeleteClick = { fileToDelete = item },
                            onPruneClick = { fileToPrune = item },
                            onRedownloadClick = {
                                if (item.sourceUrl.isNotBlank()) {
                                    com.example.videodownloader.logic.DownloadManager.startDownload(
                                        context = context,
                                        url = item.sourceUrl,
                                        title = item.name,
                                        isAudio = item.mediaType == MediaType.AUDIO,
                                        thumbnailUrl = item.thumbnailUrl
                                    )
                                    coroutineScope.launch {
                                        snackbarHostState.showInstantSnackbar("Повторная загрузка запущена")
                                    }
                                }
                            },
                            onCheckboxChange = { checked ->
                                selectedItems = if (checked) selectedItems + item.id else selectedItems - item.id
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryItemCard(
    item: DownloadedFileItem,
    index: Int,
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    showHistoryThumbnails: Boolean,
    onCardClick: () -> Unit,
    onCardLongClick: () -> Unit,
    onShareClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPruneClick: () -> Unit,
    onRedownloadClick: () -> Unit,
    onCheckboxChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = androidx.compose.ui.platform.LocalDensity.current

    LaunchedEffect(item.pathOrUri) {
        HistoryManager.checkAndUpdateFileStatus(context, item)
    }

    // 🎨 ПЛАВНАЯ GPU-АНИМАЦИЯ ПОЯВЛЕНИЯ КАРТОЧКИ
    var isAppeared by rememberSaveable(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) {
        isAppeared = true
    }

    val entranceAlpha by animateFloatAsState(
        targetValue = if (isAppeared) 1f else 0f,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "entranceAlpha"
    )
    val startOffsetPx = remember(density) { with(density) { 20.dp.toPx() } }
    val entranceTranslationY by animateFloatAsState(
        targetValue = if (isAppeared) 0f else startOffsetPx,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "entranceTranslationY"
    )

    val colorScheme = MaterialTheme.colorScheme
    val (containerColor, onContainerColor, icon) = remember(item.mediaType, colorScheme) {
        when (item.mediaType) {
            MediaType.PHOTO -> Triple(colorScheme.tertiaryContainer, colorScheme.onTertiaryContainer, Icons.Default.PhotoLibrary)
            MediaType.AUDIO -> Triple(colorScheme.secondaryContainer, colorScheme.onSecondaryContainer, Icons.Default.MusicNote)
            MediaType.VIDEO -> Triple(colorScheme.primaryContainer, colorScheme.onPrimaryContainer, Icons.Default.PlayArrow)
        }
    }

    // 🎨 ПЛАВНАЯ АНИМАЦИЯ ЦВЕТА И РАМОК ПРИ ВЫДЕЛЕНИИ
    val cardBgColor by animateColorAsState(
        targetValue = if (isSelected) colorScheme.primaryContainer.copy(alpha = 0.45f) else colorScheme.surfaceColorAtElevation(1.dp),
        animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing),
        label = "cardBgColor"
    )
    val cardBorderColor by animateColorAsState(
        targetValue = if (isSelected) colorScheme.primary else if (item.isMissing) colorScheme.error.copy(alpha = 0.6f) else colorScheme.outlineVariant,
        animationSpec = tween(durationMillis = 180, easing = LinearOutSlowInEasing),
        label = "cardBorderColor"
    )
    val cardBorderWidth by animateDpAsState(
        targetValue = if (isSelected) 2.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "cardBorderWidth"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (isSelected) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "cardScale"
    )

    val imageRequest = remember(item.thumbnailUrl, item.pathOrUri, context) {
        val thumbData = item.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { url ->
            if (url.startsWith("http://") || url.startsWith("https://")) url
            else File(url)
        } ?: if (item.pathOrUri.startsWith("content://")) Uri.parse(item.pathOrUri) else File(item.pathOrUri)

        val builder = ImageRequest.Builder(context)
            .data(thumbData)
            .crossfade(150)
            .diskCachePolicy(coil.request.CachePolicy.ENABLED)
            .memoryCachePolicy(coil.request.CachePolicy.ENABLED)

        if (item.thumbnailUrl.isNullOrBlank() && item.mediaType == MediaType.VIDEO) {
            builder.decoderFactory(VideoFrameDecoder.Factory())
        }
        builder.build()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
                translationY = entranceTranslationY
                alpha = entranceAlpha
            }
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onCardLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = com.example.videodownloader.ui.theme.getAppCardBorder(isSelected)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (showHistoryThumbnails && !item.isMissing && (item.mediaType == MediaType.PHOTO || item.mediaType == MediaType.VIDEO)) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (item.isMissing) MaterialTheme.colorScheme.errorContainer else containerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (item.isMissing) Icons.Default.LinkOff else icon, contentDescription = null, tint = if (item.isMissing) MaterialTheme.colorScheme.error else onContainerColor, modifier = Modifier.size(24.dp))
                    }
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSelected,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                val historyTitleMode by SettingsManager.historyTitleMode.collectAsState()
                val titleModifier = when (historyTitleMode) {
                    com.example.videodownloader.HistoryTitleMode.MARQUEE -> Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                    else -> Modifier
                }
                val maxLinesValue = when (historyTitleMode) {
                    com.example.videodownloader.HistoryTitleMode.TWO_LINES -> 2
                    else -> 1
                }

                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = maxLinesValue,
                    overflow = TextOverflow.Ellipsis,
                    color = if (item.isMissing) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    modifier = titleModifier
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (item.isMissing) {
                    Text(
                        text = "Файл удален с устройства",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "${item.sizeFormatted} • ${item.dateFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
            if (!isSelectionMode) {
                if (item.isMissing) {
                    if (item.sourceUrl.isNotBlank()) {
                        IconButton(onClick = onRedownloadClick) {
                            Icon(Icons.Default.Download, contentDescription = "Скачать повторно", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(onClick = onPruneClick) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Удалить запись", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                } else {
                    IconButton(onClick = onShareClick) {
                        Icon(Icons.Default.Share, contentDescription = "Поделиться", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = {
                        view.performAppHaptic(HapticType.CLICK)
                        onCheckboxChange(it)
                    },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun HistoryHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTypeFilter: MediaType?,
    onFilterClick: (MediaType?) -> Unit,
    totalCount: Int,
    videoCount: Int,
    audioCount: Int,
    photoCount: Int,
    onClearSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Поиск по названию...", fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTypeFilter == null,
                onClick = { onFilterClick(null) },
                label = { Text("Все ($totalCount)") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = selectedTypeFilter == MediaType.VIDEO,
                onClick = { onFilterClick(MediaType.VIDEO) },
                label = { Text("Видео ($videoCount)") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = selectedTypeFilter == MediaType.AUDIO,
                onClick = { onFilterClick(MediaType.AUDIO) },
                label = { Text("Аудио ($audioCount)") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = selectedTypeFilter == MediaType.PHOTO,
                onClick = { onFilterClick(MediaType.PHOTO) },
                label = { Text("Фото ($photoCount)") },
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
