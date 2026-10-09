package com.example.videodownloader.logic

import com.example.videodownloader.PlaylistItem
import com.example.videodownloader.YtDlpBridge
import com.example.videodownloader.VideoMetadata
import com.example.videodownloader.AsyncLogger
import com.example.videodownloader.LogLevel
import com.example.videodownloader.CacheManager
import com.example.videodownloader.utils.isValidUrl
import android.content.Context
import coil.Coil
import coil.request.ImageRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import org.json.JSONArray
import java.io.File

object AnalysisManager {
    private var appContext: Context? = null

    private val _url = MutableStateFlow(value = "")
    val url: StateFlow<String> = _url

    private val _isAnalyzing = MutableStateFlow(value = false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    private val _resultTitle = MutableStateFlow(value = "")
    val resultTitle: StateFlow<String> = _resultTitle

    private val _resultThumbnail = MutableStateFlow(value = "")
    val resultThumbnail: StateFlow<String> = _resultThumbnail

    private val _isPhotoPost = MutableStateFlow(value = false)
    val isPhotoPost: StateFlow<Boolean> = _isPhotoPost

    private val _errorMessage = MutableStateFlow(value = "")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _availableQualities = MutableStateFlow<List<String>>(value = emptyList())
    val availableQualities: StateFlow<List<String>> = _availableQualities

    private val _isPlaylist = MutableStateFlow(value = false)
    val isPlaylist: StateFlow<Boolean> = _isPlaylist

    private val _playlistEntries = MutableStateFlow<List<PlaylistItem>>(value = emptyList())
    val playlistEntries: StateFlow<List<PlaylistItem>> = _playlistEntries

    // Краткосрочный кэш результатов анализа (5 минут)
    private val analysisCache = mutableMapOf<String, Pair<VideoMetadata, Long>>()
    private const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L // 5 минут

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var analyzeJob: Job? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun setUrl(newUrl: String, autoAnalyze: Boolean = true) {
        val trimmed = newUrl.trim()
        _url.value = newUrl

        if (trimmed.isBlank()) {
            clear()
        } else if (isValidUrl(trimmed)) {
            _errorMessage.value = ""
            if (autoAnalyze) {
                startBackgroundAnalysis(trimmed)
            } else {
                analyzeJob?.cancel()
                _isAnalyzing.value = false
                _resultTitle.value = ""
                _resultThumbnail.value = ""
                _isPhotoPost.value = false
            }
        } else {
            analyzeJob?.cancel()
            _isAnalyzing.value = false
            _resultTitle.value = ""
            _resultThumbnail.value = ""
            _isPhotoPost.value = false
            if (trimmed.length > 3) {
                _errorMessage.value = "Введена некорректная ссылка. Она должна начинаться с http:// или https://"
            }
        }
    }

    fun clear() {
        analyzeJob?.cancel()
        _url.value = ""
        _resultTitle.value = ""
        _resultThumbnail.value = ""
        _isPhotoPost.value = false
        _errorMessage.value = ""
        _isAnalyzing.value = false
        _availableQualities.value = emptyList()
        _isPlaylist.value = false
        _playlistEntries.value = emptyList()
    }

    fun clearError() {
        _errorMessage.value = ""
    }

    private fun startBackgroundAnalysis(targetUrl: String) {
        if (analyzeJob?.isActive == true && _url.value == targetUrl) return // Не перезапускаем текущий анализ
        analyzeJob?.cancel()

        // 1. Проверка оперативной памяти
        val cached = analysisCache[targetUrl]
        if (cached != null && (System.currentTimeMillis() - cached.second < CACHE_EXPIRATION_MS)) {
            applyMetadata(cached.first)
            AsyncLogger.log(LogLevel.INFO, "Мгновенный анализ (RAM) для: $targetUrl")
            return
        }

        // 2. Проверка сохраненных данных на диске
        // Если в RAM нет, пробуем прочитать JSON файл напрямую без Python
        appContext?.let { ctx ->
            val passportFile = CacheManager.getPassportFile(ctx, targetUrl)
            if (passportFile.exists() && (System.currentTimeMillis() - passportFile.lastModified() < CACHE_EXPIRATION_MS)) {
                try {
                    val jsonStr = passportFile.readText()
                    val meta = parseMetadataFromJson(jsonStr)
                    if (meta != null) {
                        analysisCache[targetUrl] = meta to passportFile.lastModified()
                        applyMetadata(meta)
                        AsyncLogger.log(LogLevel.INFO, "Мгновенный анализ (DISK) для: $targetUrl")
                        return
                    }
                } catch (e: Exception) {
                    AsyncLogger.log(LogLevel.WARN, "Ошибка чтения паспорта: ${e.message}")
                }
            }
        }

        _isAnalyzing.value = true
        _errorMessage.value = ""
        _resultTitle.value = ""
        _resultThumbnail.value = ""
        _isPhotoPost.value = false
        _availableQualities.value = emptyList()
        _isPlaylist.value = false
        _playlistEntries.value = emptyList()

        // Проверка наличия активного подключения к интернету
        if (appContext != null && !com.example.videodownloader.utils.isNetworkAvailable(appContext)) {
            _isAnalyzing.value = false
            _errorMessage.value = "Отсутствует подключение к интернету. Проверьте сеть и повторите попытку."
            return
        }

        analyzeJob = scope.launch {
            try {
                // Ограничение времени ожидания ответа от сервера до 25 секунд
                val result = withTimeoutOrNull(25000L) {
                    YtDlpBridge.fetchInfo(targetUrl)
                }

                //Если анализ был отменен пользователем — выходим без показа ошибок
                if (!isActive) return@launch

                _isAnalyzing.value = false

                if (result != null) {
                    if (result.isSuccess) {
                        val meta = result.getOrNull()!!
                        
                        // Сохраняем в кэш
                        analysisCache[targetUrl] = meta to System.currentTimeMillis()
                        if (analysisCache.size > 5) {
                            analysisCache.remove(analysisCache.keys.first())
                        }

                        applyMetadata(meta)

                        // Предзагрузка обложки
                        appContext?.let { ctx ->
                            if (meta.thumbnailUrl.isNotBlank()) {
                                val request = ImageRequest.Builder(ctx)
                                    .data(meta.thumbnailUrl)
                                    .crossfade(true)
                                    .build()
                                Coil.imageLoader(ctx).enqueue(request)
                            }
                        }
                    } else {
                        val rawErr = result.exceptionOrNull()?.message
                        _errorMessage.value = com.example.videodownloader.utils.translateNetworkError(rawErr, "Не удалось распознать ссылку")
                    }
                } else {
                    _errorMessage.value = "Превышено время ожидания ответа (25 сек). Проверьте интернет-соединение или повторите попытку."
                }
            } catch (e: Exception) {
                _isAnalyzing.value = false
                if (e is CancellationException || !isActive) throw e
                val rawErr = e.message
                _errorMessage.value = com.example.videodownloader.utils.translateNetworkError(rawErr, "Ошибка анализа ссылки")
            }
        }
    }

    private fun applyMetadata(meta: VideoMetadata) {
        _resultTitle.value = meta.title
        _resultThumbnail.value = meta.thumbnailUrl
        _isPhotoPost.value = meta.isPhotoPost
        _availableQualities.value = meta.availableQualities
        _isPlaylist.value = meta.isPlaylist
        _playlistEntries.value = meta.playlistEntries
        _isAnalyzing.value = false
    }

    private fun parseMetadataFromJson(jsonStr: String): VideoMetadata? {
        return try {
            val json = JSONObject(jsonStr)
            val title = json.optString("title", "Медиа")
            val thumb = json.optString("thumbnail", "")
            val isPhoto = json.optBoolean("is_photo", false)
            val isPlaylist = json.optBoolean("is_playlist", false)
            
            val qualitiesList = mutableListOf<String>()
            val qArr = json.optJSONArray("available_qualities")
            if (qArr != null) {
                for (i in 0 until qArr.length()) qualitiesList.add(qArr.getString(i))
            }
            
            val playlistEntries = mutableListOf<PlaylistItem>()
            val pArr = json.optJSONArray("playlist_entries")
            if (pArr != null) {
                for (i in 0 until pArr.length()) {
                    val obj = pArr.getJSONObject(i)
                    playlistEntries.add(PlaylistItem(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        url = obj.optString("url", ""),
                        thumbnailUrl = obj.optString("thumbnail", ""),
                        duration = obj.optString("duration", ""),
                        author = obj.optString("author", "")
                    ))
                }
            }
            
            VideoMetadata(title, thumb, isPhoto, qualitiesList, isPlaylist, playlistEntries)
        } catch (_: Exception) {
            null
        }
    }

    fun onIncomingUrl(newUrl: String, autoAnalyze: Boolean) {
        _url.value = newUrl
        if (autoAnalyze && isValidUrl(newUrl)) {
            startBackgroundAnalysis(newUrl.trim())
        }
    }
}
