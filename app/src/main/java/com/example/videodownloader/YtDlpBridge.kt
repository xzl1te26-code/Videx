package com.example.videodownloader

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import com.chaquo.python.PyException
import com.chaquo.python.Python
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.milliseconds
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream

// ⭐️ Модель элемента плейлиста
@Immutable
data class PlaylistItem(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val duration: String,
    val author: String,
)

@Immutable
data class VideoMetadata(
    val title: String,
    val thumbnailUrl: String,
    val isPhotoPost: Boolean = false,
    val availableQualities: List<String> = emptyList(),
    val isPlaylist: Boolean = false, // ⭐️ Флаг плейлиста
    val playlistEntries: List<PlaylistItem> = emptyList() // ⭐️ Список роликов
)

interface DownloadProgressListener {
    fun onProgressDetailed(percent: Int, speedBytes: Long, etaSeconds: Long)
    fun isCancelled(): Boolean
}

object YtDlpBridge {

    private var appContext: Context? = null
    private const val BUFFER_SIZE = 64 * 1024

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private var isPythonReady = false
    
    fun isReady(): Boolean = isPythonReady

    /**
     * ⭐️ Умный прогрев Python в фоновом потоке с низким приоритетом CPU.
     * Вызывается с задержкой после запуска UI, чтобы не создавать лагов и фризов при старте.
     */
    fun warmUp(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Устанавливаем фоновый приоритет для потока прогрева,
                // чтобы OS отдавала высший приоритет UI и RenderThread
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)

                // 2. Откладываем тяжелый импорт Python на 3.5 сек, давая Compose полностью прорисовать интерфейс
                delay(3500.milliseconds)

                if (!Python.isStarted()) {
                    AsyncLogger.log(LogLevel.INFO, "Python: Фоновый незаметный старт рантайма...")
                }
                
                val py = Python.getInstance()
                val module = py.getModule("ytdlp_wrapper")

                // 3. Подключаем ZIP-ядро
                val latestCore = File(appContext.filesDir, "ytdlp_latest.zip")
                if (latestCore.exists()) {
                    module.callAttr("set_custom_core_path", latestCore.absolutePath)
                }
                
                // 4. Выполняем фоновый предзагруз
                val result = module.callAttr("pre_load").asMap()
                val status = result[py.builtins.callAttr("str", "status")]?.toString()
                
