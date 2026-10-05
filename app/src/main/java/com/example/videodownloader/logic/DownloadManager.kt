package com.example.videodownloader.logic

import android.content.Context
import android.widget.Toast
import androidx.work.*
import com.example.videodownloader.*
import com.example.videodownloader.utils.isWifiConnected
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import java.util.UUID

data class DownloadTaskStatus(
    val id: UUID,
    val slot: Int,
    val title: String,
    val url: String = "",
    val progress: Int,
    val state: WorkInfo.State,
    val startTime: Long = System.currentTimeMillis(),
)

object DownloadManager {
    private const val QUEUE_WORK_NAME = "video_downloader_queue"

    private val _isDownloading = MutableStateFlow(value = false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    private val _downloadProgress = MutableStateFlow(value = 0)
    // val downloadProgress: StateFlow<Int> = _downloadProgress // Unused

    private val _currentTitle = MutableStateFlow(value = "")
    // val currentTitle: StateFlow<String> = _currentTitle // Unused

    private val _queueCount = MutableStateFlow(0)
    val queueCount: StateFlow<Int> = _queueCount

    private val _activeTasks = MutableStateFlow<List<DownloadTaskStatus>>(emptyList())
    val activeTasks: StateFlow<List<DownloadTaskStatus>> = _activeTasks

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isObserving = false

    fun initObserver(context: Context) {
        if (isObserving) return
        isObserving = true

        val appContext = context.applicationContext
        val workManager = WorkManager.getInstance(appContext)
        val maxSlots = 5 // Увеличиваем лимит отслеживаемых слотов для запаса

        scope.launch {
            val flows = (1..maxSlots).map { slot ->
                workManager.getWorkInfosForUniqueWorkFlow("${QUEUE_WORK_NAME}_$slot")
            }

            combine(flows) { workInfosArray ->
                val allTasks = mutableListOf<DownloadTaskStatus>()
                var totalEnqueued = 0

                workInfosArray.forEachIndexed { index, workInfoList ->
                    val slot = index + 1
                    val runningWork = workInfoList.find { it.state == WorkInfo.State.RUNNING }
                    val enqueuedCount = workInfoList.count { 
                        (it.state == WorkInfo.State.ENQUEUED) || (it.state == WorkInfo.State.BLOCKED) 
                    }

                    totalEnqueued += enqueuedCount

                    if (runningWork != null) {
                        val taskUrl = runningWork.progress.getString(DownloadWorker.KEY_URL) ?: ""
                        val taskTitle = runningWork.progress.getString(DownloadWorker.KEY_TITLE) ?: "Загрузка..."

                        allTasks.add(
                            DownloadTaskStatus(
                                id = runningWork.id,
                                slot = slot,
                                title = taskTitle,
                                url = taskUrl,
                                progress = runningWork.progress.getInt(DownloadWorker.KEY_PROGRESS, 0),
                                state = WorkInfo.State.RUNNING,
                                startTime = runningWork.id.mostSignificantBits
                            )
                        )
                    } else if (enqueuedCount > 0) {
                        val firstEnqueued = workInfoList.find { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.BLOCKED }

                        allTasks.add(
                            DownloadTaskStatus(
                                id = firstEnqueued?.id ?: UUID.randomUUID(),
                                slot = slot,
                                title = "Ожидание в слоте $slot...",
                                url = "",
                                progress = 0,
                                state = WorkInfo.State.ENQUEUED
                            )
                        )
                    }
                }

                _activeTasks.value = allTasks
                _queueCount.value = totalEnqueued
                _isDownloading.value = allTasks.isNotEmpty()
                
                // Для обратной совместимости с существующим UI (если он еще не обновлен)
                if (allTasks.isNotEmpty()) {
                    val first = allTasks.first()
                    _downloadProgress.value = first.progress
                    _currentTitle.value = first.title
                } else {
                    _downloadProgress.value = 0
                    _currentTitle.value = ""
                }
            }.collect {}
        }
    }

    private fun getBestSlot(): Int {
        val maxSlots = SettingsManager.maxParallelDownloads.value
        val tasks = _activeTasks.value

        // Распределение нагрузки по слотам
        val slotLoads = (1..maxSlots).associateWith { slot ->
            tasks.count { it.slot == slot }
        }

        // Выбираем слот с минимальным количеством задач
        return slotLoads.minByOrNull { it.value }?.key ?: 1
    }

    fun isAlreadyDownloading(url: String, title: String = ""): Boolean {
        if (url.isBlank() && title.isBlank()) return false
        return _activeTasks.value.any { task ->
            ((url.isNotBlank() && task.url == url) || (title.isNotBlank() && task.title == title)) &&
                (task.state == WorkInfo.State.RUNNING || task.state == WorkInfo.State.ENQUEUED || task.state == WorkInfo.State.BLOCKED)
        }
    }

    fun startDownload(
        context: Context,
        url: String,
        title: String,
        isAudio: Boolean,
        quality: String = "best",
        thumbnailUrl: String? = null,
        isAutopilot: Boolean = false
    ) {
        val appContext = context.applicationContext
        initObserver(appContext)

        if (isAlreadyDownloading(url, title)) {
            AsyncLogger.log(LogLevel.WARN, "DownloadManager: Загрузка уже запущена: $url / $title")
            Toast.makeText(appContext, "Загрузка этой ссылки уже идет в фоне", Toast.LENGTH_SHORT).show()
            return
        }

        val workManager = WorkManager.getInstance(appContext)
        val slot = getBestSlot()
        val slotName = "${QUEUE_WORK_NAME}_$slot"

        val isWifi = isWifiConnected(appContext)
        val threads = if (isWifi) SettingsManager.threadsWifi.value else SettingsManager.threadsMobile.value
        val rateLimit = if (isWifi) SettingsManager.rateLimitWifi.value else SettingsManager.rateLimitMobile.value
        val throttledRate = if (isWifi) SettingsManager.throttledRateWifi.value else SettingsManager.throttledRateMobile.value
        val maxRetries = if (isWifi) SettingsManager.singleRetryWifi.value else SettingsManager.singleRetryMobile.value
        
        // Передача пути к паспорту метаданных
        val passportPath = CacheManager.getPassportFile(appContext, url).absolutePath

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val downloadWorkRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    DownloadWorker.KEY_URL to url,
                    DownloadWorker.KEY_TITLE to title.ifBlank { "Медиа" },
                    DownloadWorker.KEY_IS_AUDIO to isAudio,
                    DownloadWorker.KEY_QUALITY to quality,
                    DownloadWorker.KEY_THREADS to threads,
                    DownloadWorker.KEY_RATE_LIMIT to rateLimit,
                    DownloadWorker.KEY_THROTTLED_RATE to throttledRate,
                    DownloadWorker.KEY_MAX_RETRIES to maxRetries,
                    DownloadWorker.KEY_SLOT to slot,
                    DownloadWorker.KEY_IS_BATCH to false,
                    DownloadWorker.KEY_IS_AUTOPILOT to isAutopilot,
                    DownloadWorker.KEY_BATCH_SIZE to 1,
                    DownloadWorker.KEY_THUMBNAIL to (thumbnailUrl ?: AnalysisManager.resultThumbnail.value),
                    DownloadWorker.KEY_PASSPORT to passportPath
                )
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            slotName,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            downloadWorkRequest
        )

