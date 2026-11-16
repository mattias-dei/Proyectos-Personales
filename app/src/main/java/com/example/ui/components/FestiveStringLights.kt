package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BulbGold
import com.example.ui.theme.BulbGreen
import com.example.ui.theme.BulbIceBlue
import com.example.ui.theme.BulbRed
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun FestiveStringLights(
    enabled: Boolean,
    pattern: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "christmas_lights")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (pattern == "party" || isPlaying) 1400 else 2800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "lights_phase"
    )

    val wireColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    val bulbPalette = listOf(BulbRed, BulbGold, BulbGreen, BulbIceBlue)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        val count = 11
        val width = size.width
        val height = size.height
        val wirePath = Path()
        wirePath.moveTo(0f, 6.dp.toPx())

        val stepX = width / (count + 1)
        for (i in 0..count) {
            val x1 = i * stepX
            val x2 = (i + 1) * stepX
            val midX = (x1 + x2) / 2f
            wirePath.quadraticTo(midX, 16.dp.toPx(), x2, 6.dp.toPx())
        }
        drawPath(
            path = wirePath,
            color = wireColor,
            style = Stroke(width = 2.dp.toPx())
        )

        for (i in 1..count) {
            val bx = i * stepX
            val by = 13.dp.toPx()
            val baseColor = bulbPalette[(i - 1) % bulbPalette.size]

            val brightness = if (!enabled) {
                0.18f
            } else {
                when (pattern) {
                    "chase" -> {
                        val activeIdx = ((phase / (2f * PI.toFloat())) * count).toInt() % count
                        if ((i - 1) == activeIdx || (i - 1) == (activeIdx + 1) % count) 1.0f else 0.22f
                    }
                    "warm_glow" -> {
                        0.68f + 0.32f * ((sin(phase.toDouble()) + 1.0) / 2.0).toFloat()
                    }
                    "party" -> {
                        val pulse = sin((phase * 2.0 + i * 1.1)).toFloat()
                        if (pulse > 0f) 1.0f else 0.25f
                    }
                    else -> { // twinkle
                        val wave = sin((phase + i * 0.85f).toDouble()).toFloat()
                        0.32f + 0.68f * ((wave + 1f) / 2f)
                    }
                }
            }

            if (enabled && brightness > 0.4f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            baseColor.copy(alpha = brightness * 0.55f),
                            Color.Transparent
                        ),
                        center = Offset(bx, by + 5.dp.toPx()),
                        radius = 16.dp.toPx()
                    ),
                    radius = 16.dp.toPx(),
                    center = Offset(bx, by + 5.dp.toPx())
                )
            }

            // Socket cap
            drawRect(
                color = wireColor,
                topLeft = Offset(bx - 2.5.dp.toPx(), by - 4.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(5.dp.toPx(), 4.dp.toPx())
            )

            // Glowing bulb
            drawCircle(
                color = baseColor.copy(alpha = brightness),
                radius = 5.dp.toPx(),
                center = Offset(bx, by + 4.dp.toPx())
            )
        }
    }
}
