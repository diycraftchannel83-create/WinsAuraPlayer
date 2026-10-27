package com.example.data

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import com.example.model.Track

object MusicCatalog {
    private const val PREFS_NAME = "wins_aura_music_library"
    private const val KEY_TRACKS = "saved_track_records"

    val STUDIO_TRACKS: List<Track> = emptyList()

    fun getAllInitialTracks(context: Context): List<Track> = loadSavedTracks(context)

    fun saveTracks(context: Context, tracks: List<Track>) {
        val encoded = tracks.joinToString("\n") { track ->
            listOf(
                track.id, track.title, track.artist, track.album,
                track.durationMs.toString(), track.audioUrl, track.coverUrl, track.genre,
                track.lyricsLrc, track.isOnline.toString(), track.moodTags.joinToString("\u001f")
            ).joinToString("|") { encodeField(it) }
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TRACKS, encoded)
            .apply()
    }

    private fun loadSavedTracks(context: Context): List<Track> {
        val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TRACKS, null) ?: return emptyList()
        return saved.lineSequence().filter { it.isNotBlank() }.mapNotNull { line ->
            try {
                val fields = line.split("|").map(::decodeField)
                if (fields.size < 11) return@mapNotNull null
                Track(
                    id = fields[0],
                    title = fields[1],
                    artist = fields[2],
                    album = fields[3],
                    durationMs = fields[4].toLongOrNull() ?: 0L,
                    audioUrl = fields[5],
                    coverUrl = fields[6],
                    genre = fields[7],
                    lyricsLrc = fields[8],
                    isOnline = fields[9].toBooleanStrictOrNull() ?: false,
                    moodTags = fields[10].split("\u001f").filter { it.isNotBlank() }
                )
            } catch (e: Exception) {
                Log.w("MusicCatalog", "Skipping invalid saved track row", e)
                null
            }
        }.toList()
    }

    private fun encodeField(value: String): String =
        Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun decodeField(value: String): String =
        String(Base64.decode(value, Base64.DEFAULT), Charsets.UTF_8)

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
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val audioUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val albumId = if (albumIdCol >= 0) cursor.getLong(albumIdCol) else -1L
                    val coverUri = if (albumId > 0) {
                        AudioCoverArtHelper.getAlbumArtUriForAlbumId(albumId)
                    } else ""
                    deviceTracks.add(
                        Track(
                            id = "local_$id",
                            title = cursor.getString(titleCol)?.takeIf(String::isNotBlank) ?: "Lagu Lokal",
                            artist = cursor.getString(artistCol)?.takeIf(String::isNotBlank) ?: "Artis Lokal",
                            album = cursor.getString(albumCol)?.takeIf(String::isNotBlank) ?: "Album Penyimpanan",
                            durationMs = cursor.getLong(durationCol).takeIf { it > 0 } ?: 180000L,
                            audioUrl = audioUri.toString(),
                            coverUrl = coverUri,
                            genre = "Lokal / Perangkat",
                            isOnline = false,
                            moodTags = listOf("Santai", "Fokus Belajar"),
                            lyricsLrc = readEmbeddedLyrics(context, audioUri)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("MusicCatalog", "Failed to scan local audio", e)
        }
        return deviceTracks
    }


    private fun readEmbeddedLyrics(context: Context, uri: Uri): String {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            (
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LYRIC)
                    ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LYRICS)
            )?.trim().orEmpty()
        } catch (e: Exception) {
            Log.w("MusicCatalog", "Embedded lyrics unavailable for $uri", e)
            ""
        } finally {
            try { retriever.release() } catch (_: Exception) { }
        }
    }
}
