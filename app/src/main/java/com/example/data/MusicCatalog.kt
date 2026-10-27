package com.example.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import android.net.Uri
import android.provider.MediaStore
import com.example.model.Track

object MusicCatalog {
    private const val PREFS_NAME = "wins_aura_library"
    private const val LIBRARY_KEY = "saved_tracks"

    fun loadSavedTracks(context: Context): List<Track> {
        return try {
            val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(LIBRARY_KEY, null) ?: return emptyList()
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                Track(o.optString("id"), o.optString("title"), o.optString("artist"), o.optString("album"), o.optLong("durationMs"), o.optString("audioUrl"), o.optString("coverUrl"), o.optString("genre"), o.optString("lyricsLrc"), o.optBoolean("isOnline", false))
            }.filter { it.id.isNotBlank() && it.audioUrl.isNotBlank() }
        } catch (_: Exception) { emptyList() }
    }

    fun saveTracks(context: Context, tracks: List<Track>) {
        val a = JSONArray()
        tracks.forEach { t -> a.put(JSONObject().apply { put("id",t.id); put("title",t.title); put("artist",t.artist); put("album",t.album); put("durationMs",t.durationMs); put("audioUrl",t.audioUrl); put("coverUrl",t.coverUrl); put("genre",t.genre); put("lyricsLrc",t.lyricsLrc); put("isOnline",t.isOnline) }) }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(LIBRARY_KEY, a.toString()).apply()
    }

    private fun embeddedLyrics(context: Context, uri: Uri): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ""
        val r = MediaMetadataRetriever()
        return try { r.setDataSource(context, uri); "" }
        catch (_: Exception) { "" }
        finally { try { r.release() } catch (_: Exception) { } }
    }

    // No bundled online/demo catalog. The library is populated from the user's device only.
    val STUDIO_TRACKS: List<Track> = emptyList()

    fun getAllInitialTracks(): List<Track> = emptyList()

    fun scanLocalDeviceAudio(context: Context): List<Track> {
        val deviceTracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Lagu Lokal"
                    val artist = it.getString(artistCol) ?: "Artis Lokal"
                    val album = it.getString(albumCol) ?: "Album Penyimpanan"
                    val duration = it.getLong(durCol)
                    val albumId = if (albumIdCol >= 0) it.getLong(albumIdCol) else -1L
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val coverUri = if (albumId > 0) {
                        AudioCoverArtHelper.getAlbumArtUriForAlbumId(albumId)
                    } else ""

                    val lyrics = embeddedLyrics(context, contentUri)
                    deviceTracks.add(
                        Track(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = if (duration > 0) duration else 180000L,
                            audioUrl = contentUri.toString(),
                            coverUrl = coverUri,
                            genre = "Lokal / Perangkat",
                            isOnline = false,
                            moodTags = listOf("Santai", "Fokus Belajar"),
                            lyricsLrc = lyrics
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val uniqueTracks = deviceTracks.distinctBy { it.id }
        saveTracks(context, uniqueTracks)
        return uniqueTracks
    }
}
