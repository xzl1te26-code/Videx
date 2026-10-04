package com.example.videodownloader

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CleanupWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_ITEM_ID = "KEY_ITEM_ID"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val itemId = inputData.getString(KEY_ITEM_ID) ?: return@withContext Result.failure()
        
        try {
            val item = HistoryManager.getEntryById(context, itemId)
            if (item != null) {
                HistoryManager.deleteFile(context, item)
                AsyncLogger.log(LogLevel.INFO, "CleanupWorker: Файл $itemId успешно удален по расписанию")
            }
            Result.success()
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "CleanupWorker: Ошибка удаления файла $itemId: ${e.message}")
            Result.retry()
        }
    }
}
