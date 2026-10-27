package com.example

import com.example.ai.AiPlaylistManager
import com.example.data.MusicCatalog
import com.example.model.DspSettings
import com.example.model.LrcParser
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun lrcParser_parsesTimestampsCorrectly() {
        val sampleLrc = """
            [00:02.00]First line of lyrics
            [00:08.50]Second line with half second
            [01:15.250]Third line with milliseconds
        """.trimIndent()

        val parsed = LrcParser.parse(sampleLrc)
        assertEquals(3, parsed.size)
        assertEquals(2000L, parsed[0].timestampMs)
        assertEquals("First line of lyrics", parsed[0].text)
        assertEquals(8500L, parsed[1].timestampMs)
        assertEquals(75250L, parsed[2].timestampMs)
    }

    @Test
    fun winsTracks_containsSpotifyArtistWinsCatalog() {
        val winsTracks = MusicCatalog.WINS_TRACKS
        assertTrue("Wins catalog must contain tracks", winsTracks.isNotEmpty())
        winsTracks.forEach { track ->
            assertEquals("wins", track.artist)
            assertTrue("Track must be marked as wins track", track.isWinsTrack)
            assertTrue("Track duration must be greater than 0", track.durationMs > 0)
            assertNotNull(track.audioUrl)
        }
    }

    @Test
    fun aiPlaylistManager_generatesMoodBasedPlaylistWithWins() = runBlocking {
        val allTracks = MusicCatalog.getAllInitialTracks()
        val playlist = AiPlaylistManager.generateSmartPlaylist(
            mood = "Santai",
            genre = "Lo-Fi / Hip-Hop",
            allAvailableTracks = allTracks
        )

        assertNotNull(playlist)
        assertTrue(playlist.tracks.isNotEmpty())
        val hasWins = playlist.tracks.any { it.isWinsTrack }
        assertTrue("Generated playlist must include artist wins", hasWins)
        assertEquals("Wins Lo-Fi HipHop", playlist.recommendedEqPreset)
    }

    @Test
    fun dspSettings_has10BandsAndValidPresets() {
        val dsp = DspSettings()
        assertEquals(10, dsp.bands.size)
        assertTrue(DspSettings.PRESETS.containsKey("Studio Master"))
        assertTrue(DspSettings.PRESETS.containsKey("Bass Booster Extreme"))
        assertTrue(DspSettings.PRESETS.containsKey("Dolby 3D Cinema"))
        assertTrue(DspSettings.PRESETS.containsKey("Wins Lo-Fi HipHop"))
    }
}
