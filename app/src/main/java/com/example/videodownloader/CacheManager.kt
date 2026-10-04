package com.example.videodownloader

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

object CacheManager {

    /**
     * Асинхронный подсчет размера временных файлов кэша
     */
    suspend fun getCacheSizeBytes(context: Context): Long = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        var totalSize = 0L
        try {
            totalSize += getDirSize(appContext.cacheDir)
            val externalCache = appContext.externalCacheDir
            if (externalCache != null) {
                totalSize += getDirSize(externalCache)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        totalSize
    }

    private fun getDirSize(dir: File?): Long {
        if (dir == null || !dir.exists()) return 0L
        var size = 0L
        val files = dir.listFiles() ?: return 0L
        for (file in files) {
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 КБ"
        val mb = bytes / (1024.0 * 1024.0)
        return when {
            mb >= 1024 -> String.format(Locale.getDefault(), "%.2f ГБ", mb / 1024.0)
            mb >= 0.1 -> String.format(Locale.getDefault(), "%.1f МБ", mb)
            else -> String.format(Locale.getDefault(), "%d КБ", bytes / 1024)
        }
    }

    /**
     * Безопасная очистка всех временных файлов
     */
    suspend fun clearCache(context: Context): Boolean = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        try {
            // Не удаляем файл куки yt-dlp
            appContext.cacheDir.listFiles()?.forEach { file ->
                if (file.name != "yt_dlp_cookies.txt" && file.name != "passports") {
                    file.deleteRecursively()
                }
            }
            appContext.externalCacheDir?.deleteRecursively()
            
            AsyncLogger.log(LogLevel.INFO, "Временный кэш успешно очищен (авторизация сохранена)")
            true
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "Ошибка очистки кэша: ${e.message}")
            false
        }
    }

    /**
     * 🧠 Получить путь к файлу "паспорта" для конкретной ссылки.
     * Используется для мгновенного старта загрузки без повторного анализа.
     */
    fun getPassportFile(context: Context, url: String): File {
        val dir = File(context.cacheDir, "passports")
        if (!dir.exists()) dir.mkdirs()
        
        // Создаем уникальное имя файла на основе хеша ссылки
        val fileName = "passport_${url.hashCode()}.json"
        return File(dir, fileName)
    }

    /**
     * Удалить устаревшие паспорта (старше 24 часов)
     */
    suspend fun cleanupPassports(context: Context) = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "passports")
        if (!dir.exists()) return@withContext
        
        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > 24 * 60 * 60 * 1000) {
                file.delete()
            }
        }
    }
}