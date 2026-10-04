package com.example.videodownloader.ui.screens.settings

import com.example.videodownloader.performAppHaptic
import com.example.videodownloader.HapticType
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.SettingsManager
import com.example.videodownloader.ui.models.ThreadOption

import androidx.compose.ui.res.stringResource
import com.example.videodownloader.R

enum class NetworkProfile { WIFI, MOBILE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedSettingsScreen(onBack: () -> Unit) {
    var selectedProfile by remember { mutableStateOf(NetworkProfile.WIFI) }
    val isWifi = selectedProfile == NetworkProfile.WIFI

    val downloadThreads = if (isWifi) SettingsManager.threadsWifi.collectAsState().value else SettingsManager.threadsMobile.collectAsState().value
    val rateLimit = if (isWifi) SettingsManager.rateLimitWifi.collectAsState().value else SettingsManager.rateLimitMobile.collectAsState().value
    val throttledRate = if (isWifi) SettingsManager.throttledRateWifi.collectAsState().value else SettingsManager.throttledRateMobile.collectAsState().value
    val maxParallel = SettingsManager.maxParallelDownloads.collectAsState().value

    var showThreadsDialog by remember { mutableStateOf(false) }
    var showLimitDialog by remember { mutableStateOf(false) }
    var showAntiThrottleDialog by remember { mutableStateOf(false) }
    var showParallelDialog by remember { mutableStateOf(false) }

    val singleRetryWifi by SettingsManager.singleRetryWifi.collectAsState()
    val singleRetryMobile by SettingsManager.singleRetryMobile.collectAsState()
    val queueRetryWifi by SettingsManager.queueRetryWifi.collectAsState()
    val queueRetryMobile by SettingsManager.queueRetryMobile.collectAsState()
    
    var showSingleRetryDialog by remember { mutableStateOf(false) }
    var showQueueRetryDialog by remember { mutableStateOf(false) }

