package com.example.videodownloader.ui.components

import com.example.videodownloader.performAppHaptic
import com.example.videodownloader.HapticType
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.PlaylistItem
import com.example.videodownloader.ShareActionGlobal

@Composable
fun RollingPercentageBadge(
    progress: Int,
    modifier: Modifier = Modifier
) {
    val hundreds by remember(progress) { derivedStateOf { if (progress >= 100) (progress / 100) % 10 else null } }
    val tens by remember(progress) { derivedStateOf { if (progress >= 10) (progress / 10) % 10 else null } }
    val units by remember(progress) { derivedStateOf { progress % 10 } }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = hundreds != null,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                val h = hundreds
                if (h != null) {
                    SingleRollingDigit(digit = h)
                }
            }

            AnimatedVisibility(
                visible = tens != null,
                enter = expandHorizontally() + fadeIn(),
                exit = shrinkHorizontally() + fadeOut()
            ) {
                val t = tens
                if (t != null) {
                    SingleRollingDigit(digit = t)
                }
            }

            SingleRollingDigit(digit = units)

            Spacer(modifier = Modifier.width(1.dp))

            Text(
                text = "%",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SingleRollingDigit(digit: Int) {
    Box(
        modifier = Modifier
            .width(11.dp)
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = digit,
            transitionSpec = {
                (slideInVertically(
                    animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                    initialOffsetY = { fullHeight -> fullHeight }
                ) + fadeIn(animationSpec = tween(200)))
                    .togetherWith(
                        slideOutVertically(
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                            targetOffsetY = { fullHeight -> -fullHeight }
                        ) + fadeOut(animationSpec = tween(150))
                    )
            },
            label = "DigitDrumSlot"
        ) { currentDigit ->
            Text(
                text = currentDigit.toString(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSelectionBottomSheet(
    playlistTitle: String,
    entries: List<PlaylistItem>,
    selectedQuality: String,
    onDismiss: () -> Unit,
    onDownloadSelected: (selectedItems: List<PlaylistItem>, isAudio: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedIds by remember { mutableStateOf(entries.map { it.id }.toSet()) }
    var isAudioMode by remember { mutableStateOf(false) }
    
    val allSelected by remember(entries) { derivedStateOf { selectedIds.size == entries.size } }
    val selectedCount by remember { derivedStateOf { selectedIds.size } }
    
    val view = LocalView.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlistTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Всего медиафайлов: ${entries.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    val halfWidth = this.maxWidth / 2
                    val pillOffset by animateDpAsState(
                        targetValue = if (isAudioMode) halfWidth else 0.dp,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "PillSlideAnim"
                    )

                    Box(
                        modifier = Modifier
                            .offset { IntOffset(pillOffset.roundToPx(), 0) }
                            .width(halfWidth)
                            .fillMaxHeight()
                            .graphicsLayer { 
                                // Аппаратное ускорение для скругленного переключателя
                                // Аппаратное ускорение для скругленного переключателя
                                shape = RoundedCornerShape(12.dp)
                                clip = true
                            }
                            .background(MaterialTheme.colorScheme.primary)
                    )

                    Row(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isAudioMode) {
                                        view.performAppHaptic(HapticType.CLICK)
                                        isAudioMode = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val textColor by animateColorAsState(
                                if (!isAudioMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "vTextCol"
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Видео HD",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (!isAudioMode) {
                                        view.performAppHaptic(HapticType.CLICK)
                                        isAudioMode = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val textColor by animateColorAsState(
                                if (isAudioMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                label = "aTextCol"
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Аудио MP3",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Выбрано: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    RollingItemCounter(
                        count = selectedCount,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = " из ${entries.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                FilterChip(
                    selected = allSelected,
                    onClick = {
                        view.performAppHaptic(HapticType.CLICK)
                        selectedIds = if (allSelected) emptySet() else entries.map { it.id }.toSet()
                    },
                    label = {
                        Text(
                            text = if (allSelected) "Снять все" else "Выбрать все",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items = entries, key = { it.id }) { item ->
                    val isSelected = selectedIds.contains(item.id)

                    val cardScale by animateFloatAsState(
                        targetValue = if (isSelected) 0.985f else 1f,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "cardScale"
                    )
                    val cardBg by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
                        label = "cardBg"
                    )
                    val borderColor by animateColorAsState(
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        label = "cardBorder"
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardBg,
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(cardScale)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                view.performAppHaptic(HapticType.CLICK)
                                selectedIds = if (isSelected) selectedIds - item.id else selectedIds + item.id
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 68.dp, height = 44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                            ) {
                                if (item.thumbnailUrl.isNotBlank()) {
                                    coil.compose.AsyncImage(
                                        model = item.thumbnailUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                if (item.duration.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.Black.copy(alpha = 0.75f),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(3.dp)
                                    ) {
                                        Text(
                                            text = item.duration,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (item.author.isNotBlank()) {
                                    Text(
                                        text = item.author,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = {
                                    view.performAppHaptic(HapticType.CLICK)
                                    selectedIds = if (it) selectedIds + item.id else selectedIds - item.id
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val itemsToDownload = entries.filter { selectedIds.contains(it.id) }
                    onDownloadSelected(itemsToDownload, isAudioMode)
                },
                enabled = selectedIds.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    if (isAudioMode) Icons.Default.MusicNote else Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isAudioMode) "Скачать как аудио" else "Скачать выбранное",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                        RollingItemCounter(
                            count = selectedIds.size,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RollingItemCounter(
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 12.sp
) {
    Box(modifier = modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                (slideInVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(180)))
                    .togetherWith(
                        slideOutVertically(animationSpec = tween(280, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(140))
                    )
            },
            label = "ItemCounterRoll"
        ) { currentCount ->
            Text(
                text = "$currentCount",
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize
            )
        }
    }
}

@Composable
fun rememberShimmerBrush(): Brush {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerTranslate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset.Zero,
        end = androidx.compose.ui.geometry.Offset(x = translateAnim, y = translateAnim)
    )
}

@Composable
fun MediaCardSkeleton(showThumbnails: Boolean) {
    val brush = rememberShimmerBrush()
    val hints = listOf(
        "Подключаемся к серверу...",
        "Ищем лучшее качество...",
        "Парсим метаданные...",
        "Проверяем доступность...",
        "Подготавливаем прямую ссылку..."
    )
    
    var currentHintIndex by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(3000)
            currentHintIndex = (currentHintIndex + 1) % hints.size
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (showThumbnails) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!showThumbnails) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .width(90.dp)
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(brush),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = hints[currentHintIndex],
                transitionSpec = {
                    (slideInVertically { it / 2 } + fadeIn(tween(400)))
                        .togetherWith(slideOutVertically { -it / 2 } + fadeOut(tween(300)))
                },
                label = "LoadingHintAnim"
            ) { hint ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun M3SegmentedControl(
    selectedAction: ShareActionGlobal,
    onSelect: (ShareActionGlobal) -> Unit
) {
    val isAuto = selectedAction == ShareActionGlobal.AUTOPILOT
    val view = LocalView.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        ) {
            val halfWidth = this.maxWidth / 2

            val pillOffset by animateDpAsState(
                targetValue = if (isAuto) halfWidth else 0.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "SharePillSlide"
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(pillOffset.roundToPx(), 0) }
                    .width(halfWidth)
                    .fillMaxHeight()
                    .graphicsLayer {
                        shape = RoundedCornerShape(12.dp)
                        clip = true
                    }
                    .background(MaterialTheme.colorScheme.primary)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (isAuto) {
                                view.performAppHaptic(HapticType.CLICK)
                                onSelect(ShareActionGlobal.PREVIEW)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val contentColor by animateColorAsState(
                        if (!isAuto) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "prevColor"
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Предпросмотр",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            if (!isAuto) {
                                view.performAppHaptic(HapticType.CLICK)
                                onSelect(ShareActionGlobal.AUTOPILOT)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val contentColor by animateColorAsState(
                        if (isAuto) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "autoColor"
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Автопилот",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * ⚡ Мгновенный вывод плавающего уведомления без ожидания очереди:
 * Мгновенно сбрасывает текущее активное уведомление и показывает новое с анимированной заменой.
 */
suspend fun SnackbarHostState.showInstantSnackbar(
    message: String,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    duration: SnackbarDuration = SnackbarDuration.Short
): SnackbarResult {
    currentSnackbarData?.dismiss()
    return showSnackbar(
        message = message,
        actionLabel = actionLabel,
        withDismissAction = withDismissAction,
        duration = duration
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomAppSnackbar(snackbarData: SnackbarData) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                snackbarData.dismiss()
                true
            } else false
        },
        positionalThreshold = { distance -> distance * 0.45f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {},
        content = {
            val message = snackbarData.visuals.message.lowercase()

            val (badgeBgColor, badgeIconColor, icon) = when {
                message.contains("ошибка") || message.contains("сбой") || message.contains("не найден") -> Triple(
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                    Icons.Default.ErrorOutline
                )
                message.contains("отменен") || message.contains("удален") || message.contains("очищен") -> Triple(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                    MaterialTheme.colorScheme.error,
                    Icons.Default.DeleteOutline
                )
                message.contains("вставлен") || message.contains("буфер") || message.contains("скопирован") -> Triple(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    Icons.Default.ContentPaste
                )
                message.contains("очередь") || message.contains("повторная") || message.contains("запущена") -> Triple(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    Icons.Default.CloudDownload
                )
                message.contains("сохранен") || message.contains("успешно") || message.contains("подключено") -> Triple(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    Icons.Default.CheckCircle
                )
                message.contains("синхронизац") || message.contains("проверена") -> Triple(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                    Icons.Default.Sync
                )
                message.contains("ota") || message.contains("ядро") || message.contains("обновление") -> Triple(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.onTertiaryContainer,
                    Icons.Default.SystemUpdate
                )
                message.contains("аккаунт") || message.contains("сесси") -> Triple(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.onSecondaryContainer,
                    Icons.Default.ManageAccounts
                )
                message.contains("лог") || message.contains("консоль") -> Triple(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                    Icons.Default.Terminal
                )
                else -> Triple(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.onPrimaryContainer,
                    Icons.Default.Info
                )
            }

            val view = LocalView.current
            LaunchedEffect(snackbarData.visuals.message) {
                view.performAppHaptic(HapticType.SUCCESS)
            }

            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 2.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(badgeBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeIconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = snackbarData.visuals.message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        lineHeight = 18.sp
                    )
                    snackbarData.visuals.actionLabel?.let { actionLabel ->
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            onClick = { snackbarData.performAction() },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = actionLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    )
}

