package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LrcStorageHelper
import com.example.model.LrcParser
import com.example.model.LyricsLine
import com.example.model.Track
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
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

@Composable
fun LrcStudioScreen(
    track: Track,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSaveLyrics: (Track, List<LyricsLine>) -> Boolean,
    onDraftChanged: (Track, List<LyricsLine>) -> Unit = { _, _ -> },
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onClose() }

    val context = LocalContext.current

    // Lines state initialized from track lyrics
    var lines by remember(track.id, track.lyricsLrc) {
        val parsed = LrcParser.parsePlainOrLrc(track.lyricsLrc)
        mutableStateOf(parsed)
    }

    // Publish every edit to the ViewModel. All exits (track changes and close) use the
    // ViewModel's save guard, which keeps the draft until storage confirms success.
    SideEffect { onDraftChanged(track, lines.toList()) }
    fun updateLines(updatedLines: List<LyricsLine>) {
        lines = updatedLines
        onDraftChanged(track, updatedLines.toList())
    }

    var activeLineIndex by remember { mutableIntStateOf(0) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var showEditLineDialog by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var showAddLineDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Scroll list to keep active sync line in view
    LaunchedEffect(activeLineIndex) {
        if (lines.isNotEmpty() && activeLineIndex in lines.indices) {
            val target = (activeLineIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    // File picker launcher for .txt / .lrc
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    val parsed = LrcParser.parsePlainOrLrc(content)
                    if (parsed.isNotEmpty()) {
                        updateLines(parsed)
                        activeLineIndex = 0
                        Toast.makeText(context, "${parsed.size} baris lirik berhasil diimpor", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
            .statusBarsPadding()
            .testTag("lrc_studio_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("lrc_studio_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Pemutar",
                            tint = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LRC Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(AccentAmber.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "SYNC LIVE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAmber
                                )
                            }
                        }
                        Text(
                            text = "${track.title} • ${track.artist}",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.testTag("lrc_studio_help_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Petunjuk Penggunaan",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Button(
                        onClick = {
                            if (lines.isEmpty()) {
                                Toast.makeText(context, "Lirik masih kosong!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val saved = onSaveLyrics(track, lines)
                            if (saved) {
                                val filename = LrcStorageHelper.buildLrcFilename(track.artist, track.title)
                                Toast.makeText(
                                    context,
                                    "Lirik tersimpan: winsplayerlrc/$filename",
                                    Toast.LENGTH_LONG
                                ).show()
                                onClose()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            contentColor = StudioDarkBg
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp).widthIn(min = 0.dp).testTag("lrc_save_and_apply_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Simpan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input / Import Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("*/*") },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("upload_txt_file_button")
                ) {
                    Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Unggah .txt / .lrc", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showPasteDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.testTag("paste_lyrics_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tempel Teks Lirik", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showAddLineDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
                    modifier = Modifier.testTag("add_line_button")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah Baris", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        // Reset all timestamps to 0
                        updateLines(lines.map { it.copy(timestampMs = 0L) })
                        activeLineIndex = 0
                        Toast.makeText(context, "Semua waktu lirik di-reset ke awal", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentMagenta),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentMagenta.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("reset_all_sync_button")
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset Waktu", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mini Playback Station Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = StudioCardElevated),
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TIMELINE LAGU",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextTertiary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = formatDigitalTime(currentPositionMs),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                        }

                        // Playback control mini buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onSeekTo((currentPositionMs - 3000L).coerceAtLeast(0L)) },
                                modifier = Modifier.size(36.dp).testTag("rewind_3s_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "Mundur 3 Detik",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        Brush.linearGradient(listOf(NeonCyan, ElectricBlue)),
                                        CircleShape
                                    )
                                    .clickable { onTogglePlayPause() }
                                    .testTag("lrc_studio_play_pause_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Jeda" else "Putar",
                                    tint = StudioDarkBg,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = { onSeekTo((currentPositionMs + 3000L).coerceAtMost(durationMs)) },
                                modifier = Modifier.size(36.dp).testTag("forward_3s_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "Maju 3 Detik",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Seek slider
                    val safeDuration = if (durationMs > 0) durationMs else 1L
                    Slider(
                        value = currentPositionMs.coerceIn(0L, safeDuration).toFloat(),
                        onValueChange = { onSeekTo(it.toLong()) },
                        valueRange = 0f..safeDuration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DividerColor
                        ),
                        modifier = Modifier.fillMaxWidth().height(26.dp).testTag("lrc_seek_slider")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // LIVE LINE-BY-LINE SYNC STATION
            if (lines.isNotEmpty()) {
                val currentLine = lines.getOrNull(activeLineIndex)
                val nextLine = lines.getOrNull(activeLineIndex + 1)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioCard),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, NeonCyan.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth().testTag("sync_action_station")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Line status indicator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "BARIS KE-${activeLineIndex + 1} DARI ${lines.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentAmber,
                                letterSpacing = 1.sp
                            )
                            val timestampText = currentLine?.let {
                                if (it.timestampMs > 0L) it.formattedDisplayTime() else "Belum Disinkron"
                            } ?: ""
                            Text(
                                text = "Waktu: $timestampText",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentLine?.timestampMs ?: 0L > 0L) NeonCyan else TextTertiary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Current line text prominent display
                        Text(
                            text = currentLine?.text ?: "Pilih baris lirik di bawah",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )

                        if (nextLine != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Berikutnya: ${nextLine.text}",
                                fontSize = 12.sp,
                                color = TextTertiary,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // THE BIG SYNC BUTTON: Tap-to-record current playback timestamp
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .shadow(8.dp, RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(NeonCyan, ElectricBlue)),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (currentLine != null) {
                                        // Record timestamp
                                        val updated = lines.toMutableList()
                                        updated[activeLineIndex] = currentLine.copy(timestampMs = currentPositionMs)
                                        updateLines(updated)
                                        // Auto advance to next line
                                        if (activeLineIndex < lines.lastIndex) {
                                            activeLineIndex++
                                        }
                                    }
                                }
                                .testTag("sync_this_line_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color.Red, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SYNC BARIS INI (${formatDigitalTime(currentPositionMs)})",
                                    color = StudioDarkBg,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Fine tune & Navigation Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous line
                            OutlinedButton(
                                onClick = {
                                    if (activeLineIndex > 0) activeLineIndex--
                                },
                                enabled = activeLineIndex > 0,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                modifier = Modifier.testTag("prev_line_button")
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Kembali", fontSize = 11.sp)
                            }

                            // Offset fine-tunes: -0.5s and +0.5s
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(StudioCardElevated, RoundedCornerShape(6.dp))
                                        .border(1.dp, DividerColor, RoundedCornerShape(6.dp))
                                        .clickable {
                                            if (currentLine != null && currentLine.timestampMs > 0L) {
                                                val updated = lines.toMutableList()
                                                val newTime = (currentLine.timestampMs - 500L).coerceAtLeast(0L)
                                                updated[activeLineIndex] = currentLine.copy(timestampMs = newTime)
                                                updateLines(updated)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("offset_minus_500ms")
                                ) {
                                    Text("-0.5s", color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    modifier = Modifier
                                        .background(StudioCardElevated, RoundedCornerShape(6.dp))
                                        .border(1.dp, DividerColor, RoundedCornerShape(6.dp))
                                        .clickable {
                                            if (currentLine != null && currentLine.timestampMs > 0L) {
                                                val updated = lines.toMutableList()
                                                val newTime = currentLine.timestampMs + 500L
                                                updated[activeLineIndex] = currentLine.copy(timestampMs = newTime)
                                                updateLines(updated)
                                            }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                        .testTag("offset_plus_500ms")
                                ) {
                                    Text("+0.5s", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Next / Skip line
                            OutlinedButton(
                                onClick = {
                                    if (activeLineIndex < lines.lastIndex) activeLineIndex++
                                },
                                enabled = activeLineIndex < lines.lastIndex,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                                modifier = Modifier.testTag("skip_line_button")
                            ) {
                                Text("Lewati", fontSize = 11.sp)
                                Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            } else {
                // Empty state card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = StudioCard),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Lirik Belum Tersedia",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Unggah file .txt/.lrc atau tempel teks lirik lagu ini untuk mulai sinkronisasi timeline.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { showPasteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Tempel Teks Sekarang", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // LIST OF ALL LINES WITH TIMESTAMPS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DAFTAR BARIS LIRIK (${lines.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Ketuk baris untuk pilih / lompat audio",
                    fontSize = 10.sp,
                    color = TextTertiary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("lrc_lines_list")
            ) {
                itemsIndexed(lines) { index, line ->
                    val isCurrent = index == activeLineIndex
                    val isSynced = line.timestampMs > 0L

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) StudioCardElevated else StudioCard
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCurrent) NeonCyan else DividerColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeLineIndex = index
                                if (line.timestampMs > 0L) {
                                    onSeekTo(line.timestampMs)
                                }
                            }
                            .testTag("lrc_line_item_$index")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Line number
                            Text(
                                text = "${index + 1}",
                                fontSize = 11.sp,
                                color = TextTertiary,
                                modifier = Modifier.width(22.dp)
                            )

                            // Timestamp badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSynced) NeonCyan.copy(alpha = 0.2f) else DividerColor.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSynced) NeonCyan.copy(alpha = 0.4f) else DividerColor,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isSynced) line.formattedDisplayTime() else "--:--.--",
                                    color = if (isSynced) NeonCyan else TextTertiary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Lyric text
                            Text(
                                text = line.text,
                                color = if (isCurrent) TextPrimary else TextSecondary,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f)
                            )

                            // Quick edit line text icon
                            IconButton(
                                onClick = { showEditLineDialog = index to line.text },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Teks",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Delete line icon
                            IconButton(
                                onClick = {
                                    val updated = lines.toMutableList()
                                    updated.removeAt(index)
                                    updateLines(updated)
                                    if (activeLineIndex >= lines.size && lines.isNotEmpty()) {
                                        activeLineIndex = lines.lastIndex
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Baris",
                                    tint = TextTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Paste Lyrics Dialog
    if (showPasteDialog) {
        var rawText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasteDialog = false },
            title = {
                Text("Tempel Teks Lirik", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                Column {
                    Text(
                        text = "Tempel teks lirik lagu di bawah ini. Setiap baris baru akan dijadikan 1 baris timeline yang dapat disinkronkan satu per satu.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = { rawText = it },
                        placeholder = { Text("Contoh:\nBaris lirik 1\nBaris lirik 2\nBaris lirik 3") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 260.dp)
                            .testTag("paste_lyrics_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DividerColor
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (rawText.isNotBlank()) {
                            val parsed = LrcParser.parsePlainOrLrc(rawText)
                            if (parsed.isNotEmpty()) {
                                updateLines(parsed)
                                activeLineIndex = 0
                                Toast.makeText(context, "${parsed.size} baris berhasil dimuat", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showPasteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = StudioDarkBg),
                    modifier = Modifier.testTag("confirm_paste_button")
                ) {
                    Text("Mulai Sinkron", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = StudioCardElevated
        )
    }

    // Edit Single Line Dialog
    if (showEditLineDialog != null) {
        val (index, initialText) = showEditLineDialog!!
        var lineText by remember { mutableStateOf(initialText) }
        AlertDialog(
            onDismissRequest = { showEditLineDialog = null },
            title = {
                Text("Edit Teks Baris #${index + 1}", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                OutlinedTextField(
                    value = lineText,
                    onValueChange = { lineText = it },
                    modifier = Modifier.fillMaxWidth().testTag("edit_line_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DividerColor
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = lines.toMutableList()
                        if (index in updated.indices) {
                            updated[index] = updated[index].copy(text = lineText.trim())
                            updateLines(updated)
                        }
                        showEditLineDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = StudioDarkBg)
                ) {
                    Text("Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditLineDialog = null }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = StudioCardElevated
        )
    }

    // Add New Line Dialog
    if (showAddLineDialog) {
        var newLineText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddLineDialog = false },
            title = {
                Text("Tambah Baris Lirik", fontWeight = FontWeight.Bold, color = TextPrimary)
            },
            text = {
                OutlinedTextField(
                    value = newLineText,
                    onValueChange = { newLineText = it },
                    placeholder = { Text("Ketik baris lirik baru...") },
                    modifier = Modifier.fillMaxWidth().testTag("new_line_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DividerColor
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newLineText.isNotBlank()) {
                            val updated = lines.toMutableList()
                            updated.add(LyricsLine(currentPositionMs, newLineText.trim()))
                            updateLines(updated)
                            activeLineIndex = updated.lastIndex
                        }
                        showAddLineDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = StudioDarkBg)
                ) {
                    Text("Tambah", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddLineDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = StudioCardElevated
        )
    }

    // Instructions Dialog
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Text("Petunjuk LRC Studio", fontWeight = FontWeight.Bold, color = NeonCyan)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "1. Masukkan Lirik:\nUnggah file .txt/.lrc atau tempel teks lirik lagu mentah.\n\n" +
                                "2. Putar Lagu:\nTekan tombol Play untuk mendengarkan lagu secara langsung.\n\n" +
                                "3. Rekam Waktu:\nSetiap kali penyanyi menyanyikan baris lirik aktif, tekan tombol 'SYNC BARIS INI'. Timeline waktu saat itu otomatis tercatat dan studio lanjut ke baris berikutnya.\n\n" +
                                "4. Penyesuaian Halus:\nGunakan tombol -0.5s atau +0.5s jika ingin menggeser waktu sedikit lebih cepat/lambat.\n\n" +
                                "5. Simpan & Terapkan:\nTekan tombol 'Simpan' di pojok kanan atas. File lrc akan diekspor dengan format [Artis]-[Judul].lrc di dalam folder internal winsplayerlrc, serta langsung aktif di pemutar!",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = StudioDarkBg)
                ) {
                    Text("Mengerti", fontWeight = FontWeight.Bold)
                }
            },
            containerColor = StudioCardElevated
        )
    }
}

private fun formatDigitalTime(ms: Long): String {
    val totalHundredths = ms / 10
    val hundredths = totalHundredths % 100
    val totalSec = totalHundredths / 100
    val sec = totalSec % 60
    val min = totalSec / 60
    return "%02d:%02d.%02d".format(min, sec, hundredths)
}
