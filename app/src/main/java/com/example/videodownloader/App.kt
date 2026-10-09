package com.example.videodownloader

import com.chaquo.python.android.PyApplication
import com.example.videodownloader.logic.AnalysisManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : PyApplication() {
    override fun onCreate() {
        super.onCreate()

        // 1. Инициализация менеджеров
        SettingsManager.init(this) // Синхронно, чтобы избежать мерцания темы на старте
        AsyncLogger.init(this)
        PerformanceManager.init(this)
        YtDlpBridge.init(this)
        AnalysisManager.init(this)
        
        // Фоновые долгие задачи
        CoroutineScope(Dispatchers.IO).launch {
            YtDlpBridge.warmUp(this@App)
            CacheManager.cleanupPassports(this@App)
        }

        // 2. Глобальный перехватчик сбоев
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            AsyncLogger.log(LogLevel.ERROR, "КРИТИЧЕСКИЙ СБОЙ: ${throwable.stackTraceToString()}")
            defaultHandler?.uncaughtException(thread, throwable)
        }

        AsyncLogger.log(LogLevel.INFO, "Приложение и Python-движок успешно запущены!")
    }
}