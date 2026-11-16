package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.data.model.ThemedPlaylistDoc
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.viewmodel.CloudUiState
import com.example.widget.ChristmasCountdownWidgetProvider
import kotlinx.coroutines.delay

private data class CuratedStreamingPlaylist(
    val title: String,
    val platformLabel: String,
    val url: String,
    val description: String
)

private val CURATED_CHRISTMAS_PLAYLISTS = listOf(
    CuratedStreamingPlaylist(
        title = "Christmas Hits · Spotify",
        platformLabel = "Spotify",
        url = "https://open.spotify.com/playlist/37i9dQZF1DX0Yxoavh5qJV",
        description = "Los villancicos y clásicos festivos más escuchados"
    ),
    CuratedStreamingPlaylist(
        title = "Navidad Acústica · YouTube Music",
        platformLabel = "YouTube Music",
        url = "https://music.youtube.com/search?q=christmas+classics+playlist",
        description = "Música navideña instrumental y coral para noches de invierno"
    ),
    CuratedStreamingPlaylist(
        title = "Holiday Classics · Apple Music",
        platformLabel = "Apple Music",
        url = "https://music.apple.com/us/playlist/holiday-classics/pl.f4d106fed2bd41149aaacabb233eb5eb",
        description = "Selección tradicional de invierno junto a la chimenea"
    )
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PlaylistsAndWidgetScreen(
    playlistsState: CloudUiState<List<ThemedPlaylistDoc>>,
    onAddPlaylist: (title: String, platform: String, url: String, description: String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var countdown by remember { mutableStateOf(ChristmasCountdownWidgetProvider.calculateCountdown()) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            countdown = ChristmasCountdownWidgetProvider.calculateCountdown()
            delay(30_000L)
        }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Home Screen Christmas Countdown Widget Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0C221A),
                                    Color(0xFF173D2F),
                                    Color(0xFF3A101B)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.CalendarMonth,
                                    contentDescription = null,
                                    tint = Color(0xFFF5D06F)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "WIDGET DE CUENTA REGRESIVA",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFF5D06F)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "25 DIC ${countdown.targetYear}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (countdown.isChristmasDay) {
                            Text(
                                text = "¡Hoy es Navidad!",
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "${countdown.days} días",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White
                                )
                                Text(
                                    text = "${countdown.hours}h ${countdown.minutes}m",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 16.sp,
                                    color = Color(0xFFD7E6DF),
                                    modifier = Modifier.padding(bottom = 5.dp)
                                )
                            }
                        }

                        Text(
                            text = "Añade este widget navideño directamente a la pantalla de inicio de tu teléfono.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD7E6DF)
                        )

                        Button(
                            onClick = {
                                ChristmasCountdownWidgetProvider.refreshAllWidgets(context)
                                val requested = ChristmasCountdownWidgetProvider.requestPinWidget(context)
                                if (!requested) {
                                    onShowMessage(
                                        "Mantén pulsada la pantalla de inicio > Widgets > Navidad Buzzer para añadirlo."
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("pin_countdown_widget_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Widgets, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Fijar widget en pantalla de inicio")
                        }
                    }
                }
            }

            // 2. Streaming Music Services Integration
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Listas de Streaming Navideñas",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    FilledTonalButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_playlist_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Añadir")
                    }
                }
            }

            items(CURATED_CHRISTMAS_PLAYLISTS) { item ->
                Card(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                        }.onFailure {
                            onShowMessage("No se encontró aplicación para abrir el enlace")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.LibraryMusic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = "Abrir en ${item.platformLabel}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 3. User Saved Playlists in Cloud
            item {
                Text(
                    text = "Tus Listas Guardadas en la Nube",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            when (playlistsState) {
                is CloudUiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
                is CloudUiState.Error -> {
                    item {
                        Text(
                            text = playlistsState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is CloudUiState.Success -> {
                    if (playlistsState.data.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Text(
                                    text = "Guarda tus enlaces favoritos de Spotify, YouTube Music o Apple Music para abrirlos desde cualquier dispositivo.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    } else {
                        items(playlistsState.data, key = { it.id }) { pl ->
                            Card(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(pl.url)))
                                    }.onFailure {
                                        onShowMessage("No se pudo abrir el enlace guardado")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pl.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${pl.platform.uppercase()} · ${pl.description.ifBlank { pl.url }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { onDeletePlaylist(pl.id) }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Delete,
                                            contentDescription = "Eliminar lista",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var url by remember { mutableStateOf("https://open.spotify.com/playlist/") }
        var description by remember { mutableStateOf("") }
        var platform by remember { mutableStateOf("spotify") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Guardar Lista Navideña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nombre de la lista") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "spotify" to "Spotify",
                            "youtube_music" to "YT Music",
                            "apple_music" to "Apple"
                        ).forEach { (id, label) ->
                            FilterChip(
                                selected = platform == id,
                                onClick = { platform = id },
                                label = { Text(label) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text("Enlace HTTPS o spotify:") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción corta") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank() && url.isNotBlank()) {
                        onAddPlaylist(title, platform, url, description)
                        showAddDialog = false
                    }
                }) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
