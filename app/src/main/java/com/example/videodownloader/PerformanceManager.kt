package com.example.videodownloader

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.PowerManager
import android.view.FrameMetrics
import android.view.Window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import kotlin.math.roundToInt

object PerformanceManager {
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null
    private var frameMetricsListener: Window.OnFrameMetricsAvailableListener? = null
    private var isTracking = false

    private val _thermalStatus = MutableStateFlow(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) PowerManager.THERMAL_STATUS_NONE else -1)
    val thermalStatus: StateFlow<Int> = _thermalStatus

    private var lastJankLogTime = 0L
    private const val JANK_LOG_INTERVAL = 5000L // Логировать джанк не чаще чем раз в 5 секунд

    fun init(context: Context) {
        val appContext = context.applicationContext
        
        // Мониторинг термального статуса (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.addThermalStatusListener { status ->
                _thermalStatus.value = status
                val statusText = when (status) {
                    PowerManager.THERMAL_STATUS_NONE -> "Норма"
                    PowerManager.THERMAL_STATUS_LIGHT -> "Легкий нагрев"
                    PowerManager.THERMAL_STATUS_MODERATE -> "Умеренный нагрев"
                    PowerManager.THERMAL_STATUS_SEVERE -> "Сильный нагрев"
                    PowerManager.THERMAL_STATUS_CRITICAL -> "КРИТИЧЕСКИЙ НАГРЕВ"
                    PowerManager.THERMAL_STATUS_EMERGENCY -> "АВАРИЙНОЕ ОХЛАЖДЕНИЕ"
                    PowerManager.THERMAL_STATUS_SHUTDOWN -> "ВЫКЛЮЧЕНИЕ"
                    else -> "Неизвестно"
                }
                AsyncLogger.log(LogLevel.PERF, "Термальный статус: $statusText")
            }
        }
    }

    fun startTracking(activity: Activity) {
        if (isTracking) return
        isTracking = true

        if (handlerThread == null) {
            handlerThread = HandlerThread("FrameMetricsThread").apply { start() }
            handler = Handler(handlerThread!!.looper)
        }

        val window = activity.window
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.display
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        }

        val refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.mode?.refreshRate ?: 60f
        } else {
            @Suppress("DEPRECATION")
            display?.refreshRate ?: 60f
        }

        val frameTimeThresholdNs = (1000_000_000L / refreshRate).toLong()
        // Считаем джанком, если кадр занял более 2-х интервалов VSync (но не менее 16мс для экранов 144Hz+)
        val jankThresholdNs = maxOf(frameTimeThresholdNs * 2, 16_000_000L) 

        val listener = object : Window.OnFrameMetricsAvailableListener {
            override fun onFrameMetricsAvailable(
                window: Window?,
                frameMetrics: FrameMetrics?,
                dropCountSinceLastInvocation: Int
            ) {
                if (frameMetrics == null) return
                
                val totalDuration = frameMetrics.getMetric(FrameMetrics.TOTAL_DURATION)
                
                if (totalDuration > jankThresholdNs) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastJankLogTime > JANK_LOG_INTERVAL) {
                        lastJankLogTime = currentTime
                        val durationMs = totalDuration / 1_000_000
                        val cpuDurationMs = frameMetrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION) / 1_000_000
                        val gpuDurationMs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            frameMetrics.getMetric(FrameMetrics.GPU_DURATION) / 1_000_000
                        } else {
                            -1L
                        }
                        
                        val gpuText = if (gpuDurationMs >= 0) "GPU: ${gpuDurationMs}мс" else "GPU: N/A"
                        val batterySaver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            val pm = activity.getSystemService(Context.POWER_SERVICE) as? PowerManager
                            pm?.isPowerSaveMode == true
                        } else false
                        
                        val batteryText = if (batterySaver) "[Энергосбережение]" else ""
                        
                        AsyncLogger.log(
                            LogLevel.PERF, 
                            "UI Jank: ${durationMs}мс (Layout: ${cpuDurationMs}мс, $gpuText). $batteryText " +
                            "Drops: $dropCountSinceLastInvocation"
                        )
                        
                        if (durationMs > 100) logMemoryStatus(activity)
                    }
                }
            }
        }
        
        frameMetricsListener = listener
        window.addOnFrameMetricsAvailableListener(listener, handler)
        
        AsyncLogger.log(LogLevel.PERF, "Start UI Monitor: ${refreshRate.roundToInt()}Hz (Threshold: ${jankThresholdNs / 1_000_000}ms)")
    }

    private fun logMemoryStatus(context: Context) {
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMem = runtime.maxMemory() / (1024 * 1024)
        AsyncLogger.log(LogLevel.PERF, "Память приложения: $usedMem МБ / $maxMem МБ")
    }

    fun stopTracking(activity: Activity) {
        if (!isTracking) return
        isTracking = false
        
        try {
            frameMetricsListener?.let {
                activity.window.removeOnFrameMetricsAvailableListener(it)
            }
        } catch (e: Exception) {
            // Игнорируем ошибки при удалении слушателя, если окно уже невалидно
        }
        
        frameMetricsListener = null

        // Завершаем фоновый поток мониторинга
        try {
            handlerThread?.quitSafely()
        } catch (e: Exception) {
            // Игнорируем ошибки при завершении потока
        }
        
        handlerThread = null
        handler = null
    }
}
