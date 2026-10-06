package com.example.videodownloader

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ShareActionGlobal { PREVIEW, AUTOPILOT }
enum class PlatformDownloadRule { VIDEO, AUDIO, PREVIEW_ONLY }
enum class FileNameTemplate { TITLE_PLATFORM, DATE_TITLE, TITLE_ONLY, PLATFORM_TITLE, DATE_TIME_TITLE }
enum class MinDiskSpace(val bytes: Long) { NONE(0L), MB500(500L * 1024 * 1024), GB1(1024L * 1024 * 1024) }
enum class AppTheme { SYSTEM, DARK, LIGHT }
enum class HistoryTitleMode { TWO_LINES, MARQUEE, SINGLE_LINE }
enum class HistorySyncMode { BOTH, SWIPE_ONLY, BUTTON_ONLY }
enum class TileAction { SMART_DOWNLOAD, OPEN_ANALYSIS }
enum class TipPlaybackMode { EVERY_NAVIGATION, APP_LAUNCH_ONLY }
enum class HapticIntensity { SOFT, STANDARD, STRONG }
enum class HapticType { CLICK, LONG_PRESS, PLAYER_GESTURE, SUCCESS, SELECTION }

data class TipFrameData(
    val iconKey: String,
    val text: String
)

object SettingsManager {
    private const val PREFS_NAME = "app_settings_prefs"
    private const val KEY_CUSTOM_DIR_URI = "custom_dir_uri"
    private const val KEY_CUSTOM_DIR_NAME = "custom_dir_name"
    private const val KEY_SAVE_TO_GALLERY = "save_to_gallery"
    private const val KEY_SHOW_THUMBNAILS = "show_thumbnails"
    private const val KEY_SHOW_HISTORY_THUMBNAILS = "show_history_thumbnails"
    private const val KEY_SHOW_STORAGE_STATS = "show_storage_stats"
    private const val KEY_SHOW_QUALITY_SELECTOR = "show_quality_selector"
    private const val KEY_PREFERRED_VIDEO_QUALITY = "preferred_video_quality"
    private const val KEY_CHECK_DUPLICATES = "check_duplicates"
    private const val KEY_FILE_NAME_TEMPLATE = "file_name_template"
    private const val KEY_MIN_DISK_SPACE = "min_disk_space"
    private const val KEY_USE_SPONSORBLOCK = "use_sponsorblock"
    private const val KEY_USE_IMPERSONATE = "use_impersonate"
    private const val KEY_USE_CLIPBOARD_BUBBLE = "use_clipboard_bubble"
    private const val KEY_USE_CLIPBOARD_PREVIEW = "use_clipboard_preview"
    private const val KEY_USE_CLIPBOARD_HISTORY = "use_clipboard_history"
    private const val KEY_RECENT_CLIPBOARD_URLS = "recent_clipboard_urls_v1"
    private const val KEY_SHOW_HISTORY_SORT = "show_history_sort"
    private const val KEY_HISTORY_SYNC_MODE = "history_sync_mode_v1"
    private const val KEY_TILE_ACTION = "tile_action_v1"
    private const val KEY_HISTORY_TITLE_MODE = "history_title_mode_v1"
    private const val KEY_USE_INTERNAL_PLAYER = "use_internal_player"
    private const val KEY_PIP_ENABLED = "pip_enabled"
    private const val KEY_PIP_ACTIONS_ENABLED = "pip_actions_enabled"
    private const val KEY_AVG_SPEED_WIFI = "avg_speed_wifi"
    private const val KEY_AVG_SPEED_MOBILE = "avg_speed_mobile"
    private const val KEY_APP_THEME = "app_theme"
    private const val KEY_SHOW_DYNAMIC_TIPS = "show_dynamic_tips"
    private const val KEY_TIP_FRAMES_DATA = "tip_frames_data_v2"
    private const val KEY_TIP_ANIMATION_SPEED_MS = "tip_animation_speed_ms_v1"
    private const val KEY_TIP_PLAYBACK_MODE = "tip_playback_mode_v1"

    // Уведомления
    private const val KEY_NOTIF_ACTION_OPEN = "notif_action_open"
    private const val KEY_NOTIF_ACTION_SHARE = "notif_action_share"
    private const val KEY_NOTIF_ACTION_DELETE = "notif_action_delete"
    private const val KEY_NOTIF_ACTION_CANCEL = "notif_action_cancel"
    private const val KEY_NOTIF_SOUND = "notif_sound"
    private const val KEY_NOTIF_VIBRATE = "notif_vibrate"
    private const val KEY_NOTIF_AUTO_DISMISS = "notif_auto_dismiss_delay"
    private const val KEY_NOTIF_DETAILED_PROGRESS = "notif_detailed_progress"
    private const val KEY_NOTIF_SOUND_QUEUE_END = "notif_sound_queue_end"
    private const val KEY_NOTIF_QUEUE_SUMMARY = "notif_queue_summary"
    private const val KEY_NOTIF_GROUP_COMPLETED = "notif_group_completed"
    private const val KEY_TRANSIT_MODE_ENABLED = "transit_mode_enabled"
    private const val KEY_TRANSIT_MODE_AUTOPILOT_ONLY = "transit_mode_autopilot_only"
    private const val KEY_TRANSIT_MODE_MIN_SIZE_MB = "transit_mode_min_size_mb"
    private const val KEY_TRANSIT_MODE_PLATFORMS = "transit_mode_platforms"
    private const val KEY_AUTO_CLEANUP_LOGS = "auto_cleanup_logs"

    // Повторы (Wi-Fi и Mobile)
    private const val KEY_SINGLE_RETRY_WIFI = "single_retry_wifi"
    private const val KEY_QUEUE_RETRY_WIFI = "queue_retry_wifi"
    private const val KEY_SINGLE_RETRY_MOBILE = "single_retry_mobile"
    private const val KEY_QUEUE_RETRY_MOBILE = "queue_retry_mobile"

