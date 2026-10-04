package com.example.videodownloader.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HapticSettingsScreen(onBack: () -> Unit) {
    val localView = LocalView.current
    val hapticEnabled by SettingsManager.hapticEnabled.collectAsState()
    val hapticIntensity by SettingsManager.hapticIntensity.collectAsState()

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
                Text(
                    text = "ОБЩИЕ НАСТРОЙКИ",
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
                        SettingsSwitchRow(
                            title = "Тактильная отдача (Вибрация)",
                            subtitle = "Включить вибрацию при касаниях и действиях в Videx",
                            icon = Icons.Default.Vibration,
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            stateFlow = SettingsManager.hapticEnabled,
                            onCheckedChange = {
                                SettingsManager.setHapticEnabled(it)
                                if (it) localView.performAppHaptic(HapticType.CLICK)
                            }
                        )
                    }
                }
            }

            if (hapticEnabled) {
                item {
                    Text(
                        text = "СИЛА И ИНТЕНСИВНОСТЬ",
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
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Интенсивность вибрации",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Нажмите на вариант, чтобы сразу ощутить силу вибрации",
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
                                        localView.performAppHaptic(HapticType.CLICK)
                                    }
                                )

                                HapticIntensityChip(
                                    label = "Стандартная",
                                    selected = hapticIntensity == HapticIntensity.STANDARD,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        SettingsManager.setHapticIntensity(HapticIntensity.STANDARD)
                                        localView.performAppHaptic(HapticType.CLICK)
                                    }
                                )

                                HapticIntensityChip(
                                    label = "Сильная",
                                    selected = hapticIntensity == HapticIntensity.STRONG,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        SettingsManager.setHapticIntensity(HapticIntensity.STRONG)
                                        localView.performAppHaptic(HapticType.CLICK)
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "КАТЕГОРИИ ДЕЙСТВИЙ",
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
                            SettingsSwitchRow(
                                title = "Кнопки и переключатели",
                                subtitle = "Вибрация при нажатии на кнопки, ссылки и вкладки",
                                icon = Icons.Default.TouchApp,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                stateFlow = SettingsManager.hapticButtons,
                                onCheckedChange = {
                                    SettingsManager.setHapticButtons(it)
                                    if (it) localView.performAppHaptic(HapticType.CLICK)
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
                                    if (it) localView.performAppHaptic(HapticType.PLAYER_GESTURE)
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
                                    if (it) localView.performAppHaptic(HapticType.LONG_PRESS)
                                }
                            )
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
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
        }
    }
}
