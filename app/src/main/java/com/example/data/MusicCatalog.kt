package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.model.Track

object MusicCatalog {
    val WINS_TRACKS = listOf(
        Track(
            id = "wins_chill_01",
            title = "chill",
            artist = "wins",
            album = "chill (2023)",
            durationMs = 142000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
            genre = "Lo-Fi / Hip-Hop",
            isOnline = true,
            isWinsTrack = true,
            moodTags = listOf("Santai", "Malam Hari", "Fokus Belajar", "Melankolis"),
            lyricsLrc = """
                [00:02.00]Late night in the lab, beats rolling steady
                [00:07.50]Tape hiss in the background, minds already heavy
                [00:13.20]Nebraska to the coast, keeping the soul light
                [00:18.80]Bass frequency hums through the quiet night
                [00:24.40]Lost in the sound, we don't count the miles
                [00:30.00]Just pure lo-fi warmth turning into smiles
                [00:36.10]Spin the vinyl backward, let the rhythm breathe
                [00:42.50]Underground cadence that you can believe
                [00:48.20]Chill out now, let the frequencies align
                [00:54.00]Bass booster booming in standard design
                [01:00.00]Dolby surround wrapping round the room
                [01:06.50]Wins on the track clearing all the gloom
                [01:12.80]Yeah, keeping it chill, forever real
            """.trimIndent()
        ),
        Track(
            id = "wins_whoa_02",
            title = "whoa",
            artist = "wins",
            album = "whoa - Single (2026)",
            durationMs = 158000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
            genre = "Underground Hip-Hop",
            isOnline = true,
            isWinsTrack = true,
            moodTags = listOf("Energi & Workout", "Cyberpunk Drive", "Fokus Belajar"),
            lyricsLrc = """
                [00:03.00]Whoa, step into the frame, feel the voltage rise
                [00:09.10]Speakers clipping softly underneath the neon skies
                [00:15.00]Grind never pauses when the beat starts to hit
                [00:21.00]Heavy kick, snappy snare, every bar legit
                [00:27.20]Sub-bass rumbling down the boulevard
                [00:33.00]Wins with the flow pulling every wildcard
                [00:39.10]Hold on, let the equalizer do the talking
                [00:45.30]Headphones on, into greatness we are walking
                [00:52.00]Whoa, feel the 3D spatial sound expansion
                [00:58.50]Turn the dial up, audio satisfaction
            """.trimIndent()
        ),
        Track(
            id = "wins_step_03",
            title = "step",
            artist = "wins",
            album = "b.tape (2026)",
            durationMs = 135000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
            genre = "Alternative Hip-Hop",
            isOnline = true,
            isWinsTrack = true,
            moodTags = listOf("Energi & Workout", "Santai", "Cyberpunk Drive"),
            lyricsLrc = """
                [00:01.50]One step, two step, lock the groove in place
                [00:06.00]Watch the spectrum visualizer dancing in the space
                [00:12.00]Raw analog texture cut right from the cassette
                [00:18.00]Golden age nostalgia that nobody can forget
                [00:24.00]Step back, step up, drop the 10-band tone
                [00:30.00]Crisp high frequencies singing on their own
                [00:36.50]Wins delivering the heat across the state line
                [00:43.00]Timeless energy, handcrafted and divine
            """.trimIndent()
        ),
        Track(
            id = "wins_gotham_04",
            title = "gotham",
            artist = "wins",
            album = "gotham (2025)",
            durationMs = 170000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            coverUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&q=80",
            genre = "Dark Lo-Fi / Rap",
            isOnline = true,
            isWinsTrack = true,
            moodTags = listOf("Malam Hari", "Melankolis", "Cyberpunk Drive"),
            lyricsLrc = """
                [00:04.00]Shadows in the skyline, city lights glow dim
                [00:10.00]Gotham nocturnal vibes filled up to the brim
                [00:16.00]Synthesizer echoes bouncing off the concrete wall
                [00:22.00]Echoes of the reverb answering the call
                [00:28.00]Keep the volume maxed, let the bassline take control
                [00:34.00]Sonic therapy healing every broken soul
                [00:41.00]Late night chronicles, wins on the beat
                [00:48.00]Smooth midnight drive down an empty street
            """.trimIndent()
        ),
        Track(
            id = "wins_lovewins_05",
            title = "Love, wins",
            artist = "wins",
            album = "Love, wins (2025)",
            durationMs = 164000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            coverUrl = "https://images.unsplash.com/photo-1487180144351-b8472da7d491?w=500&q=80",
            genre = "Soulful Lo-Fi",
            isOnline = true,
            isWinsTrack = true,
            moodTags = listOf("Santai", "Melankolis", "Fokus Belajar"),
            lyricsLrc = """
                [00:02.00]Warm rhodes piano floating in the air
                [00:07.50]Melodies that tell you that somebody's there
                [00:13.00]Heartfelt cadence over vinyl crackle tone
                [00:19.00]Never truly walking in this world alone
                [00:25.00]Love always wins when the sound waves unite
                [00:31.00]Turning pitch black darkness into morning light
                [00:37.00]Harmonies surrounding with acoustic grace
                [00:44.00]Finding serenity in this digital space
            """.trimIndent()
        ),
        Track(
            id = "wins_price_06",
            title = "price",
            artist = "wins",
            album = "price (2023)",
            durationMs = 150000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            coverUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80",
            genre = "Underground Rap",
            isOnline = true,
            isWinsTrack = true,
            moodTags = listOf("Fokus Belajar", "Energi & Workout", "Malam Hari"),
            lyricsLrc = """
                [00:03.00]Count the dedication, calculate the cost
                [00:09.00]Lessons turned to wisdom, nothing ever lost
                [00:15.00]Raw drum chops echoing right and left
                [00:21.00]True underground music in its purest depth
                [00:27.00]Price of the passion, sweat on the console
                [00:33.00]Bass booster punch vibrating the whole soul
                [00:40.00]Steady flow, steady rhythm, never fall behind
                [00:46.00]Mastered perfection inside of your mind
            """.trimIndent()
        )
    )

