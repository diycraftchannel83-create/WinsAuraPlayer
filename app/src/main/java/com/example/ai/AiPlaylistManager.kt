package com.example.ai

import com.example.model.Track
import kotlinx.coroutines.delay

data class GeneratedPlaylist(
    val title: String,
    val mood: String,
    val genre: String,
    val aiVibeDescription: String,
    val recommendedEqPreset: String,
    val tracks: List<Track>
)

object AiPlaylistManager {

    val MOODS = listOf(
        "Santai",
        "Fokus Belajar",
        "Malam Hari",
        "Energi & Workout",
        "Melankolis",
        "Cyberpunk Drive"
    )

    val GENRES = listOf(
        "Semua Genre",
        "Synthwave / Electronic",
        "Lo-Fi / Mellow",
        "Deep Bass EDM",
        "Acoustic / Folk",
        "Ambient / Chillstep"
    )

    suspend fun generateSmartPlaylist(
        mood: String,
        genre: String,
        allAvailableTracks: List<Track>
    ): GeneratedPlaylist {
        delay(700L)

        val matchingTracks = allAvailableTracks.filter { track ->
            val matchMood = track.moodTags.contains(mood)
            val matchGenre = genre == "Semua Genre" || track.genre.contains(genre, ignoreCase = true)
            matchMood || matchGenre
        }.toMutableList()

        if (matchingTracks.isEmpty()) {
            matchingTracks.addAll(allAvailableTracks.take(4))
        }

        val finalTracks = matchingTracks.distinctBy { it.id }

        val (title, description, eqPreset) = when (mood) {
            "Santai" -> Triple(
                "Aura Chill & Mellow Sanctuary",
                "Playlist santai yang dirancang untuk menurunkan tingkat stres dan menenangkan pikiran dengan ritme lo-fi yang hangat.",
                "Studio Master"
            )
            "Fokus Belajar" -> Triple(
                "Deep Focus: Binaural Flow State",
                "Aliran melodi harmonis berfrekuensi stabil untuk membantu konsentrasi belajar, membaca, atau bekerja bebas gangguan.",
                "Flat / Natural"
            )
            "Malam Hari" -> Triple(
                "Late Night Cyber & Nocturnal Echoes",
                "Suasana malam hari yang luas dengan efek Dolby 3D spatial surround sound dan reverb mendalam.",
                "Dolby 3D Cinema"
            )
            "Energi & Workout" -> Triple(
                "High Voltage: Heavy Bass Protocol",
                "Dentuman ritme bertenaga tinggi dengan kick drum tajam untuk memacu adrenalin dan stamina olahraga.",
                "Bass Booster Extreme"
            )
            "Melankolis" -> Triple(
                "Acoustic Solitude & Strings",
                "Harmoni lembut dan nada gitar akustik alami dengan kehangatan vokal yang menyentuh hati.",
                "Acoustic & Vocal"
            )
            "Cyberpunk Drive" -> Triple(
                "Neon Horizon: Electronic Odyssey",
                "Transisi dinamis antara synthwave modern dan bass booster untuk pengalaman berkendara yang futuristik.",
                "Dolby 3D Cinema"
            )
            else -> Triple(
                "AuraSound AI Curated Mix",
                "Daftar putar cerdas yang dirancang khusus menyesuaikan ritme dan mood Anda.",
                "Studio Master"
            )
        }

        return GeneratedPlaylist(
            title = title,
            mood = mood,
            genre = genre,
            aiVibeDescription = description,
            recommendedEqPreset = eqPreset,
            tracks = finalTracks
        )
    }
}
