package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DspSettings
import com.example.model.EqualizerBand
import com.example.model.ReverbPreset
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
fun DspEqualizerScreen(
    dspSettings: DspSettings,
    onUpdateDspSettings: (DspSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioDarkBg)
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("dsp_equalizer_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AuraSound DSP Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "10-Band EQ, Bass Booster & Dolby 3D Spatializer",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onUpdateDspSettings(DspSettings()) },
                    modifier = Modifier.testTag("reset_dsp_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset DSP ke Default",
                        tint = TextSecondary
                    )
                }

                Switch(
                    checked = dspSettings.isEnabled,
                    onCheckedChange = { onUpdateDspSettings(dspSettings.copy(isEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StudioDarkBg,
                        checkedTrackColor = NeonCyan,
                        uncheckedThumbColor = TextTertiary,
                        uncheckedTrackColor = StudioCard
                    ),
                    modifier = Modifier.testTag("dsp_master_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Selector Horizontal Scroll
        Text(
            text = "PRESET AUDIO STUDIO",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextTertiary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DspSettings.PRESETS.forEach { (name, levels) ->
                val isSelected = dspSettings.currentPresetName == name
                val bgBrush = if (isSelected) {
                    Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.25f), ElectricBlue.copy(alpha = 0.25f)))
                } else {
                    Brush.horizontalGradient(listOf(StudioCard, StudioCard))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(bgBrush)
                        .border(
                            1.dp,
                            if (isSelected) NeonCyan else DividerColor,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            val newBands = dspSettings.bands.mapIndexed { idx, band ->
                                band.copy(levelMb = levels.getOrElse(idx) { 0 })
                            }
                            onUpdateDspSettings(
                                dspSettings.copy(
                                    currentPresetName = name,
                                    bands = newBands
                                )
                            )
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("preset_$name")
                ) {
                    Text(
                        text = name,
                        color = if (isSelected) NeonCyan else TextSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Bass Booster & Dolby 3D Spatial Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Bass Booster Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StudioCardElevated),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (dspSettings.isBassBoostEnabled) AccentMagenta.copy(alpha = 0.5f) else DividerColor
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("bass_booster_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Waves,
                                contentDescription = null,
                                tint = AccentMagenta,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bass Boost",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                        Switch(
                            checked = dspSettings.isBassBoostEnabled,
                            onCheckedChange = {
                                onUpdateDspSettings(dspSettings.copy(isBassBoostEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StudioDarkBg,
                                checkedTrackColor = AccentMagenta
                            ),
                            modifier = Modifier.testTag("bass_boost_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${(dspSettings.bassBoostStrength / 10f).toInt()}% Strength",
                        color = AccentMagenta,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = dspSettings.bassBoostStrength.toFloat(),
                        onValueChange = {
                            onUpdateDspSettings(dspSettings.copy(bassBoostStrength = it.toInt()))
                        },
                        valueRange = 0f..1000f,
                        enabled = dspSettings.isBassBoostEnabled && dspSettings.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = AccentMagenta,
                            activeTrackColor = AccentMagenta,
                            inactiveTrackColor = DividerColor
                        ),
                        modifier = Modifier.testTag("bass_boost_slider")
                    )
                }
            }

            // Dolby 3D Spatializer Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StudioCardElevated),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (dspSettings.isVirtualizerEnabled) NeonCyan.copy(alpha = 0.5f) else DividerColor
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("dolby_spatial_card")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SurroundSound,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Dolby 3D",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                        Switch(
                            checked = dspSettings.isVirtualizerEnabled,
                            onCheckedChange = {
                                onUpdateDspSettings(dspSettings.copy(isVirtualizerEnabled = it))
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StudioDarkBg,
                                checkedTrackColor = NeonCyan
                            ),
                            modifier = Modifier.testTag("dolby_spatial_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${(dspSettings.virtualizerStrength / 10f).toInt()}% Surround",
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Slider(
                        value = dspSettings.virtualizerStrength.toFloat(),
                        onValueChange = {
                            onUpdateDspSettings(dspSettings.copy(virtualizerStrength = it.toInt()))
                        },
                        valueRange = 0f..1000f,
                        enabled = dspSettings.isVirtualizerEnabled && dspSettings.isEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = DividerColor
                        ),
                        modifier = Modifier.testTag("dolby_spatial_slider")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 10-Band Graphic Equalizer Section
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioCardElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = ElectricBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "10-Band Graphic Equalizer",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontSize = 15.sp
                        )
                    }
                    Text(
                        text = "-15dB s/d +15dB",
                        fontSize = 11.sp,
                        color = TextTertiary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable 10-band sliders
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    dspSettings.bands.forEachIndexed { idx, band ->
                        BandColumn(
                            band = band,
                            enabled = dspSettings.isEnabled,
                            onLevelChange = { newLevel ->
                                val updatedBands = dspSettings.bands.toMutableList()
                                updatedBands[idx] = band.copy(levelMb = newLevel)
                                onUpdateDspSettings(
                                    dspSettings.copy(
                                        currentPresetName = "Kustom",
                                        bands = updatedBands
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Reverb Presets
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioCardElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Akustik Ruang & Reverb",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReverbPreset.entries.forEach { preset ->
                        val isSelected = dspSettings.reverbPreset == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentAmber.copy(alpha = 0.2f) else StudioCard)
                                .border(
                                    1.dp,
                                    if (isSelected) AccentAmber else DividerColor,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onUpdateDspSettings(dspSettings.copy(reverbPreset = preset))
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("reverb_${preset.name}")
                        ) {
                            Text(
                                text = preset.displayName,
                                color = if (isSelected) AccentAmber else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // padding for bottom bars
    }
}

@Composable
private fun BandColumn(
    band: EqualizerBand,
    enabled: Boolean,
    onLevelChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp)
    ) {
        Text(
            text = band.formattedDb(),
            color = if (band.levelMb > 0) NeonCyan else TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Slider for band level (-1500 to +1500 mB)
        // In vertical slider representation or horizontal slider in a constrained box
        Slider(
            value = band.levelMb.toFloat(),
            onValueChange = { onLevelChange(it.toInt()) },
            valueRange = -1500f..1500f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = NeonCyan,
                activeTrackColor = NeonCyan,
                inactiveTrackColor = DividerColor
            ),
            modifier = Modifier.height(120.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .background(StudioCard, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = band.formattedFreq(),
                color = TextTertiary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
