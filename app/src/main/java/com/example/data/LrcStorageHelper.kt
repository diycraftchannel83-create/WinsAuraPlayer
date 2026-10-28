package com.example.data

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File

object LrcStorageHelper {
    private const val TAG = "LrcStorageHelper"
    const val FOLDER_NAME = "winsplayerlrc"

    fun sanitizeFilename(name: String): String {
        return name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()
    }

    fun buildLrcFilename(artist: String, title: String): String {
        val safeArtist = sanitizeFilename(if (artist.isBlank()) "Artis" else artist)
        val safeTitle = sanitizeFilename(if (title.isBlank()) "Judul" else title)
        return "$safeArtist-$safeTitle.lrc"
    }

    /**
     * Saves LRC content to internal app storage and external storage under folder `winsplayerlrc`.
     * File format: [Artist]-[Title].lrc
     * Returns the primary saved File object.
     */
    fun saveLrcFile(
        context: Context,
        artist: String,
        title: String,
        lrcContent: String
    ): File {
        val filename = buildLrcFilename(artist, title)

        // 1. App internal files storage: /data/data/<pkg>/files/winsplayerlrc/
        val internalDir = File(context.filesDir, FOLDER_NAME)
        if (!internalDir.exists()) {
            internalDir.mkdirs()
        }
        val internalFile = File(internalDir, filename)
        internalFile.writeText(lrcContent)
        Log.d(TAG, "Saved internal LRC file: ${internalFile.absolutePath}")

        // 2. App external files dir: /storage/emulated/0/Android/data/<pkg>/files/winsplayerlrc/
        try {
            val extAppDir = File(context.getExternalFilesDir(null), FOLDER_NAME)
            if (!extAppDir.exists()) {
                extAppDir.mkdirs()
            }
            val extAppFile = File(extAppDir, filename)
            extAppFile.writeText(lrcContent)
            Log.d(TAG, "Saved external app LRC file: ${extAppFile.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "External app storage write fallback: ${e.message}")
        }

        // 3. Shared device internal storage: /storage/emulated/0/winsplayerlrc/
        try {
            val publicDir = File(Environment.getExternalStorageDirectory(), FOLDER_NAME)
            if (!publicDir.exists()) {
                publicDir.mkdirs()
            }
            val publicFile = File(publicDir, filename)
            publicFile.writeText(lrcContent)
            Log.d(TAG, "Saved public storage LRC file: ${publicFile.absolutePath}")
        } catch (e: Exception) {
            Log.w(TAG, "Public storage write fallback: ${e.message}")
        }

        return internalFile
    }

    /**
     * Attempts to find an existing saved LRC file in `winsplayerlrc` for the given artist and title.
     */
    fun loadExistingLrc(context: Context, artist: String, title: String): String? {
        val filename = buildLrcFilename(artist, title)

        // Check internal files
        val internalFile = File(File(context.filesDir, FOLDER_NAME), filename)
        if (internalFile.exists() && internalFile.length() > 0) {
            return internalFile.readText()
        }

        // Check external app files
        try {
            val extAppFile = File(File(context.getExternalFilesDir(null), FOLDER_NAME), filename)
            if (extAppFile.exists() && extAppFile.length() > 0) {
                return extAppFile.readText()
            }
        } catch (_: Exception) {}

        // Check public storage
        try {
            val publicFile = File(File(Environment.getExternalStorageDirectory(), FOLDER_NAME), filename)
            if (publicFile.exists() && publicFile.length() > 0) {
                return publicFile.readText()
            }
        } catch (_: Exception) {}

        return null
    }

    /**
     * Lists all generated LRC files in the internal winsplayerlrc directory.
     */
    fun listSavedLrcFiles(context: Context): List<File> {
        val internalDir = File(context.filesDir, FOLDER_NAME)
        return if (internalDir.exists() && internalDir.isDirectory) {
            internalDir.listFiles { file -> file.extension.equals("lrc", ignoreCase = true) }?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }
}
