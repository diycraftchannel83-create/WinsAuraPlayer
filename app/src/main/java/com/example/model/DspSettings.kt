package com.example.model

data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val levelMb: Int // Millibels, typical range -1500 to +1500 (-15dB to +15dB)
) {
    fun formattedFreq(): String {
        return if (centerFreqHz >= 1000) {
            "${centerFreqHz / 1000}k"
        } else {
            "${centerFreqHz}Hz"
        }
    }

    fun formattedDb(): String {
        val db = levelMb / 100
        return if (db > 0) "+${db}dB" else "${db}dB"
    }
}

enum class ReverbPreset(val id: Short, val displayName: String) {
    NONE(0, "Nonaktif"),
    SMALL_ROOM(1, "Studio Kecil"),
    MEDIUM_ROOM(2, "Ruang Rekaman"),
    LARGE_ROOM(3, "Chamber Hall"),
    MEDIUM_HALL(4, "Concert Hall"),
    LARGE_HALL(5, "Katedral 3D"),
    PLATE(6, "Plate Reverb")
}

data class DspSettings(
    val isEnabled: Boolean = true,
    val isBassBoostEnabled: Boolean = true,
    val bassBoostStrength: Int = 750, // 0 - 1000
    val isVirtualizerEnabled: Boolean = true, // Dolby 3D Spatializer
    val virtualizerStrength: Int = 850, // 0 - 1000
    val reverbPreset: ReverbPreset = ReverbPreset.MEDIUM_ROOM,
    val currentPresetName: String = "Studio Master",
    val bands: List<EqualizerBand> = listOf(
        EqualizerBand(0, 31, 300),
        EqualizerBand(1, 62, 450),
        EqualizerBand(2, 125, 250),
        EqualizerBand(3, 250, 0),
        EqualizerBand(4, 500, -100),
        EqualizerBand(5, 1000, 100),
        EqualizerBand(6, 2000, 250),
        EqualizerBand(7, 4000, 400),
        EqualizerBand(8, 8000, 500),
        EqualizerBand(9, 16000, 600)
    )
) {
    companion object {
        val PRESETS: Map<String, List<Int>> = mapOf(
            "Studio Master" to listOf(300, 450, 250, 0, -100, 100, 250, 400, 500, 600),
            "Bass Booster Extreme" to listOf(900, 800, 600, 300, 0, -100, -50, 100, 250, 400),
            "Dolby 3D Cinema" to listOf(400, 300, 200, 100, 0, 200, 400, 600, 750, 850),
            "Wins Lo-Fi HipHop" to listOf(600, 700, 400, 100, 50, 0, -100, -150, 200, 300),
            "Acoustic & Vocal" to listOf(0, 100, 150, 200, 350, 500, 600, 450, 300, 200),
            "Rock & Metal" to listOf(500, 400, 200, 0, -200, 100, 350, 500, 600, 700),
            "Electronic Club" to listOf(800, 650, 400, 0, 100, 250, 400, 550, 700, 800),
            "Flat / Natural" to listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
        )
    }
}
