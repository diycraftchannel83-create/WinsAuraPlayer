package com.example.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * One shared location for downloaded lyrics and LRC Studio edits.
 *
 * Android 10+: public files are accessed through MediaStore in the top-level
 * shared-storage folder "WinsLyric" (no broad storage permission required).
 * Android 9 and older: use the public root folder /WinsLyric directly.
 * An internal copy is retained as a reliable app-private fallback.
 */
object LrcStorageHelper {
    private const val TAG = "LrcStorageHelper"
    const val FOLDER_NAME = "WinsLyric"
    private const val MIME_TYPE = "text/plain"

    private val relativePath: String
        get() = "$FOLDER_NAME/"

    fun sanitizeFilename(name: String): String =
        name.replace(Regex("""[\\/:*?"<>|]"""), "_")
            .replace(Regex("[\\u0000-\\u001F]"), "_")
            .trim()
            .trim('.')
            .take(120)

    fun buildLrcFilename(artist: String, title: String): String {
        val safeArtist = sanitizeFilename(if (artist.isBlank()) "Artis" else artist)
        val safeTitle = sanitizeFilename(if (title.isBlank()) "Judul" else title)
        return "$safeArtist-$safeTitle.lrc"
    }

    /**
     * Writes the same LRC text to shared WinsLyric and an internal fallback.
     * Returns the verified internal copy for existing LRC Studio callers.
     */
    fun saveLrcFile(
        context: Context,
        artist: String,
        title: String,
        lrcContent: String
    ): File {
        require(lrcContent.isNotBlank()) { "Konten lirik kosong." }
        val appContext = context.applicationContext
        val filename = buildLrcFilename(artist, title)

        val internalDir = File(appContext.filesDir, FOLDER_NAME)
        if (!internalDir.exists() && !internalDir.mkdirs()) {
            throw IOException("Folder internal WinsLyric tidak dapat dibuat.")
        }
        val internalFile = File(internalDir, filename)
        internalFile.writeText(lrcContent, Charsets.UTF_8)
        check(internalFile.isFile && internalFile.readText(Charsets.UTF_8) == lrcContent) {
            "File lirik internal gagal diverifikasi."
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                writeSharedMediaStore(appContext, filename, lrcContent)
            } else {
                val publicDir = File(Environment.getExternalStorageDirectory(), FOLDER_NAME)
                if (!publicDir.exists() && !publicDir.mkdirs()) {
                    throw IOException("Folder publik WinsLyric tidak dapat dibuat.")
                }
                File(publicDir, filename).writeText(lrcContent, Charsets.UTF_8)
            }
            Log.d(TAG, "LRC tersimpan di shared storage: $FOLDER_NAME/$filename")
        } catch (error: Exception) {
            // The private copy remains usable if shared storage is unavailable.
            Log.w(TAG, "Shared WinsLyric save failed; internal copy retained: ${error.message}", error)
        }
        return internalFile
    }

    /**
     * Reads shared WinsLyric first, then the internal copy and legacy locations.
     */
    fun loadExistingLrc(context: Context, artist: String, title: String): String? {
        val appContext = context.applicationContext
        val filename = buildLrcFilename(artist, title)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                readSharedMediaStore(appContext, filename)?.takeIf { it.isNotBlank() }?.let { return it }
            } else {
                File(File(Environment.getExternalStorageDirectory(), FOLDER_NAME), filename).takeIf { it.isFile && it.length() > 0L }
                    ?.let { return it.readText(Charsets.UTF_8) }
            }
        } catch (error: Exception) {
            Log.w(TAG, "Could not read shared WinsLyric file: ${error.message}")
        }

        val internal = File(File(appContext.filesDir, FOLDER_NAME), filename)
        if (internal.isFile && internal.length() > 0L) {
            return try { internal.readText(Charsets.UTF_8) } catch (_: IOException) { null }
        }

        // Preserve lyrics saved by previous versions.
        val legacyStudio = File(File(appContext.filesDir, "winsplayerlrc"), filename)
        if (legacyStudio.isFile && legacyStudio.length() > 0L) {
            return try { legacyStudio.readText(Charsets.UTF_8) } catch (_: IOException) { null }
        }

        val safeArtist = sanitizeFilename(if (artist.isBlank()) "Unknown track" else artist)
        val safeTitle = sanitizeFilename(if (title.isBlank()) "Unknown track" else title)
        val legacyOnline = File(File(appContext.filesDir, "WinsLirik"), "$safeArtist - $safeTitle.lrc")
        if (legacyOnline.isFile && legacyOnline.length() > 0L) {
            return try { legacyOnline.readText(Charsets.UTF_8) } catch (_: IOException) { null }
        }
        return null
    }

    fun listSavedLrcFiles(context: Context): List<File> {
        val internalDir = File(context.applicationContext.filesDir, FOLDER_NAME)
        return if (internalDir.exists() && internalDir.isDirectory) {
            internalDir.listFiles { file -> file.extension.equals("lrc", ignoreCase = true) }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun writeSharedMediaStore(context: Context, filename: String, content: String) {
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri("external")
        val existing = findSharedUri(context, filename)
        val uri = existing ?: resolver.insert(collection, ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }) ?: throw IOException("Android tidak dapat membuat file di WinsLyric.")

        try {
            resolver.openOutputStream(uri, "wt")?.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
                output.flush()
            } ?: throw IOException("Tidak dapat membuka file lirik untuk ditulis.")

            resolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
        } catch (error: Exception) {
            if (existing == null) {
                try { resolver.delete(uri, null, null) } catch (_: Exception) { }
            }
            throw error
        }
    }

    private fun readSharedMediaStore(context: Context, filename: String): String? {
        val uri = findSharedUri(context, filename) ?: return null
        return context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }

    private fun findSharedUri(context: Context, filename: String): Uri? {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?"
        context.contentResolver.query(
            collection,
            projection,
            selection,
            arrayOf(filename, relativePath),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return ContentUris.withAppendedId(collection, cursor.getLong(0))
            }
        }
        return null
    }
}
