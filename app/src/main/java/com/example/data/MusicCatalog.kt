package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.model.Track

object MusicCatalog {
    val STUDIO_TRACKS = listOf(
        Track(
            id = "studio_neon_01",
            title = "Midnight Cyberwave",
            artist = "Aura Sound Lab",
            album = "Cyber Horizon",
            durationMs = 184000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=600&q=80",
            genre = "Synthwave / Electronic",
            isOnline = false,
            moodTags = listOf("Cyberpunk Drive", "Energi & Workout"),
            lyricsLrc = """
                [00:02.00]Neon lights reflection on the wet asphalt
                [00:07.50]Racing through the city where time has no fault
                [00:13.20]Synthesizer pulse cutting through the silent air
                [00:18.80]Echoing the rhythm with electric flair
                [00:24.40]Sub-frequencies hum deep in the machine
                [00:30.00]Brightest soundwaves that you've ever seen
                [00:36.10]Accelerating forward, break through the sound barrier
                [00:42.50]Dolby 3D spatial sound expanding everywhere
                [00:48.20]Feel the neon wave, let the equalizer glow
                [00:54.00]Deep bass booster keeping up the flow
            """.trimIndent()
        ),
        Track(
            id = "studio_chill_02",
            title = "Rainy Coffee Sanctuary",
            artist = "Echo & Velvet",
            album = "Lo-Fi Essentials",
            durationMs = 162000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            coverUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=600&q=80",
            genre = "Lo-Fi / Mellow",
            isOnline = false,
            moodTags = listOf("Santai", "Fokus Belajar", "Malam Hari"),
            lyricsLrc = """
                [00:02.00]Warm cup in hand, watching rain pour down
                [00:07.00]Gentle rhodes keys dancing around the town
                [00:13.00]Analog warmth drifting like autumn leaves
                [00:19.00]Simple peace of mind that the heart believes
                [00:25.00]Vinyl dust crackle on a vintage tape
                [00:31.00]A quiet melody, an audio escape
                [00:37.00]Soft low-end rhythm, steady and sublime
                [00:43.00]Lost inside the tranquility of time
            """.trimIndent()
        ),
        Track(
            id = "studio_bass_03",
            title = "Sub-Bass Resonance Protocol",
            artist = "Atmos Soundworks",
            album = "Frequency Odyssey",
            durationMs = 175000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            coverUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=600&q=80",
            genre = "Deep Bass EDM",
            isOnline = false,
            moodTags = listOf("Energi & Workout", "Cyberpunk Drive"),
            lyricsLrc = """
                [00:03.00]Activate the virtualizer, engage the master gain
                [00:09.10]Bass pressure building up like a thunder train
                [00:15.00]Clean 30 hertz rumble shaking up the floor
                [00:21.00]Pushing every speaker right beyond the core
                [00:27.20]Spatial sound rotation thirty-six degrees
                [00:33.00]Dolby surround power floating on the breeze
                [00:39.10]Dynamic response, maximum punch
                [00:45.30]High impact audio, hear the crisp crunch
            """.trimIndent()
        ),
        Track(
            id = "studio_acoustic_04",
            title = "Golden Meadow Solitude",
            artist = "Aura Acoustic Ensemble",
            album = "Wooden Strings",
            durationMs = 155000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            coverUrl = "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=600&q=80",
            genre = "Acoustic / Folk",
            isOnline = false,
            moodTags = listOf("Santai", "Melankolis"),
            lyricsLrc = """
                [00:02.50]Sunlight breaking softly through the tall pine trees
                [00:08.00]Acoustic guitar whispers in the morning breeze
                [00:14.00]Pure string vibration, natural and clear
                [00:20.00]Bringing all the beauty of the silence near
                [00:26.00]Chamber reverb echoes in the open sky
                [00:32.00]Watching gentle clouds wandering on high
                [00:38.50]Warmth in every strum, melody serene
                [00:45.00]The calmest soundscape that has ever been
            """.trimIndent()
        ),
        Track(
            id = "studio_night_05",
            title = "Nocturnal Horizon 3D",
            artist = "Kinetics Sound Studio",
            album = "After Midnight",
            durationMs = 168000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            coverUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=600&q=80",
            genre = "Ambient / Chillstep",
            isOnline = false,
            moodTags = listOf("Malam Hari", "Melankolis", "Fokus Belajar"),
            lyricsLrc = """
                [00:04.00]Midnight stars align in constellation grace
                [00:10.00]Traveling through time in an acoustic space
                [00:16.00]Subtle delay lines cascading down the air
                [00:22.00]Gentle audio frequencies everywhere
                [00:28.00]Close your eyes and listen, let the mind relax
                [00:34.00]Studio grade fidelity taking off the tax
                [00:41.00]Serenade of echoes, drifting out of sight
                [00:48.00]Peaceful meditation through the velvet night
            """.trimIndent()
        ),
        Track(
            id = "studio_focus_06",
            title = "Deep Coding Flow State",
            artist = "Synthetix Labs",
            album = "Neural Beats",
            durationMs = 180000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            coverUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=600&q=80",
            genre = "Electronic / Lo-Fi",
            isOnline = false,
            moodTags = listOf("Fokus Belajar", "Santai"),
            lyricsLrc = """
                [00:03.00]Constant metric rhythm, steady clockwork tick
                [00:09.00]Synapses connecting, inspiration quick
                [00:15.00]Clean harmonic balance, zero noise ahead
                [00:21.00]Focus like an arrow through the paths we tread
                [00:27.00]Binaural frequencies opening the mind
                [00:33.00]Leaving all distractions far away behind
                [00:40.00]Perfect stereo image, balanced left and right
                [00:46.00]Working through the rhythm deep into the night
            """.trimIndent()
        )
    )

    fun getAllInitialTracks(): List<Track> {
        return STUDIO_TRACKS
    }

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
