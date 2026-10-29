package com.example

import com.example.data.LrcDraftCoordinator
import com.example.model.LyricsLine
import com.example.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcDraftCoordinatorTest {
    private fun track(id: String) = Track(
        id = id,
        title = "Title $id",
        artist = "Test Artist",
        album = "Test Album",
        durationMs = 120_000L,
        audioUrl = "content://media/$id",
        coverUrl = "",
        genre = "Test"
    )

    private val lines = listOf(LyricsLine(1_250L, "Draft lyric"))

    @Test
    fun nextTrackSavesDraftBeforeSwitchAndClearsOnlyAfterSuccess() {
        val coordinator = LrcDraftCoordinator()
        val original = track("one")
        coordinator.update(original, lines)
        var savedTrackId: String? = null

        val canSwitch = coordinator.saveBeforeTrackSwitch { source, savedLines ->
            savedTrackId = source.id
            assertEquals(lines, savedLines)
            true
        }

        assertTrue(canSwitch)
        assertEquals("one", savedTrackId)
        assertNull(coordinator.pending())
    }

    @Test
    fun previousTrackKeepsDraftWhenPersistenceFails() {
        val coordinator = LrcDraftCoordinator()
        val original = track("two")
        coordinator.update(original, lines)

        val canSwitch = coordinator.saveBeforeTrackSwitch { _, _ -> false }

        assertFalse(canSwitch)
        assertNotNull(coordinator.pending())
        assertEquals("two", coordinator.pending()?.track?.id)
        assertEquals(lines, coordinator.pending()?.lines)
    }

    @Test
    fun autoNextCanRetryAndClearDraftOnlyAfterSuccessfulSave() {
        val coordinator = LrcDraftCoordinator()
        coordinator.update(track("three"), lines)

        assertFalse(coordinator.saveBeforeTrackSwitch { _, _ -> throw IllegalStateException("disk full") })
        assertNotNull(coordinator.pending())

        assertTrue(coordinator.saveBeforeTrackSwitch { _, _ -> true })
        assertNull(coordinator.pending())
    }

    @Test
    fun closingEditorUsesSameSaveGuardAndRetainsDraftOnFailure() {
        val coordinator = LrcDraftCoordinator()
        coordinator.update(track("four"), lines)

        assertFalse(coordinator.saveBeforeEditorClose { _, _ -> false })
        assertNotNull(coordinator.pending())

        assertTrue(coordinator.saveBeforeEditorClose { _, _ -> true })
        assertNull(coordinator.pending())
    }

    @Test
    fun aNewerDraftIsNotClearedByAnOlderSaveCompletion() {
        val coordinator = LrcDraftCoordinator()
        val original = track("five")
        coordinator.update(original, lines)
        val newerLines = listOf(LyricsLine(2_500L, "Newer draft"))

        val saved = coordinator.saveBeforeTrackSwitch { _, _ ->
            coordinator.update(original, newerLines)
            true
        }

        assertTrue(saved)
        assertEquals(newerLines, coordinator.pending()?.lines)
    }
}
