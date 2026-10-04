package com.example.videodownloader.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.*
import com.example.videodownloader.utils.exportAndShareLogs
import com.example.videodownloader.utils.exportSmartDiagnosticReport
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LogViewerScreen(snackbarHostState: SnackbarHostState, onBack: () -> Unit) {
    val logs by AsyncLogger.logs.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isErrorsOnly by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showShareDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showDateDialog by remember { mutableStateOf(false) }
    var selectedDateFilter by remember { mutableStateOf("ALL") }

    val errorCount = remember(logs) { logs.count { it.level == LogLevel.ERROR } }

    val filteredLogs = remember(logs, isErrorsOnly, searchQuery) {
        logs.filter { entry ->
            val matchesLevel = if (isErrorsOnly) entry.level == LogLevel.ERROR else true
            val matchesSearch = searchQuery.isBlank() ||
                    entry.message.contains(searchQuery, ignoreCase = true) ||
                    entry.timestamp.contains(searchQuery, ignoreCase = true)

            matchesLevel && matchesSearch
        }.reversed()
    }

    // 📅 ДИАЛОГ ВЫБОРА ДНЯ
    if (showDateDialog) {
        AlertDialog(
            onDismissRequest = { showDateDialog = false },
            title = {
                Text(
                    text = "Выбор дня логов",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "ALL" to ("Все дни (Полный лог)" to "${logs.size} записей"),
                        "TODAY" to ("Сегодня" to "Свежие события за сегодня"),
                        "YESTERDAY" to ("Вчера" to "Архив за вчерашний день")
                    ).forEach { (key, info) ->
                        val (title, subtitle) = info
                        val isSelected = selectedDateFilter == key

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedDateFilter = key
                                    showDateDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                RadioButton(selected = isSelected, onClick = {
                                    selectedDateFilter = key
                                    showDateDialog = false
                                })
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDateDialog = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Закрыть")
                }
            }
        )
    }

    // 🔴 ДИАЛОГ ОЧИСТКИ
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            icon = { Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Очистить логи?", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth()) },
            text = { Text("Вся записанная история системных событий будет безвозвратно удалена.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            AsyncLogger.clear()
                            showClearDialog = false
                            coroutineScope.launch { snackbarHostState.showSnackbar("Логи очищены") }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Очистить всё", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { showClearDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Отмена", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            dismissButton = {}
        )
    }

    // 📱 TELEGRAM / iOS STYLE BOTTOM SHEET "ОТЧЕТ РАЗРАБОТЧИКУ"
    if (showShareDialog) {
        val lastError = remember(logs) { logs.lastOrNull { it.level == LogLevel.ERROR }?.message ?: "Сбоев в сессии не зафиксировано" }

        ModalBottomSheet(
            onDismissRequest = { showShareDialog = false },
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
                // ШАПКА ШТОРКИ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Отчёт разработчику",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Videx v1.0.0 • Ошибок: $errorCount • Записей: ${logs.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ТЕРМИНАЛЬНЫЙ СНИППЕТ В СТИЛЕ TELEGRAM
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (errorCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ПОСЛЕДНЯЯ ЗАПИСЬ:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (errorCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                fontSize = 10.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = lastError,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // КНОПКА 1: СКОПИРОВАТЬ ОТЧЕТ
                Button(
                    onClick = {
                        exportSmartDiagnosticReport(context, logs)
                        showShareDialog = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Скопировать готовый отчёт", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                // КНОПКА 2: ПОДЕЛИТЬСЯ ФАЙЛОМ
                FilledTonalButton(
                    onClick = {
                        exportAndShareLogs(context, logs, "Full")
                        showShareDialog = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Отправить файл логов", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Системные логи", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)) {
                            com.example.videodownloader.ui.components.RollingItemCounter(
                                count = logs.size,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        val text = logs.joinToString("\n") { "[${it.timestamp}] [${it.level.name}] ${it.message}" }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Videx Logs", text))
                        coroutineScope.launch { snackbarHostState.showSnackbar("Логи скопированы в буфер") }
                    },
                    enabled = logs.isNotEmpty()
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Копировать")
                }

                IconButton(
                    onClick = { showShareDialog = true },
                    enabled = logs.isNotEmpty()
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Экспорт отчета", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = { showClearDialog = true },
                    enabled = logs.isNotEmpty()
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Очистить", tint = MaterialTheme.colorScheme.error)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                scrolledContainerColor = MaterialTheme.colorScheme.background
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // 📅 ВЫБОР ДНЯ И ФИЛЬТР ОШИБОК
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateArrowRotation by animateFloatAsState(
                    targetValue = if (showDateDialog) 180f else 0f,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "dateArrowRot"
                )

                Surface(
                    onClick = { showDateDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (selectedDateFilter) {
                                    "TODAY" -> "Сегодня"
                                    "YESTERDAY" -> "Вчера"
                                    else -> "Все дни"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp).rotate(dateArrowRotation)
                        )
                    }
                }

                // КНОПКА ФИЛЬТРА "ТОЛЬКО ОШИБКИ" С ПЛАВНЫМИ ЦВЕТАМИ И СЧЕТЧИКОМ
                val errorBtnBg by animateColorAsState(
                    targetValue = if (isErrorsOnly) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    animationSpec = tween(200),
                    label = "errBtnBg"
                )
                val errorBtnContent by animateColorAsState(
                    targetValue = if (isErrorsOnly) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
                    animationSpec = tween(200),
                    label = "errBtnContent"
                )
                val errorBtnBorder by animateColorAsState(
                    targetValue = if (isErrorsOnly) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                    animationSpec = tween(200),
                    label = "errBtnBorder"
                )

                Surface(
                    onClick = { isErrorsOnly = !isErrorsOnly },
                    shape = RoundedCornerShape(14.dp),
                    color = errorBtnBg,
                    border = BorderStroke(1.dp, errorBtnBorder),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (isErrorsOnly) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ошибки",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = errorBtnContent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        com.example.videodownloader.ui.components.RollingItemCounter(
                            count = errorCount,
                            color = errorBtnContent,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 🔎 СТРОКА ПОИСКА ПО ЛОГАМ
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск по логам...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Сброс")
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 🖥️ ЧИСТАЯ КОНСОЛЬ ЛОГОВ С МОНОШИРИННЫМ ШРИФТОМ
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
            ) {
                if (filteredLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isErrorsOnly) "Критических ошибок нет 🎉" else "Событий не найдено",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(items = filteredLogs, key = { it.id }) { log ->
                            Box(modifier = Modifier.animateItem()) {
                                ModernLogCard(log = log)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernLogCard(log: LogEntry) {
    var isExpanded by remember { mutableStateOf(false) }
    val isLongMessage = log.message.length > 110 || log.message.contains("\n")

    val (badgeBg, badgeTextColor) = when (log.level) {
        LogLevel.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        LogLevel.WARN -> Color(0xFF5A4300) to Color(0xFFFFD54F)
        LogLevel.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        LogLevel.PERF -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }

    val cardBorderColor by animateColorAsState(
        if (log.level == LogLevel.ERROR) MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
        else if (log.level == LogLevel.PERF) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        label = "logBorder"
    )

    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "chevronRot"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(if (isExpanded) 4.dp else 2.dp),
        border = BorderStroke(1.dp, cardBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isLongMessage) {
                    Modifier.clickable { isExpanded = !isExpanded }
                } else Modifier
            )
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = log.level.name,
                        color = badgeTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = log.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                    if (isLongMessage) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp).rotate(chevronRotation)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
