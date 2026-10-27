package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.model.Track
import com.example.ui.components.TrackItemRow
import com.example.ui.theme.DividerColor
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun LibraryScreen(
    tracks: List<Track>,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackSelect: (Track) -> Unit,
    onScanDevice: () -> Unit,
    isScanning: Boolean = false,
    scanMessage: String? = null,
    onScanPermissionDenied: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") } // "Semua", "Perangkat"
    var selectedGrouping by remember { mutableStateOf("Lagu") } // "Lagu", "Artis", "Album"

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onScanDevice()
        } else {
            onScanPermissionDenied()
        }
    }

    val filteredTracks = tracks.filter { track ->
        val matchesQuery = track.title.contains(searchQuery, ignoreCase = true) ||
                track.artist.contains(searchQuery, ignoreCase = true) ||
                track.album.contains(searchQuery, ignoreCase = true)
        val matchesCategory = when (selectedFilter) {
            "Perangkat" -> !track.isOnline
            else -> true
        }
        matchesQuery && matchesCategory
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
            .testTag("library_screen"),
        contentPadding = PaddingValues(bottom = 120.dp, top = 16.dp)
    ) {
        // Search & Filter header
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Pustaka Musik",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${filteredTracks.size} lagu siap diputar",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Button(
                        enabled = !isScanning,
                        onClick = {
                            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_AUDIO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                onScanDevice()
                            } else {
                                permissionLauncher.launch(permission)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioCard),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("scan_device_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isScanning) "Memindai…" else "Pindai File",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!scanMessage.isNullOrBlank()) {
                    Text(
                        text = scanMessage,
                        color = if (scanMessage.startsWith("Pemindaian gagal") || scanMessage.startsWith("Izin akses")) TextTertiary else NeonCyan,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari judul lagu, album, artis...", color = TextTertiary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DividerColor,
                        focusedContainerColor = StudioCard,
                        unfocusedContainerColor = StudioCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_music_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Filter chips: Semua, Perangkat Lokal
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Semua", "Perangkat").forEach { tab ->
                        val isSelected = selectedFilter == tab
                        val label = if (tab == "Semua") "Semua Lagu" else "Penyimpanan Perangkat"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else StudioCard)
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else DividerColor,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedFilter = tab }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                                .testTag("filter_$tab")
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) NeonCyan else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Library organization: flat song list, grouped by artist, or grouped by album.
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Lagu", "Artis", "Album").forEach { grouping ->
                        val isSelected = selectedGrouping == grouping
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else StudioCard)
                                .border(1.dp, if (isSelected) NeonCyan else DividerColor, RoundedCornerShape(10.dp))
                                .clickable { selectedGrouping = grouping }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("group_$grouping")
                        ) {
                            Text(
                                text = when (grouping) {
                                    "Artis" -> "By Artis"
                                    "Album" -> "By Album"
                                    else -> "Daftar Lagu"
                                },
                                color = if (isSelected) NeonCyan else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        if (filteredTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = TextTertiary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tidak ada lagu yang ditemukan",
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else if (selectedGrouping == "Lagu") {
            items(filteredTracks, key = { it.id }) { track ->
                val isCurrent = currentTrack?.id == track.id
                TrackItemRow(
                    track = track,
                    isCurrent = isCurrent,
                    isPlaying = isCurrent && isPlaying,
                    onSelect = { onTrackSelect(track) }
                )
            }
        } else {
            val groupedTracks = if (selectedGrouping == "Artis") {
                filteredTracks.groupBy { it.artist.trim().ifBlank { "Artis tidak diketahui" } }
                    .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            } else {
                filteredTracks.groupBy { it.album.trim().ifBlank { "Album tidak diketahui" } }
                    .toSortedMap(String.CASE_INSENSITIVE_ORDER)
            }
            groupedTracks.forEach { (groupName, groupTracks) ->
                item(key = "group_header_" + selectedGrouping + "_" + groupName) {
                    Text(
                        text = "$groupName  •  " + groupTracks.size + " lagu",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)
                    )
                }
                items(groupTracks, key = { selectedGrouping + "_" + it.id }) { track ->
                    val isCurrent = currentTrack?.id == track.id
                    TrackItemRow(
                        track = track,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        onSelect = { onTrackSelect(track) }
                    )
                }
            }
        }
    }
}
