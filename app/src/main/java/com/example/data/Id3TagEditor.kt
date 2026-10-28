package com.example.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.model.Mp3Metadata
import com.example.model.Track
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

object Id3TagEditor {

    private const val TAG = "Id3TagEditor"

    /**
     * Reads all metadata fields from a Track using MediaMetadataRetriever
     * and fallback to Track fields.
     */
    fun readMetadata(context: Context, track: Track): Mp3Metadata {
        val retriever = MediaMetadataRetriever()
        var title = track.title
        var artist = track.artist
        var album = track.album
        var albumArtist = track.artist
        var genre = track.genre
        var year = ""
        var trackNumber = ""
        var discNumber = ""
        var composer = ""
        var comment = ""
        var subtitle = ""

        try {
            if (track.isOnline || track.audioUrl.startsWith("http://") || track.audioUrl.startsWith("https://")) {
                retriever.setDataSource(track.audioUrl, HashMap())
            } else {
                retriever.setDataSource(context, Uri.parse(track.audioUrl))
            }

            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }?.let {
                title = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }?.let {
                artist = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.takeIf { it.isNotBlank() }?.let {
                album = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.takeIf { it.isNotBlank() }?.let {
                albumArtist = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.takeIf { it.isNotBlank() }?.let {
                genre = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.takeIf { it.isNotBlank() }?.let {
                year = it
            } ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)?.takeIf { it.isNotBlank() }?.let {
                year = it.take(4)
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.takeIf { it.isNotBlank() }?.let {
                trackNumber = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.takeIf { it.isNotBlank() }?.let {
                discNumber = it
            }
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)?.takeIf { it.isNotBlank() }?.let {
                composer = it
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaMetadataRetriever failed for ${track.title}: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                // Ignore
            }
        }

        return Mp3Metadata(
            title = title,
            artist = artist,
            album = album,
            albumArtist = if (albumArtist.isNotBlank()) albumArtist else artist,
            genre = genre,
            year = year,
            trackNumber = trackNumber,
            discNumber = discNumber,
            composer = composer,
            comment = comment,
            subtitle = subtitle,
            coverUrl = track.coverUrl
        )
    }

    /**
     * Writes ID3v2 tags to file/storage if applicable, updates MediaStore,
     * persists custom album art if changed, and returns the updated Track model.
     */
    fun writeMetadata(context: Context, track: Track, metadata: Mp3Metadata): Result<Track> {
        return try {
            val isLocalUri = !track.isOnline && (track.audioUrl.startsWith("content://") || track.audioUrl.startsWith("file://") || track.audioUrl.startsWith("/"))

            if (isLocalUri) {
                val uri = Uri.parse(track.audioUrl)
                updateMediaStore(context, uri, metadata)
                tryWriteId3Bytes(context, uri, metadata)
            }

            // Persist custom album art if selected from photo picker (content://)
            var finalCoverUrl = metadata.coverUrl
            if (metadata.coverUrl.isNotBlank() && metadata.coverUrl.startsWith("content://")) {
                try {
                    val coverDir = File(context.filesDir, "album_covers").apply { mkdirs() }
                    val coverFile = File(coverDir, "cover_${track.id}_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(Uri.parse(metadata.coverUrl))?.use { input ->
                        coverFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    finalCoverUrl = Uri.fromFile(coverFile).toString()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed copying album cover from content URI: ${e.message}")
                }
            }

            AudioCoverArtHelper.clearArtworkCache(track.audioUrl)

            // Return updated Track instance
            val updatedTrack = track.copy(
                title = metadata.title.ifBlank { track.title },
                artist = metadata.artist.ifBlank { track.artist },
                album = metadata.album.ifBlank { track.album },
                genre = metadata.genre.ifBlank { track.genre },
                coverUrl = finalCoverUrl
            )

            Result.success(updatedTrack)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing metadata: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun updateMediaStore(context: Context, uri: Uri, metadata: Mp3Metadata) {
        if (!uri.toString().startsWith("content://")) return
        try {
            val values = ContentValues().apply {
                if (metadata.title.isNotBlank()) {
                    put(MediaStore.Audio.Media.TITLE, metadata.title)
                }
                if (metadata.artist.isNotBlank()) {
                    put(MediaStore.Audio.Media.ARTIST, metadata.artist)
                }
                if (metadata.album.isNotBlank()) {
                    put(MediaStore.Audio.Media.ALBUM, metadata.album)
                }
                if (metadata.year.isNotBlank()) {
                    metadata.year.toIntOrNull()?.let { put(MediaStore.Audio.Media.YEAR, it) }
                }
                if (metadata.genre.isNotBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    put(MediaStore.Audio.Media.GENRE, metadata.genre)
                }
                if (metadata.composer.isNotBlank()) {
                    put(MediaStore.Audio.Media.COMPOSER, metadata.composer)
                }
                if (metadata.trackNumber.isNotBlank()) {
                    metadata.trackNumber.toIntOrNull()?.let { put(MediaStore.Audio.Media.TRACK, it) }
                }
            }

            context.contentResolver.update(uri, values, null, null)
        } catch (e: Exception) {
            Log.w(TAG, "Could not update MediaStore values for $uri: ${e.message}")
        }
    }

    private fun tryWriteId3Bytes(context: Context, uri: Uri, metadata: Mp3Metadata) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "rw") ?: return
            pfd.use { descriptor ->
                val fis = FileInputStream(descriptor.fileDescriptor)
                val existingBytes = fis.readBytes()
                fis.close()

                val audioStartOffset = findAudioStreamOffset(existingBytes)
                val audioData = if (audioStartOffset < existingBytes.size) {
                    existingBytes.copyOfRange(audioStartOffset, existingBytes.size)
                } else {
                    existingBytes
                }

                val newId3Block = buildId3v2Tag(metadata)

                val fos = FileOutputStream(descriptor.fileDescriptor)
                fos.channel.truncate(0)
                fos.write(newId3Block)
                fos.write(audioData)
                fos.flush()
                fos.close()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct ID3 byte write not permitted or failed: ${e.message}")
        }
    }

    /**
     * Determines where the existing ID3v2 tag ends and the audio frames begin.
     */
    private fun findAudioStreamOffset(bytes: ByteArray): Int {
        if (bytes.size >= 10 && bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte() && bytes[2] == '3'.code.toByte()) {
            val b0 = bytes[6].toInt() and 0x7F
            val b1 = bytes[7].toInt() and 0x7F
            val b2 = bytes[8].toInt() and 0x7F
            val b3 = bytes[9].toInt() and 0x7F
            val tagSize = (b0 shl 21) or (b1 shl 14) or (b2 shl 7) or b3
            return 10 + tagSize
        }
        return 0
    }

    /**
     * Constructs a valid ID3v2.3 tag byte array containing all supplied metadata fields.
     */
    fun buildId3v2Tag(metadata: Mp3Metadata): ByteArray {
        val framesOut = ByteArrayOutputStream()

        fun writeTextFrame(frameId: String, text: String) {
            if (text.isBlank()) return
            val textBytes = text.toByteArray(StandardCharsets.UTF_8)
            // Frame Header: ID (4 bytes), Size (4 bytes), Flags (2 bytes)
            framesOut.write(frameId.toByteArray(StandardCharsets.US_ASCII))
            val payloadSize = 1 + textBytes.size // 1 byte encoding + text
            framesOut.write((payloadSize shr 24) and 0xFF)
            framesOut.write((payloadSize shr 16) and 0xFF)
            framesOut.write((payloadSize shr 8) and 0xFF)
            framesOut.write(payloadSize and 0xFF)
            framesOut.write(0) // Flag 1
            framesOut.write(0) // Flag 2
            framesOut.write(3) // UTF-8 encoding
            framesOut.write(textBytes)
        }

        fun writeCommentFrame(comment: String) {
            if (comment.isBlank()) return
            val commentBytes = comment.toByteArray(StandardCharsets.UTF_8)
            framesOut.write("COMM".toByteArray(StandardCharsets.US_ASCII))
            val payloadSize = 1 + 3 + 1 + commentBytes.size // enc (1) + lang (3) + short desc null (1) + comment
            framesOut.write((payloadSize shr 24) and 0xFF)
            framesOut.write((payloadSize shr 16) and 0xFF)
            framesOut.write((payloadSize shr 8) and 0xFF)
            framesOut.write(payloadSize and 0xFF)
            framesOut.write(0)
            framesOut.write(0)
            framesOut.write(3) // UTF-8
            framesOut.write("eng".toByteArray(StandardCharsets.US_ASCII))
            framesOut.write(0) // empty short description terminator
            framesOut.write(commentBytes)
        }

        writeTextFrame("TIT2", metadata.title)
        writeTextFrame("TPE1", metadata.artist)
        writeTextFrame("TALB", metadata.album)
        writeTextFrame("TPE2", metadata.albumArtist)
        writeTextFrame("TCON", metadata.genre)
        writeTextFrame("TYER", metadata.year)
        writeTextFrame("TRCK", metadata.trackNumber)
        writeTextFrame("TPOS", metadata.discNumber)
        writeTextFrame("TCOM", metadata.composer)
        writeTextFrame("TIT3", metadata.subtitle)
        writeCommentFrame(metadata.comment)

        val framesBytes = framesOut.toByteArray()
        val tagPayloadSize = framesBytes.size

        // Build 10-byte ID3 header
        val header = ByteArray(10)
        header[0] = 'I'.code.toByte()
        header[1] = 'D'.code.toByte()
        header[2] = '3'.code.toByte()
        header[3] = 3 // ID3v2.3
        header[4] = 0 // Revision
        header[5] = 0 // Flags

        // Write synchsafe integer for size
        header[6] = ((tagPayloadSize shr 21) and 0x7F).toByte()
        header[7] = ((tagPayloadSize shr 14) and 0x7F).toByte()
        header[8] = ((tagPayloadSize shr 7) and 0x7F).toByte()
        header[9] = (tagPayloadSize and 0x7F).toByte()

        val fullTag = ByteArrayOutputStream()
        fullTag.write(header)
        fullTag.write(framesBytes)
        return fullTag.toByteArray()
    }
}