    // Сеть
    private const val KEY_THREADS_WIFI = "threads_wifi"
    private const val KEY_RATE_LIMIT_WIFI = "rate_limit_wifi"
    private const val KEY_THROTTLED_RATE_WIFI = "throttled_rate_wifi"
    private const val KEY_THREADS_MOBILE = "threads_mobile"
    private const val KEY_RATE_LIMIT_MOBILE = "rate_limit_mobile"
    private const val KEY_THROTTLED_RATE_MOBILE = "throttled_rate_mobile"
    private const val KEY_MAX_PARALLEL_DOWNLOADS = "max_parallel_downloads"
    private const val KEY_DYNAMIC_COLORS = "dynamic_colors"
    private const val KEY_SHARE_GLOBAL_ACTION = "share_global_action"
    private const val KEY_SILENT_SHARE_DOWNLOAD = "silent_share_download"
    private const val KEY_RULE_YOUTUBE = "rule_youtube"
    private const val KEY_RULE_TIKTOK = "rule_tiktok"
    private const val KEY_RULE_VK = "rule_vk"
    private const val KEY_RULE_INSTAGRAM = "rule_instagram"
    private const val KEY_RULE_PINTEREST = "rule_pinterest"
    private const val KEY_RULE_OTHER = "rule_other"

    private const val KEY_HAPTIC_ENABLED = "haptic_enabled"
    private const val KEY_HAPTIC_INTENSITY = "haptic_intensity"
    private const val KEY_HAPTIC_BUTTONS = "haptic_buttons"
    private const val KEY_HAPTIC_PLAYER = "haptic_player"
    private const val KEY_HAPTIC_LONG_PRESS = "haptic_long_press"

    private const val DEFAULT_FOLDER_NAME = "Downloads/Videx"

    private var prefs: SharedPreferences? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    private val _customDirName = MutableStateFlow(DEFAULT_FOLDER_NAME)
    val customDirName: StateFlow<String> = _customDirName

    private val _saveToGallery = MutableStateFlow(true)
    val saveToGallery: StateFlow<Boolean> = _saveToGallery

    private val _showThumbnails = MutableStateFlow(true)
    val showThumbnails: StateFlow<Boolean> = _showThumbnails

    private val _showHistoryThumbnails = MutableStateFlow(true)
    val showHistoryThumbnails: StateFlow<Boolean> = _showHistoryThumbnails

    private val _showStorageStats = MutableStateFlow(true)
    val showStorageStats: StateFlow<Boolean> = _showStorageStats

    private val _showQualitySelector = MutableStateFlow(true)
    val showQualitySelector: StateFlow<Boolean> = _showQualitySelector

    private val _preferredVideoQuality = MutableStateFlow("best")
    val preferredVideoQuality: StateFlow<String> = _preferredVideoQuality

    private val _checkDuplicates = MutableStateFlow(true)
    val checkDuplicates: StateFlow<Boolean> = _checkDuplicates

    private val _notifActionOpen = MutableStateFlow(true)
    val notifActionOpen: StateFlow<Boolean> = _notifActionOpen

    private val _notifActionShare = MutableStateFlow(true)
    val notifActionShare: StateFlow<Boolean> = _notifActionShare

    private val _notifActionDelete = MutableStateFlow(true)
    val notifActionDelete: StateFlow<Boolean> = _notifActionDelete

    private val _notifActionCancel = MutableStateFlow(true)
    val notifActionCancel: StateFlow<Boolean> = _notifActionCancel

    private val _notifSound = MutableStateFlow(true)
    val notifSound: StateFlow<Boolean> = _notifSound

    private val _notifVibrate = MutableStateFlow(true)
    val notifVibrate: StateFlow<Boolean> = _notifVibrate

    private val _notifSoundQueueEnd = MutableStateFlow(true)
    val notifSoundQueueEnd: StateFlow<Boolean> = _notifSoundQueueEnd

    private val _notifQueueSummary = MutableStateFlow(true)
    val notifQueueSummary: StateFlow<Boolean> = _notifQueueSummary

    private val _notifGroupCompleted = MutableStateFlow(true)
    val notifGroupCompleted: StateFlow<Boolean> = _notifGroupCompleted

    private val _showHistorySort = MutableStateFlow(true)
    val showHistorySort: StateFlow<Boolean> = _showHistorySort

    private val _historySyncMode = MutableStateFlow(HistorySyncMode.BOTH)
    val historySyncMode: StateFlow<HistorySyncMode> = _historySyncMode

    private val _tileAction = MutableStateFlow(TileAction.SMART_DOWNLOAD)
    val tileAction: StateFlow<TileAction> = _tileAction

    private val _historyTitleMode = MutableStateFlow(HistoryTitleMode.TWO_LINES)
    val historyTitleMode: StateFlow<HistoryTitleMode> = _historyTitleMode

    private val _transitModeEnabled = MutableStateFlow(false)
    val transitModeEnabled: StateFlow<Boolean> = _transitModeEnabled

    private val _transitModeAutopilotOnly = MutableStateFlow(true)
    val transitModeAutopilotOnly: StateFlow<Boolean> = _transitModeAutopilotOnly

    private val _transitModeMinSizeMb = MutableStateFlow(0)
    val transitModeMinSizeMb: StateFlow<Int> = _transitModeMinSizeMb

    private val _transitModePlatforms = MutableStateFlow(setOf("TikTok", "Instagram", "YouTube"))
    val transitModePlatforms: StateFlow<Set<String>> = _transitModePlatforms

    private val _notifAutoDismiss = MutableStateFlow(0L)
    val notifAutoDismiss: StateFlow<Long> = _notifAutoDismiss

    private val _notifDetailedProgress = MutableStateFlow(true)
    val notifDetailedProgress: StateFlow<Boolean> = _notifDetailedProgress

    private val _autoCleanupLogs = MutableStateFlow(false)
    val autoCleanupLogs: StateFlow<Boolean> = _autoCleanupLogs

    private val _fileNameTemplate = MutableStateFlow(FileNameTemplate.TITLE_PLATFORM)
    val fileNameTemplate: StateFlow<FileNameTemplate> = _fileNameTemplate

    private val _minDiskSpace = MutableStateFlow(MinDiskSpace.NONE)
    val minDiskSpace: StateFlow<MinDiskSpace> = _minDiskSpace

    private val _useSponsorBlock = MutableStateFlow(false)
    val useSponsorBlock: StateFlow<Boolean> = _useSponsorBlock

    private val _useImpersonate = MutableStateFlow(true)
    val useImpersonate: StateFlow<Boolean> = _useImpersonate

    private val _useClipboardBubble = MutableStateFlow(true)
    val useClipboardBubble: StateFlow<Boolean> = _useClipboardBubble

    private val _useClipboardPreview = MutableStateFlow(true)
    val useClipboardPreview: StateFlow<Boolean> = _useClipboardPreview

    private val _useClipboardHistory = MutableStateFlow(true)
    val useClipboardHistory: StateFlow<Boolean> = _useClipboardHistory

