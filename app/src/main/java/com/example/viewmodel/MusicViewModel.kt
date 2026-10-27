package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdMobManager
import com.example.audio.AudioPlayerManager
import com.example.data.MusicCatalog
import com.example.model.DspSettings
import com.example.model.RepeatMode
import com.example.model.Track
import com.example.model.VisualizerMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class NavigationTab(val title: String) {
    NOW_PLAYING("Pemutar"),
    LIBRARY("Pustaka"),
    DSP_STUDIO("Audio FX"),
    AI_PLAYLIST("AI Cerdas")
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val playerManager = AudioPlayerManager(application)
    private val _allTracks = MutableStateFlow(MusicCatalog.getAllInitialTracks(application))
    val allTracks: StateFlow<List<Track>> = _allTracks.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _activeTab = MutableStateFlow(NavigationTab.NOW_PLAYING)
    val activeTab: StateFlow<NavigationTab> = _activeTab.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _appOpenAdVisible = MutableStateFlow(true)
    val appOpenAdVisible: StateFlow<Boolean> = _appOpenAdVisible.asStateFlow()

    private val _interstitialAdVisible = MutableStateFlow(false)
    val interstitialAdVisible: StateFlow<Boolean> = _interstitialAdVisible.asStateFlow()

    private val _dspSettings = MutableStateFlow(DspSettings())
    val dspSettings: StateFlow<DspSettings> = _dspSettings.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    private val _visualizerMode = MutableStateFlow(VisualizerMode.SPECTRUM_BARS)
    val visualizerMode: StateFlow<VisualizerMode> = _visualizerMode.asStateFlow()

    val currentTrack = playerManager.currentTrack
    val isPlaying = playerManager.isPlaying
    val currentPositionMs = playerManager.currentPositionMs
    val durationMs = playerManager.durationMs
    val amplitudes = playerManager.amplitudes

    private var songChangeCount = 0

    init {
        AdMobManager.initialize(application)
        playerManager.onTrackCompleted = {
            when (_repeatMode.value) {
                RepeatMode.ONE -> currentTrack.value?.let(::playTrack)
                RepeatMode.ALL -> playNext()
                RepeatMode.OFF -> {
                    val tracks = _allTracks.value
                    val currentIndex = tracks.indexOfFirst { it.id == currentTrack.value?.id }
                    if (currentIndex >= 0 && currentIndex < tracks.lastIndex) {
                        playTrack(tracks[currentIndex + 1])
                    }
                }
            }
        }
    }

    fun playTrack(track: Track) {
        playerManager.playTrack(track)
        songChangeCount++
        if (songChangeCount % 5 == 0) _interstitialAdVisible.value = true
    }

    fun togglePlayPause() {
        if (currentTrack.value == null) {
            _allTracks.value.firstOrNull()?.let(::playTrack)
        } else {
            playerManager.togglePlayPause()
        }
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.OFF
        }
    }

    fun playNext() {
        val tracks = _allTracks.value
        if (tracks.isEmpty()) return
        val currentIndex = tracks.indexOfFirst { it.id == currentTrack.value?.id }
        val nextIndex = when {
            currentIndex < 0 -> 0
            currentIndex < tracks.lastIndex -> currentIndex + 1
            _repeatMode.value == RepeatMode.ALL -> 0
            else -> return
        }
        playTrack(tracks[nextIndex])
    }

    fun playPrevious() {
        val tracks = _allTracks.value
        if (tracks.isEmpty()) return
        val currentIndex = tracks.indexOfFirst { it.id == currentTrack.value?.id }
        val previousIndex = when {
            currentIndex > 0 -> currentIndex - 1
            _repeatMode.value == RepeatMode.ALL -> tracks.lastIndex
            else -> 0
        }
        playTrack(tracks[previousIndex])
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
        updateDspSettings(current.copy(currentPresetName = presetName, bands = newBands))
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

    fun dismissAppOpenAd() {
        _appOpenAdVisible.value = false
    }

    fun showInterstitialAd() {
        _interstitialAdVisible.value = true
    }

    fun dismissInterstitialAd() {
        _interstitialAdVisible.value = false
    }

    fun scanDeviceAudio() {
        viewModelScope.launch {
            val scannedTracks = withContext(Dispatchers.IO) {
                MusicCatalog.scanLocalDeviceAudio(getApplication())
            }.distinctBy { it.id }

            // Never erase a previously saved library because a scan failed or returned no results.
            if (scannedTracks.isNotEmpty()) {
                _allTracks.value = scannedTracks
                withContext(Dispatchers.IO) {
                    MusicCatalog.saveTracks(getApplication(), scannedTracks)
                }
            } else if (_allTracks.value.isEmpty()) {
                // Keep startup useful if the device has not yet been scanned successfully.
                _allTracks.value = MusicCatalog.getAllInitialTracks(getApplication())
            }
        }
    }

    override fun onCleared() {
        playerManager.release()
        super.onCleared()
    }
}
