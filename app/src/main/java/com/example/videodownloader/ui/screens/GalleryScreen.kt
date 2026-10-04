package com.example.videodownloader.ui.screens

import android.app.Activity
import android.net.Uri
import com.example.videodownloader.performAppHaptic
import com.example.videodownloader.HapticType
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.example.videodownloader.DownloadedFileItem
import com.example.videodownloader.HistoryManager
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryScreen(
    item: DownloadedFileItem,
    onBack: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current

    val density = context.resources.displayMetrics.density
    val screenWidth = remember(configuration, density) { configuration.screenWidthDp.toFloat() * density }
    val screenHeight = remember(configuration, density) { configuration.screenHeightDp.toFloat() * density }

    val scale = remember { Animatable(1f) }
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val entranceScale = remember { Animatable(0.92f) }
    val entranceAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { entranceScale.animateTo(1f, tween(400, easing = androidx.compose.animation.core.EaseOutBack)) }
        launch { entranceAlpha.animateTo(1f, tween(300)) }
    }

    var showUi by remember { mutableStateOf(true) }
    var showInfo by remember { mutableStateOf(false) }
    var isSwipingToDismiss by remember { mutableStateOf(false) }
    val backgroundAlpha = remember { Animatable(1f) }
    val infoPanelOffsetY = remember { Animatable(0f) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showInfo) { if (showInfo) infoPanelOffsetY.snapTo(0f) }

    // 🚀 ЖЕСТКОЕ УДАЛЕНИЕ ЦВЕТНЫХ ПОЛОС (СТАТУС БАРА И НАВИГАЦИИ) ПРИ ПРОСМОТРЕ
    LaunchedEffect(showUi) {
        val window = activity?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        
        // Отключаем принудительный контраст Android (scrims), который рисует полосы
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        
        // Делаем бэкграунд баров прозрачным на уровне Window
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        if (showUi) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            activity?.window?.let { window ->
                WindowCompat.setDecorFitsSystemWindows(window, true)
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val resetTransforms: suspend () -> Unit = {
        coroutineScope {
            launch { scale.animateTo(1f, spring()) }
            launch { offsetX.animateTo(0f, spring()) }
            launch { offsetY.animateTo(0f, spring()) }
            launch { backgroundAlpha.animateTo(1f, spring()) }
        }
    }

    BackHandler {
        if (showInfo) showInfo = false
        else if (scale.value > 1f || offsetX.value != 0f || offsetY.value != 0f) {
            coroutineScope.launch { resetTransforms(); isSwipingToDismiss = false }
        } else onBack()
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        if (showInfo) return@rememberTransformableState
        coroutineScope.launch {
            val newScale = (scale.value * zoomChange).coerceIn(1f, 5f)
            scale.snapTo(newScale)
            if (newScale > 1f) {
                val maxOffsetX = (screenWidth * (newScale - 1f)) / 2f
                val maxOffsetY = (screenHeight * (newScale - 1f)) / 2f
                offsetX.snapTo((offsetX.value + panChange.x).coerceIn(-maxOffsetX, maxOffsetX))
                offsetY.snapTo((offsetY.value + panChange.y).coerceIn(-maxOffsetY, maxOffsetY))
            } else {
                val nextY = offsetY.value + panChange.y
                offsetY.snapTo(nextY)
                backgroundAlpha.snapTo((1f - (abs(nextY) / (screenHeight / 3f))).coerceIn(0f, 1f))
                isSwipingToDismiss = true
            }
        }
    }

    val modelUriOrFile = remember(item.pathOrUri) {
        if (item.pathOrUri.startsWith("content://")) Uri.parse(item.pathOrUri) else File(item.pathOrUri)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .transformable(state = transformState)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        view.performAppHaptic(HapticType.CLICK)
                        if (showInfo) showInfo = false else showUi = !showUi 
                    },
                    onDoubleTap = { tapOffset ->
                        if (showInfo) return@detectTapGestures
                        view.performAppHaptic(HapticType.LONG_PRESS)
                        coroutineScope.launch {
                            if (scale.value > 1f) { resetTransforms(); isSwipingToDismiss = false }
                            else {
                                launch { scale.animateTo(2.5f, spring()) }
                                launch { offsetX.animateTo((size.width / 2f - tapOffset.x) * 1.5f, spring()) }
                                launch { offsetY.animateTo((size.height / 2f - tapOffset.y) * 1.5f, spring()) }
                            }
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.all { !it.pressed } && isSwipingToDismiss && !showInfo) {
                            if (abs(offsetY.value) > screenHeight / 6f) {
                                coroutineScope.launch {
                                    val exitY = if (offsetY.value > 0) screenHeight else -screenHeight
                                    launch { offsetY.animateTo(exitY, tween(250)) }
                                    launch { backgroundAlpha.animateTo(0f, tween(200)) }
                                    onBack()
                                }
                            } else coroutineScope.launch { resetTransforms(); isSwipingToDismiss = false }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // 🔮 Фоновое размытие
        AsyncImage(
            model = modelUriOrFile,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().blur(40.dp).graphicsLayer(alpha = backgroundAlpha.value * 0.4f)
        )
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = (1f - backgroundAlpha.value).coerceAtLeast(0.3f))))

        // 🖼️ Основное фото
        AsyncImage(
            model = modelUriOrFile,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale.value * entranceScale.value,
                    scaleY = scale.value * entranceScale.value,
                    translationX = offsetX.value,
                    translationY = offsetY.value,
                    alpha = entranceAlpha.value
                )
        )

        // 🔝 TOP BAR
        AnimatedVisibility(
            visible = showUi,
            enter = fadeIn(tween(250)) + slideInVertically(animationSpec = tween(250), initialOffsetY = { -it }),
            exit = fadeOut(tween(200)) + slideOutVertically(animationSpec = tween(200), targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(0.75f), Color.Black.copy(0.3f), Color.Transparent)))
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp, top = 38.dp), // Фиксированный верхний отступ убирает дёрганье при скрытии статус-бара
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp).background(Color.White.copy(0.15f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                }
                
                Column(
                    modifier = Modifier.weight(1f).padding(start = 12.dp, end = 4.dp),
                    horizontalAlignment = Alignment.Start // Выравниваем по левому краю для максимальной ширины
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp),
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = item.sizeFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.6f)
                    )
                }
            }
        }

        // ⚓ BOTTOM ACTION DOCK: Идеальное центрирование кнопок
        AnimatedVisibility(
            visible = showUi && !showInfo,
            enter = fadeIn(tween(250)) + slideInVertically(animationSpec = tween(250), initialOffsetY = { it }),
            exit = fadeOut(tween(200)) + slideOutVertically(animationSpec = tween(200), targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .height(64.dp)
                    .wrapContentWidth(), // Автоматическая идеальная ширина по контенту
                color = Color.White.copy(0.12f),
                shape = RoundedCornerShape(32.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.White.copy(0.12f))
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight().padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.Center, // Четкое выравнивание кнопок по центру дока
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Поделиться
                    ActionIconButton(Icons.Default.Share) {
                        view.performAppHaptic(HapticType.CLICK)
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(android.content.Intent.EXTRA_STREAM, modelUriOrFile.let { if (it is File) Uri.fromFile(it) else it as Uri })
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Отправить"))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Информация
                    ActionIconButton(Icons.Rounded.Info) {
                        view.performAppHaptic(HapticType.CLICK)
                        showInfo = true
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Удалить
                    ActionIconButton(Icons.Default.DeleteOutline, tint = Color(0xFFFF453A)) {
                        view.performAppHaptic(HapticType.LONG_PRESS)
                        showDeleteDialog = true
                    }
                }
            }
        }

        // ℹ️ ШТОРКА СВОЙСТВ: Полное восстановление лучшего дизайна с круглыми иконками
        AnimatedVisibility(
            visible = showUi && showInfo,
            enter = fadeIn(tween(250)) + slideInVertically(animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing), initialOffsetY = { it }),
            exit = fadeOut(tween(200)) + slideOutVertically(animationSpec = tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing), targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                    .graphicsLayer { translationY = infoPanelOffsetY.value.coerceAtLeast(0f) }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (infoPanelOffsetY.value > 120f) { showInfo = false; view.performAppHaptic(HapticType.CLICK) }
                                    else infoPanelOffsetY.animateTo(0f, spring())
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch { infoPanelOffsetY.snapTo((infoPanelOffsetY.value + dragAmount).coerceAtLeast(0f)) }
                            }
                        )
                    },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shape = RoundedCornerShape(30.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Box(modifier = Modifier.size(40.dp, 5.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), CircleShape).align(Alignment.CenterHorizontally))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Свойства файла",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = 0.2.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (item.name.contains(".")) item.name.substringAfterLast(".").uppercase() else "МЕДИА",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))

                    // Выстраиваем свойства друг под другом на всю ширину, чтобы длинная дата не переносилась
                    InfoItemCard(icon = Icons.Rounded.Storage, label = "Размер", value = item.sizeFormatted)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    InfoItemCard(icon = Icons.Rounded.CalendarToday, label = "Загружен", value = item.dateFormatted, maxLines = 1)
                    Spacer(modifier = Modifier.height(10.dp))

                    InfoItemCard(icon = Icons.Rounded.Description, label = "Имя файла", value = item.name, maxLines = 2)
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    // Блок пути к файлу с кнопкой копирования
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.FolderOpen, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Локальный путь", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(item.pathOrUri, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    view.performAppHaptic(HapticType.CLICK)
                                    clipboardManager.setText(AnnotatedString(item.pathOrUri))
                                    Toast.makeText(context, "Путь скопирован в буфер", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.Default.ContentCopy, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Удалить файл?") },
            text = { Text("Файл «${item.name}» будет полностью удален из памяти устройства.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        HistoryManager.deleteFile(context, item)
                        launch(kotlinx.coroutines.Dispatchers.Main) { onBack() }
                    }
                }) { Text("Удалить", color = Color(0xFFFF453A)) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") } }
        )
    }
}

@Composable
fun ActionIconButton(icon: ImageVector, tint: Color = Color.White, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun InfoItemCard(icon: ImageVector, label: String, value: String, maxLines: Int = 1) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
