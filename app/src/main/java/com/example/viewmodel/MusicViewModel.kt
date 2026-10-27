package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlayerManager
import com.example.data.MusicCatalog
import com.example.model.DspSettings
import com.example.model.Track
import com.example.model.VisualizerMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class NavigationTab(val title: String) {
    LIBRARY("Pustaka"),
    WINS_ONLINE("Wins & Daring"),
    DSP_STUDIO("DSP & EQ"),
    AI_PLAYLIST("AI Cerdas")
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val playerManager = AudioPlayerManager(application)

    private val _allTracks = MutableStateFlow<List<Track>>(MusicCatalog.getAllInitialTracks())
    val allTracks: StateFlow<List<Track>> = _allTracks.asStateFlow()

    private val _activeTab = MutableStateFlow(NavigationTab.WINS_ONLINE)
    val activeTab: StateFlow<NavigationTab> = _activeTab.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _welcomeAdVisible = MutableStateFlow(true)
    val welcomeAdVisible: StateFlow<Boolean> = _welcomeAdVisible.asStateFlow()

    private val _dspSettings = MutableStateFlow(DspSettings())
    val dspSettings: StateFlow<DspSettings> = _dspSettings.asStateFlow()

    private val _visualizerMode = MutableStateFlow(VisualizerMode.SPECTRUM_BARS)
    val visualizerMode: StateFlow<VisualizerMode> = _visualizerMode.asStateFlow()

    // Delegated player states
    val currentTrack = playerManager.currentTrack
    val isPlaying = playerManager.isPlaying
    val currentPositionMs = playerManager.currentPositionMs
    val durationMs = playerManager.durationMs
    val amplitudes = playerManager.amplitudes

    init {
        // Automatically queue the first track from artist wins
        val initialWins = MusicCatalog.WINS_TRACKS.firstOrNull()
        if (initialWins != null) {
            // Pre-select track without auto-starting sound until user taps or presses play
        }

        playerManager.onTrackCompleted = {
            playNext()
        }
    }

    fun playTrack(track: Track) {
        playerManager.playTrack(track)
    }

    fun togglePlayPause() {
        if (currentTrack.value == null) {
            val first = _allTracks.value.firstOrNull()
            if (first != null) {
                playTrack(first)
            }
        } else {
            playerManager.togglePlayPause()
        }
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun playNext() {
        val tracks = _allTracks.value
        if (tracks.isEmpty()) return
        val current = currentTrack.value
        val currentIndex = tracks.indexOfFirst { it.id == current?.id }
        val nextIndex = if (currentIndex in 0 until tracks.lastIndex) currentIndex + 1 else 0
        playTrack(tracks[nextIndex])
    }

    fun playPrevious() {
        val tracks = _allTracks.value
        if (tracks.isEmpty()) return
        val current = currentTrack.value
        val currentIndex = tracks.indexOfFirst { it.id == current?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else tracks.lastIndex
        playTrack(tracks[prevIndex])
    }

    fun updateDspSettings(settings: DspSettings) {
        _dspSettings.value = settings
        playerManager.applyDspSettings(settings)
    }

    fun applyEqPreset(presetName: String) {
        val levels = DspSettings.PRESETS[presetName] ?: return
        val current = _dspSettings.value
        val newBands = current.bands.mapIndexed { idx, band ->
            band.copy(levelMb = levels.getOrElse(idx) { 0 })
        }
        val updated = current.copy(
            currentPresetName = presetName,
            bands = newBands
        )
        updateDspSettings(updated)
    }

    fun changeVisualizerMode(mode: VisualizerMode) {
        _visualizerMode.value = mode
    }

    fun setActiveTab(tab: NavigationTab) {
        _activeTab.value = tab
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    fun dismissWelcomeAd() {
        _welcomeAdVisible.value = false
    }

    fun scanDeviceAudio() {
        viewModelScope.launch {
            val localTracks = MusicCatalog.scanLocalDeviceAudio(getApplication())
            if (localTracks.isNotEmpty()) {
                val combined = (MusicCatalog.getAllInitialTracks() + localTracks).distinctBy { it.id }
                _allTracks.value = combined
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
