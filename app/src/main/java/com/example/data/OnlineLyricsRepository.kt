package com.example.data

import android.content.Context
import com.example.model.Track
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Fetches timestamped lyrics from LRCLIB and caches successful results locally.
 * Network calls must be made off the main thread.
 */
object OnlineLyricsRepository {
    private const val PREFS_NAME = "online_lyrics_cache"
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    fun getLyrics(context: Context, track: Track): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = cacheKey(track)
        // Keep a real .lrc file in the app's internal storage as well as the fast key-value cache.
        // The directory is private to WinsAuraPlayer and needs no storage permission.
        readSavedLrc(context, track)?.takeIf { it.isNotBlank() }?.let {
            prefs.edit().putString(key, it).apply()
            return it
        }
        prefs.getString(key, null)?.takeIf { it.isNotBlank() }?.let {
            saveLrcFile(context, track, it)
            return it
        }

        val exact = requestObject(
            endpoint = "get",
            track = track
        )
        val exactLrc = exact?.optString("syncedLyrics").orEmpty()
        if (exactLrc.isNotBlank()) {
            prefs.edit().putString(key, exactLrc).apply()
            saveLrcFile(context, track, exactLrc)
            return exactLrc
        }

        val searchResults = requestArray(track)
        for (i in 0 until searchResults.length()) {
            val item = searchResults.optJSONObject(i) ?: continue
            val synced = item.optString("syncedLyrics").orEmpty()
            if (synced.isNotBlank()) {
                prefs.edit().putString(key, synced).apply()
                saveLrcFile(context, track, synced)
                return synced
            }
        }
        return ""
    }

    private fun lyricsDirectory(context: Context): File =
        File(context.applicationContext.filesDir, "WinsLirik").apply { mkdirs() }

    private fun lyricsFile(context: Context, track: Track): File {
        val safeArtist = track.artist.fileSafePart()
        val safeTitle = track.title.fileSafePart()
        val baseName = listOf(safeArtist, safeTitle)
            .filter { it.isNotBlank() }
            .joinToString(" - ")
            .ifBlank { "Unknown track" }
        return File(lyricsDirectory(context), "$baseName.lrc")
    }

    private fun readSavedLrc(context: Context, track: Track): String? {
        val file = lyricsFile(context, track)
        return try {
            if (file.isFile && file.length() > 0L) file.readText(Charsets.UTF_8) else null
        } catch (_: IOException) {
            null
        }
    }

    private fun saveLrcFile(context: Context, track: Track, lyrics: String) {
        if (lyrics.isBlank()) return
        try {
            val file = lyricsFile(context, track)
            file.parentFile?.mkdirs()
            file.writeText(lyrics.trimEnd() + "\n", Charsets.UTF_8)
        } catch (_: IOException) {
            // Lyrics display should still work if file storage is temporarily unavailable.
        }
    }

    private fun String.fileSafePart(): String =
        replace(Regex("[\\\\/:*?\\"<>|\\u0000-\\u001F]"), "_")
            .trim()
            .trim('.')
            .take(80)

    private fun requestObject(endpoint: String, track: Track): JSONObject? {
        val url = buildUrl(endpoint, track).build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "WinsAuraPlayer/1.0 (Android music player)")
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                JSONObject(body)
            }
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun requestArray(track: Track): JSONArray {
        val url = buildUrl("search", track).build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "WinsAuraPlayer/1.0 (Android music player)")
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return JSONArray()
                JSONArray(response.body?.string().orEmpty())
            }
        } catch (_: Exception) {
            JSONArray()
        }
    }

    private fun buildUrl(endpoint: String, track: Track): HttpUrl.Builder {
        val base = HttpUrl.Builder()
            .scheme("https")
            .host("lrclib.net")
            .addPathSegments("api/$endpoint")
            .addQueryParameter("track_name", track.title)
            .addQueryParameter("artist_name", track.artist)
        if (track.album.isNotBlank()) base.addQueryParameter("album_name", track.album)
        if (track.durationMs > 0L) {
            base.addQueryParameter("duration", ((track.durationMs + 500L) / 1000L).toString())
        }
        return base
    }

    private fun cacheKey(track: Track): String {
        val stable = "${track.title.trim().lowercase()}|${track.artist.trim().lowercase()}|${track.durationMs / 1000L}"
        return "lrc_" + stable.hashCode().toUInt().toString(16)
    }
}
