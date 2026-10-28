package com.example.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AudioCoverArtHelper {
    private const val MAX_ART_SIZE = 512
    private val memoryCache = object : LruCache<String, Bitmap>(8 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    fun getAlbumArtUriForAlbumId(albumId: Long): String {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        ).toString()
    }

    fun loadTrackCoverBitmap(context: Context, track: com.example.model.Track): Bitmap {
        memoryCache.get(track.audioUrl)?.let { return it }
        try {
            val cover = track.coverUrl
            if (cover.isNotBlank()) {
                val bitmap = decodeCoverSource(context, cover)
                if (bitmap != null) {
                    memoryCache.put(track.audioUrl, bitmap)
                    return bitmap
                }
            }
        } catch (_: Exception) { }

        val retriever = MediaMetadataRetriever()
        try {
            val audio = track.audioUrl
            if (audio.startsWith("content://") || audio.startsWith("file://")) retriever.setDataSource(context, Uri.parse(audio))
            else if (audio.startsWith("http://") || audio.startsWith("https://")) retriever.setDataSource(audio, HashMap())
            else if (audio.isNotBlank()) retriever.setDataSource(audio)
            val bytes = retriever.embeddedPicture
            if (bytes != null) {
                val bitmap = decodeSampled(bytes)
                if (bitmap != null) {
                    memoryCache.put(track.audioUrl, bitmap)
                    return bitmap
                }
            }
        } catch (_: Exception) {
            // Album art is optional; invalid tags must not interrupt playback.
        } finally {
            try { retriever.release() } catch (_: Exception) { }
        }
        return createFallbackBitmap(context)
    }

    private fun createFallbackBitmap(context: Context): Bitmap {
        try {
            val drawable = context.getDrawable(com.example.R.mipmap.ic_launcher)
            if (drawable != null) {
                val width = drawable.intrinsicWidth.coerceIn(1, MAX_ART_SIZE)
                val height = drawable.intrinsicHeight.coerceIn(1, MAX_ART_SIZE)
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                drawable.setBounds(0, 0, width, height)
                drawable.draw(Canvas(bitmap))
                return bitmap
            }
        } catch (_: Exception) { }
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    }

    private fun decodeCoverSource(context: Context, cover: String): Bitmap? {
        if (cover.startsWith("content://") || cover.startsWith("file://")) {
            val bytes = context.contentResolver.openInputStream(Uri.parse(cover))?.use { it.readBytes() } ?: return null
            return decodeSampled(bytes)
        }
        if (cover.startsWith("http://") || cover.startsWith("https://")) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(cover, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeFile(cover, options)
    }

    private fun decodeSampled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        val largest = max(width, height)
        while (largest / (sample * 2) >= MAX_ART_SIZE) sample *= 2
        return sample
    }

    /** Remove cached artwork after the corresponding audio file metadata changes. */
    fun clearArtworkCache(audioUri: String) {
        memoryCache.remove(audioUri)
    }

    suspend fun loadEmbeddedCoverArt(context: Context, audioUri: String): Bitmap? = withContext(Dispatchers.IO) {
        if (audioUri.isBlank()) return@withContext null
        memoryCache.get(audioUri)?.let { return@withContext it }

        val retriever = MediaMetadataRetriever()
        try {
            if (audioUri.startsWith("content://") || audioUri.startsWith("file://")) {
                retriever.setDataSource(context, Uri.parse(audioUri))
            } else if (audioUri.startsWith("http://") || audioUri.startsWith("https://")) {
                retriever.setDataSource(audioUri, HashMap())
            } else {
                retriever.setDataSource(audioUri)
            }
            val artBytes = retriever.embeddedPicture
            if (artBytes != null) {
                val bitmap = decodeSampled(artBytes)
                if (bitmap != null) {
                    memoryCache.put(audioUri, bitmap)
                    return@withContext bitmap
                }
            }
        } catch (e: Exception) {
            // No embedded artwork or unreadable format
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
        null
    }
}
