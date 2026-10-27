package com.example.model

enum class VisualizerMode(val displayName: String, val description: String) {
    SPECTRUM_BARS("Spektrum Neon", "32-band audio visualizer dengan peak hold"),
    NEON_WAVE("Gelombang Fluida", "Smooth fluid audio waveform oscilator"),
    CIRCULAR_PULSE("Circular Pulse", "Radial bass pulse aura reaktif")
}