    private val _recentClipboardUrls = MutableStateFlow<List<String>>(emptyList())
    val recentClipboardUrls: StateFlow<List<String>> = _recentClipboardUrls

    private val _showDynamicTips = MutableStateFlow(true)
    val showDynamicTips: StateFlow<Boolean> = _showDynamicTips

    val defaultTipFrames = listOf(
        TipFrameData("link", "Копируйте ссылку — Videx уже наготове"),
        TipFrameData("quality", "Без сжатия, без рекламы, в один клик"),
        TipFrameData("bolt", "Вставьте ссылку — остальное сделает Videx")
    )

    private val _tipFrames = MutableStateFlow(defaultTipFrames)
    val tipFrames: StateFlow<List<TipFrameData>> = _tipFrames

    private val _tipAnimationSpeedMs = MutableStateFlow(2400L)
    val tipAnimationSpeedMs: StateFlow<Long> = _tipAnimationSpeedMs

    private val _tipPlaybackMode = MutableStateFlow(TipPlaybackMode.EVERY_NAVIGATION)
    val tipPlaybackMode: StateFlow<TipPlaybackMode> = _tipPlaybackMode

    private val _tipConfigVersion = MutableStateFlow(0)
    val tipConfigVersion: StateFlow<Int> = _tipConfigVersion

    private val _useInternalPlayer = MutableStateFlow(true)
    val useInternalPlayer: StateFlow<Boolean> = _useInternalPlayer

    private val _pipEnabled = MutableStateFlow(true)
    val pipEnabled: StateFlow<Boolean> = _pipEnabled

    private val _pipActionsEnabled = MutableStateFlow(true)
    val pipActionsEnabled: StateFlow<Boolean> = _pipActionsEnabled

    private val _avgSpeedWifi = MutableStateFlow(10 * 1024 * 1024L) // Дефолт Wi-Fi: 10 МБ/с
    val avgSpeedWifi: StateFlow<Long> = _avgSpeedWifi

    private val _avgSpeedMobile = MutableStateFlow(2 * 1024 * 1024L) // Дефолт Mobile: 2 МБ/с
    val avgSpeedMobile: StateFlow<Long> = _avgSpeedMobile

    // Повторы загрузки
    private val _singleRetryWifi = MutableStateFlow(1)
    val singleRetryWifi: StateFlow<Int> = _singleRetryWifi

    private val _queueRetryWifi = MutableStateFlow(3)
    val queueRetryWifi: StateFlow<Int> = _queueRetryWifi

    private val _singleRetryMobile = MutableStateFlow(1)
    val singleRetryMobile: StateFlow<Int> = _singleRetryMobile

    private val _queueRetryMobile = MutableStateFlow(2)
    val queueRetryMobile: StateFlow<Int> = _queueRetryMobile

    // Сеть
    private val _threadsWifi = MutableStateFlow(4)
    val threadsWifi: StateFlow<Int> = _threadsWifi

    private val _rateLimitWifi = MutableStateFlow(0L)
    val rateLimitWifi: StateFlow<Long> = _rateLimitWifi

    private val _throttledRateWifi = MutableStateFlow(102400L)
    val throttledRateWifi: StateFlow<Long> = _throttledRateWifi

    private val _threadsMobile = MutableStateFlow(2)
    val threadsMobile: StateFlow<Int> = _threadsMobile

    private val _rateLimitMobile = MutableStateFlow(0L)
    val rateLimitMobile: StateFlow<Long> = _rateLimitMobile

    private val _throttledRateMobile = MutableStateFlow(102400L)
    val throttledRateMobile: StateFlow<Long> = _throttledRateMobile

    private val _maxParallelDownloads = MutableStateFlow(1)
    val maxParallelDownloads: StateFlow<Int> = _maxParallelDownloads

    private val _useDynamicColors = MutableStateFlow(true)
    val useDynamicColors: StateFlow<Boolean> = _useDynamicColors

    private val _hapticEnabled = MutableStateFlow(true)
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled

    private val _hapticIntensity = MutableStateFlow(HapticIntensity.STANDARD)
    val hapticIntensity: StateFlow<HapticIntensity> = _hapticIntensity

    private val _hapticButtons = MutableStateFlow(true)
    val hapticButtons: StateFlow<Boolean> = _hapticButtons

    private val _hapticPlayer = MutableStateFlow(true)
    val hapticPlayer: StateFlow<Boolean> = _hapticPlayer

    private val _hapticLongPress = MutableStateFlow(true)
    val hapticLongPress: StateFlow<Boolean> = _hapticLongPress

    private val _appTheme = MutableStateFlow(AppTheme.SYSTEM)
    val appTheme: StateFlow<AppTheme> = _appTheme

    private val _shareGlobalAction = MutableStateFlow(ShareActionGlobal.PREVIEW)
    val shareGlobalAction: StateFlow<ShareActionGlobal> = _shareGlobalAction

    private val _silentShareDownload = MutableStateFlow(true)
    val silentShareDownload: StateFlow<Boolean> = _silentShareDownload

    private val _ruleYoutube = MutableStateFlow(PlatformDownloadRule.VIDEO)
    val ruleYoutube: StateFlow<PlatformDownloadRule> = _ruleYoutube

    private val _ruleTiktok = MutableStateFlow(PlatformDownloadRule.VIDEO)
    val ruleTiktok: StateFlow<PlatformDownloadRule> = _ruleTiktok

    private val _ruleVk = MutableStateFlow(PlatformDownloadRule.VIDEO)
    val ruleVk: StateFlow<PlatformDownloadRule> = _ruleVk

    private val _ruleInstagram = MutableStateFlow(PlatformDownloadRule.VIDEO)
    val ruleInstagram: StateFlow<PlatformDownloadRule> = _ruleInstagram

    private val _rulePinterest = MutableStateFlow(PlatformDownloadRule.VIDEO)
    val rulePinterest: StateFlow<PlatformDownloadRule> = _rulePinterest

    private val _ruleOther = MutableStateFlow(PlatformDownloadRule.VIDEO)
    val ruleOther: StateFlow<PlatformDownloadRule> = _ruleOther

