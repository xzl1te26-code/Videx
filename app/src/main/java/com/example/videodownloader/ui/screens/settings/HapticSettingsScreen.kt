package com.example.videodownloader.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HapticSettingsScreen(onBack: () -> Unit) {
    val localView = LocalView.current
    val hapticEnabled by SettingsManager.hapticEnabled.collectAsState()
    val hapticIntensity by SettingsManager.hapticIntensity.collectAsState()

    var triggerPulse by remember { mutableIntStateOf(0) }
    val pulseScale by animateFloatAsState(
        targetValue = if (triggerPulse > 0) 1.25f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "pulseScale"
    )

    LaunchedEffect(triggerPulse) {
        if (triggerPulse > 0) {
            delay(150)
            triggerPulse = 0
        }
    }

    fun fireHaptic(type: HapticType) {
        localView.performAppHaptic(type)
        triggerPulse++
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тактильный отклик", fontWeight = FontWeight.Bold) },
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            val outerAlpha by animateFloatAsState(
                                targetValue = if (triggerPulse > 0) 0.35f else 0f,
                                animationSpec = tween(200),
                                label = "outerAlpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .scale(pulseScale * 1.15f)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = outerAlpha))
                            )

                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .scale(pulseScale)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hapticEnabled) Icons.Default.Vibration else Icons.Default.PortableWifiOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (hapticEnabled) "Отклик активен" else "Вибрация отключена",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = if (hapticEnabled) "Нажмите на любой вариант ниже для проверки силы" else "Включите тактильный отклик для настройки интенсивности",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "ОБЩИЕ НАСТРОЙКИ",
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
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    border = com.example.videodownloader.ui.theme.getAppCardBorder()
                ) {
                    Column {
                        SettingsSwitchRow(
                            title = "Тактильная отдача (Вибрация)",
                            subtitle = "Включить вибрацию при касаниях и действиях в Videx",
                            icon = Icons.Default.Vibration,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            stateFlow = SettingsManager.hapticEnabled,
                            onCheckedChange = {
                                SettingsManager.setHapticEnabled(it)
                                if (it) fireHaptic(HapticType.CLICK)
                            }
                        )
                    }
                }
            }

            item {
                AnimatedVisibility(
                    visible = hapticEnabled,
                    enter = fadeIn(tween(250)) + expandVertically(spring(stiffness = Spring.StiffnessMediumLow)),
                    exit = fadeOut(tween(200)) + shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "СИЛА И ИНТЕНСИВНОСТЬ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                            border = com.example.videodownloader.ui.theme.getAppCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Интенсивность вибрации",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Выберите желаемую силу тактильной отдачи:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    HapticIntensityChip(
                                        label = "Мягкая",
                                        selected = hapticIntensity == HapticIntensity.SOFT,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            SettingsManager.setHapticIntensity(HapticIntensity.SOFT)
                                            fireHaptic(HapticType.CLICK)
                                        }
                                    )

                                    HapticIntensityChip(
                                        label = "Стандарт",
                                        selected = hapticIntensity == HapticIntensity.STANDARD,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            SettingsManager.setHapticIntensity(HapticIntensity.STANDARD)
                                            fireHaptic(HapticType.CLICK)
                                        }
                                    )

                                    HapticIntensityChip(
                                        label = "Сильная",
                                        selected = hapticIntensity == HapticIntensity.STRONG,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            SettingsManager.setHapticIntensity(HapticIntensity.STRONG)
                                            fireHaptic(HapticType.CLICK)
                                        }
                                    )
                                }
                            }
                        }

                        Text(
                            text = "КАТЕГОРИИ ДЕЙСТВИЙ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                            border = com.example.videodownloader.ui.theme.getAppCardBorder()
                        ) {
                            Column {
                                SettingsSwitchRow(
                                    title = "Кнопки и переключатели",
                                    subtitle = "Вибрация при нажатии на кнопки, ссылки и вкладки",
                                    icon = Icons.Default.TouchApp,
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    stateFlow = SettingsManager.hapticButtons,
                                    onCheckedChange = {
                                        SettingsManager.setHapticButtons(it)
                                        if (it) fireHaptic(HapticType.CLICK)
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                SettingsSwitchRow(
                                    title = "Видеоплеер и жесты",
                                    subtitle = "Вибрация при регулировке громкости, яркости и перемотке",
                                    icon = Icons.Default.PlayCircle,
                                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    stateFlow = SettingsManager.hapticPlayer,
                                    onCheckedChange = {
                                        SettingsManager.setHapticPlayer(it)
                                        if (it) fireHaptic(HapticType.PLAYER_GESTURE)
                                    }
                                )

                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                                SettingsSwitchRow(
                                    title = "Долгое нажатие и зажатие",
                                    subtitle = "Вибрация при вызове контекстных меню и копировании",
                                    icon = Icons.Default.Gesture,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    stateFlow = SettingsManager.hapticLongPress,
                                    onCheckedChange = {
                                        SettingsManager.setHapticLongPress(it)
                                        if (it) fireHaptic(HapticType.LONG_PRESS)
                                    }
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
fun HapticIntensityChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "chipScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(220),
        label = "chipBg"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220),
        label = "chipContent"
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .height(44.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        contentColor = contentColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
