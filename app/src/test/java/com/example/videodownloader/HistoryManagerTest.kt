package com.example.videodownloader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HistoryManagerTest {

    @Test
    fun testFormatBytes() {
        assertEquals("0 КБ", HistoryManager.formatBytes(0L))
        assertEquals("0,5 МБ", HistoryManager.formatBytes(500L * 1024L))
        assertEquals("15,5 МБ", HistoryManager.formatBytes((15.5 * 1024 * 1024).toLong()))
        assertEquals("1,50 ГБ", HistoryManager.formatBytes((1.5 * 1024 * 1024 * 1024).toLong()))
    }

    @Test
    fun testIsTrashedOrTempFile() {
        assertTrue(HistoryManager.isTrashedOrTempFile(File(".hidden_file")))
        assertTrue(HistoryManager.isTrashedOrTempFile(File("video.trashed-123456.mp4")))
        assertTrue(HistoryManager.isTrashedOrTempFile(File("download.tmp")))
        assertTrue(HistoryManager.isTrashedOrTempFile(File("video.ytdl")))
        assertTrue(HistoryManager.isTrashedOrTempFile(File("video.part")))

        assertFalse(HistoryManager.isTrashedOrTempFile(File("my_video.mp4")))
        assertFalse(HistoryManager.isTrashedOrTempFile(File("audio_track.mp3")))
        assertFalse(HistoryManager.isTrashedOrTempFile(File("photo.jpg")))
    }

    @Test
    fun testIsTrashedOrTempPath() {
        assertTrue(HistoryManager.isTrashedOrTempPath("/storage/emulated/0/Videx/.trashed-123.mp4"))
        assertTrue(HistoryManager.isTrashedOrTempPath("/storage/emulated/0/Videx/.pending-456.jpg"))
        assertTrue(HistoryManager.isTrashedOrTempPath("content://media/external/file/123.trashed"))

        assertFalse(HistoryManager.isTrashedOrTempPath("/storage/emulated/0/Videx/my_cool_video.mp4"))
        assertFalse(HistoryManager.isTrashedOrTempPath("content://media/external/images/media/789"))
    }
}