    fun init(context: Context) {
        val appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _customDirName.value = prefs?.getString(KEY_CUSTOM_DIR_NAME, DEFAULT_FOLDER_NAME) ?: DEFAULT_FOLDER_NAME
        _saveToGallery.value = prefs?.getBoolean(KEY_SAVE_TO_GALLERY, true) ?: true
        _showThumbnails.value = prefs?.getBoolean(KEY_SHOW_THUMBNAILS, true) ?: true
        _showHistoryThumbnails.value = prefs?.getBoolean(KEY_SHOW_HISTORY_THUMBNAILS, true) ?: true
        _showHistorySort.value = prefs?.getBoolean(KEY_SHOW_HISTORY_SORT, true) ?: true
        val syncModeStr = prefs?.getString(KEY_HISTORY_SYNC_MODE, HistorySyncMode.BOTH.name)
        _historySyncMode.value = try { HistorySyncMode.valueOf(syncModeStr!!) } catch (_: Exception) { HistorySyncMode.BOTH }
        val tileActStr = prefs?.getString(KEY_TILE_ACTION, TileAction.SMART_DOWNLOAD.name)
        _tileAction.value = try { TileAction.valueOf(tileActStr!!) } catch (_: Exception) { TileAction.SMART_DOWNLOAD }
        val titleModeStr = prefs?.getString(KEY_HISTORY_TITLE_MODE, HistoryTitleMode.TWO_LINES.name)
        _historyTitleMode.value = try { HistoryTitleMode.valueOf(titleModeStr!!) } catch (_: Exception) { HistoryTitleMode.TWO_LINES }
        _showStorageStats.value = prefs?.getBoolean(KEY_SHOW_STORAGE_STATS, true) ?: true
        _showQualitySelector.value = prefs?.getBoolean(KEY_SHOW_QUALITY_SELECTOR, true) ?: true
        _preferredVideoQuality.value = prefs?.getString(KEY_PREFERRED_VIDEO_QUALITY, "best") ?: "best"
        _checkDuplicates.value = prefs?.getBoolean(KEY_CHECK_DUPLICATES, true) ?: true

        _notifActionOpen.value = prefs?.getBoolean(KEY_NOTIF_ACTION_OPEN, true) ?: true
        _notifActionShare.value = prefs?.getBoolean(KEY_NOTIF_ACTION_SHARE, true) ?: true
        _notifActionDelete.value = prefs?.getBoolean(KEY_NOTIF_ACTION_DELETE, true) ?: true
        _notifActionCancel.value = prefs?.getBoolean(KEY_NOTIF_ACTION_CANCEL, true) ?: true
        _notifSound.value = prefs?.getBoolean(KEY_NOTIF_SOUND, true) ?: true
        _notifVibrate.value = prefs?.getBoolean(KEY_NOTIF_VIBRATE, true) ?: true
        _notifSoundQueueEnd.value = prefs?.getBoolean(KEY_NOTIF_SOUND_QUEUE_END, true) ?: true
        _notifQueueSummary.value = prefs?.getBoolean(KEY_NOTIF_QUEUE_SUMMARY, true) ?: true
        _notifGroupCompleted.value = prefs?.getBoolean(KEY_NOTIF_GROUP_COMPLETED, true) ?: true
        _transitModeEnabled.value = prefs?.getBoolean(KEY_TRANSIT_MODE_ENABLED, false) ?: false
        _transitModeAutopilotOnly.value = prefs?.getBoolean(KEY_TRANSIT_MODE_AUTOPILOT_ONLY, true) ?: true
        _transitModeMinSizeMb.value = prefs?.getInt(KEY_TRANSIT_MODE_MIN_SIZE_MB, 0) ?: 0
        _transitModePlatforms.value = prefs?.getStringSet(KEY_TRANSIT_MODE_PLATFORMS, setOf("TikTok", "Instagram", "YouTube")) ?: setOf("TikTok", "Instagram", "YouTube")
        
        _notifAutoDismiss.value = prefs?.getLong(KEY_NOTIF_AUTO_DISMISS, 0L) ?: 0L
        _notifDetailedProgress.value = prefs?.getBoolean(KEY_NOTIF_DETAILED_PROGRESS, true) ?: true
        _autoCleanupLogs.value = prefs?.getBoolean(KEY_AUTO_CLEANUP_LOGS, false) ?: false
        _useInternalPlayer.value = prefs?.getBoolean(KEY_USE_INTERNAL_PLAYER, true) ?: true
        _pipEnabled.value = prefs?.getBoolean(KEY_PIP_ENABLED, true) ?: true
        _pipActionsEnabled.value = prefs?.getBoolean(KEY_PIP_ACTIONS_ENABLED, true) ?: true

        val templateStr = prefs?.getString(KEY_FILE_NAME_TEMPLATE, FileNameTemplate.TITLE_PLATFORM.name) ?: FileNameTemplate.TITLE_PLATFORM.name
        _fileNameTemplate.value = try { FileNameTemplate.valueOf(templateStr) } catch (e: Exception) { FileNameTemplate.TITLE_PLATFORM }

        val diskSpaceStr = prefs?.getString(KEY_MIN_DISK_SPACE, MinDiskSpace.NONE.name) ?: MinDiskSpace.NONE.name
        _minDiskSpace.value = try { MinDiskSpace.valueOf(diskSpaceStr) } catch (e: Exception) { MinDiskSpace.NONE }

        _useSponsorBlock.value = prefs?.getBoolean(KEY_USE_SPONSORBLOCK, false) ?: false
        _useImpersonate.value = prefs?.getBoolean(KEY_USE_IMPERSONATE, true) ?: true
        _useClipboardBubble.value = prefs?.getBoolean(KEY_USE_CLIPBOARD_BUBBLE, true) ?: true
        _useClipboardPreview.value = prefs?.getBoolean(KEY_USE_CLIPBOARD_PREVIEW, true) ?: true
        _useClipboardHistory.value = prefs?.getBoolean(KEY_USE_CLIPBOARD_HISTORY, true) ?: true
        val savedRecentUrls = prefs?.getString(KEY_RECENT_CLIPBOARD_URLS, null)
        if (!savedRecentUrls.isNullOrBlank()) {
            _recentClipboardUrls.value = savedRecentUrls.split(";;;").filter { it.isNotBlank() }.take(5)
        }
        _showDynamicTips.value = prefs?.getBoolean(KEY_SHOW_DYNAMIC_TIPS, true) ?: true
        _tipAnimationSpeedMs.value = prefs?.getLong(KEY_TIP_ANIMATION_SPEED_MS, 2400L) ?: 2400L
        val modeStr = prefs?.getString(KEY_TIP_PLAYBACK_MODE, TipPlaybackMode.EVERY_NAVIGATION.name)
        _tipPlaybackMode.value = try { TipPlaybackMode.valueOf(modeStr!!) } catch (_: Exception) { TipPlaybackMode.EVERY_NAVIGATION }

        val encodedFrames = prefs?.getString(KEY_TIP_FRAMES_DATA, null)
        if (!encodedFrames.isNullOrBlank()) {
            try {
                val parsed = encodedFrames.split(";;;").mapNotNull { part ->
                    val pieces = part.split(":::")
                    if (pieces.size == 2 && pieces[1].isNotBlank()) {
                        TipFrameData(pieces[0], pieces[1])
                    } else null
                }
                if (parsed.isNotEmpty()) {
                    _tipFrames.value = parsed.take(5)
                }
            } catch (_: Exception) {}
        }

        _singleRetryWifi.value = prefs?.getInt(KEY_SINGLE_RETRY_WIFI, 1) ?: 1
        _queueRetryWifi.value = prefs?.getInt(KEY_QUEUE_RETRY_WIFI, 3) ?: 3
        _singleRetryMobile.value = prefs?.getInt(KEY_SINGLE_RETRY_MOBILE, 1) ?: 1
        _queueRetryMobile.value = prefs?.getInt(KEY_QUEUE_RETRY_MOBILE, 2) ?: 2

        _threadsWifi.value = prefs?.getInt(KEY_THREADS_WIFI, 4) ?: 4
        _rateLimitWifi.value = prefs?.getLong(KEY_RATE_LIMIT_WIFI, 0L) ?: 0L
        _throttledRateWifi.value = prefs?.getLong(KEY_THROTTLED_RATE_WIFI, 102400L) ?: 102400L

        _threadsMobile.value = prefs?.getInt(KEY_THREADS_MOBILE, 2) ?: 2
        _rateLimitMobile.value = prefs?.getLong(KEY_RATE_LIMIT_MOBILE, 0L) ?: 0L
        _throttledRateMobile.value = prefs?.getLong(KEY_THROTTLED_RATE_MOBILE, 102400L) ?: 102400L

        _maxParallelDownloads.value = prefs?.getInt(KEY_MAX_PARALLEL_DOWNLOADS, 1) ?: 1

        _useDynamicColors.value = prefs?.getBoolean(KEY_DYNAMIC_COLORS, true) ?: true
        _hapticEnabled.value = prefs?.getBoolean(KEY_HAPTIC_ENABLED, true) ?: true
        val hapticStr = prefs?.getString(KEY_HAPTIC_INTENSITY, HapticIntensity.STANDARD.name) ?: HapticIntensity.STANDARD.name
        _hapticIntensity.value = try { HapticIntensity.valueOf(hapticStr) } catch (_: Exception) { HapticIntensity.STANDARD }
        _hapticButtons.value = prefs?.getBoolean(KEY_HAPTIC_BUTTONS, true) ?: true
        _hapticPlayer.value = prefs?.getBoolean(KEY_HAPTIC_PLAYER, true) ?: true
        _hapticLongPress.value = prefs?.getBoolean(KEY_HAPTIC_LONG_PRESS, true) ?: true

        val themeStr = prefs?.getString(KEY_APP_THEME, AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        _appTheme.value = try { AppTheme.valueOf(themeStr) } catch (_: Exception) { AppTheme.SYSTEM }
        val globalActionStr = prefs?.getString(KEY_SHARE_GLOBAL_ACTION, ShareActionGlobal.PREVIEW.name) ?: ShareActionGlobal.PREVIEW.name
        _shareGlobalAction.value = try { ShareActionGlobal.valueOf(globalActionStr) } catch (e: Exception) { ShareActionGlobal.PREVIEW }
        _silentShareDownload.value = prefs?.getBoolean(KEY_SILENT_SHARE_DOWNLOAD, true) ?: true

        _ruleYoutube.value = loadRule(KEY_RULE_YOUTUBE, PlatformDownloadRule.VIDEO)
        _ruleTiktok.value = loadRule(KEY_RULE_TIKTOK, PlatformDownloadRule.VIDEO)
        _ruleVk.value = loadRule(KEY_RULE_VK, PlatformDownloadRule.VIDEO)
        _ruleInstagram.value = loadRule(KEY_RULE_INSTAGRAM, PlatformDownloadRule.VIDEO)
        _rulePinterest.value = loadRule(KEY_RULE_PINTEREST, PlatformDownloadRule.VIDEO)
        _ruleOther.value = loadRule(KEY_RULE_OTHER, PlatformDownloadRule.VIDEO)
        _avgSpeedWifi.value = prefs?.getLong(KEY_AVG_SPEED_WIFI, 10 * 1024 * 1024L) ?: (10 * 1024 * 1024L)
        _avgSpeedMobile.value = prefs?.getLong(KEY_AVG_SPEED_MOBILE, 2 * 1024 * 1024L) ?: (2 * 1024 * 1024L)
        
        _isInitialized.value = true
    }

    private fun loadRule(key: String, default: PlatformDownloadRule): PlatformDownloadRule {
        val str = prefs?.getString(key, default.name) ?: default.name
        return try { PlatformDownloadRule.valueOf(str) } catch (e: Exception) { default }
    }

    private fun saveRule(key: String, rule: PlatformDownloadRule) {
        prefs?.edit()?.putString(key, rule.name)?.apply()
    }

    // Настройка количества повторов
    fun setSingleRetryCount(count: Int, isWifi: Boolean) {
        if (isWifi) {
            _singleRetryWifi.value = count
            prefs?.edit()?.putInt(KEY_SINGLE_RETRY_WIFI, count)?.apply()
        } else {
            _singleRetryMobile.value = count
            prefs?.edit()?.putInt(KEY_SINGLE_RETRY_MOBILE, count)?.apply()
        }
    }

    fun setQueueRetryCount(count: Int, isWifi: Boolean) {
        if (isWifi) {
            _queueRetryWifi.value = count
            prefs?.edit()?.putInt(KEY_QUEUE_RETRY_WIFI, count)?.apply()
        } else {
            _queueRetryMobile.value = count
            prefs?.edit()?.putInt(KEY_QUEUE_RETRY_MOBILE, count)?.apply()
        }
    }

    fun setCheckDuplicates(value: Boolean) {
        _checkDuplicates.value = value
        prefs?.edit()?.putBoolean(KEY_CHECK_DUPLICATES, value)?.apply()
    }

    fun setNotifQueueSummary(value: Boolean) {
        _notifQueueSummary.value = value
        prefs?.edit()?.putBoolean(KEY_NOTIF_QUEUE_SUMMARY, value)?.apply()
    }

    fun setNotifGroupCompleted(value: Boolean) {
        _notifGroupCompleted.value = value
        prefs?.edit()?.putBoolean(KEY_NOTIF_GROUP_COMPLETED, value)?.apply()
    }

    fun setTransitModeEnabled(value: Boolean) {
        _transitModeEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_TRANSIT_MODE_ENABLED, value)?.apply()
    }

