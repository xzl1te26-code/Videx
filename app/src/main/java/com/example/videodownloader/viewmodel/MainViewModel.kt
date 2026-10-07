package com.example.videodownloader.viewmodel

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.videodownloader.HistoryManager
import com.example.videodownloader.SettingsManager
import com.example.videodownloader.logic.AnalysisManager
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import com.example.videodownloader.DownloadedFileItem
import com.example.videodownloader.MediaType

enum class HistorySortOrder {
    NEWEST,          // Сначала новые (По умолчанию)
    OLDEST,          // Сначала старые
    SIZE_LARGEST,    // Сначала большие по весу
    SIZE_SMALLEST,   // Сначала легкие по весу
    DURATION_LONGEST // Самые длинные (для аудио/видео)
}

class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    
    // Хранилище состояния истории
    private val _historyState = MutableStateFlow<List<DownloadedFileItem>>(emptyList())
    val historyState: StateFlow<List<DownloadedFileItem>> = _historyState.asStateFlow()

    // Состояние экрана истории
    var historySearchQuery by mutableStateOf("")
    var historySelectedTypeFilter by mutableStateOf<MediaType?>(null)
    var historySortOrder by mutableStateOf(HistorySortOrder.NEWEST)
    
    // Состояние скролла списка истории
    var historyScrollIndex by mutableIntStateOf(0)
    var historyScrollOffset by mutableIntStateOf(0)

    // Текущий активный экран
    var currentScreen by mutableStateOf(com.example.videodownloader.Screen.Home)

    // Кэш для превью смарт-детектора (чтобы не переигрывать анимацию при возврате на экран)
    var cachedClipboardUrl by mutableStateOf<String?>(null)
    var cachedClipboardMetadata by mutableStateOf<com.example.videodownloader.VideoMetadata?>(null)

    // Флаг однократного воспроизведения анимации кадров за сессию
    var hasTipAnimationPlayedThisSession by mutableStateOf(false)
    var lastTipConfigVersion by mutableIntStateOf(-1)

    // 📋 Отклоненная ссылка из буфера обмена (персистентное состояние между вкладками)
    var dismissedClipboardUrl by mutableStateOf<String?>(null)

    fun initHistoryCollection(context: Context) {
        viewModelScope.launch {
            HistoryManager.getHistoryFlow(context).collect { list ->
                _historyState.value = list
            }
        }
    }

    var selectedVideoQuality by mutableStateOf(
        savedStateHandle.get<String>("video_quality") ?: SettingsManager.preferredVideoQuality.value
    )
        private set

    fun updateVideoQuality(quality: String) {
        selectedVideoQuality = quality
        savedStateHandle["video_quality"] = quality
    }

    // Качество аудио (персистивное)
    var selectedAudioQuality by mutableStateOf(
        savedStateHandle.get<String>("audio_quality") ?: "best"
    )
        private set

    fun updateAudioQuality(quality: String) {
        selectedAudioQuality = quality
        savedStateHandle["audio_quality"] = quality
    }

    // Состояние UI диалогов (не обязательно персистить, но можно для UX)
    var showQualityDialog by mutableStateOf(false)
    var showPlaylistSheet by mutableStateOf(false)

    // Статус дубликата
    var isDuplicateDetected by mutableStateOf(false)

    init {
        // Восстановление URL из сохраненного состояния, если приложение было убито системой
        val savedUrl = savedStateHandle.get<String>("current_url")
        if (!savedUrl.isNullOrBlank() && AnalysisManager.url.value.isBlank()) {
            AnalysisManager.setUrl(savedUrl)
        }

        // Следим за изменениями глобального URL, чтобы сохранить его
        viewModelScope.launch {
            AnalysisManager.url.collectLatest { url ->
                savedStateHandle["current_url"] = url
            }
        }

        // Автосброс качества на дефолтное при новом анализе
        viewModelScope.launch {
            AnalysisManager.isAnalyzing.collectLatest { analyzing ->
                if (analyzing) {
                    val defaultQuality = SettingsManager.preferredVideoQuality.value
                    updateVideoQuality(defaultQuality)
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    fun checkDuplicate(context: Context, checkDuplicates: Boolean) {
        viewModelScope.launch {
            // Объединяем потоки URL и Заголовка, чтобы проверять дубликат только когда данные стабильны
            combine(AnalysisManager.url, AnalysisManager.resultTitle) { url, title ->
                url to title
            }.debounce(400) // Ждем 400мс затишья (чтобы не моргало при вводе)
             .collectLatest { (url, title) ->
                if (checkDuplicates && (url.isNotBlank() || title.isNotBlank())) {
                    isDuplicateDetected = HistoryManager.isDuplicate(context, url, title)
                } else {
                    isDuplicateDetected = false
                }
            }
        }
    }
}
