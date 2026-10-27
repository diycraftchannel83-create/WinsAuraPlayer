package com.example

import com.example.ads.AdMobManager
import com.example.ai.AiPlaylistManager
import com.example.data.MusicCatalog
import com.example.model.DspSettings
import com.example.model.LrcParser
import com.example.model.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
    fun initialCatalogIsEmptyUntilDeviceMediaIsScanned() {
        assertTrue(MusicCatalog.STUDIO_TRACKS.isEmpty())
        assertTrue(MusicCatalog.getAllInitialTracks().isEmpty())
    }

    @Test
    fun aiPlaylistManagerHandlesEmptyLocalLibrary() = runBlocking {
        val playlist = AiPlaylistManager.generateSmartPlaylist(
            mood = "Santai",
            genre = "Lo-Fi / Mellow",
            allAvailableTracks = emptyList()
        )

        assertNotNull(playlist)
        assertTrue(playlist.tracks.isEmpty())
        assertEquals("Studio Master", playlist.recommendedEqPreset)
    }

    @Test
    fun aiPlaylistManagerSelectsTracksFromLocalLibrary() = runBlocking {
        val localTrack = Track(
            id = "local_1",
            title = "Test Track",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 180_000L,
            audioUrl = "content://media/external/audio/media/1",
            coverUrl = "",
            genre = "Lo-Fi / Mellow",
            moodTags = listOf("Santai")
        )

        val playlist = AiPlaylistManager.generateSmartPlaylist(
            mood = "Santai",
            genre = "Lo-Fi / Mellow",
            allAvailableTracks = listOf(localTrack)
        )

        assertEquals(1, playlist.tracks.size)
        assertEquals("local_1", playlist.tracks.single().id)
        assertEquals("Studio Master", playlist.recommendedEqPreset)
    }

    @Test
    fun dspSettingsHasTenBandsAndValidPresets() {
        val dsp = DspSettings()
        assertEquals(10, dsp.bands.size)
        assertTrue(DspSettings.PRESETS.containsKey("Studio Master"))
        assertTrue(DspSettings.PRESETS.containsKey("Bass Booster Extreme"))
        assertTrue(DspSettings.PRESETS.containsKey("Dolby 3D Cinema"))
        assertTrue(DspSettings.PRESETS.containsKey("Flat / Natural"))
    }

    @Test
    fun adMobManagerHasConfiguredAdUnits() {
        assertEquals("ca-app-pub-1254774552944426/9151767484", AdMobManager.APP_OPEN_AD_UNIT_ID)
        assertEquals("ca-app-pub-1254774552944426/3914322052", AdMobManager.INTERSTITIAL_AD_UNIT_ID)
    }

}
