package com.example.videodownloader

import android.content.Context
import java.io.File

object FFmpegManager {

    fun getFFmpegFile(context: Context): File {
        // На Android 10+ (API 29+) включена защита W^X. 
        // Мы не можем скачивать исполняемые файлы в папку filesDir и запускать их.
        // Поэтому мы "маскируем" ffmpeg под нативную библиотеку libffmpeg.so,
        // и Android при установке распаковывает ее с правильными правами доступа 
        // в папку nativeLibraryDir.
        val nativeDir = context.applicationInfo.nativeLibraryDir
        return File(nativeDir, "libffmpeg.so")
    }

    fun isInstalled(context: Context): Boolean {
        val file = getFFmpegFile(context)
        return file.exists() && file.canExecute()
    }

    suspend fun installFFmpeg(context: Context, onProgress: (String) -> Unit = {}): Result<Boolean> {
        // Больше не нужно скачивать из интернета в рантайме.
        // FFmpeg запакован в сам APK с помощью Gradle-таски во время сборки.
        // Этот метод оставлен для обратной совместимости.
        val installed = isInstalled(context)
        return if (installed) {
            Result.success(true)
        } else {
            Result.failure(Exception("Встроенный FFmpeg (libffmpeg.so) не найден в системе. Переустановите приложение."))
        }
    }
}