        AsyncLogger.log(LogLevel.INFO, "Задача '$title' добавлена в слот $slot")
    }

    fun startBatchDownload(
        context: Context,
        items: List<PlaylistItem>,
        isAudio: Boolean,
        quality: String = "best"
    ) {
        if (items.isEmpty()) return
        val appContext = context.applicationContext
        initObserver(appContext)
        val workManager = WorkManager.getInstance(appContext)
        val maxSlots = SettingsManager.maxParallelDownloads.value
        
        val isWifi = isWifiConnected(appContext)
        val threads = if (isWifi) SettingsManager.threadsWifi.value else SettingsManager.threadsMobile.value
        val rateLimit = if (isWifi) SettingsManager.rateLimitWifi.value else SettingsManager.rateLimitMobile.value
        val throttledRate = if (isWifi) SettingsManager.throttledRateWifi.value else SettingsManager.throttledRateMobile.value
        val maxRetries = if (isWifi) SettingsManager.queueRetryWifi.value else SettingsManager.queueRetryMobile.value

        items.forEachIndexed { index, item ->
            val slot = (index % maxSlots) + 1
            val slotName = "${QUEUE_WORK_NAME}_$slot"
            
            val passportPath = CacheManager.getPassportFile(appContext, item.url).absolutePath

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        DownloadWorker.KEY_URL to item.url,
                        DownloadWorker.KEY_TITLE to item.title.ifBlank { "Медиа" },
                        DownloadWorker.KEY_IS_AUDIO to isAudio,
                        DownloadWorker.KEY_QUALITY to quality,
                        DownloadWorker.KEY_THREADS to threads,
                        DownloadWorker.KEY_RATE_LIMIT to rateLimit,
                        DownloadWorker.KEY_THROTTLED_RATE to throttledRate,
                        DownloadWorker.KEY_MAX_RETRIES to maxRetries,
                        DownloadWorker.KEY_SLOT to slot,
                        DownloadWorker.KEY_IS_BATCH to true,
                        DownloadWorker.KEY_IS_AUTOPILOT to false,
                        DownloadWorker.KEY_BATCH_SIZE to items.size,
                        DownloadWorker.KEY_THUMBNAIL to item.thumbnailUrl,
                        DownloadWorker.KEY_PASSPORT to passportPath
                    )
                )
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()

            workManager.enqueueUniqueWork(
                slotName,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                request
            )
        }

        AsyncLogger.log(LogLevel.INFO, "Пакетная загрузка распределена по $maxSlots слотам")
    }

    fun cancelTask(context: Context, workId: UUID) {
        WorkManager.getInstance(context.applicationContext).cancelWorkById(workId)
        AsyncLogger.log(LogLevel.INFO, "Загрузка $workId отменена")
    }

    fun cancelAll(context: Context) {
        val appContext = context.applicationContext
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        (1..5).forEach { slot ->
            WorkManager.getInstance(appContext).cancelUniqueWork("${QUEUE_WORK_NAME}_$slot")
            notificationManager.cancel(DownloadWorker.NOTIFICATION_ID + slot)
        }
        notificationManager.cancel(DownloadWorker.NOTIFICATION_ID)
        _isDownloading.value = false
        _downloadProgress.value = 0
        _queueCount.value = 0
        _activeTasks.value = emptyList()
        Toast.makeText(appContext, "Все загрузки отменены", Toast.LENGTH_SHORT).show()
    }
}
