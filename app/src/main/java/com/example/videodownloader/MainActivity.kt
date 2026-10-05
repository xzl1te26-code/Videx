package com.example.videodownloader

import android.Manifest
import androidx.compose.runtime.DisposableEffect
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.RECEIVER_EXPORTED
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.example.videodownloader.logic.AnalysisManager
import com.example.videodownloader.logic.DownloadManager
import com.example.videodownloader.ui.screens.AudioPlayerScreen
import com.example.videodownloader.ui.screens.GalleryScreen
import com.example.videodownloader.ui.screens.HistoryScreen
import com.example.videodownloader.ui.screens.LogViewerScreen
import com.example.videodownloader.ui.screens.MainScreen
import com.example.videodownloader.ui.screens.PlayerScreen
import com.example.videodownloader.ui.screens.settings.AboutScreen
import com.example.videodownloader.ui.screens.settings.AccountsScreen
import com.example.videodownloader.ui.screens.settings.NotifSettingsScreen
import com.example.videodownloader.ui.screens.settings.PlayerSettingsScreen
import com.example.videodownloader.ui.screens.settings.SettingsMainScreen
import com.example.videodownloader.ui.screens.settings.ShareRulesScreen
import com.example.videodownloader.ui.screens.settings.SpeedSettingsScreen
import com.example.videodownloader.ui.screens.settings.WebViewScreen
import com.example.videodownloader.ui.theme.VideoDownloaderTheme
import com.example.videodownloader.utils.detectPlatformName
import com.example.videodownloader.utils.extractUrlFromText
import com.example.videodownloader.utils.isValidUrl
import com.example.videodownloader.viewmodel.MainViewModel
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var isPlayerActive = false
    private val _isInPipMode = MutableStateFlow(false)
    var isLeavingForPip = false
        private set
    
    private val _pipCommands = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val pipCommands: SharedFlow<Int> = _pipCommands

    private var pipSourceRect: Rect? = null
    private var isVideoPlaying = true

    private val pipReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_PIP_CONTROL) {
                val type = intent.getIntExtra(EXTRA_CONTROL_TYPE, 0)
                lifecycleScope.launch { _pipCommands.emit(type) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        splashScreen.setKeepOnScreenCondition {
            !SettingsManager.isInitialized.value
        }

        enableEdgeToEdge()

        // 🌑 УБИРАЕМ БЕЛЫЙ ФОН ПРИ ЖЕСТЕ НАЗАД: задаем фону окна темный цвет
        window.setBackgroundDrawable("#000000".toColorInt().toDrawable())
        window.decorView.setBackgroundColor("#000000".toColorInt())

        lifecycleScope.launch(Dispatchers.Default) {
            delay(500.milliseconds)

            withContext(Dispatchers.Main) {
                DownloadManager.initObserver(applicationContext)
            }

            if (!FFmpegManager.isInstalled(applicationContext)) {
                FFmpegManager.installFFmpeg(applicationContext) { }
            }
        }

        val shouldCloseImmediately = handleIncomingIntent(intent)
        if (shouldCloseImmediately) {
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Регистрация приемника сигналов PiP
            registerReceiver(pipReceiver, IntentFilter(ACTION_PIP_CONTROL), RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(pipReceiver, IntentFilter(ACTION_PIP_CONTROL))
        }

        setContent {
            val useDynamicColors by SettingsManager.useDynamicColors.collectAsState()
            val appTheme by SettingsManager.appTheme.collectAsState()
            val isInPip by _isInPipMode.collectAsState()

            VideoDownloaderTheme(appTheme = appTheme, dynamicColor = useDynamicColors) {
                RequestNotificationPermission()
                AppNavigation(
                    isInPip = isInPip,
                    onPlayerActiveChange = {
                        isPlayerActive = it
                        updatePipParams()
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(pipReceiver)
        } catch (_: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val shouldClose = handleIncomingIntent(intent)
        if (shouldClose) finish()
    }

    override fun onStart() {
        super.onStart()
        PerformanceManager.startTracking(this)
    }

    override fun onResume() {
        super.onResume()
        isLeavingForPip = false
    }

    private var pendingIntentAction: (() -> Unit)? = null

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) {
            lifecycleScope.launch {
                delay(250.milliseconds)
                if (hasWindowFocus()) {
                    checkClipboardForUrl()
                }
            }
            pendingIntentAction?.invoke()
            pendingIntentAction = null
        }
    }

    override fun onStop() {
        super.onStop()
        PerformanceManager.stopTracking(this)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (isPlayerActive && SettingsManager.pipEnabled.value) {
            isLeavingForPip = true
            updatePipParams()
            enterPictureInPictureMode(pipParamsBuilder.build())
        }
    }

    private var pipParamsBuilder = PictureInPictureParams.Builder()

    fun updatePipAspectRatio(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            val ratio = width.toFloat() / height.toFloat()
            val clampedRatio = ratio.coerceIn(0.42f, 2.38f)
            val rational = if (clampedRatio > 1) {
                 Rational((clampedRatio * 100).toInt(), 100)
            } else {
                 Rational(100, (100 / clampedRatio).toInt())
            }
            
            pipParamsBuilder.setAspectRatio(rational)
            updatePipParams()
        }
    }

    fun updatePipSourceRect(rect: Rect?) {
        pipSourceRect = rect
        pipParamsBuilder.setSourceRectHint(rect)
    }

    fun updatePipPlaybackState(playing: Boolean) {
        isVideoPlaying = playing
        if (_isInPipMode.value) {
            updatePipParams()
        }
    }

    private fun updatePipParams() {
        val actions = mutableListOf<RemoteAction>()
        
        if (SettingsManager.pipActionsEnabled.value) {
            actions.add(createPipAction(
                R.drawable.ic_replay_10,
                "Назад 10с",
                CONTROL_TYPE_BACK,
                101
            ))

            val playPauseIcon = if (isVideoPlaying) R.drawable.ic_pause else R.drawable.ic_play
            actions.add(createPipAction(
                playPauseIcon,
                if (isVideoPlaying) "Пауза" else "Играть",
                CONTROL_TYPE_PLAY_PAUSE,
                102
            ))

            actions.add(createPipAction(
                R.drawable.ic_forward_10,
                "Вперед 10с",
                CONTROL_TYPE_FORWARD,
                103
            ))
        }

        pipParamsBuilder.setActions(actions)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pipParamsBuilder.setAutoEnterEnabled(isPlayerActive)
            pipParamsBuilder.setSeamlessResizeEnabled(true)
        }

        setPictureInPictureParams(pipParamsBuilder.build())
    }

    private fun createPipAction(iconRes: Int, title: String, controlType: Int, requestCode: Int): RemoteAction {
        val intent = Intent(ACTION_PIP_CONTROL).apply {
            putExtra(EXTRA_CONTROL_TYPE, controlType)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val icon = Icon.createWithResource(this, iconRes)
        return RemoteAction(icon, title, title, pendingIntent)
    }

    companion object {
        const val ACTION_PIP_CONTROL = "pip_control"
        const val EXTRA_CONTROL_TYPE = "control_type"
        const val CONTROL_TYPE_PLAY_PAUSE = 1
        const val CONTROL_TYPE_BACK = 2
        const val CONTROL_TYPE_FORWARD = 3
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        _isInPipMode.value = isInPictureInPictureMode
        if (!isInPictureInPictureMode) {
            isLeavingForPip = false
        }
    }

    private fun checkClipboardForUrl() {
        try {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                val item = clipboard.primaryClip?.getItemAt(0)
                val text = item?.text?.toString() ?: ""
                val cleanUrl = extractUrlFromText(text)

                if (cleanUrl.isNotBlank() && isValidUrl(cleanUrl)) {
                    val isAnalyzing = AnalysisManager.isAnalyzing.value
                    if (!isAnalyzing && AnalysisManager.url.value != cleanUrl) {
                        AsyncLogger.log(LogLevel.INFO, "В буфере обнаружена ссылка: $cleanUrl")
                    }
                }
            }
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.INFO, "Clipboard access suppressed: ${e.message}")
        }
    }

    private fun handleIncomingIntent(intent: Intent?): Boolean {
        if (intent?.getStringExtra("action") == "paste_link") {
            pendingIntentAction = {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val text = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                val cleanUrl = extractUrlFromText(text)
                if (cleanUrl.isNotBlank() && isValidUrl(cleanUrl)) {
                    AnalysisManager.onIncomingUrl(cleanUrl, autoAnalyze = true)
                } else {
                    Toast.makeText(this, "Буфер обмена пуст или не содержит ссылки", Toast.LENGTH_SHORT).show()
                }
            }
            return false
        }

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val rawText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val cleanUrl = extractUrlFromText(rawText)
            if (cleanUrl.isNotBlank() && isValidUrl(cleanUrl)) {
                AsyncLogger.log(LogLevel.INFO, "Перехвачена ссылка: $cleanUrl")

                val globalMode = SettingsManager.shareGlobalAction.value
                val isSilent = SettingsManager.silentShareDownload.value

                if (globalMode == ShareActionGlobal.AUTOPILOT) {
                    val rule = SettingsManager.getRuleForUrl(cleanUrl)
                    if (rule == PlatformDownloadRule.PREVIEW_ONLY) {
                        AnalysisManager.onIncomingUrl(cleanUrl, autoAnalyze = true)
                    } else {
                        val isAudio = rule == PlatformDownloadRule.AUDIO
                        val platform = detectPlatformName(cleanUrl)
                        val typeText = if (isAudio) "аудио" else "видео"

                        DownloadManager.startDownload(
                            context = this@MainActivity,
                            url = cleanUrl,
                            title = "Анализ $platform...",
                            isAudio = isAudio,
                            quality = "best",
                            isAutopilot = true
                        )

                        val queue = DownloadManager.queueCount.value
                        val toastMsg = if (queue > 1) {
                            "⚡ Добавлено в очередь ($platform)"
                        } else {
                            "⚡ Фоновая загрузка ($typeText, $platform)"
                        }
                        Toast.makeText(this@MainActivity, toastMsg, Toast.LENGTH_SHORT).show()

                        if (isSilent) {
                            return true
                        }
                    }
                } else {
                    AnalysisManager.onIncomingUrl(cleanUrl, autoAnalyze = true)
                }
            }
        }
        return false
    }
}

