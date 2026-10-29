package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import android.media.MediaMetadataRetriever
import android.app.Activity
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.OnlineLyricsRepository
import com.example.model.DspSettings
import com.example.model.LrcParser
import com.example.model.Track
import com.example.model.VisualizerMode
import com.example.viewmodel.RepeatMode
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun NowPlayingScreen(
    track: Track?,
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
    onToggleRepeat: () -> Unit = {},
    onOpenDsp: () -> Unit,
    onShowAd: () -> Unit = {},
    isDarkMode: Boolean = true,
    onToggleTheme: () -> Unit = {},
    onOpenLrcStudio: () -> Unit = {},
    onOpenMetadataEditor: (Track) -> Unit = {},
    onImmersiveModeChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (track == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(StudioDarkBg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(56.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Pilih lagu di tab Pustaka untuk mulai memutar",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
        return
    }

    var centerMode by remember { mutableStateOf(CenterDisplayMode.ALBUM_ART) }
    var fullScreenStage by remember { mutableStateOf(FullScreenStage.NORMAL) }
    LaunchedEffect(fullScreenStage) { onImmersiveModeChanged(fullScreenStage != FullScreenStage.NORMAL) }
    var isShuffle by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "screen_disc_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = AnimationRepeatMode.Restart
        ),
        label = "rotation"
    )

    val context = LocalContext.current
    // Keep Now Playing labels in sync immediately after a successful physical tag write.
    var displayTitle by remember(track.id) { mutableStateOf(track.title) }
    var displayArtist by remember(track.id) { mutableStateOf(track.artist) }
    var displayAlbum by remember(track.id) { mutableStateOf(track.album) }
    var showTagEditor by remember(track.id) { mutableStateOf(false) }
    var tagStatus by remember(track.id) { mutableStateOf<String?>(null) }
    var pendingTagSave by remember(track.id) { mutableStateOf<Map<String, String>?>(null) }
    val tagSaveScope = androidx.compose.runtime.rememberCoroutineScope()
    val editableTags = remember(track.id) { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    // Metadata editor updates the Track with the same ID; observe its fields too so
    // Now Playing refreshes immediately after a successful tag write.
    LaunchedEffect(track.title, track.artist, track.album) {
        displayTitle = track.title
        displayArtist = track.artist
        displayAlbum = track.album
        editableTags["Judul"] = track.title
        editableTags["Artis"] = track.artist
        editableTags["Album"] = track.album
    }

    fun savePhysicalTags(values: Map<String, String>) {
        tagStatus = "Menyimpan tag langsung ke file audio…"
        tagSaveScope.launch {
            val result = withContext(Dispatchers.IO) {
                writeTagsToAudioFile(context, Uri.parse(track.audioUrl), values)
            }
            tagStatus = result
            if (result.startsWith("Berhasil menyimpan")) {
                displayTitle = values["Judul"]?.takeIf { it.isNotBlank() } ?: displayTitle
                displayArtist = values["Artis"]?.takeIf { it.isNotBlank() } ?: displayArtist
                displayAlbum = values["Album"]?.takeIf { it.isNotBlank() } ?: displayAlbum
                // Keep the editor values and visible Now Playing labels synchronized.
                editableTags["Judul"] = displayTitle
                editableTags["Artis"] = displayArtist
                editableTags["Album"] = displayAlbum
            }
        }
    }

    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val values = pendingTagSave
        pendingTagSave = null
        if (result.resultCode == Activity.RESULT_OK && values != null) {
            savePhysicalTags(values)
        } else if (values != null) {
            tagStatus = "Izin menulis file dibatalkan. Tag fisik belum diubah."
        }
    }
    var onlineLyrics by remember(track.id) { mutableStateOf(track.lyricsLrc) }
    var isLoadingLyrics by remember(track.id) { mutableStateOf(false) }
    var lyricsRetryCount by remember(track.id) { mutableStateOf(0) }
    val lrcBrowseScope = androidx.compose.runtime.rememberCoroutineScope()
    val browseLrcLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { selectedUri ->
        if (selectedUri != null) {
            lrcBrowseScope.launch {
                isLoadingLyrics = true
                val loadedLrc = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openInputStream(selectedUri)
                            ?.bufferedReader(Charsets.UTF_8)
                            ?.use { it.readText() }
                    } catch (_: Exception) {
                        null
                    }
                }
                if (!loadedLrc.isNullOrBlank()) {
                    onlineLyrics = loadedLrc
                    centerMode = CenterDisplayMode.LYRICS
                } else {
                    tagStatus = "File LRC tidak dapat dibaca atau kosong."
                }
                isLoadingLyrics = false
            }
        }
    }

    LaunchedEffect(track.id, lyricsRetryCount) {
        if (track.lyricsLrc.isNotBlank()) {
            onlineLyrics = track.lyricsLrc
            isLoadingLyrics = false
        } else {
            isLoadingLyrics = true
            onlineLyrics = withContext(Dispatchers.IO) {
                OnlineLyricsRepository.getLyrics(context, track)
            }
            isLoadingLyrics = false
        }
    }

    val parsedLyrics = remember(onlineLyrics) {
        LrcParser.parse(onlineLyrics)
    }
    val lyricsSyncOffsetMs = remember(track.id) {
        context.getSharedPreferences("wins_aura_settings", android.content.Context.MODE_PRIVATE)
            .getInt("lyrics_sync_offset_ms", 0).toLong()
    }
    val currentLyricLine = remember(parsedLyrics, currentPositionMs, lyricsSyncOffsetMs) {
        val lyricPosition = (currentPositionMs + lyricsSyncOffsetMs).coerceAtLeast(0L)
        parsedLyrics.lastOrNull { it.timestampMs <= lyricPosition }?.text
            ?: parsedLyrics.firstOrNull()?.text.orEmpty()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("now_playing_screen")
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SEDANG MEMUTAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = displayAlbum,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = if (isDarkMode) "Beralih ke Mode Terang" else "Beralih ke Mode Gelap",
                        tint = if (isDarkMode) AccentAmber else NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Always-visible metadata editor action in the Now Playing header.
                IconButton(
                    onClick = { onOpenMetadataEditor(track) },
                    modifier = Modifier.testTag("now_playing_metadata_editor_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit metadata lagu",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onShowAd,
                    modifier = Modifier.testTag("trigger_interstitial_button")
                ) {
                    Box(
                        modifier = Modifier
                            .background(AccentAmber.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ADS",
                            color = AccentAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onOpenLrcStudio,
                    modifier = Modifier.testTag("open_lrc_studio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Lyrics,
                        contentDescription = "Buka WinsLyric Editor LRC Studio",
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                IconButton(
                    onClick = onOpenDsp,
                    modifier = Modifier.testTag("now_playing_dsp_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Buka Equalizer & DSP",
                        tint = if (dspSettings.isEnabled) NeonCyan else TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Center Mode Switcher Tabs (Sampul | Visualizer | Lirik)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StudioCard, RoundedCornerShape(12.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CenterDisplayMode.entries.forEach { mode ->
                val isSelected = centerMode == mode
                val label = when (mode) {
                    CenterDisplayMode.ALBUM_ART -> "Sampul"
                    CenterDisplayMode.VISUALIZER -> "Visualizer"
                    CenterDisplayMode.LYRICS -> "Lirik Lagu"
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

        Spacer(modifier = Modifier.height(12.dp))

        // Center Content Area: Album Art / Visualizer / Lyrics
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clickable {
                    if (fullScreenStage == FullScreenStage.NORMAL) {
                        fullScreenStage = FullScreenStage.COVER
                    }
                }
                .testTag("now_playing_center_visual_area"),
            contentAlignment = Alignment.Center
        ) {
            when (centerMode) {
                CenterDisplayMode.ALBUM_ART -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.82f)
                            .aspectRatio(1f)
                            .shadow(20.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .border(3.5.dp, NeonCyan.copy(alpha = 0.45f), CircleShape)
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
                                .size(34.dp)
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
                        // Visualizer mode switcher chips
                        Row(
                            modifier = Modifier.padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VisualizerMode.entries.forEach { vMode ->
                                val isSelected = visualizerMode == vMode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ElectricBlue.copy(alpha = 0.25f) else StudioCard)
                                        .border(
                                            1.dp,
                                            if (isSelected) ElectricBlue else DividerColor,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onChangeVisualizerMode(vMode) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                        .testTag("tab_vis_mode_${vMode.name}")
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
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isLoadingLyrics) {
                            Text(
                                text = "Mencari lirik online…",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else if (parsedLyrics.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Lirik belum ditemukan",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Button(
                                    onClick = { lyricsRetryCount++ },
                                    modifier = Modifier.testTag("retry_online_lyrics_button")
                                ) {
                                    Text("Cari lagi", maxLines = 1)
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Button(
                                    onClick = { browseLrcLauncher.launch(arrayOf("text/*", "application/octet-stream")) },
                                    modifier = Modifier.testTag("browse_lrc_button")
                                ) {
                                    Text("Browse LRC", maxLines = 1)
                                }
                            }
                        }
                        SynchronizedLyricsView(
                            lyrics = parsedLyrics,
                            currentPositionMs = (currentPositionMs + lyricsSyncOffsetMs).coerceAtLeast(0L),
                            onSeekToTimestamp = onSeekTo,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }

            // Keep the current lyric visible over the cover artwork, in the highlighted
            // center area, even when the Sampul tab is selected.
            if (centerMode == CenterDisplayMode.ALBUM_ART && currentLyricLine.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioDarkBg.copy(alpha = 0.82f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentLyricLine,
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Opens the full WinsLyricEditor metadata editor.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenMetadataEditor(track) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayTitle,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = displayArtist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonCyan,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                modifier = Modifier.weight(0.85f),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "ALBUM",
                    color = TextTertiary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = displayAlbum.ifBlank { "Album tidak diketahui" },
                    color = AccentAmber,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }



        Spacer(modifier = Modifier.height(8.dp))

        // DSP status indicators: three equal-width slots, balanced left-to-right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background((if (dspSettings.isBassBoostEnabled) AccentMagenta else TextTertiary).copy(alpha = 0.15f))
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Waves,
                        contentDescription = null,
                        tint = if (dspSettings.isBassBoostEnabled) AccentMagenta else TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (dspSettings.isBassBoostEnabled) "BASS +${(dspSettings.bassBoostStrength / 10f).toInt()}%" else "BASS OFF",
                        color = if (dspSettings.isBassBoostEnabled) AccentMagenta else TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background((if (dspSettings.isVirtualizerEnabled) NeonCyan else TextTertiary).copy(alpha = 0.15f))
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SurroundSound,
                        contentDescription = null,
                        tint = if (dspSettings.isVirtualizerEnabled) NeonCyan else TextTertiary,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (dspSettings.isVirtualizerEnabled) "DOLBY ${(dspSettings.virtualizerStrength / 10f).toInt()}%" else "DOLBY OFF",
                        color = if (dspSettings.isVirtualizerEnabled) NeonCyan else TextTertiary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentAmber.copy(alpha = 0.15f))
                    .clickable { onOpenDsp() }
                    .padding(horizontal = 4.dp, vertical = 7.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EQ: ${dspSettings.currentPresetName}",
                    color = AccentAmber,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                .testTag("screen_playback_seek_slider")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatDuration(currentPositionMs),
                color = TextTertiary,
                fontSize = 11.sp
            )
            Text(
                text = formatDuration(durationMs),
                color = TextTertiary,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Playback Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { isShuffle = !isShuffle },
                modifier = Modifier.testTag("screen_shuffle_button")
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
                    .size(44.dp)
                    .testTag("screen_previous_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Sebelumnya",
                    tint = TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Glowing Center Play/Pause Button
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .background(
                        Brush.linearGradient(listOf(NeonCyan, ElectricBlue)),
                        CircleShape
                    )
                    .clickable { onTogglePlayPause() }
                    .testTag("screen_play_pause_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Jeda" else "Putar",
                    tint = StudioDarkBg,
                    modifier = Modifier.size(34.dp)
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("screen_next_button")
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Berikutnya",
                    tint = TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(
                onClick = onToggleRepeat,
                modifier = Modifier.testTag("screen_repeat_button")
            ) {
                Icon(
                    imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    contentDescription = when (repeatMode) { RepeatMode.OFF -> "Repeat Off"; RepeatMode.ALL -> "Repeat All"; RepeatMode.ONE -> "Repeat One" },
                    tint = if (repeatMode != RepeatMode.OFF) NeonCyan else TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

        // Immersive three-stage player: cover + synced lyrics -> visualizer + lyrics -> normal UI.
        if (fullScreenStage != FullScreenStage.NORMAL) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF050811))
                    .clickable {
                        fullScreenStage = when (fullScreenStage) {
                            FullScreenStage.NORMAL -> FullScreenStage.COVER
                            FullScreenStage.COVER -> FullScreenStage.VISUALIZER
                            FullScreenStage.VISUALIZER -> FullScreenStage.NORMAL
                        }
                    }
                    .testTag("immersive_player_overlay"),
                contentAlignment = Alignment.Center
            ) {
                if (fullScreenStage == FullScreenStage.COVER) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .aspectRatio(1f)
                            .align(Alignment.Center)
                            .shadow(24.dp, CircleShape)
                            .clip(CircleShape)
                            .border(3.dp, NeonCyan.copy(alpha = 0.8f), CircleShape)
                            .rotate(if (isPlaying) rotationAngle else 0f),
                        contentAlignment = Alignment.Center
                    ) {
                        AudioCoverImage(
                            track = track,
                            contentDescription = "Sampul album berputar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(StudioDarkBg, CircleShape)
                                .border(2.dp, NeonCyan, CircleShape)
                        )
                    }
                } else {
                    AudioVisualizerView(
                        amplitudes = amplitudes,
                        mode = visualizerMode,
                        isPlaying = isPlaying,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                SynchronizedLyricsView(
                    lyrics = parsedLyrics,
                    currentPositionMs = (currentPositionMs + lyricsSyncOffsetMs).coerceAtLeast(0L),
                    onSeekToTimestamp = onSeekTo,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}


/** Writes metadata to the actual audio file bytes, not only to MediaStore's index. */
private fun writeTagsToAudioFile(
    context: android.content.Context,
    uri: Uri,
    values: Map<String, String>
): String {
    val displayName = context.contentResolver.query(
        uri,
        arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
    val extension = displayName
        ?.substringAfterLast('.', "")
        ?.lowercase()
        ?.takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
        ?: when (context.contentResolver.getType(uri)?.lowercase()) {
            "audio/mpeg" -> "mp3"
            "audio/mp4", "video/mp4" -> "m4a"
            "audio/flac" -> "flac"
            "audio/ogg" -> "ogg"
            "audio/wav", "audio/x-wav" -> "wav"
            else -> "mp3"
        }
    val tempFile = File.createTempFile("wins-aura-tags-", ".$extension", context.cacheDir)
    return try {
        val input = context.contentResolver.openInputStream(uri)
            ?: return "Gagal membuka file audio untuk dibaca."
        input.use { source -> FileOutputStream(tempFile).use { target -> source.copyTo(target) } }

        val audioFile = AudioFileIO.read(tempFile)
        val tag = audioFile.tagOrCreateAndSetDefault
        val fields = listOf(
            "Judul" to FieldKey.TITLE,
            "Artis" to FieldKey.ARTIST,
            "Album" to FieldKey.ALBUM,
            "Artis Album" to FieldKey.ALBUM_ARTIST,
            "Komposer" to FieldKey.COMPOSER,
            "Penulis" to FieldKey.LYRICIST,
            "Genre" to FieldKey.GENRE,
            "Tahun" to FieldKey.YEAR,
            "Nomor Track" to FieldKey.TRACK,
            "Nomor Disc" to FieldKey.DISC_NO
        )
        var writtenFields = 0
        fields.forEach { (label, fieldKey) ->
            val value = values[label]?.trim().orEmpty()
            if (value.isNotEmpty()) {
                try {
                    tag.setField(fieldKey, value)
                    writtenFields++
                } catch (_: Exception) {
                    // Optional fields differ between audio formats.
                }
            }
        }
        if (writtenFields == 0) return "Format file ini tidak mendukung tag yang dapat ditulis."
        AudioFileIO.write(audioFile)

        val descriptor = context.contentResolver.openFileDescriptor(uri, "rwt")
            ?: return "Tag telah diproses, tetapi file asli tidak dapat dibuka untuk ditulis."
        descriptor.use { pfd ->
            FileInputStream(tempFile).use { source ->
                FileOutputStream(pfd.fileDescriptor).use { target ->
                    source.copyTo(target)
                    target.flush()
                }
            }
        }
        "Berhasil menyimpan $writtenFields tag ke file audio fisik."
    } catch (e: Exception) {
        "Gagal menyimpan tag ke file fisik: ${e.localizedMessage ?: e.javaClass.simpleName}"
    } finally {
        tempFile.delete()
    }
}

private fun formatDuration(ms: Long): String {

    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}

private enum class FullScreenStage { NORMAL, COVER, VISUALIZER }
