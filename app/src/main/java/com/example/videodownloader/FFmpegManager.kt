package com.example.videodownloader

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

object FFmpegManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    // Официальные статические бинарники FFmpeg для разных архитектур
    private const val FFMPEG_ARM64_URL = "https://github.com/eugeneware/ffmpeg-static/releases/download/b6.0/ffmpeg-linux-arm64.gz"
    private const val FFMPEG_X86_64_URL = "https://github.com/eugeneware/ffmpeg-static/releases/download/b6.0/ffmpeg-linux-x64.gz"
    private const val FFMPEG_ARMV7_URL = "https://github.com/eugeneware/ffmpeg-static/releases/download/b6.0/ffmpeg-linux-arm.gz"
    private const val FFMPEG_X86_32_URL = "https://github.com/eugeneware/ffmpeg-static/releases/download/b6.0/ffmpeg-linux-ia32.gz"

    /**
     * Определение URL бинарника под архитектуру текущего устройства
     */
    private fun getDownloadUrlForDevice(): Pair<String, String> {
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull()?.lowercase() ?: "arm64-v8a"

        return when {
            primaryAbi.contains("x86_64") || primaryAbi.contains("amd64") -> {
                Pair(FFMPEG_X86_64_URL, "x86_64 (Intel/AMD/Emulator)")
            }
            primaryAbi.contains("arm64") || primaryAbi.contains("aarch64") -> {
                Pair(FFMPEG_ARM64_URL, "arm64-v8a (ARM 64-bit)")
            }
            primaryAbi.contains("x86") -> {
                Pair(FFMPEG_X86_32_URL, "x86 (Intel 32-bit)")
            }
            else -> {
                Pair(FFMPEG_ARMV7_URL, "armeabi-v7a (ARM 32-bit)")
            }
        }
    }

    fun getFFmpegFile(context: Context): File {
        val binDir = File(context.applicationContext.filesDir, "bin")
        if (!binDir.exists()) binDir.mkdirs()
        return File(binDir, "ffmpeg")
    }

    fun isInstalled(context: Context): Boolean {
        val appContext = context.applicationContext
        val file = getFFmpegFile(appContext)
        return file.exists() && file.length() > 5_000_000 && file.canExecute()
    }

    suspend fun installFFmpeg(context: Context, onProgress: (String) -> Unit = {}): Result<Boolean> = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        try {
            val targetFile = getFFmpegFile(appContext)
            if (isInstalled(appContext)) {
                return@withContext Result.success(true)
            }

            val (downloadUrl, archName) = getDownloadUrlForDevice()
            AsyncLogger.log(LogLevel.INFO, "FFmpeg: Определена архитектура $archName. Запуск загрузки...")
            onProgress("Загрузка FFmpeg ($archName)...")

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Mozilla/5.0 (Android; Videx)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Ошибка скачивания FFmpeg: HTTP ${response.code}")
            }

            val body = response.body ?: throw Exception("Пустой ответ от сервера загрузки")
            val tempFile = File(appContext.filesDir, "ffmpeg_extracted.tmp")

            // Распаковка GZ-архива на лету
            GZIPInputStream(body.byteStream()).use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output, bufferSize = 64 * 1024)
                }
            }

            if (targetFile.exists()) targetFile.delete()
            tempFile.renameTo(targetFile)

            // Выдаем права на выполнение файла (chmod 755)
            targetFile.setExecutable(true, false)
            targetFile.setReadable(true, false)
            try {
                Runtime.getRuntime().exec("chmod 755 ${targetFile.absolutePath}").waitFor()
            } catch (e: Exception) {
                AsyncLogger.log(LogLevel.WARN, "Предупреждение chmod: ${e.message}")
            }

            AsyncLogger.log(LogLevel.INFO, "FFmpeg ($archName) успешно установлен: ${targetFile.length() / (1024 * 1024)} МБ")
            Result.success(true)
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "Ошибка установки FFmpeg: ${e.message}")
            Result.failure(e)
        }
    }
}