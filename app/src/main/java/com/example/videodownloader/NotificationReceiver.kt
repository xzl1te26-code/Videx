package com.example.videodownloader

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.net.Uri
import androidx.work.WorkManager
import kotlinx.coroutines.*
import java.util.UUID

object AudioControlBus {
    var onPrevTrack: (() -> Unit)? = null
    var onPlayPause: (() -> Unit)? = null
    var onNextTrack: (() -> Unit)? = null
}

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DELETE_FILE = "com.example.videodownloader.ACTION_DELETE_FILE"
        const val ACTION_CANCEL_DOWNLOAD = "com.example.videodownloader.ACTION_CANCEL_DOWNLOAD"
        const val ACTION_SHARE_FILE = "com.example.videodownloader.ACTION_SHARE_FILE"
        const val ACTION_SHARE_SUCCESS = "com.example.videodownloader.ACTION_SHARE_SUCCESS"

        const val ACTION_AUDIO_PREV = "com.example.videodownloader.ACTION_AUDIO_PREV"
        const val ACTION_AUDIO_PLAY_PAUSE = "com.example.videodownloader.ACTION_AUDIO_PLAY_PAUSE"
        const val ACTION_AUDIO_NEXT = "com.example.videodownloader.ACTION_AUDIO_NEXT"

        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_NOTIF_ID = "extra_notif_id"
        const val EXTRA_WORK_ID = "extra_work_id"
        const val EXTRA_MIME_TYPE = "extra_mime_type"
        const val EXTRA_IS_AUTOPILOT = "extra_is_autopilot"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val pendingResult = goAsync()

        when (intent?.action) {
            ACTION_AUDIO_PREV -> {
                AudioControlBus.onPrevTrack?.invoke()
                pendingResult.finish()
            }
            ACTION_AUDIO_PLAY_PAUSE -> {
                AudioControlBus.onPlayPause?.invoke()
                pendingResult.finish()
            }
            ACTION_AUDIO_NEXT -> {
                AudioControlBus.onNextTrack?.invoke()
                pendingResult.finish()
            }
            ACTION_CANCEL_DOWNLOAD -> {
                val workIdStr = intent.getStringExtra(EXTRA_WORK_ID)
                val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)
                if (!workIdStr.isNullOrBlank()) {
                    try {
                        val workId = UUID.fromString(workIdStr)
                        WorkManager.getInstance(context).cancelWorkById(workId)
                        if (notifId != -1) {
                            notificationManager.cancel(notifId)
                        } else {
                            // Если ID не передан, пробуем отменить базовый и все возможные слоты (1-10)
                            notificationManager.cancel(DownloadWorker.NOTIFICATION_ID)
                            for (slot in 1..10) {
                                notificationManager.cancel(DownloadWorker.NOTIFICATION_ID + slot)
                            }
                        }
                        Toast.makeText(context, "Загрузка отменена", Toast.LENGTH_SHORT).show()
                        AsyncLogger.log(LogLevel.INFO, "Загрузка отменена пользователем из шторки")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                pendingResult.finish()
            }

            ACTION_DELETE_FILE -> {
                val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
                val notifId = intent.getIntExtra(EXTRA_NOTIF_ID, -1)

                if (itemId != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val targetItem = HistoryManager.getEntryById(context, itemId)
                            if (targetItem != null) {
                                HistoryManager.deleteFile(context, targetItem)
                            }
                            if (notifId != -1) {
                                notificationManager.cancel(notifId)
                            }
                        } catch (e: Exception) {
                            AsyncLogger.log(LogLevel.ERROR, "Ошибка удаления через пуш: ${e.message}")
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    pendingResult.finish()
                }
            }

            ACTION_SHARE_FILE -> {
                val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
                val mimeType = intent.getStringExtra(EXTRA_MIME_TYPE) ?: "*/*"

                if (itemId != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val item = HistoryManager.getEntryById(context, itemId)
                            if (item != null) {
                                val uri: Uri = if (item.pathOrUri.startsWith("content://")) {
                                    Uri.parse(item.pathOrUri)
                                } else {
                                    val file = java.io.File(item.pathOrUri)
                                    androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                }

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = mimeType
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                val receiverIntent = Intent(context, NotificationReceiver::class.java).apply {
                                    action = ACTION_SHARE_SUCCESS
                                    putExtra(EXTRA_ITEM_ID, itemId)
                                }

                                val pendingIntent = android.app.PendingIntent.getBroadcast(
                                    context, 0, receiverIntent,
                                    android.app.PendingIntent.FLAG_MUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                )

                                val chooserIntent = Intent.createChooser(shareIntent, "Поделиться через", pendingIntent.intentSender)
                                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooserIntent)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    pendingResult.finish()
                }
            }

            ACTION_SHARE_SUCCESS -> {
                val itemId = intent.getStringExtra(EXTRA_ITEM_ID)
                val isAutopilot = intent.getBooleanExtra(EXTRA_IS_AUTOPILOT, false)
                val transitAutoOnly = SettingsManager.transitModeAutopilotOnly.value

                if (itemId != null && SettingsManager.transitModeEnabled.value) {
                    if (transitAutoOnly && !isAutopilot) {
                        AsyncLogger.log(LogLevel.INFO, "Умный транзит пропущен: Файл скачан вручную, а включена опция 'Только автопилот'")
                        pendingResult.finish()
                        return
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val item = HistoryManager.getEntryById(context, itemId)
                            if (item != null) {
                                // 🧠 Усовершенствованный алгоритм "Умного транзита"
                                val isWifi = com.example.videodownloader.utils.isWifiConnected(context)
                                val avgSpeed = if (isWifi) SettingsManager.avgSpeedWifi.value else SettingsManager.avgSpeedMobile.value
                                
                                // Коэффициент запаса 2.5x (учитываем, что Upload обычно медленнее Download)
                                val estimatedUploadTimeMs = ((item.sizeBytes.toDouble() / avgSpeed) * 2500).toLong()
                                val baseSafetyTimeMs = 20000L
                                
                                val finalDelayMs = (estimatedUploadTimeMs + baseSafetyTimeMs).coerceIn(30000L, 600000L)

                                AsyncLogger.log(LogLevel.INFO, "Умный транзит: Планирование удаления через ${finalDelayMs / 1000}с для файла $itemId (Сеть: ${if (isWifi) "Wi-Fi" else "Mobile"})")

                                // 🚀 Гарантированное удаление через WorkManager
                                val cleanupRequest = androidx.work.OneTimeWorkRequestBuilder<CleanupWorker>()
                                    .setInitialDelay(finalDelayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
                                    .setInputData(androidx.work.workDataOf(CleanupWorker.KEY_ITEM_ID to itemId))
                                    .addTag("cleanup_$itemId")
                                    .build()

                                androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
                                    "cleanup_$itemId",
                                    androidx.work.ExistingWorkPolicy.REPLACE,
                                    cleanupRequest
                                )
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                } else {
                    pendingResult.finish()
                }
            }
            else -> pendingResult.finish()
        }
    }
}