                if ((status == "success") || (status == "already_warm")) {
                    isPythonReady = true
                    AsyncLogger.log(LogLevel.INFO, "Python: Движок полностью прогрет и yt-dlp загружен.")
                } else {
                    val err = result[py.builtins.callAttr("str", "error_msg")]?.toString() ?: "Unknown error"
                    throw Exception(err)
                }
            } catch (e: Exception) {
                isPythonReady = false
                AsyncLogger.log(LogLevel.WARN, "Python Warm-up fail: ${e.message}")
            }
        }
    }

    private fun ensureCustomCore(context: Context?, module: com.chaquo.python.PyObject) {
        if (context == null) return
        try {
            val latestCore = File(context.filesDir, "ytdlp_latest.zip")
            if (latestCore.exists()) {
                module.callAttr("set_custom_core_path", latestCore.absolutePath)
            }
        } catch (_: Exception) {}
    }

    suspend fun fetchInfo(url: String): Result<VideoMetadata> = withContext(Dispatchers.IO) {
        try {
            val py = Python.getInstance()
            val module = py.getModule("ytdlp_wrapper")
            ensureCustomCore(appContext, module)

            val cookiePath = appContext?.let { AuthManager.exportCookies(it)?.absolutePath } ?: ""
            val useImpersonate = SettingsManager.useImpersonate.value

            AsyncLogger.log(LogLevel.INFO, "Парсинг ссылки: $url")
            val passportPath = appContext?.let { CacheManager.getPassportFile(it, url).absolutePath } ?: ""
            val result = module.callAttr("get_video_info", url, cookiePath, useImpersonate, passportPath).asMap()

            if (result[py.builtins.callAttr("str", "status")]?.toString() == "success") {
                val title = result[py.builtins.callAttr("str", "title")]?.toString() ?: "Без названия"
                val thumb = result[py.builtins.callAttr("str", "thumbnail")]?.toString() ?: ""
                val isPhoto = result[py.builtins.callAttr("str", "is_photo")]?.toBoolean() ?: false
                val isPlaylist = result[py.builtins.callAttr("str", "is_playlist")]?.toBoolean() ?: false

                val qualitiesJson = result[py.builtins.callAttr("str", "available_qualities")]?.toString() ?: "[]"
                val qualitiesList = mutableListOf<String>()
                try {
                    val arr = JSONArray(qualitiesJson)
                    for (i in 0 until arr.length()) qualitiesList.add(arr.getString(i))
                } catch (_: Exception) {}

                // Парсинг элементов плейлиста
                val playlistEntries = mutableListOf<PlaylistItem>()
                if (isPlaylist) {
                    val entriesJson = result[py.builtins.callAttr("str", "playlist_entries")]?.toString() ?: "[]"
                    try {
                        val arr = JSONArray(entriesJson)
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            playlistEntries.add(
                                PlaylistItem(
                                    id = obj.optString("id", i.toString()),
                                    title = obj.optString("title", "Видео ${i + 1}"),
                                    url = obj.optString("url", url),
                                    thumbnailUrl = obj.optString("thumbnail", ""),
                                    duration = obj.optString("duration", ""),
                                    author = obj.optString("author", "")
                                )
                            )
                        }
                    } catch (e: Exception) {
                        AsyncLogger.log(LogLevel.WARN, "Ошибка парсинга элементов плейлиста: ${e.message}")
                    }
                }

                AsyncLogger.log(LogLevel.INFO, "Найдено: $title | Плейлист: $isPlaylist (${playlistEntries.size} эл.)")
                Result.success(VideoMetadata(title, thumb, isPhoto, qualitiesList, isPlaylist, playlistEntries))
            } else {
                val err = result[py.builtins.callAttr("str", "error_msg")]?.toString() ?: "Не удалось обработать ссылку"
                AsyncLogger.log(LogLevel.ERROR, "Ошибка yt-dlp: $err")
                Result.failure(Exception(err))
            }
        } catch (e: PyException) {
            AsyncLogger.log(LogLevel.ERROR, "Python Exception: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "Системная ошибка: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun downloadVideo(
        context: Context,
        url: String,
        isAudioOnly: Boolean = false,
        quality: String = "best",
        threads: Int = 4,
        rateLimit: Long = 0L,
        throttledRate: Long = 102400L,
        customFilename: String? = null,
        thumbnailUrl: String? = null,
        passportPath: String = "",
        isCancelled: () -> Boolean = { false },
        onProgress: (percent: Int, speedBytes: Long, etaSeconds: Long) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val cacheDir = File(appContext.cacheDir, "downloads_temp")
        try {
            cacheDir.mkdirs()
            val py = Python.getInstance()
            val module = py.getModule("ytdlp_wrapper")
            ensureCustomCore(appContext, module)

            val cookiePath = AuthManager.exportCookies(appContext)?.absolutePath ?: ""
            val ffmpegFile = FFmpegManager.getFFmpegFile(appContext)
            val ffmpegPath = if (FFmpegManager.isInstalled(appContext)) ffmpegFile.absolutePath else ""
            
            val useSponsorBlock = SettingsManager.useSponsorBlock.value
            val useImpersonate = SettingsManager.useImpersonate.value

            val listener = object : DownloadProgressListener {
                override fun onProgressDetailed(percent: Int, speedBytes: Long, etaSeconds: Long) {
                    onProgress(percent, speedBytes, etaSeconds)
                }

                override fun isCancelled(): Boolean {
                    return isCancelled()
                }
            }

            val result = module.callAttr(
                "download_video",
                url,
                cacheDir.absolutePath,
                listener,
                isAudioOnly,
                quality,
                threads,
                rateLimit,
                throttledRate,
                cookiePath,
                ffmpegPath,
                customFilename,
                useSponsorBlock,
                useImpersonate,
                passportPath
            ).asMap()

            val status = result[py.builtins.callAttr("str", "status")]?.toString()

            if (status == "cancelled") {
                AsyncLogger.log(LogLevel.INFO, "Загрузка отменена")
                return@withContext Result.failure(CancellationException("Download cancelled"))
            }

            if (status == "success") {
                val isPhoto = result[py.builtins.callAttr("str", "is_photo")]?.toBoolean() ?: false
                val allFilesJson = result[py.builtins.callAttr("str", "all_files")]?.toString() ?: "[]"
                val jsonArr = JSONArray(allFilesJson)
                val downloadedFiles = (0 until jsonArr.length()).map { File(jsonArr.getString(it)) }

                val customUri = SettingsManager.getCustomDirUri()
                val saveToGallery = SettingsManager.saveToGallery.value

                var firstSavedFile: File? = null

                fun cleanFileNameForHistory(rawName: String): String {
                    var clean = rawName.replace(Regex("\\s*\\[[a-zA-Z0-9_-]{5,}\\]$"), "").trim()
                    if (clean.startsWith("Анализ ")) {
                        clean = clean.replace(Regex("^Анализ\\s+\\w+\\s*[-._]*"), "").trim()
                    }
                    return if (clean.isBlank()) "Медиафайл" else clean
                }

                for (srcFile in downloadedFiles) {
                    if (!srcFile.exists()) continue
                    val fileSize = srcFile.length()

                    // Умное определение MIME-типа на основе расширения после Remux
                    val ext = srcFile.extension.lowercase()
                    val mimeType = when {
                        isPhoto -> "image/jpeg"
                        isAudioOnly -> "audio/mp4"
                        ext == "mkv" -> "video/x-matroska"
                        else -> "video/mp4"
                    }

                    val mediaType = when {
                        isPhoto -> MediaType.PHOTO
                        isAudioOnly -> MediaType.AUDIO
                        else -> MediaType.VIDEO
                    }

                    if (customUri != null) {
                        val treeDoc = DocumentFile.fromTreeUri(appContext, customUri)
                        val newDoc = treeDoc?.createFile(mimeType, srcFile.name)
                            ?: throw Exception("Не удалось создать файл в выбранной папке")

                        appContext.contentResolver.openOutputStream(newDoc.uri)?.use { output ->
                            FileInputStream(srcFile).use { input ->
                                input.copyTo(output, bufferSize = BUFFER_SIZE)
                            }
                        }
                        srcFile.delete()
                        if (firstSavedFile == null) firstSavedFile = File(srcFile.name)

                        val cleanHistoryName = cleanFileNameForHistory(srcFile.nameWithoutExtension)

                        HistoryManager.addEntry(
                            context = appContext,
                            pathOrUri = newDoc.uri.toString(),
                            name = cleanHistoryName,
                            sizeBytes = fileSize,
                            mediaType = mediaType,
                            sourceUrl = url,
                            thumbnailUrl = thumbnailUrl
                        )
                    } else {
                        val savedUriOrPath = saveToPublicStorage(
                            context = appContext,
                            srcFile = srcFile,
                            mimeType = mimeType,
                            mediaType = mediaType,
                            saveToGallery = saveToGallery
                        )

                        if (firstSavedFile == null) firstSavedFile = File(srcFile.name)
                        srcFile.delete()

                        val cleanHistoryName = cleanFileNameForHistory(srcFile.nameWithoutExtension)

                        HistoryManager.addEntry(
                            context = appContext,
                            pathOrUri = savedUriOrPath,
                            name = cleanHistoryName,
                            sizeBytes = fileSize,
                            mediaType = mediaType,
                            sourceUrl = url,
                            thumbnailUrl = thumbnailUrl
                        )
                    }
                }

                AsyncLogger.log(LogLevel.INFO, "Успешно сохранено файлов: ${downloadedFiles.size}")
                Result.success(firstSavedFile ?: File(cacheDir, "done"))
            } else {
                val err = result[py.builtins.callAttr("str", "error_msg")]?.toString() ?: "Неизвестная ошибка"
                AsyncLogger.log(LogLevel.ERROR, "Ошибка при скачивании: $err")
                Result.failure(Exception(err))
            }
        } catch (e: PyException) {
            if (e.message?.contains("DOWNLOAD_CANCELLED") == true) {
                Result.failure(CancellationException("Download cancelled"))
            } else {
                AsyncLogger.log(LogLevel.ERROR, "Python краш: ${e.message}")
                Result.failure(e)
            }
        } catch (e: CancellationException) {
            Result.failure(e)
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "Системный сбой: ${e.message}")
            Result.failure(e)
        } finally {
            try {
                if (cacheDir.exists()) {
                    cacheDir.listFiles()?.forEach { it.delete() }
                }
            } catch (e: Exception) {
                AsyncLogger.log(LogLevel.WARN, "Ошибка очистки кэша: ${e.message}")
            }
        }
    }

    private fun saveToPublicStorage(
        context: Context,
        srcFile: File,
        mimeType: String,
        mediaType: MediaType,
        saveToGallery: Boolean
    ): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, srcFile.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.IS_PENDING, 1)

                val relativeDir = when {
                    !saveToGallery -> Environment.DIRECTORY_DOWNLOADS + "/Videx"
                    mediaType == MediaType.PHOTO -> Environment.DIRECTORY_PICTURES + "/Videx"
                    mediaType == MediaType.AUDIO -> Environment.DIRECTORY_MUSIC + "/Videx"
                    else -> Environment.DIRECTORY_MOVIES + "/Videx"
                }
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativeDir)
            }

            val collectionUri = when {
                !saveToGallery -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
                mediaType == MediaType.PHOTO -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                mediaType == MediaType.AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            val itemUri = context.contentResolver.insert(collectionUri, contentValues)
                ?: throw Exception("Не удалось создать запись в MediaStore")

            context.contentResolver.openOutputStream(itemUri)?.use { out ->
                FileInputStream(srcFile).use { input ->
                    input.copyTo(out, bufferSize = BUFFER_SIZE)
                }
            }

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(itemUri, contentValues, null, null)

            return itemUri.toString()
        } else {
            val baseDir = when {
                !saveToGallery -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                mediaType == MediaType.PHOTO -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                mediaType == MediaType.AUDIO -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                else -> Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            }
            val targetDir = File(baseDir, "Videx").apply { mkdirs() }
            val destFile = File(targetDir, srcFile.name)

            FileInputStream(srcFile).use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = BUFFER_SIZE)
                }
            }

            if (saveToGallery) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf(mimeType),
                    null
                )
            }

            return destFile.absolutePath
        }
    }
}