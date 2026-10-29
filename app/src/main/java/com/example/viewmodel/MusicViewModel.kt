package com.example.viewmodel

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.IntentSender
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdMobManager
import com.example.audio.AudioPlayerManager
import com.example.audio.PlaybackService
import com.example.data.MusicCatalog
import com.example.data.Id3TagEditor
import com.example.data.LrcStorageHelper
import com.example.data.LrcDraftCoordinator
import com.example.model.LyricsLine
import com.example.model.LrcParser
import com.example.model.Mp3Metadata
import com.example.model.DspSettings
import com.example.model.Track
import com.example.model.VisualizerMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

data class MetadataWritePermissionRequest(
    val uri: Uri,
    val actionIntentSender: IntentSender? = null
)

enum class RepeatMode { OFF, ALL, ONE }

enum class NavigationTab(val title: String) {
    NOW_PLAYING("Pemutar"),
    LIBRARY("Pustaka"),
    DSP_STUDIO("Audio FX"),
    AI_PLAYLIST("AI Cerdas"),
    SETTINGS("Settings")
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val _isLrcStudioOpen = MutableStateFlow(false)
    val isLrcStudioOpen: StateFlow<Boolean> = _isLrcStudioOpen.asStateFlow()
    fun openLrcStudio() { _isLrcStudioOpen.value = true }
    fun closeLrcStudio() {
        // Do not dismiss the editor if the last draft could not be persisted.
        if (lrcDraftCoordinator.saveBeforeEditorClose(::saveAndApplyLyrics)) {
            _isLrcStudioOpen.value = false
        } else {
            _lrcSaveMessage.value = "Lirik belum tersimpan. Draft dipertahankan; periksa penyimpanan lalu coba lagi."
        }
    }

    // Shared guard for Next, Previous, auto-next, track selection, and editor close.
    private val lrcDraftCoordinator = LrcDraftCoordinator()

    private val _lrcSaveMessage = MutableStateFlow<String?>(null)
    val lrcSaveMessage: StateFlow<String?> = _lrcSaveMessage.asStateFlow()
    fun clearLrcSaveMessage() { _lrcSaveMessage.value = null }

    fun updateLrcStudioDraft(track: Track, lines: List<LyricsLine>) {
        lrcDraftCoordinator.update(track, lines)
    }

    fun saveExplicitLyrics(track: Track, lines: List<LyricsLine>): Boolean {
        val saved = saveAndApplyLyrics(track, lines)
        if (saved) lrcDraftCoordinator.clearSavedDraft(track, lines)
        return saved
    }

    /**
     * Returns true only after the LRC file has been written and verified.
     * The coordinator clears the draft only after this function succeeds.
     */
    fun saveAndApplyLyrics(track: Track, lines: List<LyricsLine>): Boolean {
        return try {
            val content = LrcParser.formatToLrc(lines, track.artist, track.title)
            val file = LrcStorageHelper.saveLrcFile(
                context = getApplication(),
                artist = track.artist,
                title = track.title,
                lrcContent = content
            )
            check(file.isFile && file.readText() == content) {
                "File LRC tidak dapat diverifikasi setelah penyimpanan."
            }

            val updated = _allTracks.value.map { if (it.id == track.id) it.copy(lyricsLrc = content) else it }
            MusicCatalog.saveTracks(getApplication(), updated)
            _allTracks.value = updated
            if (currentTrack.value?.id == track.id) playerManager.updateCurrentTrackLyrics(content)

            // The coordinator clears only the exact draft after this verified save returns.
            _lrcSaveMessage.value = null
            true
        } catch (error: Exception) {
            Log.e("WinsAuraPlayer", "LRC save failed; keeping the draft", error)
            _lrcSaveMessage.value = "Gagal menyimpan lirik. Draft tetap dipertahankan; periksa ruang penyimpanan dan coba lagi."
            false
        }
    }

    private val playerManager = AudioPlayerManager(application)

    private val _editingTrack = MutableStateFlow<Track?>(null)
    val editingTrack: StateFlow<Track?> = _editingTrack.asStateFlow()
    fun openMetadataEditor(track: Track) { _editingTrack.value = track }
    fun closeMetadataEditor() { _editingTrack.value = null }

