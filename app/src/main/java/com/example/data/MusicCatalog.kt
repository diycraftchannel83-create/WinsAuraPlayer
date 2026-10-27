package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.model.Track

object MusicCatalog {
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
                            lyricsLrc = "[00:00.00]Pemutaran audio lokal dari penyimpanan perangkat"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return deviceTracks
    }
}