    fun setTransitModeAutopilotOnly(value: Boolean) {
        _transitModeAutopilotOnly.value = value
        prefs?.edit()?.putBoolean(KEY_TRANSIT_MODE_AUTOPILOT_ONLY, value)?.apply()
    }

    fun setTransitModeMinSizeMb(value: Int) {
        _transitModeMinSizeMb.value = value
        prefs?.edit()?.putInt(KEY_TRANSIT_MODE_MIN_SIZE_MB, value)?.apply()
    }

    fun toggleTransitPlatform(platform: String) {
        val current = _transitModePlatforms.value.toMutableSet()
        if (current.contains(platform)) current.remove(platform) else current.add(platform)
        _transitModePlatforms.value = current
        prefs?.edit()?.putStringSet(KEY_TRANSIT_MODE_PLATFORMS, current)?.apply()
    }

    fun setNotifSoundQueueEnd(value: Boolean) {
        _notifSoundQueueEnd.value = value
        prefs?.edit()?.putBoolean(KEY_NOTIF_SOUND_QUEUE_END, value)?.apply()
    }

    fun setNotifActionCancel(value: Boolean) {
        _notifActionCancel.value = value
        prefs?.edit()?.putBoolean(KEY_NOTIF_ACTION_CANCEL, value)?.apply()
    }

