package com.example.videodownloader.activities

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.videodownloader.MainActivity
import com.example.videodownloader.SettingsManager
import com.example.videodownloader.TileAction
import com.example.videodownloader.logic.DownloadManager
import com.example.videodownloader.utils.extractUrlFromText
import com.example.videodownloader.utils.isValidUrl

class TileClipHandlerActivity : ComponentActivity() {

    private var hasProcessed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !hasProcessed) {
            hasProcessed = true
            processClipboard()
        }
    }

    private fun processClipboard() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val rawText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
            val cleanUrl = extractUrlFromText(rawText)

            if (cleanUrl.isNotBlank() && isValidUrl(cleanUrl)) {
                DownloadManager.startDownload(
                    context = applicationContext,
                    url = cleanUrl,
                    title = "Загрузка из плитки",
                    isAudio = false,
                    quality = SettingsManager.preferredVideoQuality.value
                )
                Toast.makeText(applicationContext, "Ссылка отправлена на скачивание", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "В буфере обмена нет поддерживаемой ссылки", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Ошибка чтения буфера: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            finish()
        }
    }
}