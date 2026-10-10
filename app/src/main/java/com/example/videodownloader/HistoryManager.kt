package com.example.videodownloader

import android.content.Context
import android.content.Intent
import android.content.ContentResolver
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.content.ContentUris
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.util.UUID
import android.os.Environment
import android.os.Build
import android.annotation.SuppressLint
import android.graphics.Bitmap
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.Immutable

enum class MediaType { VIDEO, AUDIO, PHOTO }

// ==========================================
// 1. ROOM ENTITY (Таблица истории)
// ==========================================
@Immutable
@Entity(
    tableName = "media_history",
    indices = [
        Index(value = ["sourceUrl"]),
        Index(value = ["timestamp"]),
    ]
)
data class DownloadedFileItem(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val pathOrUri: String,
    val name: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val dateFormatted: String,
    val mediaType: MediaType,
    val sourceUrl: String = "",
    val thumbnailUrl: String? = null,
    val durationMs: Long = 0L, // Длительность видео/аудио
    val timestamp: Long = System.currentTimeMillis(),
    val isMissing: Boolean = false, // Статус наличия файла на диске
)

// ==========================================
// 2. ROOM DAO (Методы доступа к базе)
// ==========================================
@Dao
interface HistoryDao {
    @Query("SELECT * FROM media_history ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<DownloadedFileItem>>

    @Query("SELECT * FROM media_history ORDER BY timestamp DESC")
    suspend fun getAll(): List<DownloadedFileItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: DownloadedFileItem)

    @Update
    suspend fun update(item: DownloadedFileItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<DownloadedFileItem>)

    @Delete
    suspend fun delete(item: DownloadedFileItem)

    @Query("DELETE FROM media_history WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM media_history")
    suspend fun clearAll()

    @Query("SELECT * FROM media_history WHERE id = :id")
    suspend fun getById(id: String): DownloadedFileItem?

    @Query("SELECT * FROM media_history WHERE sourceUrl = :url LIMIT 1")
    suspend fun findByUrl(url: String): DownloadedFileItem?

    @Query("SELECT COUNT(*) FROM media_history WHERE sourceUrl LIKE '%' || :id || '%'")
    suspend fun countByVideoId(id: String): Int

    @Query("SELECT * FROM media_history ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): DownloadedFileItem?

    @Query("SELECT COUNT(*) FROM media_history WHERE sourceUrl = :url OR name = :name")
    suspend fun countDuplicate(url: String, name: String): Int
}

// ==========================================
// 3. ROOM DATABASE (База данных SQLite)
// ==========================================
@Database(entities = [DownloadedFileItem::class], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_history ADD COLUMN thumbnailUrl TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_history ADD COLUMN isMissing INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_history_sourceUrl ON media_history(sourceUrl)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_media_history_timestamp ON media_history(timestamp)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE media_history ADD COLUMN durationMs INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "video_downloader_history.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 4. МЕНЕДЖЕР ИСТОРИИ
// ==========================================
object HistoryManager {
    private const val PREFS_NAME = "history_db_prefs"
    private const val KEY_HISTORY = "saved_media_history"
    private const val KEY_MIGRATED = "room_migration_completed"

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 КБ"
        val mb = bytes / (1024.0 * 1024.0)
        return when {
            mb >= 1024 -> String.format(Locale.US, "%.2f ГБ", mb / 1024.0)
            mb >= 0.1 -> String.format(Locale.US, "%.1f МБ", mb)
            else -> String.format(Locale.US, "%.0f КБ", bytes / 1024.0)
        }
    }

    fun extractVideoId(url: String): String? {
        val cleanUrl = url.trim()
        try {
            // 1. YouTube (including Shorts, Embed, Attribution links)
            if (cleanUrl.contains("youtube.com") || cleanUrl.contains("youtu.be")) {
                val ytRegex = Regex("""(?:v=|youtu\.be/|shorts/|embed/|v/|attribution_link.*v%3D|watch\?v=)([a-zA-Z0-9_-]{11})""")
                val match = ytRegex.find(cleanUrl)
                if (match != null) {
                return "yt_${match.groupValues[1]}"
            }
            }

            // 2. TikTok (Video IDs are numeric, 15-22 digits)
            if (cleanUrl.contains("tiktok.com")) {
                val ttRegex = Regex("""/(?:video|photo|v)/(\d+)""")
                val match = ttRegex.find(cleanUrl)
                if (match != null) return "tt_${match.groupValues[1]}"
                
                // Try finding long numeric ID anywhere in URL if it's a tiktok link
                val ttIdRegex = Regex("""(\d{15,22})""")
                val matchId = ttIdRegex.find(cleanUrl)
                if (matchId != null) return "tt_${matchId.groupValues[1]}"
            }

            // 3. Instagram (Reels, Posts)
            if (cleanUrl.contains("instagram.com")) {
                val igRegex = Regex("""/(?:reels?|p|tv)/([a-zA-Z0-9_-]+)""")
                val match = igRegex.find(cleanUrl)
                if (match != null) return "ig_${match.groupValues[1]}"
            }

            // 4. VK / VK Video
            if (cleanUrl.contains("vk.com") || cleanUrl.contains("vkvideo.ru")) {
                val vkRegex = Regex("""video-?(\d+_\d+)""")
                val match = vkRegex.find(cleanUrl)
                if (match != null) return "vk_${match.groupValues[1]}"
            }
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.WARN, "ID Extraction fail: ${e.message}")
        }
        return null
    }

