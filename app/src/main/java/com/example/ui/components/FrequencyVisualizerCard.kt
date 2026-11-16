package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BulbGold
import com.example.ui.theme.BulbGreen
import com.example.ui.theme.BulbRed
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun FrequencyVisualizerCard(
    isPlaying: Boolean,
    frequencyHz: Int,
    noteLabel: String,
    spectrumBars: List<Float>,
    tempoBpm: Int,
    volumePercent: Int,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VISUALIZADOR DE FRECUENCIA",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isPlaying && frequencyHz > 0) {
                            "$frequencyHz Hz · Nota $noteLabel"
                        } else if (isPlaying) {
                            "0 Hz · Pausa"
                        } else {
                            "En espera · Listo para sonar"
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isPlaying) primaryColor else MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "$tempoBpm BPM · $volumePercent%",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isPlaying) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val animatedBars = spectrumBars.map { raw ->
                val v by animateFloatAsState(
                    targetValue = raw,
                    animationSpec = tween(durationMillis = 90),
                    label = "bar"
                )
                v
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(86.dp)
            ) {
                val count = animatedBars.size.coerceAtLeast(1)
                val gap = 4.dp.toPx()
                val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(4f)
                val maxBarHeight = size.height * 0.72f

                // Spectrum bars
                animatedBars.forEachIndexed { idx, value ->
                    val barH = (value * maxBarHeight).coerceAtLeast(6.dp.toPx())
                    val x = idx * (barWidth + gap)
                    val y = size.height - barH

                    drawRoundRect(
                        color = trackColor,
                        topLeft = Offset(x, size.height - maxBarHeight),
                        size = Size(barWidth, maxBarHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )

                    val barColor = when {
                        idx % 3 == 0 -> BulbRed
                        idx % 3 == 1 -> BulbGold
                        else -> BulbGreen
                    }
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(barColor, primaryColor.copy(alpha = 0.7f))
                        ),
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                // Square-wave passive buzzer trace overlay at top
                if (isPlaying && frequencyHz > 0) {
                    val cycles = ((frequencyHz / 130f).coerceIn(2f, 10f)).toInt()
                    val waveTop = 4.dp.toPx()
                    val waveBot = 20.dp.toPx()
                    val stepW = size.width / (cycles * 2)
                    val path = Path().apply {
                        moveTo(0f, waveBot)
                        for (c in 0 until cycles) {
                            val startX = c * 2 * stepW
                            lineTo(startX, waveTop)
                            lineTo(startX + stepW, waveTop)
                            lineTo(startX + stepW, waveBot)
                            lineTo(startX + 2 * stepW, waveBot)
                        }
                    }
                    drawPath(
                        path = path,
                        color = secondaryColor.copy(alpha = 0.85f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                } else {
                    drawLine(
                        color = tertiaryColor.copy(alpha = 0.4f),
                        start = Offset(0f, 12.dp.toPx()),
                        end = Offset(size.width, 12.dp.toPx()),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
        }
    }
}
