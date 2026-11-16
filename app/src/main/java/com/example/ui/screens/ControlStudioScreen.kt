package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.audio.AudioPlaybackTelemetry
import com.example.data.bluetooth.BtConnectionState
import com.example.data.bluetooth.DiscoveredEsp32Device
import com.example.data.bluetooth.Esp32BluetoothManager
import com.example.data.bluetooth.Esp32BluetoothUiState
import com.example.data.model.ChristmasSong
import com.example.data.model.PlaybackLoopMode
import com.example.data.model.UserSettingsDoc
import com.example.ui.components.FrequencyVisualizerCard
import com.example.ui.theme.JetBrainsMonoFontFamily

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ControlStudioScreen(
    settings: UserSettingsDoc,
    selectedSong: ChristmasSong,
    loopMode: PlaybackLoopMode,
    telemetry: AudioPlaybackTelemetry,
    btState: Esp32BluetoothUiState,
    remainingAutoOffSeconds: Long,
    requiredBtPermissions: Array<String>,
    onRefreshBt: () -> Unit,
    onStartScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnectDevice: (DiscoveredEsp32Device) -> Unit,
    onDisconnectDevice: () -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onPreviousSong: () -> Unit,
    onNextSong: () -> Unit,
    onSelectLoopMode: (PlaybackLoopMode) -> Unit,
    onTempoChange: (Int) -> Unit,
    onVolumeChange: (Int) -> Unit,
    onToggleLights: () -> Unit,
    onSelectLightsPattern: (String) -> Unit,
    onStartAutoOff: (Int) -> Unit,
    onCancelAutoOff: () -> Unit,
    onOpenTutorial: () -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val context = LocalContext.current
    var showBtSheet by remember { mutableStateOf(false) }
    var showSketchDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        onRefreshBt()
        onStartScan()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Banner + ESP32 Bluetooth Connection Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_christmas_hero),
                        contentDescription = "Banner navideño de estudio ESP32",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xD9071612),
                                        Color(0x99071612)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Estudio ESP32 Navideño",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White
                                )
                                Text(
                                    text = btState.statusMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFD15C),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = onOpenTutorial,
                                    modifier = Modifier.testTag("open_tutorial_button")
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFFD15C).copy(alpha = 0.22f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = "Abrir saludo y tutorial navideño",
                                            tint = Color(0xFFFFE28A),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { showSketchDialog = true },
                                    modifier = Modifier.testTag("esp32_sketch_button")
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.14f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Code,
                                            contentDescription = "Ver código Arduino ESP32",
                                            tint = Color.White,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (!btState.hasPermissions) {
                                        permissionLauncher.launch(requiredBtPermissions)
                                    } else {
                                        onRefreshBt()
                                    }
                                    showBtSheet = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("bluetooth_connect_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (btState.connectionState == BtConnectionState.CONNECTED) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                            ) {
                                Icon(
                                    imageVector = when (btState.connectionState) {
                                        BtConnectionState.CONNECTED -> Icons.Rounded.BluetoothConnected
                                        BtConnectionState.SCANNING -> Icons.Rounded.BluetoothSearching
                                        else -> Icons.Rounded.Bluetooth
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (btState.connectionState) {
                                        BtConnectionState.CONNECTED ->
                                            btState.connectedDevice?.name ?: "ESP32 Conectado"
                                        BtConnectionState.CONNECTING -> "Conectando..."
                                        BtConnectionState.SCANNING -> "Buscando..."
                                        BtConnectionState.DISCONNECTED -> "Conectar ESP32"
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (btState.connectionState == BtConnectionState.CONNECTED) {
                                OutlinedButton(
                                    onClick = onDisconnectDevice,
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("bluetooth_disconnect_button"),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Desconectar", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // 2. Active Song + Play / Stop Transport
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "MELODÍA ACTIVA",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = selectedSong.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val scaledSec = remember(selectedSong.id, settings.tempoBpm) {
                                    val baseBpm = selectedSong.defaultBpm.coerceAtLeast(40).toFloat()
                                    val curBpm = settings.tempoBpm.coerceIn(40L, 240L).toFloat()
                                    ((selectedSong.totalDurationMs * (baseBpm / curBpm)) / 1000f)
                                        .toInt().coerceAtLeast(1)
                                }
                                Text(
                                    text = "${selectedSong.subtitle} · ${scaledSec / 60}:${String.format("%02d", scaledSec % 60)} · ${selectedSong.notes.size} notas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = onNavigateToLibrary,
                                modifier = Modifier.testTag("change_song_button")
                            ) {
                                Text("Cambiar")
                            }
                        }

                        if (telemetry.isPlaying && telemetry.totalNotes > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = {
                                    ((telemetry.currentNoteIndex + 1).toFloat() / telemetry.totalNotes.toFloat())
                                        .coerceIn(0f, 1f)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = onPreviousSong,
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("previous_song_button"),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Canción anterior")
                            }

                            Button(
                                onClick = onPlay,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("play_melody_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (telemetry.isPlaying) "Reiniciar" else "Reproducir",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            FilledTonalButton(
                                onClick = onStop,
                                enabled = telemetry.isPlaying,
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("stop_melody_button"),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = "Detener")
                            }

                            FilledTonalButton(
                                onClick = onNextSong,
                                modifier = Modifier
                                    .height(50.dp)
                                    .testTag("next_song_button"),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente canción")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PlaybackLoopMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = loopMode == mode,
                                    onClick = { onSelectLoopMode(mode) },
                                    modifier = Modifier.testTag("loop_mode_${mode.name.lowercase()}"),
                                    leadingIcon = when (mode) {
                                        PlaybackLoopMode.LOOP_ALL -> ({
                                            Icon(
                                                Icons.Rounded.AllInclusive,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        })
                                        PlaybackLoopMode.REPEAT_SONG -> ({
                                            Icon(
                                                Icons.Rounded.RepeatOne,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        })
                                        PlaybackLoopMode.ONCE -> null
                                    },
                                    label = { Text(mode.label) }
                                )
                            }
                        }

                        if (loopMode != PlaybackLoopMode.ONCE) {
                            Text(
                                text = if (loopMode == PlaybackLoopMode.LOOP_ALL) {
                                    "Reproducción continua: pasará automáticamente al siguiente villancico sin tocar botones"
                                } else {
                                    "Bucle infinito activo: la canción se repetirá continuamente"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // 3. Real-Time Frequency Visualizer
            item {
                FrequencyVisualizerCard(
                    isPlaying = telemetry.isPlaying,
                    frequencyHz = telemetry.currentFrequencyHz,
                    noteLabel = telemetry.currentNoteLabel,
                    spectrumBars = telemetry.spectrumBars,
                    tempoBpm = settings.tempoBpm.toInt(),
                    volumePercent = settings.volumePercent.toInt()
                )
            }

            // 4. Tempo & Real-Time Volume Controls
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Tempo
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Rounded.Speed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Selector de Tempo", style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    text = "${settings.tempoBpm} BPM",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Slider(
                                value = settings.tempoBpm.toFloat().coerceIn(40f, 240f),
                                onValueChange = { onTempoChange(it.toInt()) },
                                valueRange = 40f..240f,
                                modifier = Modifier.testTag("tempo_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.secondary,
                                    activeTrackColor = MaterialTheme.colorScheme.secondary
                                )
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(80 to "Lento", 120 to "Clásico", 150 to "Alegre", 190 to "Rápido").forEach { (bpm, label) ->
                                    FilterChip(
                                        selected = kotlin.math.abs(settings.tempoBpm - bpm) <= 4,
                                        onClick = { onTempoChange(bpm) },
                                        label = { Text("$label ($bpm)") }
                                    )
                                }
                            }
                        }

                        // Real-time Volume
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.VolumeUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Volumen en Tiempo Real", style = MaterialTheme.typography.titleMedium)
                                }
                                Text(
                                    text = "${settings.volumePercent}%",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = settings.volumePercent.toFloat().coerceIn(0f, 100f),
                                onValueChange = { onVolumeChange(it.toInt()) },
                                valueRange = 0f..100f,
                                modifier = Modifier.testTag("volume_slider")
                            )
                        }
                    }
                }
            }

            // 5. Blinking Christmas Lights & Programmable Auto-Off Timer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Luces Navideñas Intermitentes", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = "Sincroniza guirnalda y LEDs del ESP32",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.lightsEnabled,
                                onCheckedChange = { onToggleLights() },
                                modifier = Modifier.testTag("lights_switch")
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "twinkle" to "Destellos",
                                "chase" to "Persecución",
                                "warm_glow" to "Brillo cálido",
                                "party" to "Fiesta"
                            ).forEach { (key, label) ->
                                FilterChip(
                                    selected = settings.lightsPattern == key,
                                    onClick = { onSelectLightsPattern(key) },
                                    label = { Text(label) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // Auto-off Timer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Apagado Automático", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = if (remainingAutoOffSeconds > 0L) {
                                            val mins = remainingAutoOffSeconds / 60
                                            val secs = remainingAutoOffSeconds % 60
                                            String.format("Se apagará en %02d:%02d", mins, secs)
                                        } else {
                                            "Programa el tiempo antes de dormir"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (remainingAutoOffSeconds > 0L) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                            if (remainingAutoOffSeconds > 0L) {
                                TextButton(
                                    onClick = onCancelAutoOff,
                                    modifier = Modifier.testTag("cancel_auto_off_button")
                                ) {
                                    Text("Cancelar")
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(5, 15, 30, 60, 120).forEach { mins ->
                                AssistChip(
                                    onClick = { onStartAutoOff(mins) },
                                    label = { Text("$mins min") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBtSheet) {
        ModalBottomSheet(onDismissRequest = { showBtSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dispositivos Bluetooth ESP32", style = MaterialTheme.typography.titleLarge)
                    Row {
                        IconButton(onClick = {
                            if (btState.connectionState == BtConnectionState.SCANNING) {
                                onStopScan()
                            } else if (!btState.hasPermissions) {
                                permissionLauncher.launch(requiredBtPermissions)
                            } else {
                                onStartScan()
                            }
                        }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Escanear Bluetooth")
                        }
                    }
                }

                Text(
                    text = btState.statusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!btState.isBluetoothEnabled && btState.isBluetoothSupported) {
                    Button(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Abrir ajustes de Bluetooth")
                    }
                }

                if (btState.discoveredDevices.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "No hay dispositivos ESP32 detectados aún",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Enciende tu placa ESP32 con Bluetooth BLE o Classic SPP (ej. ESP32_Navidad_Buzzer) y pulsa el botón de actualizar.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(btState.discoveredDevices, key = { it.address }) { dev ->
                            Card(
                                onClick = {
                                    onConnectDevice(dev)
                                    showBtSheet = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dev.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${dev.address} · ${dev.transport.name}" +
                                                if (dev.isPaired) " · Emparejado" else " · ${dev.rssi} dBm",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "Conectar",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showSketchDialog) {
        AlertDialog(
            onDismissRequest = { showSketchDialog = false },
            title = { Text("Firmware ESP32 (Buzzer GPIO 25)") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Carga este código Arduino en tu ESP32 para recibir tonos en tiempo real vía Bluetooth:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = Esp32BluetoothManager.ESP32_ARDUINO_SKETCH,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(
                        ClipData.newPlainText("ESP32 Sketch", Esp32BluetoothManager.ESP32_ARDUINO_SKETCH)
                    )
                    showSketchDialog = false
                }) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar código")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSketchDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}
