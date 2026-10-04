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
        AsyncLogger.init(this)
        PerformanceManager.init(this)
        YtDlpBridge.init(this)
        AnalysisManager.init(this)
        
        // Фоновая инициализация настроек
        CoroutineScope(Dispatchers.IO).launch {
            SettingsManager.init(this@App)
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