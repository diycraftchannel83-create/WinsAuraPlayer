package com.example.data

import com.example.model.LyricsLine
import com.example.model.Track

/**
 * Keeps the latest LRC Studio draft until persistence reports success.
 * The same guard is used by track switches and editor close so every exit path
 * follows the same save-before-clear rule.
 */
class LrcDraftCoordinator {
    data class Draft(val track: Track, val lines: List<LyricsLine>)

    private var pendingDraft: Draft? = null

    @Synchronized
    fun update(track: Track, lines: List<LyricsLine>) {
        pendingDraft = Draft(track, lines.toList())
    }

    @Synchronized
    fun pending(): Draft? = pendingDraft

    @Synchronized
    fun clearSavedTrack(trackId: String) {
        if (pendingDraft?.track?.id == trackId) pendingDraft = null
    }

    fun saveBeforeTrackSwitch(persist: (Track, List<LyricsLine>) -> Boolean): Boolean =
        savePending(persist)

    fun saveBeforeEditorClose(persist: (Track, List<LyricsLine>) -> Boolean): Boolean =
        savePending(persist)

    @Synchronized
    fun savePending(persist: (Track, List<LyricsLine>) -> Boolean): Boolean {
        val draft = pendingDraft ?: return true
        if (draft.lines.isEmpty()) {
            pendingDraft = null
            return true
        }

        val saved = try {
            persist(draft.track, draft.lines.toList())
        } catch (_: Exception) {
            false
        }
        if (saved && pendingDraft == draft) {
            pendingDraft = null
        }
        return saved
    }
}