    fun setNotifDetailedProgress(value: Boolean) {
        _notifDetailedProgress.value = value
        prefs?.edit()?.putBoolean(KEY_NOTIF_DETAILED_PROGRESS, value)?.apply()
    }

    fun setNotifAutoDismiss(millis: Long) {
        _notifAutoDismiss.value = millis
        prefs?.edit()?.putLong(KEY_NOTIF_AUTO_DISMISS, millis)?.apply()
    }

    fun setNotifActionOpen(value: Boolean) {
        _notifActionOpen.value = value
        prefs?.edit()?.putBoolean(KEY_NOTIF_ACTION_OPEN, value)?.apply()
    }

    fun setNotifActionShare(value: Boolean) {
        _notifActionShare.value = value
        prefs?.edit()?.putBoolean(KEY_NOTIF_ACTION_SHARE, value)?.apply()
    }

    fun setNotifActionDelete(value: Boolean) {
        _notifActionDelete.value = value
        prefs?.edit()?.putBoolean(KEY_NOTIF_ACTION_DELETE, value)?.apply()
    }

    fun setNotifSound(value: Boolean) {
        _notifSound.value = value
        prefs?.edit()?.putBoolean(KEY_NOTIF_SOUND, value)?.apply()
    }

    fun setNotifVibrate(value: Boolean) {
        _notifVibrate.value = value
        prefs?.edit()?.putBoolean(KEY_NOTIF_VIBRATE, value)?.apply()
    }

    fun setSilentShareDownload(value: Boolean) {
        _silentShareDownload.value = value
        prefs?.edit()?.putBoolean(KEY_SILENT_SHARE_DOWNLOAD, value)?.apply()
    }

    fun setDownloadThreads(threads: Int, isWifi: Boolean) {
        if (isWifi) {
            _threadsWifi.value = threads
            prefs?.edit()?.putInt(KEY_THREADS_WIFI, threads)?.apply()
        } else {
            _threadsMobile.value = threads
            prefs?.edit()?.putInt(KEY_THREADS_MOBILE, threads)?.apply()
        }
    }

    fun setRateLimit(limitBytes: Long, isWifi: Boolean) {
        if (isWifi) {
            _rateLimitWifi.value = limitBytes
            prefs?.edit()?.putLong(KEY_RATE_LIMIT_WIFI, limitBytes)?.apply()
        } else {
            _rateLimitMobile.value = limitBytes
            prefs?.edit()?.putLong(KEY_RATE_LIMIT_MOBILE, limitBytes)?.apply()
        }
    }

    fun setThrottledRate(rateBytes: Long, isWifi: Boolean) {
        if (isWifi) {
            _throttledRateWifi.value = rateBytes
            prefs?.edit()?.putLong(KEY_THROTTLED_RATE_WIFI, rateBytes)?.apply()
        } else {
            _throttledRateMobile.value = rateBytes
            prefs?.edit()?.putLong(KEY_THROTTLED_RATE_MOBILE, rateBytes)?.apply()
        }
    }

    fun setPreferredVideoQuality(quality: String) {
        _preferredVideoQuality.value = quality
        prefs?.edit()?.putString(KEY_PREFERRED_VIDEO_QUALITY, quality)?.apply()
    }

    fun setShowQualitySelector(value: Boolean) {
        _showQualitySelector.value = value
        prefs?.edit()?.putBoolean(KEY_SHOW_QUALITY_SELECTOR, value)?.apply()
    }

    fun setShareGlobalAction(action: ShareActionGlobal) {
        _shareGlobalAction.value = action
        prefs?.edit()?.putString(KEY_SHARE_GLOBAL_ACTION, action.name)?.apply()
    }

    fun setRuleYoutube(rule: PlatformDownloadRule) {
        _ruleYoutube.value = rule
        saveRule(KEY_RULE_YOUTUBE, rule)
    }

    fun setRuleTiktok(rule: PlatformDownloadRule) {
        _ruleTiktok.value = rule
        saveRule(KEY_RULE_TIKTOK, rule)
    }

    fun setRuleVk(rule: PlatformDownloadRule) {
        _ruleVk.value = rule
        saveRule(KEY_RULE_VK, rule)
    }

    fun setRuleInstagram(rule: PlatformDownloadRule) {
        _ruleInstagram.value = rule
        saveRule(KEY_RULE_INSTAGRAM, rule)
    }

    fun setRulePinterest(rule: PlatformDownloadRule) {
        _rulePinterest.value = rule
        saveRule(KEY_RULE_PINTEREST, rule)
    }

    fun setRuleOther(rule: PlatformDownloadRule) {
        _ruleOther.value = rule
        saveRule(KEY_RULE_OTHER, rule)
    }

