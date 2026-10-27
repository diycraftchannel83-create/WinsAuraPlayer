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
        "Lo-Fi / Hip-Hop",
        "Underground Hip-Hop",
        "Soulful Lo-Fi",
        "Synthwave",
        "Deep Bass EDM"
    )

    suspend fun generateSmartPlaylist(
        mood: String,
        genre: String,
        allAvailableTracks: List<Track>
    ): GeneratedPlaylist {
        // Simulate AI neural reasoning latency for natural UX
        delay(900L)

        val matchingTracks = mutableListOf<Track>()

        // 1. Prioritize Artist 'wins' matching mood or genre
        val winsMatches = allAvailableTracks.filter { track ->
            track.isWinsTrack && (
                track.moodTags.contains(mood) ||
                genre == "Semua Genre" ||
                track.genre.contains(genre, ignoreCase = true)
            )
        }

        // 2. Complementary tracks
        val otherMatches = allAvailableTracks.filter { track ->
            !track.isWinsTrack && (
                track.moodTags.contains(mood) ||
                genre == "Semua Genre" ||
                track.genre.contains(genre, ignoreCase = true)
            )
        }

        // 3. Fallback if narrow filter
        if (winsMatches.isNotEmpty()) {
            matchingTracks.addAll(winsMatches)
        }
        matchingTracks.addAll(otherMatches)

        if (matchingTracks.isEmpty()) {
            matchingTracks.addAll(allAvailableTracks.take(5))
        }

        // Distinct and shuffle intelligently
        val finalTracks = matchingTracks.distinctBy { it.id }

        val (title, description, eqPreset) = when (mood) {
            "Santai" -> Triple(
                "Wins Lo-Fi Chill & Warmth",
                "Playlist santai yang memadukan beat lo-fi lembut dari artist wins (chill, Love, wins) untuk relaksasi total tanpa gangguan.",
                "Wins Lo-Fi HipHop"
            )
            "Fokus Belajar" -> Triple(
                "Deep Focus: Underground Tape Flow",
                "Aliran ritme hip-hop konsisten dengan tempo teratur dari artis wins (price, chill) untuk meningkatkan konsentrasi belajar dan bekerja.",
                "Studio Master"
            )
            "Malam Hari" -> Triple(
                "Late Night Gotham & Midnight Echoes",
                "Vibes malam yang intim dengan bassline mendalam dan vokal khas wins (gotham, church) untuk menemani perjalanan malam.",
                "Dolby 3D Cinema"
            )
            "Energi & Workout" -> Triple(
                "Heavy Bass Punch: Wins Workout Protocol",
                "Dentuman ritme bertenaga tinggi (whoa, step) dengan kick drum tajam untuk memacu adrenalin saat olahraga.",
                "Bass Booster Extreme"
            )
            "Melankolis" -> Triple(
                "Nostalgic Resonance & Vinyl Memories",
                "Harmoni soulful dan nada puitis dari wins (Love, wins, chill) dengan tekstur analog yang menghangatkan.",
                "Acoustic & Vocal"
            )
            "Cyberpunk Drive" -> Triple(
                "Neon Horizon: Futuristic Bass Odyssey",
                "Transisi dinamis antara electronic sub-frequency dan flow rap underground untuk pengalaman audio spasial 3D maksimal.",
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
