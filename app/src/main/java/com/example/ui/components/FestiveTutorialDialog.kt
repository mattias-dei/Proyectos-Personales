package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import kotlin.math.PI
import kotlin.math.sin

private data class TutorialStep(
    val icon: ImageVector,
    val badge: String,
    val title: String,
    val description: String
)

private val TUTORIAL_STEPS = listOf(
    TutorialStep(
        icon = Icons.Rounded.AutoAwesome,
        badge = "Bienvenida",
        title = "¡Feliz Navidad con música mágica!",
        description = "Santa y el Niñito Jesús te dan la bienvenida. Controla villancicos completos en el buzzer de tu ESP32, luces navideñas y listas temáticas desde un solo lugar."
    ),
    TutorialStep(
        icon = Icons.Rounded.Bluetooth,
        badge = "Paso 1 · Conexión",
        title = "Conecta tu ESP32 por Bluetooth",
        description = "Pulsa «Conectar ESP32», toca actualizar y selecciona ESP32_Navidad_Buzzer. Si aún no subes el código al ESP32, abre el botón </> arriba para copiar el firmware para GPIO 25."
    ),
    TutorialStep(
        icon = Icons.Rounded.AllInclusive,
        badge = "Paso 2 · Reproducción continua",
        title = "Villancicos completos y en bucle",
        description = "Elige «Todas en bucle» o «Repetir canción» en el estudio para escuchar música continua sin volver a tocar botones. También puedes ajustar BPM, volumen o importar archivos .rtttl y .json."
    ),
    TutorialStep(
        icon = Icons.Rounded.Lightbulb,
        badge = "Paso 3 · Ambiente navideño",
        title = "Luces, apagado automático y widget",
        description = "Activa los destellos navideños, programa el temporizador antes de dormir, personaliza recordatorios diarios y fija el widget de cuenta regresiva en tu pantalla de inicio."
    )
)

