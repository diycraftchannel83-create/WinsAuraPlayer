package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.media.audiofx.Visualizer
import android.net.Uri
import android.util.Log
import com.example.model.DspSettings
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

class AudioPlayerManager(private val context: Context) {

    private val TAG = "AudioPlayerManager"
    private var mediaPlayer: MediaPlayer? = null
    private var playerPrepared = false

    // Audio effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var visualizer: Visualizer? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    // Playback state flows
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    // 32-band real-time visualizer amplitudes (values between 0.0f and 1.0f)
    private val _amplitudes = MutableStateFlow(FloatArray(32) { 0.05f })
    val amplitudes: StateFlow<FloatArray> = _amplitudes.asStateFlow()

    private var currentDspSettings: DspSettings = DspSettings()

    var onTrackCompleted: (() -> Unit)? = null

    init {
        startProgressTracker()
    }

    fun playTrack(track: Track) {
        var candidatePlayer: MediaPlayer? = null
        try {
            releasePlayerOnly()
            _currentTrack.value = track
            _isBuffering.value = true
            _isPlaying.value = false
            _currentPositionMs.value = 0L
            _durationMs.value = 0L

            val player = MediaPlayer().also { candidatePlayer = it }.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                if (track.isOnline) {
                    setDataSource(track.audioUrl)
                } else {
                    setDataSource(context, Uri.parse(track.audioUrl))
                }

                setOnPreparedListener { mp ->
                    _isBuffering.value = false
                    playerPrepared = true
                    _durationMs.value = mp.duration.coerceAtLeast(0).toLong()
                    try {
                        mp.start()
                        _isPlaying.value = true
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Unable to start prepared player", e)
                        _isPlaying.value = false
                    }
                    attachAudioEffects(mp.audioSessionId)
                    attachVisualizer(mp.audioSessionId)
                }

                setOnCompletionListener {
                    _isPlaying.value = false
                    _currentPositionMs.value = 0L
                    onTrackCompleted?.invoke()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    _isBuffering.value = false
                    _isPlaying.value = false
                    playerPrepared = false
                    _currentPositionMs.value = 0L
                    releaseVisualizer()
                    releaseEffects()
                    true
                }

                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Error playing track", e)
            _isBuffering.value = false
            _isPlaying.value = false
            playerPrepared = false
            // prepareAsync()/setDataSource may fail before the candidate is assigned
            // to mediaPlayer; release it explicitly to avoid leaking the native player.
            if (candidatePlayer != null && candidatePlayer !== mediaPlayer) {
                try {
                    candidatePlayer?.release()
                } catch (releaseError: Exception) {
                    Log.w(TAG, "Error releasing failed candidate player", releaseError)
                }
            }
            releasePlayerOnly()
        }
    }

    fun pause() {
        val player = mediaPlayer ?: return
        if (!playerPrepared) return
        try {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Pause ignored because player is not ready", e)
        }
    }

