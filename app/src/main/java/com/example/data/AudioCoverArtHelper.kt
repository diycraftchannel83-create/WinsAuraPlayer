package com.example.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AudioCoverArtHelper {
    private val memoryCache = LruCache<String, Bitmap>(50)

    fun getAlbumArtUriForAlbumId(albumId: Long): String {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        ).toString()
    }

    fun loadTrackCoverBitmap(context: Context, track: com.example.model.Track): Bitmap {
        try {
            val cover = track.coverUrl
            if (cover.isNotBlank()) {
                val uri = Uri.parse(cover)
                val bitmap = if (cover.startsWith("content://") || cover.startsWith("file://")) context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } else if (!cover.startsWith("http")) BitmapFactory.decodeFile(cover) else null
                if (bitmap != null) return bitmap
            }
        } catch (_: Exception) { }
        val retriever = MediaMetadataRetriever()
        try {
            val audio = track.audioUrl
            if (audio.startsWith("content://") || audio.startsWith("file://")) retriever.setDataSource(context, Uri.parse(audio)) else if (audio.startsWith("http")) retriever.setDataSource(audio, HashMap()) else if (audio.isNotBlank()) retriever.setDataSource(audio)
            val bytes = retriever.embeddedPicture
            if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { return it }
        } catch (_: Exception) { } finally { try { retriever.release() } catch (_: Exception) { } }
        return BitmapFactory.decodeResource(context.resources, com.example.R.mipmap.ic_launcher)
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
                val bitmap = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
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
