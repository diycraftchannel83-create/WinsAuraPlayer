package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import com.example.ads.AdMobManager
import com.example.ui.components.AppOpenAdDialog
import com.example.ui.components.BottomBannerAd
import com.example.ui.components.InterstitialAdDialog
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.AiPlaylistScreen
import com.example.ui.screens.DspEqualizerScreen
import com.example.ui.screens.EditMetadataDialog
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LrcStudioScreen
import com.example.ui.screens.NowPlayingModal
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.AuraSoundTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextTertiary
import com.example.viewmodel.MusicViewModel
import com.example.viewmodel.NavigationTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AdMobManager.initialize(applicationContext)
        setContent {
            val viewModel: MusicViewModel = viewModel()
            val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
            AuraSoundTheme(darkTheme = isDarkMode) {
                MainAppContent(viewModel = viewModel, isDarkMode = isDarkMode)
            }
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: MusicViewModel = viewModel(),
    isDarkMode: Boolean = true
) {
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val amplitudes by viewModel.amplitudes.collectAsStateWithLifecycle()
    val dspSettings by viewModel.dspSettings.collectAsStateWithLifecycle()
    val visualizerMode by viewModel.visualizerMode.collectAsStateWithLifecycle()
    val appOpenAdVisible by viewModel.appOpenAdVisible.collectAsStateWithLifecycle()
    val interstitialAdVisible by viewModel.interstitialAdVisible.collectAsStateWithLifecycle()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsStateWithLifecycle()
    val isLrcStudioOpen by viewModel.isLrcStudioOpen.collectAsStateWithLifecycle()
    val editingTrack by viewModel.editingTrack.collectAsStateWithLifecycle()
    val isScanningMedia by viewModel.isScanningMedia.collectAsStateWithLifecycle()
    val scanMessage by viewModel.scanMessage.collectAsStateWithLifecycle()

    // Android's audio-session Visualizer requires RECORD_AUDIO permission on many devices.
    val context = LocalContext.current
    var visualizerPermissionRequested by remember { mutableStateOf(false) }
    val visualizerPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.refreshVisualizer()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(currentTrack?.id) {
        if (currentTrack != null && !visualizerPermissionRequested &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            visualizerPermissionRequested = true
            visualizerPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
        Scaffold(
            bottomBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    // Mini player shown if track is active and not on NOW_PLAYING tab
                    if (currentTrack != null && activeTab != NavigationTab.NOW_PLAYING) {
                        MiniPlayerBar(
                            track = currentTrack!!,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.playNext() },
                            onClick = { viewModel.setActiveTab(NavigationTab.NOW_PLAYING) }
                        )
                    }

                    // Bottom Banner Ad: Slim & positioned non-intrusively above navigation
                    BottomBannerAd()

                    // M3 Bottom Navigation Bar
                    NavigationBar(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("main_bottom_nav")
                    ) {
                        // 1. Now Playing Tab (replacing old online artist tab)
                        NavigationBarItem(
                            selected = activeTab == NavigationTab.NOW_PLAYING,
                            onClick = { viewModel.setActiveTab(NavigationTab.NOW_PLAYING) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.PlayCircle,
                                    contentDescription = "Pemutar Musik"
                                )
                            },
                            label = {
                                Text(
                                    text = "Pemutar",
                                    fontSize = 11.sp,
                                    fontWeight = if (activeTab == NavigationTab.NOW_PLAYING) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            ),
                            modifier = Modifier.testTag("tab_now_playing")
                        )

                        // 2. Library Tab
                        NavigationBarItem(
                            selected = activeTab == NavigationTab.LIBRARY,
                            onClick = { viewModel.setActiveTab(NavigationTab.LIBRARY) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.LibraryMusic,
                                    contentDescription = "Pustaka Musik"
                                )
                            },
                            label = {
                                Text(
                                    text = "Pustaka",
                                    fontSize = 11.sp,
                                    fontWeight = if (activeTab == NavigationTab.LIBRARY) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            ),
                            modifier = Modifier.testTag("tab_library")
                        )

                        // 3. Audio FX & DSP Studio Tab
                        NavigationBarItem(
                            selected = activeTab == NavigationTab.DSP_STUDIO,
                            onClick = { viewModel.setActiveTab(NavigationTab.DSP_STUDIO) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = "Audio FX & Equalizer"
                                )
                            },
                            label = {
                                Text(
                                    text = "Audio FX",
                                    fontSize = 11.sp,
                                    fontWeight = if (activeTab == NavigationTab.DSP_STUDIO) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            ),
                            modifier = Modifier.testTag("tab_dsp_studio")
                        )

                        // 4. AI Cerdas Playlist Tab
                        NavigationBarItem(
                            selected = activeTab == NavigationTab.AI_PLAYLIST,
                            onClick = { viewModel.setActiveTab(NavigationTab.AI_PLAYLIST) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI Playlist"
                                )
                            },
                            label = {
                                Text(
                                    text = "AI Cerdas",
                                    fontSize = 11.sp,
                                    fontWeight = if (activeTab == NavigationTab.AI_PLAYLIST) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            ),
                            modifier = Modifier.testTag("tab_ai_playlist")
                        )

                        // 5. Settings
                        NavigationBarItem(
                            selected = activeTab == NavigationTab.SETTINGS,
                            onClick = { viewModel.setActiveTab(NavigationTab.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Pengaturan"
                                )
                            },
                            label = {
                                Text(
                                    text = "Settings",
                                    fontSize = 11.sp,
                                    fontWeight = if (activeTab == NavigationTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                selectedTextColor = NeonCyan,
                                indicatorColor = NeonCyan,
                                unselectedIconColor = TextTertiary,
                                unselectedTextColor = TextTertiary
                            ),
                            modifier = Modifier.testTag("tab_settings")
                        )
                    }
                }
            },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeTab) {
                    NavigationTab.NOW_PLAYING -> {
                        NowPlayingScreen(
                            track = currentTrack ?: allTracks.firstOrNull(),
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            amplitudes = amplitudes,
                            dspSettings = dspSettings,
                            visualizerMode = visualizerMode,
                            onChangeVisualizerMode = { viewModel.changeVisualizerMode(it) },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSeekTo = { viewModel.seekTo(it) },
                            onNext = { viewModel.playNext() },
                            onPrevious = { viewModel.playPrevious() },
                            repeatMode = repeatMode,
                            onToggleRepeat = { viewModel.cycleRepeatMode() },
                            onOpenDsp = { viewModel.setActiveTab(NavigationTab.DSP_STUDIO) },
                            onShowAd = { viewModel.showInterstitialAd() },
                            isDarkMode = isDarkMode,
                            onToggleTheme = { viewModel.toggleDarkMode() },
                            onOpenLrcStudio = { viewModel.openLrcStudio() },
                            onOpenMetadataEditor = { viewModel.openMetadataEditor(it) }
                        )
                    }

                    NavigationTab.LIBRARY -> {
                        LibraryScreen(
                            tracks = allTracks,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            onTrackSelect = {
                                viewModel.playTrack(it)
                                viewModel.setActiveTab(NavigationTab.NOW_PLAYING)
                            },
                            onScanDevice = { viewModel.scanDeviceAudio() },
                            isScanning = isScanningMedia,
                            scanMessage = scanMessage,
                            onScanPermissionDenied = { viewModel.reportScanPermissionDenied() }
                        )
                    }

                    NavigationTab.DSP_STUDIO -> {
                        DspEqualizerScreen(
                            dspSettings = dspSettings,
                            onUpdateDspSettings = { viewModel.updateDspSettings(it) }
                        )
                    }

                    NavigationTab.AI_PLAYLIST -> {
                        AiPlaylistScreen(
                            allTracks = allTracks,
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            onTrackSelect = {
                                viewModel.playTrack(it)
                                viewModel.setActiveTab(NavigationTab.NOW_PLAYING)
                            },
                            onApplyEqPreset = { presetName ->
                                viewModel.applyEqPreset(presetName)
                                viewModel.setActiveTab(NavigationTab.DSP_STUDIO)
                            }
                        )
                    }

                    NavigationTab.SETTINGS -> {
                        SettingsScreen(
                            tracks = allTracks,
                            onUpdateTrack = { viewModel.updateTrackMetadata(it) }
                        )
                    }
                }
            }
        }

        // Fullscreen Now Playing Modal Overlay (if opened as modal)
        AnimatedVisibility(
            visible = isNowPlayingExpanded && currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            currentTrack?.let { track ->
                NowPlayingModal(
                    track = track,
                    isPlaying = isPlaying,
                    currentPositionMs = currentPositionMs,
                    durationMs = durationMs,
                    amplitudes = amplitudes,
                    dspSettings = dspSettings,
                    visualizerMode = visualizerMode,
                    onChangeVisualizerMode = { viewModel.changeVisualizerMode(it) },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onNext = { viewModel.playNext() },
                    onPrevious = { viewModel.playPrevious() },
                    repeatMode = repeatMode,
                    onToggleRepeat = { viewModel.cycleRepeatMode() },
                    onOpenDsp = {
                        viewModel.setNowPlayingExpanded(false)
                        viewModel.setActiveTab(NavigationTab.DSP_STUDIO)
                    },
                    onDismiss = { viewModel.setNowPlayingExpanded(false) },
                    isDarkMode = isDarkMode,
                    onToggleTheme = { viewModel.toggleDarkMode() },
                    onOpenMetadataEditor = { viewModel.openMetadataEditor(it) }
                )
            }
        }

        // LRC Studio imported from WinsLyricEditor.
        AnimatedVisibility(
            visible = isLrcStudioOpen && (currentTrack ?: allTracks.firstOrNull()) != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            val activeSong = currentTrack ?: allTracks.first()
            LrcStudioScreen(
                track = activeSong,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                onTogglePlayPause = { viewModel.togglePlayPause() },
                onSeekTo = { viewModel.seekTo(it) },
                onSaveLyrics = { song, lines -> viewModel.saveAndApplyLyrics(song, lines) },
                onDraftChanged = { song, lines -> viewModel.updateLrcStudioDraft(song, lines) },
                onClose = { viewModel.closeLrcStudio() }
            )
        }

        editingTrack?.let { trackToEdit ->
            EditMetadataDialog(track = trackToEdit, onDismiss = { viewModel.closeMetadataEditor() }, onSave = { metadata -> viewModel.saveTrackMetadata(trackToEdit, metadata) })
        }

        // App Open Ad on launch with ID ca-app-pub-1254774552944426/9151767484
        AppOpenAdDialog(
            visible = appOpenAdVisible,
            onDismiss = { viewModel.dismissAppOpenAd() }
        )

        // Interstitial Ad with ID ca-app-pub-1254774552944426/3914322052
        InterstitialAdDialog(
            visible = interstitialAdVisible,
            onDismiss = { viewModel.dismissInterstitialAd() }
        )
    }
}