    private val _metadataSaveMessage = MutableStateFlow<String?>(null)
    val metadataSaveMessage: StateFlow<String?> = _metadataSaveMessage.asStateFlow()
    fun clearMetadataSaveMessage() { _metadataSaveMessage.value = null }

    private val _metadataWritePermissionRequest = MutableStateFlow<MetadataWritePermissionRequest?>(null)
    val metadataWritePermissionRequest: StateFlow<MetadataWritePermissionRequest?> =
        _metadataWritePermissionRequest.asStateFlow()

    private var pendingMetadataWrite: Pair<Track, Mp3Metadata>? = null

    fun cancelMetadataWritePermissionRequest() {
        pendingMetadataWrite = null
        _metadataWritePermissionRequest.value = null
        _metadataSaveMessage.value = "Izin tulis tidak diberikan. Metadata audio belum diubah."
    }

    fun retryPendingMetadataSave() {
        val pending = pendingMetadataWrite
        pendingMetadataWrite = null
        _metadataWritePermissionRequest.value = null
        if (pending != null) saveTrackMetadata(pending.first, pending.second)
    }

    fun saveTrackMetadata(track: Track, metadata: Mp3Metadata) {
        viewModelScope.launch {
            _metadataSaveMessage.value = null
            val result = withContext(Dispatchers.IO) {
                Id3TagEditor.writeMetadata(getApplication(), track, metadata)
            }
            result.onSuccess { updatedTrack ->
                pendingMetadataWrite = null
                _metadataWritePermissionRequest.value = null
                val updated = _allTracks.value.map { if (it.id == track.id) updatedTrack else it }
                _allTracks.value = updated
                MusicCatalog.saveTracks(getApplication(), updated)
                playerManager.updateCurrentTrackMetadata(updatedTrack)
                _editingTrack.value = null
                _metadataSaveMessage.value = "Metadata berhasil disimpan ke file audio."
            }.onFailure { error ->
                Log.e("WinsAuraPlayer", "Metadata save failed", error)
                val recoverable = error as? RecoverableSecurityException
                if (error is SecurityException && track.audioUrl.startsWith("content://")) {
                    pendingMetadataWrite = track to metadata
                    _metadataWritePermissionRequest.value = MetadataWritePermissionRequest(
                        uri = Uri.parse(track.audioUrl),
                        actionIntentSender = recoverable?.userAction?.actionIntent?.intentSender
                    )
                    _metadataSaveMessage.value =
                        "File audio memerlukan izin tulis. Setujui permintaan Android agar metadata dapat disimpan."
                } else {
                    _metadataSaveMessage.value =
                        "Metadata gagal disimpan ke file audio: ${error.message ?: "periksa izin tulis file"}"
                }
            }
        }
    }

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

        PlaybackService.onPlayPauseAction = { togglePlayPause() }
        PlaybackService.onStopAction = { playerManager.stopPlayback() }
        PlaybackService.onNextAction = { playNext() }
        PlaybackService.onPrevAction = { playPrevious() }
        viewModelScope.launch {
            combine(currentTrack, isPlaying) { track, playing -> track to playing }
                .distinctUntilChanged()
                .collect { (track, playing) -> PlaybackService.updateState(application, track, playing) }
        }

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
        // Every playback transition uses the same guard. If persistence fails, keep the
        // current track and editor draft instead of switching away and losing the user's work.
        if (!lrcDraftCoordinator.saveBeforeTrackSwitch(::saveAndApplyLyrics)) {
            _lrcSaveMessage.value = "Pergantian lagu dibatalkan karena lirik belum tersimpan. Draft tetap aman; coba simpan lagi."
            return
        }
        val saved = LrcStorageHelper.loadExistingLrc(getApplication(), track.artist, track.title)
        val trackToPlay = if (!saved.isNullOrBlank()) track.copy(lyricsLrc = saved) else track
        playerManager.playTrack(trackToPlay)
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

    fun updateTrackMetadata(updatedTrack: Track) {
        val updated = _allTracks.value.map { if (it.id == updatedTrack.id) updatedTrack else it }
        _allTracks.value = updated
        MusicCatalog.saveTracks(getApplication(), updated)
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
        PlaybackService.onPlayPauseAction = null
        PlaybackService.onStopAction = null
        PlaybackService.onNextAction = null
        PlaybackService.onPrevAction = null
        PlaybackService.stopPlaybackService(getApplication())
        super.onCleared()
        playerManager.release()
    }
}
