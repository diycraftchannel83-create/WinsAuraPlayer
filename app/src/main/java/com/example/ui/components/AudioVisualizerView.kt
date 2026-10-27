package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.model.VisualizerMode
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.AccentMagenta
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AudioVisualizerView(
    amplitudes: FloatArray,
    mode: VisualizerMode,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val phaseAnim = remember { Animatable(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            phaseAnim.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        } else {
            phaseAnim.snapTo(0f)
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (mode) {
            VisualizerMode.SPECTRUM_BARS -> {
                val numBars = amplitudes.size.coerceAtMost(32)
                val totalPadding = w * 0.15f
                val availableWidth = w - totalPadding
                val barWidth = (availableWidth / numBars) * 0.72f
                val gap = (availableWidth / numBars) * 0.28f
                val startX = totalPadding / 2f

                for (i in 0 until numBars) {
                    val amp = amplitudes[i].coerceIn(0.04f, 1f)
                    val barHeight = h * 0.75f * amp
                    val x = startX + i * (barWidth + gap)
                    val y = h - barHeight - 20f

                    // Gradient for each bar: Cyan -> Electric Blue -> Amber/Magenta on peaks
                    val barBrush = Brush.verticalGradient(
                        colors = if (amp > 0.75f) {
                            listOf(AccentMagenta, AccentAmber, NeonCyan)
                        } else {
                            listOf(ElectricBlue, NeonCyan)
                        },
                        startY = y,
                        endY = h - 20f
                    )

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                    )

                    // Peak cap dot
                    if (amp > 0.15f) {
                        drawCircle(
                            color = if (amp > 0.75f) AccentAmber else Color.White,
                            radius = barWidth * 0.45f,
                            center = Offset(x + barWidth / 2f, y - 6f)
                        )
                    }
                }
            }

            VisualizerMode.NEON_WAVE -> {
                val wavePath = Path()
                val wavePath2 = Path()
                val centerY = h * 0.55f
                val points = amplitudes.size
                val stepX = w / (points - 1)

                val phase = Math.toRadians(phaseAnim.value.toDouble()).toFloat()

                for (i in 0 until points) {
                    val amp = amplitudes[i]
                    val x = i * stepX
                    val yOffset = sin((i * 0.45f + phase).toDouble()).toFloat() * (h * 0.35f * amp)
                    val y = centerY + yOffset

                    if (i == 0) {
                        wavePath.moveTo(x, y)
                    } else {
                        wavePath.lineTo(x, y)
                    }

                    // Secondary mirrored harmonic wave
                    val yOffset2 = cos((i * 0.35f + phase * 1.4f).toDouble()).toFloat() * (h * 0.25f * amp)
                    val y2 = centerY - yOffset2
                    if (i == 0) {
                        wavePath2.moveTo(x, y2)
                    } else {
                        wavePath2.lineTo(x, y2)
                    }
                }

                // Draw secondary wave
                drawPath(
                    path = wavePath2,
                    brush = Brush.horizontalGradient(listOf(ElectricBlue, AccentMagenta, AccentAmber)),
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                // Draw primary glowing neon wave
                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(listOf(NeonCyan, ElectricBlue, NeonCyan)),
                    style = Stroke(width = 7f, cap = StrokeCap.Round)
                )
            }

            VisualizerMode.CIRCULAR_PULSE -> {
                val center = Offset(w / 2f, h / 2f)
                val baseRadius = (w.coerceAtMost(h) * 0.22f)
                val bassEnergy = if (amplitudes.isNotEmpty()) {
                    (amplitudes[0] + amplitudes[1] + amplitudes[2] + amplitudes[3]) / 4f
                } else 0.2f

                // Central glowing bass orb
                val pulseRadius = baseRadius + (bassEnergy * 45f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(NeonCyan.copy(alpha = 0.5f), Color.Transparent),
                        center = center,
                        radius = pulseRadius * 1.5f
                    ),
                    radius = pulseRadius * 1.5f,
                    center = center
                )

                drawCircle(
                    color = NeonCyan.copy(alpha = 0.85f),
                    radius = pulseRadius,
                    center = center,
                    style = Stroke(width = 6f)
                )

                // 24 Radial frequency rays emitting around circle
                val rayCount = 24
                val angleStep = (2 * Math.PI / rayCount).toFloat()
                for (i in 0 until rayCount) {
                    val amp = amplitudes[i % amplitudes.size]
                    val angle = i * angleStep + Math.toRadians(phaseAnim.value.toDouble()).toFloat() * 0.3f
                    val innerR = pulseRadius + 12f
                    val outerR = innerR + (amp * 65f)

                    val start = Offset(
                        center.x + cos(angle.toDouble()).toFloat() * innerR,
                        center.y + sin(angle.toDouble()).toFloat() * innerR
                    )
                    val end = Offset(
                        center.x + cos(angle.toDouble()).toFloat() * outerR,
                        center.y + sin(angle.toDouble()).toFloat() * outerR
                    )

                    val rayColor = if (i % 2 == 0) ElectricBlue else AccentAmber
                    drawLine(
                        color = rayColor.copy(alpha = (amp * 0.9f).coerceIn(0.2f, 1f)),
                        start = start,
                        end = end,
                        strokeWidth = 5f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
