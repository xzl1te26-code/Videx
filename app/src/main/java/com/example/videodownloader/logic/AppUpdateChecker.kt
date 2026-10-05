package com.example.videodownloader.logic

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.videodownloader.AsyncLogger
import com.example.videodownloader.LogLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class AppUpdateInfo(
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val hasUpdate: Boolean
)

object AppUpdateChecker {

    private const val GITHUB_OWNER = "xzl1te26-code"
    private const val GITHUB_REPO = "Videx"

    private var pendingInstallFile: File? = null
    private var userRedirectedToSettings: Boolean = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getInstalledVersionName(context: Context): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    fun getApkFileVersion(context: Context, apkFile: File): String? {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            info?.versionName
        } catch (_: Exception) {
            null
        }
    }

    suspend fun checkForUpdates(context: Context): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
            if (!com.example.videodownloader.utils.isNetworkAvailable(context)) {
                throw Exception("Отсутствует подключение к интернету")
            }

            val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Android-Videx-App")
                .header("Accept", "application/vnd.github+json")
                .build()

            val jsonData = httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Код ошибки GitHub: ${response.code}")
                }
                response.body?.string() ?: throw Exception("Пустой ответ от GitHub")
            }

            val jsonObject = JSONObject(jsonData)
            val tagName = jsonObject.optString("tag_name", "").removePrefix("v").trim()
            val rawReleaseNotes = jsonObject.optString("body", "")
            val cleanNotes = cleanMarkdownText(rawReleaseNotes)

            var downloadUrl = ""
            val assets = jsonObject.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk") || name == "app-release.apk") {
                        downloadUrl = asset.optString("browser_download_url", "")
                        break
                    }
                }
            }

            val installedVersion = getInstalledVersionName(context)
            val hasUpdate = isVersionNewer(tagName, installedVersion)

            // Если обновления нет — очищаем устаревшие скачанные APK из кэша
            if (!hasUpdate) {
                clearOldCachedApks(context)
            }

            Result.success(
                AppUpdateInfo(
                    latestVersion = tagName,
                    releaseNotes = cleanNotes,
                    downloadUrl = downloadUrl,
                    hasUpdate = hasUpdate
                )
            )
        } catch (e: Exception) {
            val userMsg = com.example.videodownloader.utils.translateNetworkError(e.message, "Не удалось проверить обновления")
            AsyncLogger.log(LogLevel.ERROR, "AppUpdateChecker error: ${e.message}")
            Result.failure(Exception(userMsg))
        }
    }

    private fun cleanMarkdownText(raw: String): String {
        return raw
            .replace("```markdown", "")
            .replace("```", "")
            .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
            .replace(Regex("""\*(.*?)\*"""), "$1")
            .replace(Regex("""#(.*?)\n"""), "$1\n")
            .trim()
    }

    fun isVersionNewer(latest: String, current: String): Boolean {
        if (latest.isBlank()) return false
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        val length = maxOf(latestParts.size, currentParts.size)
        for (i in 0 until length) {
            val latestNum = latestParts.getOrElse(i) { 0 }
            val currentNum = currentParts.getOrElse(i) { 0 }
            if (latestNum > currentNum) return true
            if (latestNum < currentNum) return false
        }
        return false
    }

    suspend fun downloadAndInstallApk(
        context: Context,
        apkUrl: String,
        latestVersion: String,
        forceRedownload: Boolean = false,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val safeVersion = latestVersion.ifBlank { "latest" }
            val apkFile = File(context.cacheDir, "videx_update_$safeVersion.apk")

            // Если версия совпадает и файл уже скачан (> 1 МБ) — берем готовый из кэша
            val cachedApkVersion = getApkFileVersion(context, apkFile)
            if (!forceRedownload && apkFile.exists() && apkFile.length() > 1024 * 1024L && cachedApkVersion == latestVersion) {
                AsyncLogger.log(LogLevel.INFO, "Используем валидный APK из кэша (Версия $cachedApkVersion)")
                onProgress(1.0f)
                pendingInstallFile = apkFile
                return@withContext Result.success(apkFile)
            }

            if (!com.example.videodownloader.utils.isNetworkAvailable(context)) {
                throw Exception("Отсутствует подключение к интернету для скачивания обновления")
            }

            // Если версия в кэше не совпадает или устарела — удаляем старый файл
            if (apkFile.exists()) apkFile.delete()

            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "Android-Videx-App")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw Exception("Ошибка скачивания APK (${response.code})")
                val body = response.body ?: throw Exception("Пустое тело ответа")
                val contentLength = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(apkFile).use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var bytesRead: Int
                        var totalRead = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                onProgress(totalRead.toFloat() / contentLength)
                            }
                        }
                    }
                }
            }

            pendingInstallFile = apkFile
            Result.success(apkFile)
        } catch (e: Exception) {
            val userMsg = com.example.videodownloader.utils.translateNetworkError(e.message, "Ошибка скачивания файла обновления")
            Result.failure(Exception(userMsg))
        }
    }

    fun promptInstallApk(context: Context, apkFile: File) {
        try {
            // 🛡️ ПРОВЕРКА ВЕРСИИ: Убеждаемся, что скачанный APK РЕАЛЬНО новее установленной версии!
            val apkVersion = getApkFileVersion(context, apkFile)
            val currentVersion = getInstalledVersionName(context)

            if (apkVersion != null && !isVersionNewer(apkVersion, currentVersion)) {
                AsyncLogger.log(LogLevel.WARN, "Пропуск установки: Версия в APK ($apkVersion) не новее установленной ($currentVersion)")
                apkFile.delete()
                pendingInstallFile = null
                userRedirectedToSettings = false
                return
            }

            pendingInstallFile = apkFile

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                AsyncLogger.log(LogLevel.INFO, "Перенаправление в Настройки за разрешением установки неизвестных приложений")
                userRedirectedToSettings = true
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(settingsIntent)
                return
            }

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            pendingInstallFile = null
            userRedirectedToSettings = false
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "Не удалось запустить установщик APK: ${e.message}")
        }
    }

    fun checkAndResumePendingInstall(context: Context) {
        try {
            // Возобновляем установку ТОЛЬКО если был явный редирект в Настройки во время этого сеанса
            if (!userRedirectedToSettings) return
            userRedirectedToSettings = false

            val file = pendingInstallFile ?: return
            if (file.exists() && file.length() > 1024 * 1024L) {
                val apkVersion = getApkFileVersion(context, file)
                val currentVersion = getInstalledVersionName(context)
                if (apkVersion != null && isVersionNewer(apkVersion, currentVersion)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context.packageManager.canRequestPackageInstalls()) {
                        AsyncLogger.log(LogLevel.INFO, "Автоматический запуск установки APK после получения прав")
                        promptInstallApk(context, file)
                    }
                } else {
                    file.delete()
                    pendingInstallFile = null
                }
            }
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "Ошибка возобновления установки APK: ${e.message}")
        }
    }

    fun clearOldCachedApks(context: Context) {
        try {
            pendingInstallFile = null
            userRedirectedToSettings = false
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("videx_update") && file.name.endsWith(".apk")) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}