    fun getRuleForUrl(url: String): PlatformDownloadRule {
        return when {
            url.contains("youtube.com") || url.contains("youtu.be") -> _ruleYoutube.value
            url.contains("tiktok.com") -> _ruleTiktok.value
            url.contains("vk.com") || url.contains("vkvideo.ru") -> _ruleVk.value
            url.contains("instagram.com") -> _ruleInstagram.value
            url.contains("pinterest.com") || url.contains("pin.it") -> _rulePinterest.value
            else -> _ruleOther.value
        }
    }

    fun setCustomDir(context: Context, uri: Uri?) {
        if (uri == null) return
        val appContext = context.applicationContext
        try {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            val docFile = DocumentFile.fromTreeUri(appContext, uri)
            val folderName = docFile?.name ?: "Выбранная папка"
            _customDirName.value = folderName

            prefs?.edit()
                ?.putString(KEY_CUSTOM_DIR_URI, uri.toString())
                ?.putString(KEY_CUSTOM_DIR_NAME, folderName)
                ?.apply()

            AsyncLogger.log(LogLevel.INFO, "Выбрана новая папка для загрузок: $folderName")
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "Ошибка сохранения папки SAF: ${e.message}")
        }
    }

    fun getCustomDirUri(): Uri? {
        val uriStr = prefs?.getString(KEY_CUSTOM_DIR_URI, null) ?: return null
        return Uri.parse(uriStr)
    }

    fun setSaveToGallery(value: Boolean) {
        _saveToGallery.value = value
        prefs?.edit()?.putBoolean(KEY_SAVE_TO_GALLERY, value)?.apply()
    }

    fun setShowThumbnails(value: Boolean) {
        _showThumbnails.value = value
        prefs?.edit()?.putBoolean(KEY_SHOW_THUMBNAILS, value)?.apply()
    }

    fun setShowHistoryThumbnails(value: Boolean) {
        _showHistoryThumbnails.value = value
        prefs?.edit()?.putBoolean(KEY_SHOW_HISTORY_THUMBNAILS, value)?.apply()
    }

    fun setShowHistorySort(value: Boolean) {
        _showHistorySort.value = value
        prefs?.edit()?.putBoolean(KEY_SHOW_HISTORY_SORT, value)?.apply()
    }

    fun setHistorySyncMode(mode: HistorySyncMode) {
        _historySyncMode.value = mode
        prefs?.edit()?.putString(KEY_HISTORY_SYNC_MODE, mode.name)?.apply()
    }

    fun setTileAction(action: TileAction) {
        _tileAction.value = action
        prefs?.edit()?.putString(KEY_TILE_ACTION, action.name)?.apply()
    }

    fun setHistoryTitleMode(mode: HistoryTitleMode) {
        _historyTitleMode.value = mode
        prefs?.edit()?.putString(KEY_HISTORY_TITLE_MODE, mode.name)?.apply()
    }

    fun setShowStorageStats(value: Boolean) {
        _showStorageStats.value = value
        prefs?.edit()?.putBoolean(KEY_SHOW_STORAGE_STATS, value)?.apply()
    }

    fun setUseDynamicColors(value: Boolean) {
        _useDynamicColors.value = value
        prefs?.edit()?.putBoolean(KEY_DYNAMIC_COLORS, value)?.apply()
    }

    fun setHapticEnabled(value: Boolean) {
        _hapticEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_HAPTIC_ENABLED, value)?.apply()
    }

    fun setHapticIntensity(intensity: HapticIntensity) {
        _hapticIntensity.value = intensity
        prefs?.edit()?.putString(KEY_HAPTIC_INTENSITY, intensity.name)?.apply()
    }

    fun setHapticButtons(value: Boolean) {
        _hapticButtons.value = value
        prefs?.edit()?.putBoolean(KEY_HAPTIC_BUTTONS, value)?.apply()
    }

    fun setHapticPlayer(value: Boolean) {
        _hapticPlayer.value = value
        prefs?.edit()?.putBoolean(KEY_HAPTIC_PLAYER, value)?.apply()
    }

    fun setHapticLongPress(value: Boolean) {
        _hapticLongPress.value = value
        prefs?.edit()?.putBoolean(KEY_HAPTIC_LONG_PRESS, value)?.apply()
    }

    fun setAppTheme(theme: AppTheme) {
        _appTheme.value = theme
        prefs?.edit()?.putString(KEY_APP_THEME, theme.name)?.apply()
    }

    fun setMaxParallelDownloads(value: Int) {
        _maxParallelDownloads.value = value
        prefs?.edit()?.putInt(KEY_MAX_PARALLEL_DOWNLOADS, value)?.apply()
    }

    fun setAutoCleanupLogs(value: Boolean) {
        _autoCleanupLogs.value = value
        prefs?.edit()?.putBoolean(KEY_AUTO_CLEANUP_LOGS, value)?.apply()
    }

    fun setFileNameTemplate(value: FileNameTemplate) {
        _fileNameTemplate.value = value
        prefs?.edit()?.putString(KEY_FILE_NAME_TEMPLATE, value.name)?.apply()
    }

    fun setMinDiskSpace(value: MinDiskSpace) {
        _minDiskSpace.value = value
        prefs?.edit()?.putString(KEY_MIN_DISK_SPACE, value.name)?.apply()
    }

    fun setUseSponsorBlock(value: Boolean) {
        _useSponsorBlock.value = value
        prefs?.edit()?.putBoolean(KEY_USE_SPONSORBLOCK, value)?.apply()
    }

    fun setUseImpersonate(value: Boolean) {
        _useImpersonate.value = value
        prefs?.edit()?.putBoolean(KEY_USE_IMPERSONATE, value)?.apply()
    }

    fun setUseClipboardBubble(value: Boolean) {
        _useClipboardBubble.value = value
        prefs?.edit()?.putBoolean(KEY_USE_CLIPBOARD_BUBBLE, value)?.apply()
    }

    fun setUseClipboardPreview(value: Boolean) {
        _useClipboardPreview.value = value
        prefs?.edit()?.putBoolean(KEY_USE_CLIPBOARD_PREVIEW, value)?.apply()
    }

    fun setUseClipboardHistory(value: Boolean) {
        _useClipboardHistory.value = value
        prefs?.edit()?.putBoolean(KEY_USE_CLIPBOARD_HISTORY, value)?.apply()
    }

    fun addRecentClipboardUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        val current = _recentClipboardUrls.value.toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        val updated = current.take(5)
        _recentClipboardUrls.value = updated
        val encoded = updated.joinToString(";;;")
        prefs?.edit()?.putString(KEY_RECENT_CLIPBOARD_URLS, encoded)?.apply()
    }

    fun removeRecentClipboardUrl(url: String) {
        val current = _recentClipboardUrls.value.toMutableList()
        current.remove(url)
        _recentClipboardUrls.value = current
        val encoded = current.joinToString(";;;")
        prefs?.edit()?.putString(KEY_RECENT_CLIPBOARD_URLS, encoded)?.apply()
    }

    fun clearRecentClipboardUrls() {
        _recentClipboardUrls.value = emptyList()
        prefs?.edit()?.remove(KEY_RECENT_CLIPBOARD_URLS)?.apply()
    }

    fun setShowDynamicTips(value: Boolean) {
        _showDynamicTips.value = value
        _tipConfigVersion.value++
        prefs?.edit()?.putBoolean(KEY_SHOW_DYNAMIC_TIPS, value)?.apply()
    }

    fun setTipAnimationSpeedSec(seconds: Float) {
        val ms = (seconds.coerceIn(1.0f, 10.0f) * 1000f).toLong()
        _tipAnimationSpeedMs.value = ms
        _tipConfigVersion.value++
        prefs?.edit()?.putLong(KEY_TIP_ANIMATION_SPEED_MS, ms)?.apply()
    }

    fun setTipPlaybackMode(mode: TipPlaybackMode) {
        _tipPlaybackMode.value = mode
        _tipConfigVersion.value++
        prefs?.edit()?.putString(KEY_TIP_PLAYBACK_MODE, mode.name)?.apply()
    }

    fun setTipFrames(frames: List<TipFrameData>) {
        val sanitized = frames.take(5).filter { it.text.isNotBlank() }
        val finalFrames = if (sanitized.isEmpty()) defaultTipFrames else sanitized
        _tipFrames.value = finalFrames
        _tipConfigVersion.value++

        val encoded = finalFrames.joinToString(";;;") { "${it.iconKey}:::${it.text}" }
        prefs?.edit()?.putString(KEY_TIP_FRAMES_DATA, encoded)?.apply()
    }

    fun resetTipFrames() {
        setTipFrames(defaultTipFrames)
        setTipAnimationSpeedSec(2.4f)
        setTipPlaybackMode(TipPlaybackMode.EVERY_NAVIGATION)
        _tipConfigVersion.value++
    }

    fun setUseInternalPlayer(value: Boolean) {
        _useInternalPlayer.value = value
        prefs?.edit()?.putBoolean(KEY_USE_INTERNAL_PLAYER, value)?.apply()
    }

    fun setPipEnabled(value: Boolean) {
        _pipEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_PIP_ENABLED, value)?.apply()
    }

    fun setPipActionsEnabled(value: Boolean) {
        _pipActionsEnabled.value = value
        prefs?.edit()?.putBoolean(KEY_PIP_ACTIONS_ENABLED, value)?.apply()
    }

    fun updateAvgSpeed(newSpeedBps: Long, isWifi: Boolean) {
        if (newSpeedBps <= 0) return
        if (isWifi) {
            val current = _avgSpeedWifi.value
            val updated = ((current * 0.8) + (newSpeedBps * 0.2)).toLong()
            _avgSpeedWifi.value = updated
            prefs?.edit()?.putLong(KEY_AVG_SPEED_WIFI, updated)?.apply()
        } else {
            val current = _avgSpeedMobile.value
            val updated = ((current * 0.8) + (newSpeedBps * 0.2)).toLong()
            _avgSpeedMobile.value = updated
            prefs?.edit()?.putLong(KEY_AVG_SPEED_MOBILE, updated)?.apply()
        }
    }
}