@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                if (isGranted) {
                    AsyncLogger.log(LogLevel.INFO, "Разрешение на уведомления получено")
                }
            }
        )
        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                launcher.launch(permission)
            }
        }
    }
}

enum class Screen { Home, History, Settings, Logs, About, ShareRules, SpeedSettings, Accounts, WebViewLogin, NotifSettings, Player, PlayerSettings, Gallery, AudioPlayer, AppearanceSettings, StorageSettings, TipFramesSettings, HapticSettings }

@OptIn(UnstableApi::class)
@Composable
fun AppNavigation(
    isInPip: Boolean,
    onPlayerActiveChange: (Boolean) -> Unit
) {
    val activity = LocalContext.current as? ComponentActivity
    val mainViewModel: MainViewModel = viewModel()
    val currentScreen = mainViewModel.currentScreen

    LaunchedEffect(currentScreen) {
        onPlayerActiveChange(currentScreen == Screen.Player)
    }

    val appContext = LocalContext.current.applicationContext
    LaunchedEffect(Unit) {
        mainViewModel.initHistoryCollection(appContext)
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                com.example.videodownloader.logic.AppUpdateChecker.checkAndResumePendingInstall(appContext)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(activity?.intent) {
        val navTo = activity?.intent?.getStringExtra("navigate_to")
        if (navTo == "history") {
            mainViewModel.currentScreen = Screen.History
            activity?.intent?.removeExtra("navigate_to")
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var loginUrl by remember { mutableStateOf("") }
    var loginTitle by remember { mutableStateOf("") }

    var selectedPlayerItem by remember { mutableStateOf<DownloadedFileItem?>(null) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = true) {
        if (currentScreen != Screen.Home) {
            mainViewModel.currentScreen = when (currentScreen) {
                Screen.Logs -> Screen.Settings
                Screen.About -> Screen.Settings
                Screen.ShareRules -> Screen.Settings
                Screen.SpeedSettings -> Screen.Settings
                Screen.Accounts -> Screen.Settings
                Screen.WebViewLogin -> Screen.Accounts
                Screen.NotifSettings -> Screen.Settings
                Screen.PlayerSettings -> Screen.Settings
                Screen.AppearanceSettings -> Screen.Settings
                Screen.StorageSettings -> Screen.Settings
                Screen.HapticSettings -> Screen.AppearanceSettings
                Screen.TipFramesSettings -> Screen.AppearanceSettings
                Screen.Gallery -> Screen.History
                Screen.AudioPlayer -> Screen.History
                else -> Screen.Home
            }
        } else {
            val isAnalyzing = AnalysisManager.isAnalyzing.value
            val isDownloading = DownloadManager.isDownloading.value

            if (isAnalyzing || isDownloading) {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000) {
                    activity?.finish()
                } else {
                    lastBackPressTime = currentTime
                    Toast.makeText(activity, "Нажмите еще раз для выхода", Toast.LENGTH_SHORT).show()
                }
            } else {
                activity?.finish()
            }
        }
    }

    // Скрытие панели навигации при скролле
    var isDockVisible by remember { mutableStateOf(true) }
    var scrollAccumulator by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentScreen) {
        isDockVisible = true
        scrollAccumulator = 0f
    }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // 🛑 МЕРТВАЯ ЗОНА (Hysteresis): Накапливаем скролл, прежде чем реагировать
                if (available.y > 0 && !isDockVisible) {
                    scrollAccumulator += available.y
                    if (scrollAccumulator > 50f) { // Порог появления
                        isDockVisible = true
                        scrollAccumulator = 0f
                    }
                } else if (available.y < 0 && isDockVisible) {
                    scrollAccumulator += available.y
                    if (scrollAccumulator < -70f) { // Порог скрытия (более строгий)
                        isDockVisible = false
                        scrollAccumulator = 0f
                    }
                } else {
                    // Сбрасываем аккумулятор при смене направления
                    if ((available.y > 0 && scrollAccumulator < 0) || (available.y < 0 && scrollAccumulator > 0)) {
                        scrollAccumulator = 0f
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // ⚓ КОНТЕКСТНЫЙ ВОЗВРАТ: Если пользователь уперся в "дно" списка
                // available.y < 0 означает попытку скролла вниз, consumed.y == 0 означает, что скроллить некуда
                if (available.y < -5f && consumed.y == 0f && !isDockVisible) {
                    isDockVisible = true
                }
                return super.onPostScroll(consumed, available, source)
            }
        }
    }

    val isDockActiveOnScreen = currentScreen in listOf(Screen.Home, Screen.History, Screen.Settings)
    val effectiveDockVisible = isDockVisible && isDockActiveOnScreen
    
    val dockTranslationY by animateDpAsState(
        targetValue = if (effectiveDockVisible) 0.dp else 120.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "DockHideAnim"
    )

    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val extraDockMargin = if (navBarBottomPadding <= 24.dp) 2.dp else 8.dp
    val dynamicSnackbarBottomPadding by animateDpAsState(
        targetValue = if (effectiveDockVisible) navBarBottomPadding + 52.dp + extraDockMargin else navBarBottomPadding + 4.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "DynamicSnackbarPadding"
    )

    val isMediaScreen = currentScreen in listOf(Screen.Player, Screen.Gallery, Screen.AudioPlayer)
    val listScale by animateFloatAsState(
        targetValue = if (isMediaScreen) 0.94f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "ListScaleAnim"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
        ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .padding(bottom = dynamicSnackbarBottomPadding)
                        .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)),
                    snackbar = { snackbarData ->
                        AnimatedContent(
                            targetState = snackbarData,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.92f) + slideInVertically { it / 3 })
                                    .togetherWith(fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.92f) + slideOutVertically { -it / 3 })
                            },
                            label = "SnackbarInstantSwapAnim"
                        ) { targetData ->
                            com.example.videodownloader.ui.components.CustomAppSnackbar(targetData)
                        }
                    }
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = listScale
                    scaleY = listScale
                }
        ) { paddingValues ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .consumeWindowInsets(paddingValues)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    label = "ScreenTransition",
                    transitionSpec = {
                        if (targetState !in listOf(Screen.Home, Screen.History, Screen.Settings) || initialState !in listOf(Screen.Home, Screen.History, Screen.Settings)) {
                            slideInVertically(initialOffsetY = { it }) + fadeIn() togetherWith slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        } else {
                            val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            slideInHorizontally(initialOffsetX = { it * direction }) + fadeIn() togetherWith
                                    slideOutHorizontally(targetOffsetX = { -it * direction }) + fadeOut()
                        }
                    }
                ) { screen ->
                    when (screen) {
                        Screen.Home -> MainScreen(snackbarHostState, mainViewModel)
                        Screen.History -> HistoryScreen(
                            snackbarHostState = snackbarHostState,
                            mainViewModel = mainViewModel,
                            onOpenPlayer = { item ->
                                selectedPlayerItem = item
                                mainViewModel.currentScreen = when (item.mediaType) {
                                    MediaType.VIDEO -> Screen.Player
                                    MediaType.PHOTO -> Screen.Gallery
                                    MediaType.AUDIO -> Screen.AudioPlayer
                                }
                            }
                        )
                        Screen.Settings -> SettingsMainScreen(
                            snackbarHostState = snackbarHostState,
                            onOpenLogs = { mainViewModel.currentScreen = Screen.Logs },
                            onOpenAbout = { mainViewModel.currentScreen = Screen.About },
                            onOpenShareRules = { mainViewModel.currentScreen = Screen.ShareRules },
                            onOpenSpeedSettings = { mainViewModel.currentScreen = Screen.SpeedSettings },
                            onOpenAccounts = { mainViewModel.currentScreen = Screen.Accounts },
                            onOpenNotifSettings = { mainViewModel.currentScreen = Screen.NotifSettings },
                            onOpenPlayerSettings = { mainViewModel.currentScreen = Screen.PlayerSettings },
                            onOpenAppearanceSettings = { mainViewModel.currentScreen = Screen.AppearanceSettings },
                            onOpenStorageSettings = { mainViewModel.currentScreen = Screen.StorageSettings }
                        )
                        Screen.Logs -> LogViewerScreen(
                            snackbarHostState = snackbarHostState,
                            onBack = { mainViewModel.currentScreen = Screen.Settings }
                        )
                        Screen.About -> AboutScreen(
                            onBack = { mainViewModel.currentScreen = Screen.Settings }
                        )
                        Screen.ShareRules -> ShareRulesScreen(
                            onBack = { mainViewModel.currentScreen = Screen.Settings }
                        )
                        Screen.SpeedSettings -> SpeedSettingsScreen(
                            onBack = { mainViewModel.currentScreen = Screen.Settings }
                        )
                        Screen.Accounts -> AccountsScreen(
                            snackbarHostState = snackbarHostState,
                            onBack = { mainViewModel.currentScreen = Screen.Settings },
                            onOpenLogin = { url, title ->
                                loginUrl = url
                                loginTitle = title
                                mainViewModel.currentScreen = Screen.WebViewLogin
                            }
                        )
                        Screen.WebViewLogin -> WebViewScreen(
                            url = loginUrl,
                            title = loginTitle,
                            onBack = { mainViewModel.currentScreen = Screen.Accounts }
                        )
                        Screen.NotifSettings -> NotifSettingsScreen(
                            onBack = { mainViewModel.currentScreen = Screen.Settings }
                        )
                        Screen.PlayerSettings -> PlayerSettingsScreen(
                            onBack = { mainViewModel.currentScreen = Screen.Settings }
                        )
                        Screen.AppearanceSettings -> com.example.videodownloader.ui.screens.settings.AppearanceSettingsScreen(
                            onBack = { mainViewModel.currentScreen = Screen.Settings },
                            onOpenTipFramesSettings = { mainViewModel.currentScreen = Screen.TipFramesSettings },
                            onOpenHapticSettings = { mainViewModel.currentScreen = Screen.HapticSettings }
                        )
                        Screen.TipFramesSettings -> com.example.videodownloader.ui.screens.settings.TipFramesSettingsScreen(
                            onBack = { mainViewModel.currentScreen = Screen.AppearanceSettings }
                        )
                        Screen.HapticSettings -> com.example.videodownloader.ui.screens.settings.HapticSettingsScreen(
                            onBack = { mainViewModel.currentScreen = Screen.AppearanceSettings }
                        )
                        Screen.StorageSettings -> com.example.videodownloader.ui.screens.settings.StorageSettingsScreen(
                            snackbarHostState = snackbarHostState,
                            onBack = { mainViewModel.currentScreen = Screen.Settings }
                        )
                        Screen.Gallery -> { /* Галерея вынесена выше вне Scaffold */ }
                        Screen.AudioPlayer -> { /* Аудиоплеер вынесен выше вне Scaffold */ }
                        else -> { /* Плеер вынесен выше */ }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = currentScreen == Screen.Player,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
            label = "PlayerFullscreenTransition"
        ) {
            selectedPlayerItem?.let { item ->
                PlayerScreen(
                    item = item,
                    onBack = { mainViewModel.currentScreen = Screen.History },
                    isInPip = isInPip
                )
            }
        }

        AnimatedVisibility(
            visible = currentScreen == Screen.Gallery,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
            label = "GalleryFullscreenTransition"
        ) {
            selectedPlayerItem?.let { item ->
                GalleryScreen(
                    item = item,
                    onBack = { mainViewModel.currentScreen = Screen.History }
                )
            }
        }

        AnimatedVisibility(
            visible = currentScreen == Screen.AudioPlayer,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(),
            label = "AudioPlayerFullscreenTransition"
        ) {
            selectedPlayerItem?.let { item ->
                AudioPlayerScreen(
                    item = item,
                    onBack = { mainViewModel.currentScreen = Screen.History }
                )
            }
        }

        if (!isInPip && currentScreen in listOf(Screen.Home, Screen.History, Screen.Settings)) {
            val localView = LocalView.current
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset { IntOffset(0, with(density) { dockTranslationY.toPx() }.roundToInt()) }
                    .padding(bottom = navBarBottomPadding + extraDockMargin)
                    .height(62.dp)
                    .wrapContentWidth(),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
                shape = RoundedCornerShape(31.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxHeight().padding(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val navItems = listOf(
                        Triple(Screen.Home, Icons.Default.Home, "Главная"),
                        Triple(Screen.History, Icons.Default.VideoLibrary, "История"),
                        Triple(Screen.Settings, Icons.Default.Settings, "Настройки")
                    )

                    navItems.forEach { (screen, icon, title) ->
                        val isSelected = currentScreen == screen
                        val animatedScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.15f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "TabScale"
                        )

                        val bgAlpha by animateFloatAsState(
                            targetValue = if (isSelected) 0.15f else 0.0f,
                            label = "BackgroundAlpha"
                        )

                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (currentScreen != screen) {
                                        localView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        mainViewModel.currentScreen = screen
                                    }
                                }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .graphicsLayer(scaleX = animatedScale, scaleY = animatedScale)
                                )

                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn(tween(150)) + expandHorizontally(expandFrom = Alignment.Start, animationSpec = tween(200)),
                                    exit = fadeOut(tween(100)) + shrinkHorizontally(shrinkTowards = Alignment.Start, animationSpec = tween(150))
                                ) {
                                    Text(
                                        text = title,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(start = 8.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