    fun resume() {
        val player = mediaPlayer ?: return
        if (!playerPrepared || _isBuffering.value) return
        try {
            if (!player.isPlaying) {
                player.start()
                _isPlaying.value = true
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Resume ignored because player is not ready", e)
            _isPlaying.value = false
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = mediaPlayer ?: return
        if (!playerPrepared) return
        val safePosition = positionMs.coerceIn(0L, _durationMs.value.coerceAtLeast(0L))
        try {
            player.seekTo(safePosition.toInt())
            _currentPositionMs.value = safePosition
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Seek ignored because player is not ready", e)
        }
    }

    private fun attachAudioEffects(audioSessionId: Int) {
        try {
            releaseEffects()

            // 1. Equalizer
            try {
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = currentDspSettings.isEnabled
                }
            } catch (e: Exception) {
                Log.w(TAG, "Equalizer init fallback", e)
            }

            // 2. Bass Boost
            try {
                bassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = currentDspSettings.isEnabled && currentDspSettings.isBassBoostEnabled
                    setStrength(currentDspSettings.bassBoostStrength.toShort())
                }
            } catch (e: Exception) {
                Log.w(TAG, "BassBoost init fallback", e)
            }

            // 3. Virtualizer (Dolby 3D Spatializer)
            try {
                virtualizer = Virtualizer(0, audioSessionId).apply {
                    enabled = currentDspSettings.isEnabled && currentDspSettings.isVirtualizerEnabled
                    setStrength(currentDspSettings.virtualizerStrength.toShort())
                }
            } catch (e: Exception) {
                Log.w(TAG, "Virtualizer init fallback", e)
            }

            // 4. Preset Reverb
            try {
                presetReverb = PresetReverb(0, audioSessionId).apply {
                    enabled = currentDspSettings.isEnabled && currentDspSettings.reverbPreset.id > 0
                    if (currentDspSettings.reverbPreset.id > 0) {
                        preset = currentDspSettings.reverbPreset.id
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "PresetReverb init fallback", e)
            }

            applyDspSettings(currentDspSettings)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach audio effects", e)
        }
    }

    private fun interpolateEqLevel(bands: List<com.example.model.EqualizerBand>, frequencyHz: Double): Int {
        if (bands.isEmpty()) return 0
        val sorted = bands.sortedBy { it.centerFreqHz }
        if (frequencyHz <= sorted.first().centerFreqHz) return sorted.first().levelMb
        if (frequencyHz >= sorted.last().centerFreqHz) return sorted.last().levelMb

        for (index in 0 until sorted.lastIndex) {
            val lower = sorted[index]
            val upper = sorted[index + 1]
            if (frequencyHz in lower.centerFreqHz.toDouble()..upper.centerFreqHz.toDouble()) {
                val lowerLog = kotlin.math.ln(lower.centerFreqHz.toDouble())
                val upperLog = kotlin.math.ln(upper.centerFreqHz.toDouble())
                val targetLog = kotlin.math.ln(frequencyHz)
                val fraction = ((targetLog - lowerLog) / (upperLog - lowerLog)).coerceIn(0.0, 1.0)
                return (lower.levelMb + (upper.levelMb - lower.levelMb) * fraction).toInt()
            }
        }
        return 0
    }

    fun applyDspSettings(settings: DspSettings) {
        currentDspSettings = settings
        try {
            // Map the app's ten frequency controls onto the actual hardware EQ bands.
            // Android devices expose different band counts and center frequencies.
            equalizer?.let { eq ->
                eq.enabled = settings.isEnabled
                val numBands = eq.numberOfBands.toInt()
                val minLevel = eq.bandLevelRange[0].toInt()
                val maxLevel = eq.bandLevelRange[1].toInt()

                for (hardwareIndex in 0 until numBands) {
                    val frequencyHz = (eq.getCenterFreq(hardwareIndex.toShort()).toDouble() / 1000.0)
                        .coerceAtLeast(1.0)
                    val baseLevelMb = interpolateEqLevel(settings.bands, frequencyHz)
                    val clamped = baseLevelMb.coerceIn(minLevel, maxLevel).toShort()
                    eq.setBandLevel(hardwareIndex.toShort(), clamped)
                }
            }

            // Apply Bass Boost
            bassBoost?.let { bb ->
                bb.enabled = settings.isEnabled && settings.isBassBoostEnabled
                if (bb.strengthSupported) {
                    bb.setStrength(settings.bassBoostStrength.coerceIn(0, 1000).toShort())
                }
            }

            // Apply Virtualizer 3D Surround
            virtualizer?.let { v ->
                v.enabled = settings.isEnabled && settings.isVirtualizerEnabled
                if (v.strengthSupported) {
                    v.setStrength(settings.virtualizerStrength.coerceIn(0, 1000).toShort())
                }
            }

            // Apply Reverb
            presetReverb?.let { pr ->
                pr.enabled = settings.isEnabled && settings.reverbPreset.id > 0
                if (settings.reverbPreset.id > 0) {
                    pr.preset = settings.reverbPreset.id
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error applying DSP settings: ${e.message}")
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive) {
                if (_isPlaying.value && mediaPlayer != null) {
                    try {
                        _currentPositionMs.value = mediaPlayer?.currentPosition?.toLong() ?: 0L
                    } catch (e: Exception) {
                        Log.w(TAG, "Unable to read playback position", e)
                    }
                }
                delay(40L)
            }
        }
    }

    /**
     * Captures FFT data from this MediaPlayer's audio session. Values are mapped to
     * 32 logarithmically spaced visualizer bands; no synthetic spectrum is generated.
     */
    private fun attachVisualizer(audioSessionId: Int) {
        releaseVisualizer()
        try {
            val captureRange = Visualizer.getCaptureSizeRange()
            val captureSize = Integer.highestOneBit(captureRange[1]).coerceAtMost(1024)
                .coerceAtLeast(captureRange[0])
            visualizer = Visualizer(audioSessionId).apply {
                this.captureSize = captureSize
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) = Unit

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft == null || fft.size < 8 || !_isPlaying.value) return
                            val bands = FloatArray(32)
                            val maxBin = (fft.size / 2 - 1).coerceAtLeast(2)
                            for (i in bands.indices) {
                                val low = (1.0 * Math.pow(maxBin.toDouble(), i / 32.0)).toInt().coerceIn(1, maxBin)
                                val high = (1.0 * Math.pow(maxBin.toDouble(), (i + 1) / 32.0)).toInt().coerceIn(low + 1, maxBin + 1)
                                var energy = 0.0
                                var count = 0
                                for (bin in low until high) {
                                    val offset = bin * 2
                                    if (offset + 1 >= fft.size) break
                                    val real = fft[offset].toInt()
                                    val imaginary = fft[offset + 1].toInt()
                                    energy += sqrt((real * real + imaginary * imaginary).toDouble())
                                    count++
                                }
                                val magnitude = if (count > 0) energy / count else 0.0
                                // Compress dynamic range and clamp for stable, readable bars.
                                bands[i] = (sqrt(magnitude / 128.0) * 3.0).toFloat().coerceIn(0.025f, 1f)
                            }
                            val previous = _amplitudes.value
                            _amplitudes.value = FloatArray(32) { i ->
                                val attackOrDecay = if (bands[i] > previous[i]) 0.65f else 0.22f
                                (previous[i] + (bands[i] - previous[i]) * attackOrDecay).coerceIn(0.025f, 1f)
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate(),
                    false,
                    true
                )
                enabled = true
            }
            Log.i(TAG, "Audio-session FFT visualizer attached")
        } catch (e: Exception) {
            Log.w(TAG, "Audio visualizer unavailable for this session/device", e)
            releaseVisualizer()
            _amplitudes.value = FloatArray(32) { 0.025f }
        }
    }

    fun refreshVisualizer() {
        val sessionId = mediaPlayer?.audioSessionId ?: return
        if (_isPlaying.value) attachVisualizer(sessionId)
    }

    private fun releaseVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.setDataCaptureListener(null, 0, false, false)
            visualizer?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing visualizer", e)
        } finally {
            visualizer = null
        }
    }

    private fun releaseEffects() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            presetReverb?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing effects", e)
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
            presetReverb = null
        }
    }

    private fun releasePlayerOnly() {
        playerPrepared = false
        _isPlaying.value = false
        _isBuffering.value = false
        releaseVisualizer()
        releaseEffects()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing player", e)
        } finally {
            mediaPlayer = null
        }
    }

    fun release() {
        progressJob?.cancel()
        releaseEffects()
        releasePlayerOnly()
    }
}
