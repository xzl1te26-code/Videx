package com.example.videodownloader

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

enum class LogLevel { INFO, WARN, ERROR, PERF }

@Immutable
data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
    val level: LogLevel,
    val message: String
)

object AsyncLogger {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs

    private val logDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(logDispatcher + SupervisorJob())
    private var logFile: File? = null

    private val logChannel = Channel<LogEntry>(Channel.UNLIMITED)
    private val isInitialized = AtomicBoolean(false)

    fun init(context: Context) {
        if (isInitialized.getAndSet(true)) return
        logFile = File(context.filesDir, "app_system.log")

        // Фоновая запись логов пачками
        startLogWorker()

        // Считываем сохраненные логи с диска при запуске приложения (оптимизировано)
        scope.launch {
            try {
                // Ждем готовности настроек перед проверкой очистки
                launch {
                    SettingsManager.isInitialized.first { it }
                    if (SettingsManager.autoCleanupLogs.value) {
                        pruneOldLogs()
                    }
                }

                logFile?.let { file ->
                    if (file.exists()) {
                        val loadedLogs = mutableListOf<LogEntry>()
                        val logPattern = Regex("""^\[(.*?)\] \[(.*?)\] (.*)$""")
                        
                        // Чтение последних 64 КБ файла
                        val maxReadBytes = 64 * 1024L
                        val fileLength = file.length()
                        val startPos = if (fileLength > maxReadBytes) fileLength - maxReadBytes else 0L
                        
                        val content = RandomAccessFile(file, "r").use { raf ->
                            raf.seek(startPos)
                            val bytes = ByteArray((fileLength - startPos).toInt())
                            raf.readFully(bytes)
                            String(bytes)
                        }

                        val lines = content.lines().takeLast(300)
                        for (line in lines) {
                            val match = logPattern.find(line)
                            if (match != null) {
                                val (fullDate, levelStr, msg) = match.destructured
                                val timeOnly = if (fullDate.length >= 19) fullDate.substring(11, 19) else fullDate
                                val level = try { LogLevel.valueOf(levelStr) } catch (e: Exception) { LogLevel.INFO }
                                loadedLogs.add(LogEntry(timestamp = timeOnly, level = level, message = msg))
                            }
                        }
                        _logs.value = loadedLogs
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun log(level: LogLevel, message: String) {
        val entry = LogEntry(level = level, message = message)
        _logs.update { (it + entry).takeLast(1000) }
        
        // Отправляем в канал для фоновой записи
        logChannel.trySend(entry)
    }

    private fun startLogWorker() {
        scope.launch {
            val buffer = mutableListOf<LogEntry>()
            var lastFlushTime = System.currentTimeMillis()

            while (isActive) {
                try {
                    // Ждем лог или тайм-аут
                    val entry = withTimeoutOrNull(5000L) { logChannel.receive() }
                    
                    if (entry != null) {
                        buffer.add(entry)
                    }

                    // Условия сброса на диск:
                    // 1. Набралось 20 записей
                    // 2. Прошло 5 секунд с последнего сброса
                    // 3. Пришла критическая ошибка (ERROR)
                    val shouldFlush = buffer.size >= 20 || 
                                     (buffer.isNotEmpty() && System.currentTimeMillis() - lastFlushTime > 5000L) ||
                                     (entry?.level == LogLevel.ERROR)

                    if (shouldFlush && buffer.isNotEmpty()) {
                        flushBufferToFile(buffer)
                        buffer.clear()
                        lastFlushTime = System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    delay(1000) // Пауза при ошибке, чтобы не зациклиться
                }
            }
        }
    }

    private fun flushBufferToFile(buffer: List<LogEntry>) {
        try {
            logFile?.let { file ->
                // Ротация файла при превышении 2МБ
                if (file.exists() && file.length() > 2 * 1024 * 1024) {
                    val backup = File(file.parent, "${file.name}.bak")
                    if (backup.exists()) backup.delete()
                    file.renameTo(backup)
                }
                
                val fullDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                val sb = StringBuilder()
                for (entry in buffer) {
                    sb.append("[$fullDate] [${entry.level.name}] ${entry.message}\n")
                }
                file.appendText(sb.toString())
            }
        } catch (e: Exception) {
            // В самом логгере ошибку логировать сложно, выводим в консоль
            e.printStackTrace()
        }
    }

    fun clear() {
        _logs.value = emptyList()
        scope.launch {
            try {
                logFile?.writeText("")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun pruneOldLogs() {
        scope.launch {
            try {
                logFile?.let { file ->
                    if (!file.exists()) return@let
                    
                    val threeDaysAgo = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -3)
                    }.time
                    
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    val logPattern = Regex("""^\[(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})\]""")
                    
                    val lines = file.readLines()
                    val filteredLines = lines.filter { line ->
                        val match = logPattern.find(line)
                        if (match != null) {
                            val dateStr = match.groupValues[1]
                            val logDate = try { dateFormat.parse(dateStr) } catch (e: Exception) { null }
                            logDate == null || logDate.after(threeDaysAgo)
                        } else {
                            true // Keep lines that don't match pattern (shouldn't happen)
                        }
                    }
                    
                    if (filteredLines.size < lines.size) {
                        file.writeText(filteredLines.joinToString("\n") + "\n")
                        log(LogLevel.INFO, "Логи старше 3-х дней удалены (${lines.size - filteredLines.size} строк)")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}