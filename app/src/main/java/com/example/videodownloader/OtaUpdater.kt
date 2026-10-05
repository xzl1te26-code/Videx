package com.example.videodownloader

import android.content.Context
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

object OtaUpdater {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun updateCore(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!com.example.videodownloader.utils.isNetworkAvailable(context)) {
                return@withContext Result.failure(Exception("Отсутствует подключение к интернету"))
            }

            AsyncLogger.log(LogLevel.INFO, "OTA: Проверка обновлений на GitHub...")

            // Получаем ТЕКУЩУЮ версию yt-dlp из Python (если он запущен)
            val currentVersion = if (Python.isStarted()) {
                try {
                    val py = Python.getInstance()
                    py.getModule("yt_dlp.version").get("__version__")?.toString() ?: ""
                } catch (e: Exception) {
                    ""
                }
            } else ""

            // Запрашиваем информацию о последнем релизе yt-dlp
            val releaseRequest = Request.Builder()
                .url("https://api.github.com/repos/yt-dlp/yt-dlp/releases/latest")
                .header("User-Agent", "Android-Videx-App")
                .build()

            val jsonData = httpClient.newCall(releaseRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Ошибка GitHub API (Код: ${response.code})")
                }
                response.body?.string() ?: throw Exception("Пустой ответ от GitHub")
            }

            val jsonObject = JSONObject(jsonData)

            // GitHub обычно возвращает версию, например, "2024.04.09" или "v2024.04.09"
            val rawLatestVersion = jsonObject.getString("tag_name")
            val latestVersion = rawLatestVersion.removePrefix("v")

            AsyncLogger.log(LogLevel.INFO, "OTA: Текущая версия: $currentVersion | Последняя на GitHub: $latestVersion")

            // ПРОВЕРКА НА АКТУАЛЬНОСТЬ (Если версии совпадают - отменяем загрузку)
            if (currentVersion.isNotEmpty() && currentVersion == latestVersion) {
                return@withContext Result.success("ALREADY_LATEST|$currentVersion")
            }

            // Ищем прямую ссылку на архив yt-dlp
            val assetsArray = jsonObject.getJSONArray("assets")
            var downloadUrl: String? = null

            for (i in 0 until assetsArray.length()) {
                val asset = assetsArray.getJSONObject(i)
                val name = asset.getString("name")
                if (name == "yt-dlp" || name.endsWith(".tar.gz") || name.endsWith(".zip")) {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }

            val finalUrl = downloadUrl ?: jsonObject.optString("zipball_url", "")
            if (finalUrl.isBlank()) {
                throw Exception("Не удалось получить ссылку для загрузки архива")
            }

            AsyncLogger.log(LogLevel.INFO, "OTA: Скачивание пакета с $finalUrl...")

            // Скачиваем архив во временный файл
            val appContext = context.applicationContext
            val tempFile = File(appContext.filesDir, "ytdlp_temp.zip")
            val targetFile = File(appContext.filesDir, "ytdlp_latest.zip")
            val backupFile = File(appContext.filesDir, "ytdlp_backup.zip")

            val downloadRequest = Request.Builder()
                .url(finalUrl)
                .header("User-Agent", "Android-Videx-App")
                .build()

            httpClient.newCall(downloadRequest).execute().use { fileResponse ->
                if (!fileResponse.isSuccessful) {
                    throw Exception("Не удалось скачать архив (Код: ${fileResponse.code})")
                }

                val body = fileResponse.body ?: throw Exception("Пустой поток данных")
                body.byteStream().use { inputStream ->
                    FileOutputStream(tempFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }
            }

            AsyncLogger.log(LogLevel.INFO, "OTA: Файл загружен (${tempFile.length() / 1024} КБ)")

            // Атомарная замена файлов (Zero-brick mechanism)
            if (targetFile.exists()) {
                if (backupFile.exists()) backupFile.delete()
                targetFile.renameTo(backupFile)
            }

            val renamed = tempFile.renameTo(targetFile)
            if (!renamed) {
                backupFile.renameTo(targetFile) // Откат при ошибке
                throw Exception("Ошибка атомарной замены файлов ядра")
            }

            // Горячее подключение архива к Python
            if (!Python.isStarted()) {
                throw Exception("Python-движок не запущен. Перезапустите приложение.")
            }
            
            val py = Python.getInstance()
            val module = py.getModule("ytdlp_wrapper")
            val pyResult = module.callAttr("set_custom_core_path", targetFile.absolutePath).asMap()

            if (pyResult[py.builtins.callAttr("str", "status")]?.toString() == "success") {
                AsyncLogger.log(LogLevel.INFO, "OTA: Ядро успешно обновлено до $latestVersion!")
                Result.success(latestVersion)
            } else {
                throw Exception("Python не смог подключить новый путь к ядру")
            }

        } catch (e: Exception) {
            val userMsg = com.example.videodownloader.utils.translateNetworkError(e.message, "Ошибка обновления ядра")
            AsyncLogger.log(LogLevel.ERROR, "OTA Ошибка: ${e.message}")
            Result.failure(Exception(userMsg))
        }
    }
}