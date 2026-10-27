package com.example

import com.example.ads.AdMobManager
import com.example.ai.AiPlaylistManager
import com.example.data.MusicCatalog
import com.example.model.DspSettings
import com.example.model.LrcParser
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
    fun studioTracks_containsStudioCatalog() {
        val studioTracks = MusicCatalog.STUDIO_TRACKS
        assertTrue("Studio catalog must contain tracks", studioTracks.isNotEmpty())
        studioTracks.forEach { track ->
            assertTrue("Track title must not be blank", track.title.isNotBlank())
            assertTrue("Track duration must be greater than 0", track.durationMs > 0)
            assertNotNull(track.audioUrl)
        }
    }

    @Test
    fun aiPlaylistManager_generatesMoodBasedPlaylist() = runBlocking {
        val allTracks = MusicCatalog.getAllInitialTracks()
        val playlist = AiPlaylistManager.generateSmartPlaylist(
            mood = "Santai",
            genre = "Lo-Fi / Mellow",
            allAvailableTracks = allTracks
        )

        assertNotNull(playlist)
        assertTrue(playlist.tracks.isNotEmpty())
        assertEquals("Studio Master", playlist.recommendedEqPreset)
    }

    @Test
    fun dspSettings_has10BandsAndValidPresets() {
        val dsp = DspSettings()
        assertEquals(10, dsp.bands.size)
        assertTrue(DspSettings.PRESETS.containsKey("Studio Master"))
        assertTrue(DspSettings.PRESETS.containsKey("Bass Booster Extreme"))
        assertTrue(DspSettings.PRESETS.containsKey("Dolby 3D Cinema"))
    }

    @Test
    fun adMobManager_configuredWithCorrectAdUnits() {
        assertEquals("ca-app-pub-1254774552944426/9151767484", AdMobManager.APP_OPEN_AD_UNIT_ID)
        assertEquals("ca-app-pub-1254774552944426/3914322052", AdMobManager.INTERSTITIAL_AD_UNIT_ID)
    }

    @Test
    fun audioCoverArtHelper_constructsValidAlbumArtUri() {
        val uri = com.example.data.AudioCoverArtHelper.getAlbumArtUriForAlbumId(12345L)
        assertTrue(uri.contains("content://media/external/audio/albumart/12345"))
    }
}