    if (showSingleRetryDialog) {
        val retryOptions = listOf(
            Triple(0, stringResource(R.string.skip_immediately), stringResource(R.string.failure_means_cancel)),
            Triple(1, stringResource(R.string.one_retry), stringResource(R.string.one_retry_desc)),
            Triple(2, stringResource(R.string.two_retries), stringResource(R.string.two_retries_desc)),
            Triple(3, stringResource(R.string.three_retries), stringResource(R.string.max_persistence_desc))
        )
        AlertDialog(
            onDismissRequest = { showSingleRetryDialog = false },
            title = { Text(stringResource(R.string.single_downloads_title), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.single_downloads_retry_desc), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    retryOptions.forEach { (count, title, subtitle) ->
                        val isSelected = if (isWifi) singleRetryWifi == count else singleRetryMobile == count
                        Surface(shape = RoundedCornerShape(16.dp), color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp), border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { SettingsManager.setSingleRetryCount(count, isWifi); showSingleRetryDialog = false }) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(8.dp), color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) { Text(text = "${count}x", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp) }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                RadioButton(selected = isSelected, onClick = { SettingsManager.setSingleRetryCount(count, isWifi); showSingleRetryDialog = false })
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showSingleRetryDialog = false }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close_action)) } },
            dismissButton = {}
        )
    }

    if (showQueueRetryDialog) {
        val retryOptions = listOf(
            Triple(0, stringResource(R.string.skip_immediately), stringResource(R.string.queue_skip_desc)),
            Triple(1, stringResource(R.string.one_retry), stringResource(R.string.one_retry_desc)),
            Triple(3, stringResource(R.string.three_retries_recommended), stringResource(R.string.persistently_download_desc)),
            Triple(5, stringResource(R.string.five_retries), stringResource(R.string.bad_internet_desc))
        )
        AlertDialog(
            onDismissRequest = { showQueueRetryDialog = false },
            title = { Text(stringResource(R.string.queue_and_autopilot_title), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.queue_retry_desc), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    retryOptions.forEach { (count, title, subtitle) ->
                        val isSelected = if (isWifi) queueRetryWifi == count else queueRetryMobile == count
                        Surface(shape = RoundedCornerShape(16.dp), color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp), border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)), modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { SettingsManager.setQueueRetryCount(count, isWifi); showQueueRetryDialog = false }) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(8.dp), color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) { Text(text = "${count}x", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 11.sp) }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                RadioButton(selected = isSelected, onClick = { SettingsManager.setQueueRetryCount(count, isWifi); showQueueRetryDialog = false })
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showQueueRetryDialog = false }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close_action)) } },
            dismissButton = {}
        )
    }

    if (showThreadsDialog) {
        val threadOptions = listOf(
            ThreadOption(1, stringResource(R.string.thread_1_title), stringResource(R.string.thread_1_desc), "1x"),
            ThreadOption(2, stringResource(R.string.thread_2_title), stringResource(R.string.thread_2_desc), "2x"),
            ThreadOption(4, stringResource(R.string.thread_4_title), stringResource(R.string.thread_4_desc), "4x"),
            ThreadOption(8, stringResource(R.string.thread_8_title), stringResource(R.string.thread_8_desc), "8x"),
            ThreadOption(16, stringResource(R.string.thread_16_title), stringResource(R.string.thread_16_desc), "16x")
        )

        AlertDialog(
            onDismissRequest = { showThreadsDialog = false },
            title = { Text(stringResource(R.string.turbo_threads_title), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (opt in threadOptions) {
                        val isSelected = downloadThreads == opt.threads
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    SettingsManager.setDownloadThreads(opt.threads, isWifi)
                                    showThreadsDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = opt.badge,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(opt.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(opt.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        SettingsManager.setDownloadThreads(opt.threads, isWifi)
                                        showThreadsDialog = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showThreadsDialog = false }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close_action)) }
            },
            dismissButton = {}
        )
    }

    if (showLimitDialog) {
        val limitOptions = listOf(
            Triple(0L, stringResource(R.string.skip_immediately), stringResource(R.string.unlimited_speed_desc)),
            Triple(10485760L, stringResource(R.string.limit_10mb_title), stringResource(R.string.limit_10mb_desc)),
            Triple(5242880L, stringResource(R.string.limit_5mb_title), stringResource(R.string.limit_5mb_desc)),
            Triple(2097152L, stringResource(R.string.limit_2mb_title), stringResource(R.string.limit_2mb_desc)),
            Triple(1048576L, stringResource(R.string.limit_1mb_title), stringResource(R.string.limit_1mb_desc))
        )

        AlertDialog(
            onDismissRequest = { showLimitDialog = false },
            title = { Text(stringResource(R.string.rate_limit_title), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    limitOptions.forEach { (limitBytes, title, desc) ->
                        val isSelected = rateLimit == limitBytes
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    SettingsManager.setRateLimit(limitBytes, isWifi)
                                    showLimitDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                RadioButton(selected = isSelected, onClick = {
                                    SettingsManager.setRateLimit(limitBytes, isWifi)
                                    showLimitDialog = false
                                })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showLimitDialog = false }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close_action)) }
            },
            dismissButton = {}
        )
    }

    if (showAntiThrottleDialog) {
        val antiThrottleOptions = listOf(
            Triple(102400L, stringResource(R.string.anti_throttle_100kb_title), stringResource(R.string.anti_throttle_100kb_desc)),
            Triple(51200L, stringResource(R.string.anti_throttle_50kb_title), stringResource(R.string.anti_throttle_50kb_desc)),
            Triple(204800L, stringResource(R.string.anti_throttle_200kb_title), stringResource(R.string.anti_throttle_200kb_desc)),
            Triple(0L, stringResource(R.string.off_status), stringResource(R.string.anti_throttle_off_desc))
        )

        AlertDialog(
            onDismissRequest = { showAntiThrottleDialog = false },
            title = { Text(stringResource(R.string.anti_throttling_title), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    antiThrottleOptions.forEach { (rateBytes, title, desc) ->
                        val isSelected = throttledRate == rateBytes
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    SettingsManager.setThrottledRate(rateBytes, isWifi)
                                    showAntiThrottleDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                RadioButton(selected = isSelected, onClick = {
                                    SettingsManager.setThrottledRate(rateBytes, isWifi)
                                    showAntiThrottleDialog = false
                                })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAntiThrottleDialog = false }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close_action)) }
            },
            dismissButton = {}
        )
    }

    if (showParallelDialog) {
        val parallelOptions = listOf(
            Triple(1, stringResource(R.string.parallel_1x_title), stringResource(R.string.parallel_1x_desc)),
            Triple(2, stringResource(R.string.parallel_2x_title), stringResource(R.string.parallel_2x_desc)),
            Triple(3, stringResource(R.string.parallel_3x_title), stringResource(R.string.parallel_3x_desc))
        )
        AlertDialog(
            onDismissRequest = { showParallelDialog = false },
            title = { Text(stringResource(R.string.parallel_downloads_title), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    parallelOptions.forEach { (count, title, desc) ->
                        val isSelected = maxParallel == count
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { SettingsManager.setMaxParallelDownloads(count); showParallelDialog = false }
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(8.dp), color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant) {
                                    Text(text = "${count}x", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                RadioButton(selected = isSelected, onClick = { SettingsManager.setMaxParallelDownloads(count); showParallelDialog = false })
                            }
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showParallelDialog = false }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.close_action)) } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.speed_and_network_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_content_description))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                val isMobile = selectedProfile == NetworkProfile.MOBILE
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
                            targetValue = if (isMobile) halfWidth else 0.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "SpeedPillSlide"
                        )
                        Box(
                            modifier = Modifier
                                .offset(x = pillOffset)
                                .width(halfWidth)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (isMobile) {
                                            view.performAppHaptic(HapticType.CLICK)
                                            selectedProfile = NetworkProfile.WIFI
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val contentColor by animateColorAsState(
                                    if (!isMobile) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    label = "wifiColor"
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Wifi, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Wi-Fi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = contentColor)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (!isMobile) {
                                            view.performAppHaptic(HapticType.CLICK)
                                            selectedProfile = NetworkProfile.MOBILE
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                val contentColor by animateColorAsState(
                                    if (isMobile) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    label = "mobileColor"
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CellTower, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = stringResource(R.string.mobile_network), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = contentColor)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (isWifi) stringResource(R.string.wifi_settings_header) else stringResource(R.string.mobile_settings_header),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .clickable { showThreadsDialog = true }
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.turbo_threads_title), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                Text(text = stringResource(R.string.parallel_threads_count, downloadThreads), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier
                                .clickable { showAntiThrottleDialog = true }
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.bypass_speed_reduction_title), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                Text(text = if (throttledRate > 0) stringResource(R.string.anti_throttle_active_desc, throttledRate / 1024) else stringResource(R.string.off_status), style = MaterialTheme.typography.bodySmall, color = if (throttledRate > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier
                                .clickable { showLimitDialog = true }
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.rate_limit_title), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                Text(text = if (rateLimit > 0) stringResource(R.string.speed_limit_max_desc, rateLimit / (1024 * 1024)) else stringResource(R.string.skip_immediately), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier
                                .clickable { showParallelDialog = true }
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Dashboard, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.parallel_downloads_title), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                Text(text = stringResource(R.string.parallel_tasks_active_desc, maxParallel), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.download_reliability_header), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier.clickable { showSingleRetryDialog = true }.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.single_files_title), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                val currentCount = if (isWifi) singleRetryWifi else singleRetryMobile
                                val retryText = if (currentCount == 0) stringResource(R.string.skip_immediately) else stringResource(R.string.repeat_count_desc, currentCount)
                                Text(retryText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Row(
                            modifier = Modifier.clickable { showQueueRetryDialog = true }.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.LowPriority, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.queue_files_title), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                                val currentCount = if (isWifi) queueRetryWifi else queueRetryMobile
                                val retryText = if (currentCount == 0) stringResource(R.string.skip_immediately) else stringResource(R.string.repeat_count_desc, currentCount)
                                Text(retryText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}
