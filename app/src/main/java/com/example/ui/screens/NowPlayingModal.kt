package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode as AnimationRepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.DspSettings
import com.example.model.LrcParser
import com.example.model.RepeatMode
import com.example.model.Track
import com.example.model.VisualizerMode
import com.example.ui.components.AudioCoverImage
import com.example.ui.components.AudioVisualizerView
import com.example.ui.components.SynchronizedLyricsView
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentMagenta
import com.example.ui.theme.DividerColor
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

enum class CenterDisplayMode {
    ALBUM_ART,
    VISUALIZER,
    LYRICS
}

@Composable
fun NowPlayingModal(
    track: Track,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    amplitudes: FloatArray,
    dspSettings: DspSettings,
    visualizerMode: VisualizerMode,
    onChangeVisualizerMode: (VisualizerMode) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    repeatMode: RepeatMode = RepeatMode.OFF,
    onCycleRepeatMode: () -> Unit = {},
    onLoadLyrics: ((Track, (String) -> Unit) -> Unit)? = null,
    onOpenDsp: () -> Unit,
    onDismiss: () -> Unit,
    isDarkMode: Boolean = true,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    var centerMode by remember { mutableStateOf(CenterDisplayMode.ALBUM_ART) }
    var isShuffle by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "disc_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = AnimationRepeatMode.Restart
        ),
        label = "rotation"
    )

    var effectiveLyrics by remember(track.id, track.lyricsLrc) {
        mutableStateOf(track.lyricsLrc)
    }
    LaunchedEffect(track.id, track.audioUrl, track.lyricsLrc) {
        effectiveLyrics = track.lyricsLrc
        if (effectiveLyrics.isBlank()) {
            onLoadLyrics?.invoke(track) { loaded -> effectiveLyrics = loaded }
        }
    }
    val parsedLyrics = remember(effectiveLyrics) {
        LrcParser.parse(effectiveLyrics)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("now_playing_modal")
    ) {
        // Ambient Album Cover Background from MP3 tag or coverUrl
        AudioCoverImage(
            track = track,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Multi-stop gradient overlay adapting to Light and Dark mode
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDarkMode) {
                            listOf(
                                StudioDarkBg.copy(alpha = 0.82f),
                                StudioDarkBg.copy(alpha = 0.88f),
                                StudioDarkBg.copy(alpha = 0.96f)
                            )
                        } else {
                            listOf(
                                MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.92f),
                                MaterialTheme.colorScheme.background.copy(alpha = 0.97f)
                            )
                        }
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dismiss_player_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Tutup Pemutar",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SEDANG MEMUTAR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextTertiary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = track.album,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("modal_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkMode) "Beralih ke Mode Terang" else "Beralih ke Mode Gelap",
                            tint = if (isDarkMode) AccentAmber else NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenDsp,
                        modifier = Modifier.testTag("open_dsp_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Buka Equalizer & DSP",
                            tint = if (dspSettings.isEnabled) NeonCyan else TextSecondary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Display Mode Switcher Tabs (Cover Art | Visualizer | Lirik)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StudioCard, RoundedCornerShape(12.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CenterDisplayMode.entries.forEach { mode ->
                    val isSelected = centerMode == mode
                    val label = when (mode) {
                        CenterDisplayMode.ALBUM_ART -> "Sampul"
                        CenterDisplayMode.VISUALIZER -> "Visualizer"
                        CenterDisplayMode.LYRICS -> "Lirik"
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) NeonCyan else Color.Transparent)
                            .clickable { centerMode = mode }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) StudioDarkBg else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Center Content: Album Art / Visualizer / Lyrics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (centerMode) {
                    CenterDisplayMode.ALBUM_ART -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f)
                                .shadow(24.dp, shape = CircleShape)
                                .clip(CircleShape)
                                .border(4.dp, NeonCyan.copy(alpha = 0.4f), CircleShape)
                                .rotate(if (isPlaying) rotationAngle else 0f),
                            contentAlignment = Alignment.Center
                        ) {
                            AudioCoverImage(
                                track = track,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Center Vinyl Hole
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isDarkMode) StudioDarkBg else Color.White, CircleShape)
                                    .border(2.dp, NeonCyan, CircleShape)
                            )
                        }
                    }

                    CenterDisplayMode.VISUALIZER -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Visualizer mode selector chips
                            Row(
                                modifier = Modifier.padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VisualizerMode.entries.forEach { vMode ->
                                    val isSelected = visualizerMode == vMode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) ElectricBlue.copy(alpha = 0.2f) else StudioCard)
                                            .border(
                                                1.dp,
                                                if (isSelected) ElectricBlue else DividerColor,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { onChangeVisualizerMode(vMode) }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                            .testTag("vis_mode_${vMode.name}")
                                    ) {
                                        Text(
                                            text = vMode.displayName,
                                            color = if (isSelected) ElectricBlue else TextTertiary,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            AudioVisualizerView(
                                amplitudes = amplitudes,
                                mode = visualizerMode,
                                isPlaying = isPlaying,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            )
                        }
                    }

                    CenterDisplayMode.LYRICS -> {
                        SynchronizedLyricsView(
                            lyrics = parsedLyrics,
                            currentPositionMs = currentPositionMs,
                            onSeekToTimestamp = onSeekTo,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Track metadata & Wins Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // DSP Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (dspSettings.isBassBoostEnabled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentMagenta.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Waves,
                                contentDescription = null,
                                tint = AccentMagenta,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "BASS +${(dspSettings.bassBoostStrength / 10f).toInt()}%",
                                color = AccentMagenta,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (dspSettings.isVirtualizerEnabled) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SurroundSound,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "DOLBY 3D ${(dspSettings.virtualizerStrength / 10f).toInt()}%",
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentAmber.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "EQ: ${dspSettings.currentPresetName}",
                        color = AccentAmber,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Playback Seek Bar
            val safeDuration = if (durationMs > 0) durationMs else 1L
            val sliderValue = currentPositionMs.coerceIn(0L, safeDuration).toFloat()

            Slider(
                value = sliderValue,
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..safeDuration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = NeonCyan,
                    activeTrackColor = NeonCyan,
                    inactiveTrackColor = DividerColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("playback_seek_slider")
            )

            // Duration labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatMs(currentPositionMs),
                    color = TextTertiary,
                    fontSize = 12.sp
                )
                Text(
                    text = formatMs(durationMs),
                    color = TextTertiary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { isShuffle = !isShuffle },
                    modifier = Modifier.testTag("shuffle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Acak",
                        tint = if (isShuffle) NeonCyan else TextTertiary
                    )
                }

                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("previous_track_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Sebelumnya",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Glowing Play/Pause Button
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            Brush.linearGradient(listOf(NeonCyan, ElectricBlue)),
                            CircleShape
                        )
                        .clickable { onTogglePlayPause() }
                        .testTag("play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Jeda" else "Putar",
                        tint = StudioDarkBg,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("next_track_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Berikutnya",
                        tint = TextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                IconButton(
                    onClick = onCycleRepeatMode,
                    modifier = Modifier.testTag("repeat_button")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                            contentDescription = when (repeatMode) {
                                RepeatMode.OFF -> "Repeat mati"
                                RepeatMode.ONE -> "Repeat One aktif"
                                RepeatMode.ALL -> "Repeat All aktif"
                            },
                            tint = if (repeatMode != RepeatMode.OFF) NeonCyan else TextTertiary
                        )
                        if (repeatMode == RepeatMode.ALL) {
                            Text("ALL", fontSize = 8.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
