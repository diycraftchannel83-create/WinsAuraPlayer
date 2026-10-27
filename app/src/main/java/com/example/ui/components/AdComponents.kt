package com.example.ui.components

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ads.AdMobManager
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentGreen
import com.example.ui.theme.AccentMagenta
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioCardElevated
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.delay

@Composable
fun AppOpenAdDialog(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    var countdown by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        val activity = context as? Activity
        if (activity != null && AdMobManager.isAppOpenAdReady()) {
            AdMobManager.showAppOpenAd(activity) {
                onDismiss()
            }
        } else {
            while (countdown > 0) {
                delay(1000L)
                countdown--
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = StudioCardElevated),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.6f), ElectricBlue.copy(alpha = 0.3f)))
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .testTag("app_open_ad_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(AccentAmber.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "APP OPEN AD",
                            color = AccentAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("close_app_open_ad_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup Iklan",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(
                            Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.25f), ElectricBlue.copy(alpha = 0.15f))),
                            CircleShape
                        )
                        .border(1.5.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Headphones,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "AuraSound Studio Hi-Fi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    text = "Unit ID: ${AdMobManager.APP_OPEN_AD_UNIT_ID}",
                    fontSize = 10.sp,
                    color = TextTertiary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Pemutar audio studio dengan 10-Band EQ, Bass Booster, dan visualizer neon dinamis. Selamat datang di pengalaman musik audiofil terbaik.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("continue_from_open_ad_button")
                ) {
                    Text(
                        text = if (countdown > 0) "Lanjut ke Musik ($countdown detik)" else "Buka Aplikasi Sekarang",
                        color = StudioDarkBg,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun InterstitialAdDialog(
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible) return

    val context = LocalContext.current
    LaunchedEffect(visible) {
        val activity = context as? Activity
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            onDismiss()
            return@LaunchedEffect
        }

        if (AdMobManager.isInterstitialAdReady()) {
            AdMobManager.showInterstitialAd(activity) {
                onDismiss()
            }
        } else {
            // Do not display a fake interstitial dialog. Load the real AdMob ad for
            // the next eligible trigger and close this trigger safely.
            AdMobManager.loadInterstitialAd(activity)
            onDismiss()
        }
    }
}

@Composable
fun BottomBannerAd(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val adWidth = configuration.screenWidthDp.coerceAtLeast(320)
    var adView by remember(context, adWidth) { mutableStateOf<AdView?>(null) }

    // Wait for the Google Mobile Ads SDK initialization before requesting a banner.
    LaunchedEffect(context, adWidth) {
        var attempts = 0
        while (!AdMobManager.isInitialized && attempts < 100) {
            delay(100L)
            attempts++
        }

        if (AdMobManager.isInitialized && adView == null) {
            adView = AdView(context).apply {
                adUnitId = AdMobManager.BANNER_AD_UNIT_ID
                setAdSize(
                    AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
                )
                loadAd(AdRequest.Builder().build())
            }
        }
    }

    DisposableEffect(adView) {
        onDispose {
            adView?.destroy()
        }
    }

    val loadedAdView = adView
    if (loadedAdView != null) {
        AndroidView(
            factory = { loadedAdView },
            modifier = modifier
                .fillMaxWidth()
                .testTag("bottom_banner_ad")
        )
    }
}