@Composable
fun FestiveTutorialDialog(
    initialShowOnStart: Boolean,
    onDismiss: (showOnStart: Boolean) -> Unit
) {
    var stepIndex by remember { mutableIntStateOf(0) }
    var showOnStart by remember(initialShowOnStart) { mutableStateOf(initialShowOnStart) }

    val transition = rememberInfiniteTransition(label = "christmas_characters_anim")

    val snowProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_fall"
    )

    val santaBob by transition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "santa_bob"
    )

    val santaTilt by transition.animateFloat(
        initialValue = -4.5f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "santa_tilt"
    )

    val babyJesusFloat by transition.animateFloat(
        initialValue = 5f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "baby_jesus_float"
    )

    val haloPulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_pulse"
    )

    val starTwinkle by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_twinkle"
    )

    Dialog(
        onDismissRequest = { onDismiss(showOnStart) },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .testTag("festive_tutorial_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Animated Santa & Niñito Jesús Greeting Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(248.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF071912),
                                    Color(0xFF0F2E22),
                                    Color(0xFF331119)
                                )
                            )
                        )
                ) {
                    // Animated snowfall & Bethlehem starlight
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val snowCount = 24
                        for (i in 0 until snowCount) {
                            val baseX = ((i * 73 + 19) % 100) / 100f
                            val phaseOffset = (i * 0.137f) % 1f
                            val yFrac = (snowProgress + phaseOffset) % 1f
                            val sway = sin((snowProgress * 2f * PI + i).toDouble()).toFloat() * 10f
                            val x = baseX * size.width + sway
                            val y = yFrac * size.height
                            val radius = if (i % 3 == 0) 3.5f else 2.2f
                            drawCircle(
                                color = Color.White.copy(alpha = if (i % 2 == 0) 0.55f else 0.3f),
                                radius = radius,
                                center = Offset(x, y)
                            )
                        }

                        val stars = listOf(
                            0.12f to 0.14f, 0.28f to 0.09f, 0.50f to 0.10f,
                            0.74f to 0.12f, 0.88f to 0.18f, 0.42f to 0.22f
                        )
                        stars.forEachIndexed { idx, (sx, sy) ->
                            val alpha = if (idx % 2 == 0) starTwinkle else 1.25f - starTwinkle
                            drawCircle(
                                color = Color(0xFFFFDF7A).copy(alpha = alpha.coerceIn(0.2f, 0.95f)),
                                radius = if (idx == 2) 5.5f else 3f,
                                center = Offset(size.width * sx, size.height * sy)
                            )
                        }
                    }

                    IconButton(
                        onClick = { onDismiss(showOnStart) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .testTag("tutorial_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cerrar tutorial",
                            tint = Color.White.copy(alpha = 0.88f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFFD15C).copy(alpha = 0.18f),
                            modifier = Modifier.border(
                                width = 1.dp,
                                color = Color(0xFFFFD15C).copy(alpha = 0.55f),
                                shape = RoundedCornerShape(50)
                            )
                        ) {
                            Text(
                                text = "✨ ¡FELIZ NAVIDAD! ✨",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color(0xFFFFE28A),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Animated Santa Claus
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(112.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(106.dp)
                                            .graphicsLayer {
                                                scaleX = haloPulse
                                                scaleY = haloPulse
                                            }
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0x66C62828),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                    Image(
                                        painter = painterResource(id = R.drawable.img_santa_tutorial),
                                        contentDescription = "Santa Claus deseando Feliz Navidad",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(96.dp)
                                            .graphicsLayer {
                                                translationY = santaBob
                                                rotationZ = santaTilt
                                                scaleX = 1.18f
                                                scaleY = 1.18f
                                            }
                                            .clip(CircleShape)
                                            .border(2.5.dp, Color(0xFFFFD15C), CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Santa Claus",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Animated Niñito Jesús
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(112.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(110.dp)
                                            .graphicsLayer {
                                                scaleX = haloPulse
                                                scaleY = haloPulse
                                                alpha = starTwinkle.coerceIn(0.45f, 0.95f)
                                            }
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    colors = listOf(
                                                        Color(0x99FFD54F),
                                                        Color.Transparent
                                                    )
                                                )
                                            )
                                    )
                                    Image(
                                        painter = painterResource(id = R.drawable.img_nino_jesus_tutorial),
                                        contentDescription = "Niñito Jesús deseando Feliz Navidad",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(96.dp)
                                            .graphicsLayer {
                                                translationY = babyJesusFloat
                                            }
                                            .clip(CircleShape)
                                            .border(2.5.dp, Color(0xFFFFE57F), CircleShape)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Niñito Jesús",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFFE57F),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = "Te desean una Navidad llena de música, paz y alegría",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE7F0EC),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Tutorial Step Content
                val currentStep = TUTORIAL_STEPS[stepIndex]
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                        label = "tutorial_step_content"
                    ) { step ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        imageVector = step.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = step.badge.uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = step.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = step.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Step indicator dots
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TUTORIAL_STEPS.indices.forEach { idx ->
                            val selected = idx == stepIndex
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp)
                                    .height(8.dp)
                                    .width(if (selected) 22.dp else 8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }

                    // Option to keep showing animated greeting on launch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = showOnStart,
                            onCheckedChange = { showOnStart = it },
                            modifier = Modifier.testTag("tutorial_show_on_start_checkbox")
                        )
                        Text(
                            text = "Mostrar saludo navideño al iniciar la app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Navigation buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (stepIndex > 0) {
                            TextButton(
                                onClick = { stepIndex-- },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("tutorial_prev_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Anterior")
                            }
                        } else {
                            TextButton(
                                onClick = { onDismiss(showOnStart) },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("tutorial_skip_button")
                            ) {
                                Text("Saltar")
                            }
                        }

                        if (stepIndex < TUTORIAL_STEPS.lastIndex) {
                            Button(
                                onClick = { stepIndex++ },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("tutorial_next_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Siguiente")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Button(
                                onClick = { onDismiss(showOnStart) },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("tutorial_finish_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Text("¡Feliz Navidad!")
                            }
                        }
                    }
                }
            }
        }
    }
}
