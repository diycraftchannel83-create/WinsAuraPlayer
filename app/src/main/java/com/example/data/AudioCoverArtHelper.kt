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
