package com.example.videodownloader.logic

import android.content.Context
import com.example.videodownloader.OtaUpdater
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object OtaManager {
    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startUpdate(context: Context, onResult: (String) -> Unit) {
        if (_isUpdating.value) return
        _isUpdating.value = true
        val appContext = context.applicationContext
        scope.launch {
            val result = OtaUpdater.updateCore(appContext)
            _isUpdating.value = false
            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val msg = result.getOrNull() ?: ""
                    if (msg.startsWith("ALREADY_LATEST|")) {
                        val ver = msg.split("|")[1]
                        onResult("У вас установлена последняя версия (v$ver). Обновление не требуется!")
                    } else {
                        onResult("Ядро yt-dlp успешно обновлено до $msg")
                    }
                } else {
                    onResult("Ошибка обновления: ${result.exceptionOrNull()?.message}")
                }
            }
        }
    }
}
