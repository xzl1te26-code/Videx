package com.example.videodownloader.services

import android.app.PendingIntent
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import com.example.videodownloader.MainActivity
import com.example.videodownloader.R
import com.example.videodownloader.SettingsManager
import com.example.videodownloader.TileAction
import com.example.videodownloader.logic.DownloadManager
import com.example.videodownloader.utils.isValidUrl

@Suppress("DEPRECATION")
class VidexTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        tile.label = getString(R.string.tile_download_label)
        tile.state = Tile.STATE_INACTIVE
        tile.updateTile()
    }

    override fun onClick() {
        super.onClick()
        val tile = qsTile ?: return

        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""

            if (text.isNotBlank() && isValidUrl(text)) {
                val action = SettingsManager.tileAction.value
                if (action == TileAction.SMART_DOWNLOAD) {
                    tile.state = Tile.STATE_ACTIVE
                    tile.label = "Анализ..."
                    tile.updateTile()

                    DownloadManager.startDownload(
                        context = applicationContext,
                        url = text,
                        title = "Загрузка из плитки",
                        isAudio = false,
                        quality = SettingsManager.preferredVideoQuality.value
                    )

                    Toast.makeText(applicationContext, "Ссылка отправлена на скачивание", Toast.LENGTH_SHORT).show()

                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        updateTileState()
                    }, 1500)
                } else {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("navigate_to", "home")
                        putExtra("url_to_analyze", text)
                    }
                    val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    launchActivity(intent, pendingIntent)
                }
            } else {
                tile.label = "Буфер пуст"
                tile.updateTile()
                Toast.makeText(applicationContext, "В буфере обмена нет поддерживаемой ссылки", Toast.LENGTH_SHORT).show()

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    updateTileState()
                }, 1500)
            }
        } catch (e: Exception) {
            updateTileState()
            Toast.makeText(applicationContext, "Ошибка чтения буфера: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @Suppress("DEPRECATION")
    private fun launchActivity(intent: Intent, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(pendingIntent)
        } else {
            try {
                val method = TileService::class.java.getMethod("startActivityAndCollapse", Intent::class.java)
                method.invoke(this, intent)
            } catch (_: Exception) {
            }
        }
    }
}