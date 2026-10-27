package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.AudioCoverArtHelper
import com.example.model.Track
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioCardElevated

@Composable
fun AudioCoverImage(
    track: Track,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    var embeddedBitmap by remember(track.id, track.audioUrl) { mutableStateOf<Bitmap?>(null) }
    var loadFinished by remember(track.id) { mutableStateOf(false) }

    LaunchedEffect(track.id, track.audioUrl) {
        if (track.audioUrl.isNotEmpty()) {
            val bitmap = AudioCoverArtHelper.loadEmbeddedCoverArt(context, track.audioUrl)
            if (bitmap != null) {
                embeddedBitmap = bitmap
            }
        }
        loadFinished = true
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        when {
            embeddedBitmap != null -> {
                Image(
                    bitmap = embeddedBitmap!!.asImageBitmap(),
                    contentDescription = contentDescription ?: track.title,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }

            track.coverUrl.isNotEmpty() -> {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(track.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = contentDescription ?: track.title,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(StudioCardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