    val ONLINE_STATION_TRACKS = listOf(
        Track(
            id = "online_cyber_01",
            title = "Neon Overdrive 2099",
            artist = "Aura Synthetics",
            album = "Cyber Horizon",
            durationMs = 184000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80",
            genre = "Synthwave / Cyberpunk",
            isOnline = true,
            isWinsTrack = false,
            moodTags = listOf("Cyberpunk Drive", "Energi & Workout"),
            lyricsLrc = """
                [00:03.00]Grid lines expanding into infinity
                [00:08.50]Speeding on the highway through the cyber city
                [00:14.00]Analog arpeggiator powering the drive
                [00:20.00]Neon pulses keeping digital dream alive
            """.trimIndent()
        ),
        Track(
            id = "online_chill_02",
            title = "Midnight Coffee Drift",
            artist = "Tokyo Lo-Fi Lab",
            album = "Rainy Shibuya",
            durationMs = 175000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            coverUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500&q=80",
            genre = "Lo-Fi Beats",
            isOnline = true,
            isWinsTrack = false,
            moodTags = listOf("Santai", "Malam Hari", "Fokus Belajar"),
            lyricsLrc = """
                [00:02.00]Raindrops tapping softly on the window pane
                [00:07.00]Steam from hot coffee washing out the strain
                [00:13.00]Gentle guitar loop swinging slow and sweet
                [00:19.00]Cozy vibes settling right into the beat
            """.trimIndent()
        ),
        Track(
            id = "online_bass_03",
            title = "Sub-Frequency Protocol",
            artist = "Dolby Atmosfera",
            album = "Subwoofer Odyssey",
            durationMs = 192000,
            audioUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
            coverUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=500&q=80",
            genre = "Deep Bass EDM",
            isOnline = true,
            isWinsTrack = false,
            moodTags = listOf("Energi & Workout", "Cyberpunk Drive"),
            lyricsLrc = """
                [00:05.00]Activate the virtualizer, engage surround sound
                [00:12.00]Deep sub-harmonics vibrating the ground
                [00:19.00]Frequency sweeping from thirty up to twelve k
                [00:26.00]Total sonic immersion taking you away
            """.trimIndent()
        )
    )

    fun getAllInitialTracks(): List<Track> {
        return WINS_TRACKS + ONLINE_STATION_TRACKS
    }

    fun scanLocalDeviceAudio(context: Context): List<Track> {
        val deviceTracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
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
                val durCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Lagu Lokal"
                    val artist = it.getString(artistCol) ?: "Artis Tidak Dikenal"
                    val album = it.getString(albumCol) ?: "Album Lokal"
                    val duration = it.getLong(durCol)
                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    deviceTracks.add(
                        Track(
                            id = "local_$id",
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = if (duration > 0) duration else 180000L,
                            audioUrl = contentUri.toString(),
                            coverUrl = "",
                            genre = "Lokal / Perangkat",
                            isOnline = false,
                            isWinsTrack = false,
                            moodTags = listOf("Santai", "Fokus Belajar"),
                            lyricsLrc = "[00:00.00]Pemutaran berkas lokal dari penyimpanan perangkat"
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
