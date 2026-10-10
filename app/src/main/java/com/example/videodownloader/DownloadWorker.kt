package com.example.videodownloader

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.videodownloader.utils.detectPlatformName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.StatFs
import android.os.Environment
import android.provider.DocumentsContract

class DownloadWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val CHANNEL_ID = "video_download_channel"
        const val NOTIFICATION_ID = 1001
        const val KEY_URL = "KEY_URL"
        const val KEY_TITLE = "KEY_TITLE"
        const val KEY_IS_AUDIO = "KEY_IS_AUDIO"
        const val KEY_QUALITY = "KEY_QUALITY"
        const val KEY_THREADS = "KEY_THREADS"
        const val KEY_RATE_LIMIT = "KEY_RATE_LIMIT"
        const val KEY_THROTTLED_RATE = "KEY_THROTTLED_RATE"
        const val KEY_MAX_RETRIES = "KEY_MAX_RETRIES"
        const val KEY_THUMBNAIL = "KEY_THUMBNAIL"
        const val KEY_PASSPORT = "KEY_PASSPORT" // Путь к JSON-паспорту
        const val KEY_PROGRESS = "KEY_PROGRESS"
        const val KEY_SLOT = "KEY_SLOT"
        const val KEY_IS_BATCH = "KEY_IS_BATCH"
        const val KEY_BATCH_SIZE = "KEY_BATCH_SIZE"
        const val KEY_IS_AUTOPILOT = "KEY_IS_AUTOPILOT"
        
        const val GROUP_KEY_DOWNLOADS = "com.example.videodownloader.DOWNLOADS"
        const val GROUP_KEY_COMPLETED = "com.example.videodownloader.COMPLETED"
        const val SUMMARY_ID_DOWNLOADS = 1000
        const val SUMMARY_ID_COMPLETED = 2000
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @SuppressLint("WakelockTimeout")
    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        val url = inputData.getString(KEY_URL) ?: return@withContext Result.failure()
        val title = inputData.getString(KEY_TITLE) ?: "Медиа"
        val isAudio = inputData.getBoolean(KEY_IS_AUDIO, false)
        val quality = inputData.getString(KEY_QUALITY) ?: "best"
        val thumbnailUrl = inputData.getString(KEY_THUMBNAIL)
        val passportPath = inputData.getString(KEY_PASSPORT) ?: ""

        // Проверка сети перед стартом скачивания
        if (!com.example.videodownloader.utils.isNetworkAvailable(context)) {
            val errorMsg = "Отсутствует подключение к интернету"
            AsyncLogger.log(LogLevel.ERROR, "DownloadWorker: $errorMsg")
            showErrorNotification(title, errorMsg)
            return@withContext ListenableWorker.Result.failure()
        }

        // Параметры сети (Wi-Fi vs Mobile)
        val isWifi = com.example.videodownloader.utils.isWifiConnected(context)
        val threads = if (isWifi) SettingsManager.threadsWifi.value else SettingsManager.threadsMobile.value
        val rateLimit = if (isWifi) SettingsManager.rateLimitWifi.value else SettingsManager.rateLimitMobile.value
        val throttledRate = if (isWifi) SettingsManager.throttledRateWifi.value else SettingsManager.throttledRateMobile.value

        val isBatch = inputData.getBoolean(KEY_IS_BATCH, false)
        val isAutopilot = inputData.getBoolean(KEY_IS_AUTOPILOT, false)
        val maxRetries = if (isBatch) {
            if (isWifi) SettingsManager.queueRetryWifi.value else SettingsManager.queueRetryMobile.value
        } else {
            if (isWifi) SettingsManager.singleRetryWifi.value else SettingsManager.singleRetryMobile.value
        }

        val slot = inputData.getInt(KEY_SLOT, 1)
        val currentNotifId = NOTIFICATION_ID + slot

        createNotificationChannel()

        try {
            setForeground(createForegroundInfo(currentNotifId, title, 0, 0, 0))
            showGroupSummaryNotification(GROUP_KEY_DOWNLOADS, SUMMARY_ID_DOWNLOADS)
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.WARN, "Foreground info warning: ${e.message}")
        }

        val typeText = if (isAudio) "Аудио" else "Видео"
        AsyncLogger.log(LogLevel.INFO, "DownloadWorker: Фоновая загрузка ($typeText) для '$title'")

        // 1. ПРОВЕРКА МЕСТА НА ДИСКЕ
        val minSpace = SettingsManager.minDiskSpace.value
        if (minSpace != MinDiskSpace.NONE) {
            val customUri = SettingsManager.getCustomDirUri()
            val available = if (customUri != null) {
                getAvailableSpaceForUri(context, customUri)
            } else {
                getAvailableDiskSpace()
            }
            
            if ((available < minSpace.bytes) && available != -1L) {
                val needed = formatSize(minSpace.bytes)
                val current = formatSize(available)
                val errorMsg = "Недостаточно места! Нужно: $needed, доступно: $current"
                AsyncLogger.log(LogLevel.ERROR, "DownloadWorker: $errorMsg")
                showErrorNotification(title, errorMsg) 
                return@withContext ListenableWorker.Result.failure()
            }
        }

        var downloadTitle = title
        var downloadThumbnail = thumbnailUrl

        // Разрешение метаданных: извлечение имени до генерации имени файла
        if (title.startsWith("Анализ ") || title.isBlank() || title == "Медиа") {
            try {
                val result = YtDlpBridge.fetchInfo(url)
                val meta = result.getOrNull()
                if (meta != null && meta.title.isNotBlank() && !meta.title.startsWith("Анализ ")) {
                    downloadTitle = meta.title
                    downloadThumbnail = meta.thumbnailUrl
                    AsyncLogger.log(LogLevel.INFO, "DownloadWorker: Настоящее имя извлечено в фоне: '$downloadTitle'")
                }
            } catch (e: Exception) {
                AsyncLogger.log(LogLevel.WARN, "DownloadWorker: Ошибка фонового анализа имени: ${e.message}")
            }
        }

        // Генерация имени файла по шаблону
        val template = SettingsManager.fileNameTemplate.value
        val platform = detectPlatformName(url)
        val isPlaceholderTitle = downloadTitle.startsWith("Анализ ") || downloadTitle.isBlank()

        val customFilename = if (isPlaceholderTitle) {
            null // Eсли имя все еще заглушка, передаем null, чтобы yt-dlp сам вытащил тег %(title)s при скачивании
        } else {
            when (template) {
                FileNameTemplate.TITLE_PLATFORM -> {
                    val cleanTitle = downloadTitle.replace(Regex("[\\\\/*?:\"<>|]"), "").trim().take(50)
                    if (cleanTitle.isEmpty()) "video_$platform" else "$cleanTitle - $platform"
                }
                FileNameTemplate.DATE_TITLE -> {
                    val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
                    val cleanTitle = downloadTitle.replace(Regex("[\\\\/*?:\"<>|]"), "").trim().take(50)
                    if (cleanTitle.isEmpty()) "${dateStr}_video" else "${dateStr}_$cleanTitle"
                }
                FileNameTemplate.TITLE_ONLY -> {
                    val cleanTitle = downloadTitle.replace(Regex("[\\\\/*?:\"<>|]"), "").trim().take(50)
                    if (cleanTitle.isEmpty()) "video_$platform" else cleanTitle
                }
                FileNameTemplate.PLATFORM_TITLE -> {
                    val cleanTitle = downloadTitle.replace(Regex("[\\\\/*?:\"<>|]"), "").trim().take(50)
                    if (cleanTitle.isEmpty()) "[$platform] video" else "[$platform] $cleanTitle"
                }
                FileNameTemplate.DATE_TIME_TITLE -> {
                    val dateTimeStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                    val cleanTitle = downloadTitle.replace(Regex("[\\\\/*?:\"<>|]"), "").trim().take(50)
                    if (cleanTitle.isEmpty()) "${dateTimeStr}_video" else "${dateTimeStr}_$cleanTitle"
                }
            }
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "VideoDownloader:DownloadWakeLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(60 * 60 * 1000L)
        }

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiLock = wifiManager?.createWifiLock(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                @Suppress("DEPRECATION")
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            },
            "VideoDownloader:DownloadWifiLock"
        )?.apply {
            setReferenceCounted(false)
            acquire()
        }

        var lastProgress = 0
        var lastUpdateTime = 0L
        var attempt = 0
        var downloadResult: kotlin.Result<File>? = null

        try {
            while (attempt <= maxRetries && !isStopped) {
                if (attempt > 0) {
                    AsyncLogger.log(LogLevel.WARN, "DownloadWorker: Повторная попытка ($attempt/$maxRetries) для '$downloadTitle'")

                    notificationManager.notify(
                        currentNotifId,
                        NotificationCompat.Builder(context, CHANNEL_ID)
                            .setContentTitle(downloadTitle)
                            .setContentText("Сбой сети. Повтор через 3 сек ($attempt/$maxRetries)...")
                            .setSmallIcon(android.R.drawable.stat_sys_download)
                            .setOngoing(true)
                            .build()
                    )
                    delay(3000.milliseconds)
                }

                val optimizeTransit = SettingsManager.transitOptimizeSize.value
                val maxRes = SettingsManager.transitMaxQuality.value
                val codec = SettingsManager.transitCodec.value
                val effectiveQuality = if (optimizeTransit && (quality == "best" || quality.isBlank())) "transit_opt:$maxRes:$codec" else quality

                downloadResult = YtDlpBridge.downloadVideo(
                    context = context,
                    url = url,
                    isAudioOnly = isAudio,
                    quality = effectiveQuality,
                    threads = threads,
                    rateLimit = rateLimit,
                    throttledRate = throttledRate,
                    customFilename = customFilename,
                    thumbnailUrl = downloadThumbnail,
                    passportPath = passportPath,
                    isCancelled = { isStopped }
                ) { progress, speed, eta ->
                    if (isStopped) return@downloadVideo

                    val currentTime = System.currentTimeMillis()
                    
                    // Обновляем среднюю скорость сети для алгоритма "умного транзита"
                    // Разделяем замеры для Wi-Fi и мобильной сети (только при существенном изменении прогресса)
                    if (speed > 0 && progress % 5 == 0) {
                        val isWifi = com.example.videodownloader.utils.isWifiConnected(context)
                        SettingsManager.updateAvgSpeed(speed, isWifi)
                    }

                    // Ограничение нагрузки при нагреве устройства
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val status = PerformanceManager.thermalStatus.value
                        if (status >= PowerManager.THERMAL_STATUS_SEVERE) {
                            // Искусственная задержка для снижения нагрузки на CPU
                            runBlocking { delay(200) }
                        }
                    }

                    // Обновление уведомления не чаще чем раз в 1.2 секунды
                    if (currentTime - lastUpdateTime >= 1200 || progress == 100) {
                        if (progress != lastProgress || currentTime - lastUpdateTime >= 2000) {
                            lastProgress = progress
                            lastUpdateTime = currentTime

                            setProgressAsync(
                                workDataOf(
                                    KEY_PROGRESS to progress,
                                    KEY_TITLE to downloadTitle,
                                    KEY_URL to url
                                )
                            )
                            notificationManager.notify(
                                currentNotifId, 
                                buildProgressNotification(downloadTitle, progress, speed, eta)
                            )
                        }
                    }
                }

                if (downloadResult.isSuccess) {
                    break
                }
                attempt++
            }

            if (isStopped) {
                notificationManager.cancel(currentNotifId)
                return@withContext ListenableWorker.Result.failure()
            }

            if (downloadResult != null && downloadResult.isSuccess) {
                AsyncLogger.log(LogLevel.INFO, "DownloadWorker: Загрузка успешно завершена!")
                notificationManager.cancel(currentNotifId)

                val isBatchIntent = inputData.getBoolean(KEY_IS_BATCH, false)
                val isAutopilot = inputData.getBoolean(KEY_IS_AUTOPILOT, false)
                val batchSize = inputData.getInt(KEY_BATCH_SIZE, 1)
                val isQueueSummary = SettingsManager.notifQueueSummary.value
                
                val workInfos = try {
                    WorkManager.getInstance(context).getWorkInfosForUniqueWork("video_downloader_queue_$slot").get()
                } catch (_: Exception) { emptyList() }

                val remainingInQueue = workInfos.count {
                    (it.id != this@DownloadWorker.id) &&
                            (it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED || it.state == WorkInfo.State.RUNNING)
                }
                
                val isLastInQueue = remainingInQueue == 0
                val lastItem = HistoryManager.getLatestEntry(context)

                if (isBatchIntent && isQueueSummary) {
                    if (isLastInQueue) {
                        showBatchSummaryNotification(batchSize)
                    }
                } else {
                    showSingleCompletedNotification(downloadTitle, "$typeText успешно сохранен(о)", lastItem, isAutopilot)
                }

                ListenableWorker.Result.success()
            } else {
                val rawErrorMsg = downloadResult?.exceptionOrNull()?.message ?: "Неизвестная ошибка"
                val userErrorMsg = com.example.videodownloader.utils.translateNetworkError(rawErrorMsg, "Ошибка скачивания")
                AsyncLogger.log(LogLevel.ERROR, "DownloadWorker: Ошибка после всех попыток ($attempt): $rawErrorMsg")
                notificationManager.cancel(currentNotifId)
                showErrorNotification(downloadTitle, userErrorMsg)
                ListenableWorker.Result.failure()
            }
        } finally {
            try {
                if (wakeLock?.isHeld == true) wakeLock.release()
                if (wifiLock?.isHeld == true) wifiLock.release()
            } catch (_: Exception) {
            }

            if (isStopped) {
                notificationManager.cancel(currentNotifId)
            }
        }
    }

    private fun createForegroundInfo(notifId: Int, title: String, progress: Int, speed: Long, eta: Long): ForegroundInfo {
        val notification = buildProgressNotification(title, progress, speed, eta)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(notifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notifId, notification)
        }
    }

    private fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return ""
        return if (bytesPerSec >= 1024 * 1024) {
            val mb = bytesPerSec / (1024.0 * 1024.0)
            "${((mb * 10).toInt() / 10.0)} МБ/с"
        } else {
            "${bytesPerSec / 1024} КБ/с"
        }
    }

    private fun formatEta(seconds: Long): String {
        if (seconds <= 0) return ""
        return if (seconds < 60) "~$seconds сек"
        else "~${seconds / 60} мин"
    }

    private fun buildProgressNotification(title: String, progress: Int, speed: Long, eta: Long): android.app.Notification {
        val isDetailed = SettingsManager.notifDetailedProgress.value

        val contentText = if (isDetailed && progress > 0) {
            val speedStr = formatSpeed(speed)
            val etaStr = formatEta(eta)
            val parts = mutableListOf("$progress%")
            if (speedStr.isNotEmpty()) parts.add(speedStr)
            if (etaStr.isNotEmpty()) parts.add(etaStr)
            parts.joinToString(" • ")
        } else if (progress > 0) {
            "Скачивание: $progress%"
        } else {
            "Подготовка..."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress == 0)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup(GROUP_KEY_DOWNLOADS)

        if (SettingsManager.notifActionCancel.value) {
            val slot = inputData.getInt(KEY_SLOT, 1)
            val currentNotifId = NOTIFICATION_ID + slot
            val cancelIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_CANCEL_DOWNLOAD
                putExtra(NotificationReceiver.EXTRA_WORK_ID, id.toString())
                putExtra(NotificationReceiver.EXTRA_NOTIF_ID, currentNotifId)
            }
            val cancelPendingIntent = PendingIntent.getBroadcast(
                context, 9999, cancelIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Отменить", cancelPendingIntent)
        }

        return builder.build()
    }

    private fun showSingleCompletedNotification(title: String, message: String, item: DownloadedFileItem?, isAutopilot: Boolean) {
        val notifId = (3000..9999).random() 
        val soundEnabled = SettingsManager.notifSound.value
        val vibrateEnabled = SettingsManager.notifVibrate.value

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(if (soundEnabled) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_LOW)

        if (!soundEnabled) builder.setSilent(true)
        if (!vibrateEnabled) builder.setVibrate(longArrayOf(0L))

        if (SettingsManager.notifGroupCompleted.value) {
            builder.setGroup(GROUP_KEY_COMPLETED)
            showGroupSummaryNotification(GROUP_KEY_COMPLETED, SUMMARY_ID_COMPLETED)
        }

        if (item != null) {
            try {
                val uri: Uri = if (item.pathOrUri.startsWith("content://")) {
                    item.pathOrUri.toUri()
                } else {
                    val file = File(item.pathOrUri)
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }

                val mimeType = when (item.mediaType) {
                    MediaType.PHOTO -> "image/*"
                    MediaType.AUDIO -> "audio/*"
                    MediaType.VIDEO -> "video/*"
                }

                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val viewPendingIntent = PendingIntent.getActivity(
                    context, (0..1000).random(), viewIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                // Нажатие на уведомление открывает файл
                builder.setContentIntent(viewPendingIntent)

                if (SettingsManager.notifActionOpen.value) {
                    builder.addAction(android.R.drawable.ic_media_play, "Смотреть", viewPendingIntent)
                }

                if (SettingsManager.notifActionShare.value) {
                    // Передача файла через Activity PendingIntent
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    // Callback for Transit Mode
                    val receiverIntent = Intent(context, NotificationReceiver::class.java).apply {
                        action = NotificationReceiver.ACTION_SHARE_SUCCESS
                        putExtra(NotificationReceiver.EXTRA_ITEM_ID, item.id)
                        putExtra(NotificationReceiver.EXTRA_IS_AUTOPILOT, isAutopilot)
                    }
                    val callbackPendingIntent = PendingIntent.getBroadcast(
                        context, (2001..3000).random(), receiverIntent,
                        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    val chooserIntent = Intent.createChooser(shareIntent, "Поделиться через", callbackPendingIntent.intentSender)
                    chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    
                    val sharePendingIntent = PendingIntent.getActivity(
                        context, (1001..2000).random(), chooserIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    builder.addAction(android.R.drawable.ic_menu_share, "Поделиться", sharePendingIntent)
                }

                if (SettingsManager.notifActionDelete.value) {
                    val deleteIntent = Intent(context, NotificationReceiver::class.java).apply {
                        action = NotificationReceiver.ACTION_DELETE_FILE
                        putExtra(NotificationReceiver.EXTRA_ITEM_ID, item.id)
                        putExtra(NotificationReceiver.EXTRA_NOTIF_ID, notifId)
                    }
                    val deletePendingIntent = PendingIntent.getBroadcast(
                        context, (2001..3000).random(), deleteIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    builder.addAction(android.R.drawable.ic_menu_delete, "Удалить", deletePendingIntent)
                }

            } catch (e: Exception) {
                AsyncLogger.log(LogLevel.ERROR, "Ошибка создания кнопок: ${e.message}")
            }
        }

        notificationManager.notify(notifId, builder.build())
        scheduleAutoDismiss(notifId)
    }

    private fun showGroupSummaryNotification(groupKey: String, summaryId: Int) {
        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(if (groupKey == GROUP_KEY_COMPLETED) "Завершённые загрузки" else "Активные загрузки")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText(if (groupKey == GROUP_KEY_COMPLETED) "Файлы сохранены" else "В процессе...")
            )
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(summaryId, summaryNotification)
    }

    private fun showBatchSummaryNotification(totalCount: Int) {
        val notifId = 1002
        val soundEnabled = SettingsManager.notifSound.value
        val vibrateEnabled = SettingsManager.notifVibrate.value

        val historyIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "history")
        }
        val historyPendingIntent = PendingIntent.getActivity(
            context, 5555, historyIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Пакетная загрузка завершена")
            .setContentText("Все файлы ($totalCount шт.) сохранены в память")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(historyPendingIntent)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(if (soundEnabled) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_LOW)

        if (!soundEnabled) builder.setSilent(true)
        if (!vibrateEnabled) builder.setVibrate(longArrayOf(0L))

        builder.addAction(android.R.drawable.ic_menu_recent_history, "История", historyPendingIntent)

        notificationManager.notify(notifId, builder.build())
        scheduleAutoDismiss(notifId)
    }

    private fun scheduleAutoDismiss(notifId: Int) {
        val autoDismissDelay = SettingsManager.notifAutoDismiss.value
        if (autoDismissDelay > 0L) {
            Handler(Looper.getMainLooper()).postDelayed({
                notificationManager.cancel(notifId)
            }, autoDismissDelay)
        }
    }

    private fun showErrorNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        notificationManager.notify((10000..19999).random(), notification)
    }

    private fun getAvailableDiskSpace(): Long {
        // Проверяем место на основном внешнем хранилище (где лежат Downloads)
        val path = Environment.getExternalStorageDirectory()
        return try {
            val stat = StatFs(path.path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            // Фолбэк на внутреннюю память, если внешняя недоступна
            val stat = StatFs(context.filesDir.path)
            stat.availableBlocksLong * stat.blockSizeLong
        }
    }

    private fun getAvailableSpaceForUri(context: Context, uri: Uri): Long {
        return try {
            val rootId = DocumentsContract.getTreeDocumentId(uri)
            val rootUri = DocumentsContract.buildRootUri(uri.authority, rootId)
            
            val cursor = context.contentResolver.query(
                rootUri,
                arrayOf(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES),
                null, null, null
            )
            
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES)
                    if (index != -1) return it.getLong(index)
                }
            }
            getAvailableDiskSpace() 
        } catch (_: Exception) {
            getAvailableDiskSpace()
        }
    }

    private fun formatSize(bytes: Long): String {
        return if (bytes >= 1024 * 1024 * 1024) {
            String.format(Locale.getDefault(), "%.1f ГБ", bytes / (1024.0 * 1024.0 * 1024.0))
        } else {
            String.format(Locale.getDefault(), "%d МБ", bytes / (1024 * 1024))
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Загрузка медиа",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Показывает прогресс и результат скачивания"
        }
        notificationManager.createNotificationChannel(channel)
    }
}