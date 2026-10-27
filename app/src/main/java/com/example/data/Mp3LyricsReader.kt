package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * Reads the unsynchronised lyrics (USLT) frame from an ID3v2-tagged MP3.
 *
 * MediaMetadataRetriever does not expose ID3 lyric frames consistently across Android
 * versions and codecs, so this reader inspects the small ID3 header/tag directly.
 */
object Mp3LyricsReader {
    private const val TAG = "Mp3LyricsReader"
    private const val MAX_TAG_SIZE_BYTES = 4 * 1024 * 1024

    fun readLyrics(context: Context, audioUri: String): String {
        return try {
            val uri = Uri.parse(audioUri)
            context.contentResolver.openInputStream(uri)?.use(::readLyricsFromId3Tag).orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Could not read embedded lyrics from $audioUri", e)
            ""
        }
    }

    internal fun readLyricsFromId3Tag(input: InputStream): String {
        val header = ByteArray(10)
        if (!readFully(input, header)) return ""
        if (header[0] != 'I'.code.toByte() ||
            header[1] != 'D'.code.toByte() ||
            header[2] != '3'.code.toByte()
        ) return ""

        val majorVersion = header[3].toInt() and 0xff
        if (majorVersion !in 3..4) return ""

        val tagSize = readSynchsafeInt(header, 6)
        if (tagSize <= 0 || tagSize > MAX_TAG_SIZE_BYTES) return ""

        val tag = ByteArray(tagSize)
        if (!readFully(input, tag)) return ""

        var offset = 0
        val hasExtendedHeader = (header[5].toInt() and 0x40) != 0
        if (hasExtendedHeader) {
            if (tag.size < 4) return ""
            offset = if (majorVersion == 3) readBigEndianInt(tag, 0) + 4
            else readSynchsafeInt(tag, 0)
            if (offset < 0 || offset > tag.size) return ""
        }

        // ID3v2.4 can mark frames as unsynchronised; this parser supports regular ID3v2.3/2.4 frames.
        while (offset + 10 <= tag.size) {
            // ID3v2.4 uses frame-level unsynchronisation in some files. Skip those frames
            // if flagged, instead of attempting to decode potentially altered text bytes.
            val id = String(tag, offset, 4, StandardCharsets.ISO_8859_1)
            if (id.all { it == '\u0000' }) break
            if (!id.all { it in 'A'..'Z' || it in '0'..'9' }) break

            val frameSize = if (majorVersion == 4) {
                readSynchsafeInt(tag, offset + 4)
            } else {
                readBigEndianInt(tag, offset + 4)
            }
            if (frameSize <= 0 || frameSize > tag.size || offset + 10 + frameSize > tag.size) break

            val bodyStart = offset + 10
            if (id == "USLT") {
                val lyric = decodeUnsynchronisedLyrics(tag, bodyStart, frameSize)
                if (lyric.isNotBlank()) return lyric
            }

            offset = bodyStart + frameSize
        }

        return ""
    }

    private fun decodeUnsynchronisedLyrics(tag: ByteArray, start: Int, length: Int): String {
        if (length < 4) return ""
        val encoding = tag[start].toInt() and 0xff
        val descriptionStart = start + 4 // text encoding byte + 3-byte language
        val end = start + length
        if (descriptionStart > end) return ""

        val terminatorWidth = if (encoding == 1 || encoding == 2) 2 else 1
        val textStart = findTextStart(tag, descriptionStart, end, terminatorWidth)
        if (textStart > end) return ""

        val charset: Charset = when (encoding) {
            0 -> StandardCharsets.ISO_8859_1
            1 -> StandardCharsets.UTF_16
            2 -> StandardCharsets.UTF_16BE
            3 -> StandardCharsets.UTF_8
            else -> return ""
        }
        return String(tag, textStart, end - textStart, charset)
            .trim('\u0000', '\uFEFF', '\r', '\n', ' ', '\t')
    }

    private fun findTextStart(bytes: ByteArray, start: Int, end: Int, terminatorWidth: Int): Int {
        var index = start
        if (terminatorWidth == 1) {
            while (index < end && bytes[index].toInt() != 0) index++
            return if (index < end) index + 1 else end + 1
        }

        while (index + 1 < end) {
            if (bytes[index].toInt() == 0 && bytes[index + 1].toInt() == 0) return index + 2
            index += 2
        }
        return end + 1
    }

    private fun readSynchsafeInt(bytes: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 4 > bytes.size) return -1
        return ((bytes[offset].toInt() and 0x7f) shl 21) or
            ((bytes[offset + 1].toInt() and 0x7f) shl 14) or
            ((bytes[offset + 2].toInt() and 0x7f) shl 7) or
            (bytes[offset + 3].toInt() and 0x7f)
    }

    private fun readBigEndianInt(bytes: ByteArray, offset: Int): Int {
        if (offset < 0 || offset + 4 > bytes.size) return -1
        return ((bytes[offset].toInt() and 0xff) shl 24) or
            ((bytes[offset + 1].toInt() and 0xff) shl 16) or
            ((bytes[offset + 2].toInt() and 0xff) shl 8) or
            (bytes[offset + 3].toInt() and 0xff)
    }

    private fun readFully(input: InputStream, target: ByteArray): Boolean {
        var offset = 0
        while (offset < target.size) {
            val count = input.read(target, offset, target.size - offset)
            if (count < 0) return false
            if (count == 0) continue
            offset += count
        }
        return true
    }
}
