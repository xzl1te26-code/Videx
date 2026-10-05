package com.example.videodownloader.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.PowerManager
import android.os.Build

fun isValidUrl(url: String): Boolean {
    val trimmed = url.trim().lowercase()
    return (trimmed.startsWith("http://") || trimmed.startsWith("https://")) &&
            trimmed.contains(".") && trimmed.length > 8
}

fun extractUrlFromText(text: String): String {
    if (text.isBlank()) return ""
    
    // Регулярка для поиска ссылок с протоколом или без (начинающихся на www)
    // А также захватываем кириллицу для ВК и прочих
    val urlPattern = """(?:https?://|www\.)[a-zA-Z0-9.\-/_?#\[\]@!$&'()*+,;=%А-Яа-я]+""".toRegex()
    var match = urlPattern.find(text)?.value ?: ""
    
    if (match.isEmpty()) {
        // Попытка найти доменные имена популярных сервисов без протокола
        val domains = listOf("youtube.com", "youtu.be", "tiktok.com", "vk.com", "vkvideo.ru", "instagram.com")
        for (domain in domains) {
            if (text.contains(domain, ignoreCase = true)) {
                val startIdx = text.indexOf(domain, ignoreCase = true)
                val part = text.substring(startIdx).split(Regex("""\s"""))[0]
                match = part
                break
            }
        }
    }

    if (match.isEmpty()) return text.trim()

    // Если нашли www. или просто домен — добавляем https
    return if (!match.startsWith("http://", ignoreCase = true) && !match.startsWith("https://", ignoreCase = true)) {
        "https://$match"
    } else {
        match
    }
}

fun isWifiConnected(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
           capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
}

fun isNetworkAvailable(context: Context?): Boolean {
    if (context == null) return true
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
}

fun detectPlatformName(url: String): String {
    return when {
        url.contains("youtube.com") || url.contains("youtu.be") -> "YouTube"
        url.contains("tiktok.com") -> "TikTok"
        url.contains("instagram.com") -> "Instagram"
        url.contains("vk.com") || url.contains("vkvideo.ru") -> "VK Видео"
        url.contains("twitter.com") || url.contains("x.com") -> "X (Twitter)"
        url.contains("pinterest.com") || url.contains("pin.it") -> "Pinterest"
        url.contains("reddit.com") -> "Reddit"
        url.contains("soundcloud.com") -> "SoundCloud"
        url.contains("vimeo.com") -> "Vimeo"
        else -> "Медиа-поток"
    }
}
