package com.example.videodownloader.utils

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import com.chaquo.python.Python
import com.example.videodownloader.*
import java.io.File

fun exportSmartDiagnosticReport(context: Context, logs: List<LogEntry>) {
    try {
        val ytdlpVer = try {
            val py = Python.getInstance()
            py.getModule("yt_dlp.version").get("__version__")?.toString() ?: "Встроенное"
        } catch (_: Exception) { "Недоступно" }

        val appVerName = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName
        } catch (_: Exception) { "unknown" }

        val isFFmpeg = FFmpegManager.isInstalled(context)
        val isWifi = isWifiConnected(context)
        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"

        val totalMemMb = Runtime.getRuntime().totalMemory() / (1024 * 1024)
        val freeMemMb = Runtime.getRuntime().freeMemory() / (1024 * 1024)
        
        val internalFree = java.io.File(context.filesDir.path).freeSpace / (1024 * 1024)
        val displayMetrics = context.resources.displayMetrics
        val screenSize = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels} (${displayMetrics.densityDpi}dpi)"

        val errors = logs.filter { it.level == LogLevel.ERROR }.takeLast(8)
        val performanceLogs = logs.filter { it.level == LogLevel.PERF }.takeLast(15)
        val recentLogs = logs.takeLast(30)

        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("   РАСШИРЕННЫЙ ДИАГНОСТИЧЕСКИЙ ОТЧЕТ      \n")
        sb.append("=========================================\n\n")

        sb.append("📱 ПРИЛОЖЕНИЕ И СИСТЕМА:\n")
        sb.append("• Приложение: VideoDownloader v$appVerName\n")
        sb.append("• Модель: ${Build.MANUFACTURER} ${Build.MODEL}\n")
        sb.append("• Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        
        // Данные производительности
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val thermal = powerManager?.currentThermalStatus ?: -1
            sb.append("• Температура (0-6): $thermal\n")
        }
        
        sb.append("• Локаль: ${java.util.Locale.getDefault().language}\n")
        sb.append("• Экран: $screenSize\n")
        sb.append("• Архитектура: $primaryAbi\n")
        sb.append("• Свободно в системе: $internalFree МБ\n")
        sb.append("• RAM процесса: ${totalMemMb - freeMemMb} МБ из $totalMemMb МБ\n")
        sb.append("• Сеть: ${if (isWifi) "Wi-Fi" else "Мобильная сеть"}\n\n")

        sb.append("⚙️ НАСТРОЙКИ И ЯДРО:\n")
        sb.append("• Версия yt-dlp: v$ytdlpVer\n")
        sb.append("• Модуль FFmpeg: ${if (isFFmpeg) "OK" else "MISSING"}\n")
        sb.append("• Качество: ${SettingsManager.preferredVideoQuality.value}\n")
        sb.append("• Потоки: W:${SettingsManager.threadsWifi.value} / M:${SettingsManager.threadsMobile.value}\n")
        sb.append("• Параллельно: ${SettingsManager.maxParallelDownloads.value}\n")
        
        val transitModeName = if (SettingsManager.transitModeAutopilotOnly.value) "Умный" else "Глобальный"
        sb.append("• Транзит: ${if (SettingsManager.transitModeEnabled.value) "ВКЛ" else "ВЫКЛ"} ($transitModeName)\n\n")

        if (performanceLogs.isNotEmpty()) {
            sb.append("⚡ ПРОИЗВОДИТЕЛЬНОСТЬ (Последние лаги):\n")
            performanceLogs.forEach { perf ->
                sb.append("[${perf.timestamp}] ${perf.message}\n")
            }
            sb.append("\n")
        }

        if (errors.isNotEmpty()) {
            sb.append("🚨 КРИТИЧЕСКИЕ ОШИБКИ (${errors.size} шт.):\n")
            errors.forEach { err ->
                sb.append("[${err.timestamp}] ${err.message}\n")
                sb.append("-----------------------------------------\n")
            }
            sb.append("\n")
        }

        sb.append("📋 ПОСЛЕДНИЕ СОБЫТИЯ (LOGS):\n")
        recentLogs.forEach { entry ->
            val shortMsg = if (entry.message.length > 200) entry.message.take(200) + "..." else entry.message
            sb.append("[${entry.timestamp}] [${entry.level.name}] $shortMsg\n")
        }

        val file = File(context.cacheDir, "DiagnosticReport_${System.currentTimeMillis()}.log")
        file.writeText(sb.toString())

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Bug Report: VideoDownloader v$appVerName")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться отчетом"))
    } catch (e: Exception) {
        AsyncLogger.log(LogLevel.ERROR, "Ошибка формирования отчета: ${e.message}")
    }
}

fun exportAndShareLogs(context: Context, logs: List<LogEntry>, typeName: String) {
    try {
        val file = File(context.cacheDir, "VideoDownloader_Logs_$typeName.log")
        file.writeText(logs.joinToString("\n") { "[${it.timestamp}] [${it.level.name}] ${it.message}" })

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Отправить файл логов"))
    } catch (e: Exception) {
        AsyncLogger.log(LogLevel.ERROR, "Ошибка экспорта логов: ${e.message}")
    }
}
