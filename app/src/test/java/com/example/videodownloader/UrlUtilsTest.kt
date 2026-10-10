package com.example.videodownloader

import com.example.videodownloader.utils.extractUrlFromText
import com.example.videodownloader.utils.isValidUrl
import com.example.videodownloader.utils.translateNetworkError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UrlUtilsTest {

    @Test
    fun testIsValidUrl() {
        assertTrue(isValidUrl("https://youtube.com/watch?v=12345"))
        assertTrue(isValidUrl("http://vt.tiktok.com/ABCDE/"))
        assertTrue(isValidUrl("https://www.instagram.com/reel/123/"))

        assertFalse(isValidUrl(""))
        assertFalse(isValidUrl("not_a_url"))
        assertFalse(isValidUrl("http://a"))
    }

    @Test
    fun testExtractUrlFromText() {
        val rawText1 = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", extractUrlFromText(rawText1))

        val rawText2 = "tiktok.com/@user/video/1234567890123456789"
        assertEquals("https://tiktok.com/@user/video/1234567890123456789", extractUrlFromText(rawText2))

        val rawText3 = "Зацени reels instagram.com/reel/C123456/ зацени"
        assertEquals("https://instagram.com/reel/C123456/", extractUrlFromText(rawText3))
    }

    @Test
    fun testTranslateNetworkError() {
        val errUnknownHost = "java.net.UnknownHostException: Unable to resolve host"
        assertEquals(
            "Отсутствует подключение к интернету. Проверьте Wi-Fi или мобильные данные.",
            translateNetworkError(errUnknownHost)
        )

        val errTimeout = "java.net.SocketTimeoutException: timeout"
        assertEquals(
            "Превышено время ожидания ответа от сервера. Интернет слишком слабый или заблокирован.",
            translateNetworkError(errTimeout)
        )
    }
}
