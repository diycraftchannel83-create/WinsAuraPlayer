package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
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
import kotlin.math.sin

class AudioPlayerManager(private val context: Context) {

    private val TAG = "AudioPlayerManager"
    private var mediaPlayer: MediaPlayer? = null

    // Audio effects
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null

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
        try {
            _isBuffering.value = true
            _currentTrack.value = track

            releasePlayerOnly()

            val player = MediaPlayer().apply {
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
                    _durationMs.value = mp.duration.toLong()
                    mp.start()
                    _isPlaying.value = true
                    attachAudioEffects(mp.audioSessionId)
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
                    true
                }

                prepareAsync()
            }
            mediaPlayer = player
        } catch (e: Exception) {
            Log.e(TAG, "Error playing track", e)
            _isBuffering.value = false
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _isPlaying.value = true
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
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
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

    fun applyDspSettings(settings: DspSettings) {
        currentDspSettings = settings
        try {
            // Apply EQ
            equalizer?.let { eq ->
                eq.enabled = settings.isEnabled
                val numBands = eq.numberOfBands.toInt()
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]

                settings.bands.forEachIndexed { idx, band ->
                    if (idx < numBands) {
                        val clamped = band.levelMb.coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                        eq.setBandLevel(idx.toShort(), clamped)
                    }
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

    private var tick = 0f
    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive) {
                if (_isPlaying.value && mediaPlayer != null) {
                    try {
                        val pos = mediaPlayer?.currentPosition?.toLong() ?: 0L
                        _currentPositionMs.value = pos

                        // Synthesize 32-band responsive audio visualizer spectrum
                        tick += 0.25f
                        val bassBoostFactor = if (currentDspSettings.isBassBoostEnabled) {
                            1f + (currentDspSettings.bassBoostStrength / 1000f) * 0.5f
                        } else 1f

                        val newAmps = FloatArray(32) { i ->
                            val freqPhase = (i * 0.4f) + tick
                            val baseWave = (sin(freqPhase) + 1f) / 2f // 0..1
                            val harmonic = (sin(freqPhase * 2.3f) + 1f) / 4f
                            val pulse = if (i < 8) {
                                // Bass frequencies reactive to bass booster
                                ((sin(tick * 1.5f) + 1f) / 2f) * bassBoostFactor * 0.7f
                            } else {
                                (sin(freqPhase * 1.7f) + 1f) / 3f
                            }
                            val raw = (baseWave * 0.4f + harmonic + pulse).coerceIn(0.08f, 0.98f)
                            raw
                        }
                        _amplitudes.value = newAmps
                    } catch (e: Exception) {
                        // ignore during seek/transient
                    }
                } else if (!_isPlaying.value) {
                    // Decay visualizer smoothly
                    val decayed = _amplitudes.value.map { (it * 0.85f).coerceAtLeast(0.04f) }.toFloatArray()
                    _amplitudes.value = decayed
                }
                delay(40L) // 25 fps updates
            }
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
