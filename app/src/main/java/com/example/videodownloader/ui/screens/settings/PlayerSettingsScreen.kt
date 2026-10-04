package com.example.videodownloader.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.R
import com.example.videodownloader.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsScreen(onBack: () -> Unit) {
    val useInternalPlayer by SettingsManager.useInternalPlayer.collectAsState()
    val pipEnabled by SettingsManager.pipEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.player_selection_header), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            item {
                Text(
                    text = "ИНСТРУМЕНТЫ ПРОСМОТРА",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectableRuleOption(
                        icon = Icons.Default.SmartDisplay,
                        title = stringResource(R.string.internal_player_title),
                        subtitle = stringResource(R.string.internal_player_desc),
                        selected = useInternalPlayer,
                        onClick = { SettingsManager.setUseInternalPlayer(true) }
                    )

                    SelectableRuleOption(
                        icon = Icons.Default.OpenInNew,
                        title = stringResource(R.string.external_player_title),
                        subtitle = stringResource(R.string.external_player_desc),
                        selected = !useInternalPlayer,
                        onClick = { SettingsManager.setUseInternalPlayer(false) }
                    )
                }
            }

            item {
                AnimatedVisibility(
                    visible = useInternalPlayer,
                    enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "НАСТРОЙКИ ВОСПРОИЗВЕДЕНИЯ",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp, top = 8.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.PictureInPicture,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.pip_mode_title),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(R.string.pip_mode_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    Switch(
                                        checked = pipEnabled,
                                        onCheckedChange = { SettingsManager.setPipEnabled(it) },
                                        colors = customSwitchColors()
                                    )
                                }

                                AnimatedVisibility(
                                    visible = pipEnabled,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                        )

                                        val pipActionsEnabled by SettingsManager.pipActionsEnabled.collectAsState()
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.SettingsSuggest,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.secondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                            
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = stringResource(R.string.pip_actions_title),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = stringResource(R.string.pip_actions_desc),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            
                                            Switch(
                                                checked = pipActionsEnabled,
                                                onCheckedChange = { SettingsManager.setPipActionsEnabled(it) },
                                                colors = customSwitchColors()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ВОЗМОЖНОСТИ ВСТРОЕННЫХ СРЕДСТВ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AboutFeatureRow(
                            icon = Icons.Default.VideoLibrary,
                            iconBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            iconColor = MaterialTheme.colorScheme.primary,
                            title = "Продвинутый видеоплеер",
                            description = "Управление жестами яркости/громкости, умная блокировка ложных касаний и плавные анимации кинотеатра."
                        )

                        AboutFeatureRow(
                            icon = Icons.Default.MusicNote,
                            iconBg = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                            iconColor = MaterialTheme.colorScheme.secondary,
                            title = "Material You Аудиопроигрыватель",
                            description = "Погружающий интерфейс со сверхмягким размытием обложки трека на фоне и элементами управления Material 3."
                        )

                        AboutFeatureRow(
                            icon = Icons.Default.PhotoLibrary,
                            iconBg = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                            iconColor = MaterialTheme.colorScheme.tertiary,
                            title = "Иммерсивная фотогалерея",
                            description = "Чистый полноэкранный просмотр сохраненных изображений с поддержкой плавных жестов масштабирования двумя пальцами."
                        )
                    }
                }
            }
        }
    }
}
