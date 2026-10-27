package com.example.data

import android.content.Context
import com.example.model.Track
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl
import org.json.JSONArray
import org.json.JSONObject
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
        prefs.getString(key, null)?.takeIf { it.isNotBlank() }?.let { return it }

        val exact = requestObject(
            endpoint = "get",
            track = track
        )
        val exactLrc = exact?.optString("syncedLyrics").orEmpty()
        if (exactLrc.isNotBlank()) {
            prefs.edit().putString(key, exactLrc).apply()
            return exactLrc
        }

        val searchResults = requestArray(track)
        for (i in 0 until searchResults.length()) {
            val item = searchResults.optJSONObject(i) ?: continue
            val synced = item.optString("syncedLyrics").orEmpty()
            if (synced.isNotBlank()) {
                prefs.edit().putString(key, synced).apply()
                return synced
            }
        }
        return ""
    }

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
