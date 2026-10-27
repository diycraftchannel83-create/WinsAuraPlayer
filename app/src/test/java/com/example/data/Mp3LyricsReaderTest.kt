package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class Mp3LyricsReaderTest {

    @Test
    fun readsUnsynchronisedLyricsFromId3V23Tag() {
        val lyrics = "Baris pertama\nBaris kedua"
        val frameBody = byteArrayOf(3, 'e'.code.toByte(), 'n'.code.toByte(), 'g'.code.toByte(), 0) +
            lyrics.toByteArray(StandardCharsets.UTF_8)
        val frame = frameHeader("USLT", frameBody.size) + frameBody
        val tag = "ID3".toByteArray(StandardCharsets.ISO_8859_1) +
            byteArrayOf(3, 0, 0) + synchsafe(frame.size) + frame

        val result = Mp3LyricsReader.readLyricsFromStreamForTest(tag)
        assertEquals(lyrics, result)
    }

    @Test
    fun returnsEmptyWhenMp3HasNoLyricsFrame() {
        val body = byteArrayOf(3, 'e'.code.toByte(), 'n'.code.toByte(), 'g'.code.toByte(), 0) +
            "Artist".toByteArray(StandardCharsets.UTF_8)
        val frame = frameHeader("TIT2", body.size) + body
        val tag = "ID3".toByteArray(StandardCharsets.ISO_8859_1) +
            byteArrayOf(3, 0, 0) + synchsafe(frame.size) + frame

        assertTrue(Mp3LyricsReader.readLyricsFromStreamForTest(tag).isEmpty())
    }

    private fun frameHeader(id: String, size: Int): ByteArray =
        id.toByteArray(StandardCharsets.ISO_8859_1) +
            byteArrayOf((size shr 24).toByte(), (size shr 16).toByte(), (size shr 8).toByte(), size.toByte(), 0, 0)

    private fun synchsafe(size: Int): ByteArray = byteArrayOf(
        ((size shr 21) and 0x7f).toByte(),
        ((size shr 14) and 0x7f).toByte(),
        ((size shr 7) and 0x7f).toByte(),
        (size and 0x7f).toByte()
    )
}
