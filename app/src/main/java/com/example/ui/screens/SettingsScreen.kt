package com.example.ui.screens

import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.model.Track

private const val PREFS = "wins_aura_settings"
private const val LANGUAGE_KEY = "app_language"
private const val LYRICS_OFFSET_KEY = "lyrics_sync_offset_ms"

@Composable
fun SettingsScreen(
    tracks: List<Track>,
    onUpdateTrack: (Track) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE) }
    var language by remember { mutableStateOf(prefs.getString(LANGUAGE_KEY, "id") ?: "id") }
    var languageMenu by remember { mutableStateOf(false) }
    var editingTrack by remember { mutableStateOf<Track?>(null) }
    var showLyricsSync by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Pengaturan", style = MaterialTheme.typography.headlineMedium)
        Text("Atur bahasa, metadata lagu, dan sinkronisasi lirik.", style = MaterialTheme.typography.bodyMedium)

        Card(onClick = { languageMenu = true }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Language, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text("Bahasa", style = MaterialTheme.typography.titleMedium)
                    Text(if (language == "id") "Indonesia" else "English", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        DropdownMenu(expanded = languageMenu, onDismissRequest = { languageMenu = false }) {
            DropdownMenuItem(text = { Text("Bahasa Indonesia") }, onClick = {
                language = "id"; prefs.edit().putString(LANGUAGE_KEY, language).apply(); languageMenu = false
            })
            DropdownMenuItem(text = { Text("English") }, onClick = {
                language = "en"; prefs.edit().putString(LANGUAGE_KEY, language).apply(); languageMenu = false
            })
        }

        Card(onClick = { editingTrack = tracks.firstOrNull() }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text("MP3 Tag Editor", style = MaterialTheme.typography.titleMedium)
                    Text("Edit judul, artis, dan album lagu di pustaka.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Card(onClick = { showLyricsSync = true }, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Sync, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text("Lirik Sinkron", style = MaterialTheme.typography.titleMedium)
                    Text("Sesuaikan waktu tampil lirik dengan audio.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium) }
        Spacer(Modifier.height(8.dp))
        Text("Catatan: pilihan bahasa tersimpan sebagai preferensi. Teks seluruh aplikasi akan mengikuti bahasa ini setelah lokalisasi string diterapkan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (editingTrack != null) {
        TrackTagEditorDialog(
            initialTrack = editingTrack!!,
            tracks = tracks,
            onDismiss = { editingTrack = null },
            onSave = { track, title, artist, album ->
                try {
                    val uri = Uri.parse(track.audioUrl)
                    val values = ContentValues().apply {
                        put(MediaStore.Audio.Media.TITLE, title.trim())
                        put(MediaStore.Audio.Media.ARTIST, artist.trim())
                        put(MediaStore.Audio.Media.ALBUM, album.trim())
                    }
                    val rows = context.contentResolver.update(uri, values, null, null)
                    if (rows > 0) {
                        onUpdateTrack(track.copy(title = title.trim(), artist = artist.trim(), album = album.trim()))
                        status = "Metadata lagu berhasil diperbarui."
                    } else {
                        status = "Metadata tidak dapat ditulis. File mungkin hanya-baca atau izin tidak tersedia."
                    }
                } catch (_: SecurityException) {
                    status = "Akses ditolak Android. File perlu izin tulis atau konfirmasi akses."
                } catch (_: Exception) {
                    status = "Gagal memperbarui metadata lagu."
                }
                editingTrack = null
            }
        )
    }

    if (showLyricsSync) {
        val initial = prefs.getInt(LYRICS_OFFSET_KEY, 0)
        var offset by remember(showLyricsSync) { mutableFloatStateOf(initial.toFloat()) }
        AlertDialog(
            onDismissRequest = { showLyricsSync = false },
            title = { Text("Lirik Sinkron") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Offset: ${offset.toInt()} ms")
                    Slider(value = offset, onValueChange = { offset = it }, valueRange = -5000f..5000f, steps = 99)
                    Text("Nilai negatif memajukan lirik; nilai positif menunda lirik.", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(onClick = {
                    prefs.edit().putInt(LYRICS_OFFSET_KEY, offset.toInt()).apply()
                    showLyricsSync = false
                    status = "Offset lirik disimpan."
                }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { showLyricsSync = false }) { Text("Batal") } }
        )
    }
}

@Composable
private fun TrackTagEditorDialog(
    initialTrack: Track,
    tracks: List<Track>,
    onDismiss: () -> Unit,
    onSave: (Track, String, String, String) -> Unit
) {
    var selected by remember { mutableStateOf(initialTrack) }
    var title by remember(initialTrack.id) { mutableStateOf(initialTrack.title) }
    var artist by remember(initialTrack.id) { mutableStateOf(initialTrack.artist) }
    var album by remember(initialTrack.id) { mutableStateOf(initialTrack.album) }
    var trackMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("MP3 Tag Editor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { trackMenu = true }) {
                    Icon(Icons.Default.MusicNote, contentDescription = null)
                    Text("Lagu: ${selected.title}")
                }
                DropdownMenu(expanded = trackMenu, onDismissRequest = { trackMenu = false }) {
                    tracks.forEach { track ->
                        DropdownMenuItem(text = { Text(track.title) }, onClick = {
                            selected = track; title = track.title; artist = track.artist; album = track.album; trackMenu = false
                        })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Judul") }, singleLine = true)
                OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artis") }, singleLine = true)
                OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Album") }, singleLine = true)
            }
        },
        confirmButton = { Button(onClick = { onSave(selected, title, artist, album) }, enabled = title.isNotBlank() && artist.isNotBlank()) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