fun android.view.View?.performAppHaptic(type: HapticType = HapticType.CLICK) {
    if (this == null) return
    if (!SettingsManager.hapticEnabled.value) return

    val intensity = SettingsManager.hapticIntensity.value

    when (type) {
        HapticType.CLICK -> {
            if (!SettingsManager.hapticButtons.value) return
            when (intensity) {
                HapticIntensity.SOFT -> performHapticFeedback(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1)
                        android.view.HapticFeedbackConstants.TEXT_HANDLE_MOVE
                    else android.view.HapticFeedbackConstants.KEYBOARD_TAP
                )
                HapticIntensity.STANDARD -> performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                HapticIntensity.STRONG -> performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
        HapticType.LONG_PRESS -> {
            if (!SettingsManager.hapticLongPress.value) return
            when (intensity) {
                HapticIntensity.SOFT -> performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                HapticIntensity.STANDARD -> performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                HapticIntensity.STRONG -> performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
            }
        }
        HapticType.PLAYER_GESTURE -> {
            if (!SettingsManager.hapticPlayer.value) return
            when (intensity) {
                HapticIntensity.SOFT -> performHapticFeedback(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1)
                        android.view.HapticFeedbackConstants.TEXT_HANDLE_MOVE
                    else android.view.HapticFeedbackConstants.KEYBOARD_TAP
                )
                HapticIntensity.STANDARD -> performHapticFeedback(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
                        android.view.HapticFeedbackConstants.CLOCK_TICK
                    else android.view.HapticFeedbackConstants.KEYBOARD_TAP
                )
                HapticIntensity.STRONG -> performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            }
        }
        HapticType.SUCCESS -> {
            when (intensity) {
                HapticIntensity.SOFT -> performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
                HapticIntensity.STANDARD -> performHapticFeedback(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
                        android.view.HapticFeedbackConstants.CONFIRM
                    else android.view.HapticFeedbackConstants.VIRTUAL_KEY
                )
                HapticIntensity.STRONG -> performHapticFeedback(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
                        android.view.HapticFeedbackConstants.CONFIRM
                    else android.view.HapticFeedbackConstants.VIRTUAL_KEY
                )
            }
        }
        HapticType.SELECTION -> {
            when (intensity) {
                HapticIntensity.SOFT -> performHapticFeedback(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1)
                        android.view.HapticFeedbackConstants.TEXT_HANDLE_MOVE
                    else android.view.HapticFeedbackConstants.KEYBOARD_TAP
                )
                HapticIntensity.STANDARD -> performHapticFeedback(
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
                        android.view.HapticFeedbackConstants.CLOCK_TICK
                    else android.view.HapticFeedbackConstants.KEYBOARD_TAP
                )
                HapticIntensity.STRONG -> performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            }
        }
    }
}