    private suspend fun migrateFromPrefsIfNeeded(context: Context, dao: HistoryDao) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATED, false)) return

        try {
            val oldJson = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
            val jsonArray = JSONArray(oldJson)
            if (jsonArray.length() > 0) {
                val migratedList = mutableListOf<DownloadedFileItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val mediaType = try {
                        MediaType.valueOf(obj.getString("mediaType"))
                    } catch (_: Exception) {
                        MediaType.VIDEO
                    }
                    migratedList.add(
                        DownloadedFileItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            pathOrUri = obj.getString("pathOrUri"),
                            name = obj.getString("name"),
                            sizeBytes = obj.optLong("sizeBytes", 0L),
                            sizeFormatted = obj.optString("sizeFormatted", "0 КБ"),
                            dateFormatted = obj.optString("dateFormatted", ""),
                            mediaType = mediaType,
                            sourceUrl = obj.optString("sourceUrl", ""),
                            timestamp = System.currentTimeMillis() - (i * 1000L)
                        )
                    )
                }
                dao.insertAll(migratedList)
                AsyncLogger.log(LogLevel.INFO, "Успешно перенесено записей в Room: ${migratedList.size}")
            }
            prefs.edit(commit = true) {
                putBoolean(KEY_MIGRATED, true)
                remove(KEY_HISTORY)
            }
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.WARN, "Ошибка миграции в Room: ${e.message}")
        }
    }

    suspend fun isDuplicate(context: Context, url: String, title: String): Boolean = withContext(Dispatchers.IO) {
        if (url.isBlank() && title.isBlank()) return@withContext false
        val dao = AppDatabase.getDatabase(context).historyDao()

        // 1. Поиск по точному URL (самый быстрый)
        if (url.isNotBlank() && (dao.findByUrl(url) != null)) return@withContext true

        // 2. Умный поиск по ID видео через SQL LIKE
        val incomingId = extractVideoId(url)
        if (incomingId != null && dao.countByVideoId(incomingId) > 0) return@withContext true
        
        // 3. Быстрый счетчик по имени в базе
        if (title.isNotBlank() && dao.countDuplicate(url, title) > 0) return@withContext true

        false
    }

    suspend fun addEntry(
        context: Context,
        pathOrUri: String,
        name: String,
        sizeBytes: Long,
        mediaType: MediaType,
        sourceUrl: String = "",
        thumbnailUrl: String? = null,
    ) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val dao = AppDatabase.getDatabase(appContext).historyDao()
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        val sizeFormatted = formatBytes(sizeBytes)

        // Кэшируем обложку локально в файл
        val cachedThumbnail = cacheThumbnailLocally(appContext, pathOrUri, mediaType, thumbnailUrl)

        // Получаем длительность для видео/аудио
        val duration = if (mediaType != MediaType.PHOTO) getMediaDuration(appContext, pathOrUri) else 0L

        val item = DownloadedFileItem(
            id = UUID.randomUUID().toString(),
            pathOrUri = pathOrUri,
            name = name,
            sizeBytes = sizeBytes,
            sizeFormatted = sizeFormatted,
            dateFormatted = dateStr,
            mediaType = mediaType,
            sourceUrl = sourceUrl,
            thumbnailUrl = cachedThumbnail,
            durationMs = duration,
            timestamp = System.currentTimeMillis(),
            isMissing = false,
        )

        dao.insert(item)
        AsyncLogger.log(LogLevel.INFO, "Сохранено в Room: $name ($sizeFormatted)")
    }

    /**
     * 🖼️ Надежное локальное кэширование обложки:
     * - Скачивает сетевую обложку (http/https) в локальный файл кэша.
     * - Если обложки нет или это видео — извлекает кадр через MediaMetadataRetriever и сохраняет локально.
     * Возвращает абсолютный путь к локальному файлу обложки.
     */
    fun cacheThumbnailLocally(
        context: Context,
        pathOrUri: String,
        mediaType: MediaType,
        thumbnailUrl: String?
    ): String? {
        val appContext = context.applicationContext

        // Если обложка уже сохранена как локальный существующий файл — возвращаем его
        if (!thumbnailUrl.isNullOrBlank() && !thumbnailUrl.startsWith("http://") && !thumbnailUrl.startsWith("https://")) {
            val file = File(thumbnailUrl)
            if (file.exists() && file.length() > 0) {
                return thumbnailUrl
            }
        }

        try {
            val thumbDir = File(appContext.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }
            val filename = "thumb_${UUID.randomUUID().toString().take(12)}.jpg"
            val localThumbFile = File(thumbDir, filename)

            // 1. Если передана сетевая ссылка — сохраняем обложку локально
            if (!thumbnailUrl.isNullOrBlank() && (thumbnailUrl.startsWith("http://") || thumbnailUrl.startsWith("https://"))) {
                try {
                    val url = URL(thumbnailUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.doInput = true
                    connection.connect()
                    
                    val contentType = connection.contentType ?: ""
                    val contentLength = connection.contentLengthLong
                    if (connection.responseCode == HttpURLConnection.HTTP_OK && 
                        (contentType.isBlank() || contentType.contains("image", ignoreCase = true)) &&
                        (contentLength <= 0 || contentLength <= 10_000_000L)
                    ) {
                        connection.inputStream.use { input ->
                            FileOutputStream(localThumbFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (localThumbFile.exists() && localThumbFile.length() > 0) {
                            AsyncLogger.log(LogLevel.INFO, "Сетевая обложка закэширована: ${localThumbFile.name}")
                            return localThumbFile.absolutePath
                        }
                    }
                } catch (e: Exception) {
                    AsyncLogger.log(LogLevel.WARN, "Ошибка скачивания сетевой обложки: ${e.message}")
                }
            }

            // 2. Если это ВИДЕО и локальной обложки нет — декодируем кадр из видеофайла и сохраняем JPEG
            if (mediaType == MediaType.VIDEO) {
                val retriever = MediaMetadataRetriever()
                try {
                    if (pathOrUri.startsWith("content://")) {
                        retriever.setDataSource(appContext, Uri.parse(pathOrUri))
                    } else {
                        retriever.setDataSource(pathOrUri)
                    }
                    val bitmap = retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                        ?: retriever.frameAtTime
                    if (bitmap != null) {
                        FileOutputStream(localThumbFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                        }
                        bitmap.recycle()
                        if (localThumbFile.exists() && localThumbFile.length() > 0) {
                            AsyncLogger.log(LogLevel.INFO, "Кадр видео закэширован: ${localThumbFile.name}")
                            return localThumbFile.absolutePath
                        }
                    }
                } catch (e: Exception) {
                    AsyncLogger.log(LogLevel.WARN, "Ошибка извлечения кадра из видео: ${e.message}")
                } finally {
                    try { retriever.release() } catch (_: Exception) {}
                }
            }
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.WARN, "Ошибка в процессе кэширования обложки: ${e.message}")
        }

        return thumbnailUrl
    }

    suspend fun getEntryById(context: Context, id: String): DownloadedFileItem? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        AppDatabase.getDatabase(appContext).historyDao().getById(id)
    }

    suspend fun getLatestEntry(context: Context): DownloadedFileItem? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        AppDatabase.getDatabase(appContext).historyDao().getLatest()
    }

    fun getHistoryFlow(context: Context): Flow<List<DownloadedFileItem>> {
        val appContext = context.applicationContext
        return AppDatabase.getDatabase(appContext).historyDao().getAllFlow()
    }

    suspend fun checkAndUpdateFileStatus(context: Context, item: DownloadedFileItem) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val dao = AppDatabase.getDatabase(appContext).historyDao()
        val exists = fileExists(appContext, item)

        var updatedItem = item
        var needsUpdate = false

        if (exists != !item.isMissing) {
            updatedItem = updatedItem.copy(isMissing = !exists)
            needsUpdate = true
        }

        // Кэшируем обложку для видеозаписей, если локального файла нет
        if (exists && (item.thumbnailUrl.isNullOrBlank() || item.thumbnailUrl.startsWith("http://") || item.thumbnailUrl.startsWith("https://") || !File(item.thumbnailUrl).exists())) {
            val newThumbPath = cacheThumbnailLocally(appContext, item.pathOrUri, item.mediaType, item.thumbnailUrl)
            if (!newThumbPath.isNullOrBlank() && newThumbPath != item.thumbnailUrl) {
                updatedItem = updatedItem.copy(thumbnailUrl = newThumbPath)
                needsUpdate = true
            }
        }

        if (needsUpdate) {
            dao.update(updatedItem)
        }
    }

    suspend fun loadFiles(context: Context): List<DownloadedFileItem> = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val dao = AppDatabase.getDatabase(appContext).historyDao()
        migrateFromPrefsIfNeeded(appContext, dao)
        dao.getAll()
    }

    fun openFile(context: Context, item: DownloadedFileItem) {
        if (!fileExists(context, item)) {
            AsyncLogger.log(LogLevel.WARN, "Попытка открыть отсутствующий файл: ${item.name}")
            return
        }
        try {
            val uri: Uri = if (item.pathOrUri.startsWith("content://")) {
                item.pathOrUri.toUri()
            } else {
                val file = File(item.pathOrUri)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }

            val mimeType = when (item.mediaType) {
                MediaType.PHOTO -> "image/*"
                MediaType.AUDIO -> "audio/*"
                MediaType.VIDEO -> "video/*"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Открыть через"))
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "Ошибка открытия файла: ${e.message}")
        }
    }

    fun shareFile(context: Context, item: DownloadedFileItem) {
        if (!fileExists(context, item)) {
            AsyncLogger.log(LogLevel.WARN, "Попытка поделиться отсутствующим файлом: ${item.name}")
            return
        }
        try {
            val uri: Uri = if (item.pathOrUri.startsWith("content://")) {
                item.pathOrUri.toUri()
            } else {
                val file = File(item.pathOrUri)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }

            val mimeType = when (item.mediaType) {
                MediaType.PHOTO -> "image/*"
                MediaType.AUDIO -> "audio/*"
                MediaType.VIDEO -> "video/*"
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Поделиться файлом"))
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "Ошибка отправки: ${e.message}")
        }
    }

    suspend fun deleteFile(context: Context, item: DownloadedFileItem): Boolean = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        try {
            if (item.pathOrUri.startsWith("content://")) {
                val uri = Uri.parse(item.pathOrUri)
                val isTreeUri = uri.authority == "com.android.externalstorage.documents" || uri.toString().contains("/tree/")
                if (isTreeUri) {
                    DocumentFile.fromSingleUri(appContext, uri)?.delete()
                } else {
                    appContext.contentResolver.delete(uri, null, null)
                }
            } else {
                val file = File(item.pathOrUri)
                
                // 1. Синхронизация с MediaStore (Галереей)
                val mediaUri = findUriForPath(appContext, file.absolutePath)
                if (mediaUri != null) {
                    try {
                        appContext.contentResolver.delete(mediaUri, null, null)
                    } catch (e: Exception) {
                        AsyncLogger.log(LogLevel.WARN, "MediaStore delete failed: ${e.message}")
                    }
                }

                // 2. Физическое удаление
                if (file.exists()) {
                    file.delete()
                    MediaScannerConnection.scanFile(appContext, arrayOf(file.absolutePath), null, null)
                }
            }

            AppDatabase.getDatabase(appContext).historyDao().delete(item)
            AsyncLogger.log(LogLevel.INFO, "Файл удален из базы Room: ${item.name}")
            true
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "Ошибка удаления: ${e.message}")
            false
        }
    }

    suspend fun clearAllHistory(context: Context): Boolean = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        try {
            val dao = AppDatabase.getDatabase(appContext).historyDao()
            val allFiles = dao.getAll()

            for (item in allFiles) {
                try {
                    if (item.pathOrUri.startsWith("content://")) {
                        val uri = Uri.parse(item.pathOrUri)
                        val isTreeUri = uri.authority == "com.android.externalstorage.documents" || uri.toString().contains("/tree/")
                        if (isTreeUri) {
                            DocumentFile.fromSingleUri(appContext, uri)?.delete()
                        } else {
                            appContext.contentResolver.delete(uri, null, null)
                        }
                    } else {
                        val file = File(item.pathOrUri)
                        
                        val mediaUri = findUriForPath(appContext, file.absolutePath)
                        if (mediaUri != null) {
                            appContext.contentResolver.delete(mediaUri, null, null)
                        }

                        if (file.exists()) {
                            file.delete()
                            MediaScannerConnection.scanFile(appContext, arrayOf(file.absolutePath), null, null)
                        }
                    }
                } catch (_: Exception) {}
            }

            dao.clearAll()
            AsyncLogger.log(LogLevel.INFO, "История и база Room полностью очищены")
            true
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "Ошибка при полной очистке: ${e.message}")
            false
        }
    }

    @SuppressLint("NewApi")
    private fun findUriForPath(context: Context, path: String): Uri? {
        val resolver = context.contentResolver
        val collections = mutableListOf(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        )
        // Добавляем коллекцию Downloads для Android 10+ (API 29+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collections.add(MediaStore.Downloads.EXTERNAL_CONTENT_URI)
        }
        
        for (baseUri in collections) {
            try {
                // Поиск по DATA, DISPLAY_NAME и SIZE с лимитом 1 записи для быстродействия
                val fileName = File(path).name
                val fileSize = File(path).length()
                
                val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val queryArgs = android.os.Bundle().apply {
                        putString(ContentResolver.QUERY_ARG_SQL_SELECTION, "${MediaStore.MediaColumns.DATA} = ? OR (${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.SIZE} = ?)")
                        putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, arrayOf(path, fileName, fileSize.toString()))
                        putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
                    }
                    resolver.query(baseUri, arrayOf(MediaStore.MediaColumns._ID), queryArgs, null)
                } else {
                    resolver.query(
                        baseUri,
                        arrayOf(MediaStore.MediaColumns._ID),
                        "${MediaStore.MediaColumns.DATA} = ? OR (${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.SIZE} = ?)",
                        arrayOf(path, fileName, fileSize.toString()),
                        "${MediaStore.MediaColumns._ID} ASC LIMIT 1"
                    )
                }
                cursor?.use {
                    if (it.moveToFirst()) {
                        val id = it.getLong(it.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        return ContentUris.withAppendedId(baseUri, id)
                    }
                }
            } catch (_: Exception) {}
        }
        return null
    }

    fun shareMultipleFiles(context: Context, items: List<DownloadedFileItem>) {
        if (items.isEmpty()) return
        try {
            val uris = ArrayList<Uri>()
            for (item in items) {
                if (!fileExists(context, item)) continue
                val uri: Uri = if (item.pathOrUri.startsWith("content://")) {
                    Uri.parse(item.pathOrUri)
                } else {
                    val file = File(item.pathOrUri)
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }
                uris.add(uri)
            }
            
            if (uris.isEmpty()) return

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Поделиться файлами (${uris.size})"))
        } catch (e: Exception) {
            AsyncLogger.log(LogLevel.ERROR, "Ошибка массовой отправки: ${e.message}")
        }
    }

    /**
     * 🛡️ Фильтр мусорных, временных и скрытых файлов (включая корзину Google Files / Samsung / Android):
     * - Игнорирует файлы, начинающиеся с точки (напр. .trashed-12345, .pending-12345, .nomedia).
     * - Игнорирует имена файлов и пути, содержащие .trashed или .pending.
     * - Игнорирует временные файлы скачивания (.tmp, .part, .crdownload, .ytdl, .download).
     */
    fun isTrashedOrTempFile(file: File): Boolean {
        val name = file.name
        if (file.isHidden || name.startsWith(".")) return true
        if (name.contains(".trashed", ignoreCase = true) || name.contains(".pending", ignoreCase = true)) return true
        val ext = file.extension.lowercase(Locale.getDefault())
        if (ext == "tmp" || ext == "part" || ext == "crdownload" || ext == "ytdl" || ext == "download") return true
        return false
    }

    fun isTrashedOrTempPath(pathOrUri: String): Boolean {
        if (pathOrUri.isBlank()) return true
        val name = try { File(pathOrUri).name } catch (_: Exception) { "" }
        if (name.startsWith(".")) return true
        if (pathOrUri.contains(".trashed", ignoreCase = true) || pathOrUri.contains(".pending", ignoreCase = true)) return true
        return false
    }

    fun fileExists(context: Context, item: DownloadedFileItem): Boolean {
        if (isTrashedOrTempPath(item.pathOrUri) || isTrashedOrTempPath(item.name)) return false
        return try {
            if (item.pathOrUri.startsWith("content://")) {
                val uri = Uri.parse(item.pathOrUri)
                val isTreeUri = uri.authority == "com.android.externalstorage.documents" || uri.toString().contains("/tree/")
                if (isTreeUri) {
                    val doc = DocumentFile.fromSingleUri(context, uri)
                    doc?.exists() == true && (doc.name == null || !isTrashedOrTempPath(doc.name!!))
                } else {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
                }
            } else {
                val file = File(item.pathOrUri)
                file.exists() && !isTrashedOrTempFile(file)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun getMediaDuration(context: Context, pathOrUri: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            if (pathOrUri.startsWith("content://")) {
                retriever.setDataSource(context, Uri.parse(pathOrUri))
            } else {
                retriever.setDataSource(pathOrUri)
            }
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            time?.toLong() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }

    suspend fun syncWithStorage(context: Context) = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val dao = AppDatabase.getDatabase(appContext).historyDao()

        // Очищаем из БД системные временные файлы (.trashed, .pending)
        val allEntries = dao.getAll()
        allEntries.filter { isTrashedOrTempPath(it.pathOrUri) || isTrashedOrTempPath(it.name) }
            .forEach { dao.deleteById(it.id) }

        val cleanEntries = dao.getAll()
        
        // Группируем потерянные файлы по отпечатку (Размер + Длительность) для быстрого "исцеления"
        val missingMap = cleanEntries.filter { it.isMissing && it.mediaType != MediaType.PHOTO }
            .groupBy { "${it.sizeBytes}_${it.durationMs}" }
            .mapValues { it.value.toMutableList() }
            .toMutableMap()
        
        val existingPaths = cleanEntries.map { it.pathOrUri }.toSet()
        val existingFingerprints = cleanEntries.asSequence().map { 
            val cleanName = it.name.substringBeforeLast('.')
            "${cleanName}_${it.sizeBytes}" 
        }.toMutableSet()
        
        val folders = mutableListOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).path + "/Videx",
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path + "/Videx",
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).path + "/Videx",
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + "/Videx",
        )

        // Добавляем пользовательский каталог из Настроек
        val customUri = SettingsManager.getCustomDirUri()
        if (customUri != null) {
            try {
                val p = customUri.path
                if (!p.isNullOrBlank() && p.contains("primary:")) {
                    val localPath = Environment.getExternalStorageDirectory().absolutePath + "/" + p.substringAfter("primary:")
                    if (!folders.contains(localPath)) {
                        folders.add(localPath)
                    }
                }
            } catch (_: Exception) {}
        }
        
        val foundItems = mutableListOf<DownloadedFileItem>()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        
        for (folderPath in folders) {
            val dir = File(folderPath)
            if (!dir.exists() || !dir.isDirectory) continue
            
            dir.listFiles()?.forEach { file ->
                if (file.isFile && !isTrashedOrTempFile(file)) {
                    if (existingPaths.contains(file.absolutePath)) return@forEach

                    // 🛡️ Защита от дублей MediaStore: проверяем, не сохранен ли файл в БД под URI (content://)
                    val mediaUri = findUriForPath(appContext, file.absolutePath)
                    if (mediaUri != null && existingPaths.contains(mediaUri.toString())) return@forEach

                    val ext = file.extension.lowercase()
                    val mediaType = when (ext) {
                        "jpg", "jpeg", "png", "webp" -> MediaType.PHOTO
                        "mp3", "m4a", "wav", "ogg", "aac", "flac" -> MediaType.AUDIO
                        else -> MediaType.VIDEO
                    }

                    val size = file.length()
                    val duration = if (mediaType != MediaType.PHOTO) getMediaDuration(appContext, file.absolutePath) else 0L
                    val fingerprintByMeta = "${size}_$duration"
                    
                    // 🧠 Защита от дублей исцеления: забираем потерянный файл из списка, чтобы не исцелить им дважды
                    val healingList = if (mediaType != MediaType.PHOTO) missingMap[fingerprintByMeta] else null
                    val healingTarget = if (healingList != null && healingList.isNotEmpty()) healingList.removeAt(0) else null
                    
                    if (healingTarget != null) {
                        // "Исцеляем" старую запись: обновляем путь и имя, сбрасываем статус пропажи
                        dao.update(healingTarget.copy(
                            pathOrUri = file.absolutePath,
                            name = file.nameWithoutExtension,
                            isMissing = false,
                        ))
                        AsyncLogger.log(LogLevel.INFO, "Файл '${healingTarget.name}' был исцелен (новый путь: ${file.name})")
                    } else {
                        // Если это реально новый файл (не из истории)
                        val cleanFileName = file.nameWithoutExtension
                        val nameFingerprint = "${cleanFileName}_${size}"
                        
                        // 🛡️ Защита от дублей сканирования: добавляем в отпечатки сразу же
                        if (!existingFingerprints.contains(nameFingerprint)) {
                            existingFingerprints.add(nameFingerprint)
                            
                            // Кэшируем превью-кадр для найденного видеофайла
                            val localThumb = if (mediaType == MediaType.VIDEO) {
                                cacheThumbnailLocally(appContext, file.absolutePath, mediaType, null)
                            } else null

                            foundItems.add(
                                DownloadedFileItem(
                                    id = UUID.randomUUID().toString(),
                                    pathOrUri = file.absolutePath,
                                    name = cleanFileName,
                                    sizeBytes = size,
                                    sizeFormatted = formatBytes(size),
                                    dateFormatted = dateFormat.format(Date(file.lastModified())),
                                    mediaType = mediaType,
                                    durationMs = duration,
                                    thumbnailUrl = localThumb,
                                    timestamp = file.lastModified(),
                                )
                            )
                        }
                    }
                }
            }
        }
        
        if (foundItems.isNotEmpty()) {
            dao.insertAll(foundItems)
            AsyncLogger.log(LogLevel.INFO, "Восстановлено файлов из памяти: ${foundItems.size}")
        }
    }
}