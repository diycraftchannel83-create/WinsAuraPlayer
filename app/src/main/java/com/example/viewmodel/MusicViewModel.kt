package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdMobManager
import com.example.audio.AudioPlayerManager
import com.example.data.MusicCatalog
import com.example.model.DspSettings
import com.example.model.Track
import com.example.model.VisualizerMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

enum class RepeatMode { OFF, ALL, ONE }

enum class NavigationTab(val title: String) {
    NOW_PLAYING("Pemutar"),
    LIBRARY("Pustaka"),
    DSP_STUDIO("Audio FX"),
    AI_PLAYLIST("AI Cerdas")
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val playerManager = AudioPlayerManager(application)

    private val _allTracks = MutableStateFlow<List<Track>>(MusicCatalog.getAllInitialTracks())
    val allTracks: StateFlow<List<Track>> = _allTracks.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()
    fun cycleRepeatMode() { _repeatMode.value = when (_repeatMode.value) { RepeatMode.OFF -> RepeatMode.ALL; RepeatMode.ALL -> RepeatMode.ONE; RepeatMode.ONE -> RepeatMode.OFF } }

    private val _activeTab = MutableStateFlow(NavigationTab.NOW_PLAYING)
    val activeTab: StateFlow<NavigationTab> = _activeTab.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    // Ads state
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

    // Delegated player states
    val currentTrack = playerManager.currentTrack
    val isPlaying = playerManager.isPlaying
    val currentPositionMs = playerManager.currentPositionMs
    val durationMs = playerManager.durationMs
    val amplitudes = playerManager.amplitudes

    private var songChangeCount = 0

    init {
        _allTracks.value = MusicCatalog.loadSavedTracks(application)

        // Initialize AdMob SDK
        AdMobManager.initialize(application)

        playerManager.onTrackCompleted = {
            when (_repeatMode.value) {
                RepeatMode.ONE -> currentTrack.value?.let { playTrack(it) }
                RepeatMode.ALL -> playNext()
                RepeatMode.OFF -> {
                    val tracks = _allTracks.value
                    val index = tracks.indexOfFirst { it.id == currentTrack.value?.id }
                    if (index >= 0 && index < tracks.lastIndex) playTrack(tracks[index + 1])
                }
            }
        }
    }

    fun playTrack(track: Track) {
        playerManager.playTrack(track)
        songChangeCount++
        if (songChangeCount % 5 == 0) {
            _interstitialAdVisible.value = true
        }
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

    fun refreshVisualizer() {
        playerManager.refreshVisualizer()
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

    private val _isScanningMedia = MutableStateFlow(false)
    val isScanningMedia: StateFlow<Boolean> = _isScanningMedia.asStateFlow()

    private val _scanMessage = MutableStateFlow<String?>(null)
    val scanMessage: StateFlow<String?> = _scanMessage.asStateFlow()

    fun reportScanPermissionDenied() {
        _scanMessage.value = "Izin akses audio ditolak. Izinkan akses audio di pengaturan aplikasi, lalu coba pindai lagi."
    }

    fun scanDeviceAudio() {
        if (_isScanningMedia.value) return
        viewModelScope.launch {
            _isScanningMedia.value = true
            _scanMessage.value = "Sedang memindai file audio…"
            try {
                val app = getApplication<Application>()
                // Commit the new library only after both scanning and persistence succeed.
                val uniqueTracks = withContext(Dispatchers.IO) {
                    MusicCatalog.scanLocalDeviceAudio(app).distinctBy { it.id }.also { scanned ->
                        MusicCatalog.saveTracks(app, scanned)
                    }
                }
                _allTracks.value = uniqueTracks
                _scanMessage.value = if (uniqueTracks.isEmpty()) {
                    "Pemindaian selesai, tetapi tidak ada file audio yang ditemukan. Periksa lokasi file dan izin akses audio."
                } else {
                    "Pemindaian selesai: ${uniqueTracks.size} lagu ditemukan."
                }
            } catch (e: Exception) {
                Log.e("WinsAuraPlayer", "Media library scan failed; preserving previous library", e)
                _scanMessage.value = "Pemindaian gagal. Pustaka sebelumnya dipertahankan. Periksa izin dan coba lagi."
            } finally {
                _isScanningMedia.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
