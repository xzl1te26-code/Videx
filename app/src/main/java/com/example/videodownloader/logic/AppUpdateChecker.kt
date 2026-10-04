package com.example.videodownloader.logic

import android.content.Context
import android.content.Intent
import android.net.Uri
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
    private const val CURRENT_VERSION_NAME = "1.0.0"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdates(): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        try {
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
            val releaseNotes = jsonObject.optString("body", "")

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

            val hasUpdate = isVersionNewer(tagName, CURRENT_VERSION_NAME)
            Result.success(
                AppUpdateInfo(
                    latestVersion = tagName,
                    releaseNotes = releaseNotes,
                    downloadUrl = downloadUrl,
                    hasUpdate = hasUpdate
                )
            )
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "AppUpdateChecker error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun isVersionNewer(latest: String, current: String): Boolean {
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

    suspend fun downloadAndInstallApk(context: Context, apkUrl: String, onProgress: (Float) -> Unit): Result<File> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "Android-Videx-App")
                .build()

            val apkFile = File(context.cacheDir, "videx_update.apk")
            if (apkFile.exists()) apkFile.delete()

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

            Result.success(apkFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun promptInstallApk(context: Context, apkFile: File) {
        try {
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
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "Не удалось запустить установщик APK: ${e.message}")
        }
    }
}
