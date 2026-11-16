package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserSettingsDoc
import com.example.notifications.FestiveNotificationManager
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun SettingsAndSyncScreen(
    userEmail: String?,
    settings: UserSettingsDoc,
    onToggleDarkWinterMode: () -> Unit,
    onToggleFestiveSounds: () -> Unit,
    onSaveNotifications: (enabled: Boolean, title: String, message: String, hour: Int, minute: Int) -> Unit,
    onSendTestNotification: (title: String, message: String) -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var notifEnabled by remember(settings.notifEnabled) { mutableStateOf(settings.notifEnabled) }
    var notifTitle by remember(settings.notifTitle) { mutableStateOf(settings.notifTitle) }
    var notifMessage by remember(settings.notifMessage) { mutableStateOf(settings.notifMessage) }
    var notifHour by remember(settings.notifHour) { mutableIntStateOf(settings.notifHour.toInt()) }
    var notifMinute by remember(settings.notifMinute) { mutableIntStateOf(settings.notifMinute.toInt()) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSendTestNotification(notifTitle, notifMessage)
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
            // 1. Winter Nights Dark Mode & Festive Sounds
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
                        Text("Ambiente de Invierno y Sonidos", style = MaterialTheme.typography.titleMedium)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.DarkMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Modo Oscuro Noches de Invierno", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "Paleta nocturna cálida para cuidar tu vista",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.darkWinterMode,
                                onCheckedChange = { onToggleDarkWinterMode() },
                                modifier = Modifier.testTag("dark_winter_mode_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.SpatialAudio,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Sonidos Festivos de Interfaz", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        text = "Campanillas navideñas al conectar y cambiar luces",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = settings.festiveSoundsEnabled,
                                onCheckedChange = { onToggleFestiveSounds() },
                                modifier = Modifier.testTag("festive_sounds_switch")
                            )
                        }
                    }
                }
            }

            // 2. Customizable Push / Local Notifications
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Notificaciones Personalizables", style = MaterialTheme.typography.titleMedium)
                            }
                            Switch(
                                checked = notifEnabled,
                                onCheckedChange = { notifEnabled = it },
                                modifier = Modifier.testTag("notifications_switch")
                            )
                        }

                        OutlinedTextField(
                            value = notifTitle,
                            onValueChange = { notifTitle = it.take(80) },
                            label = { Text("Título de la alerta") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = notifMessage,
                            onValueChange = { notifMessage = it.take(200) },
                            label = { Text("Mensaje navideño personalizado") },
                            maxLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hora del recordatorio diario", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = String.format("%02d:%02d", notifHour, notifMinute),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }

                        Slider(
                            value = notifHour.toFloat(),
                            onValueChange = { notifHour = it.toInt() },
                            valueRange = 0f..23f,
                            steps = 22
                        )

                        Slider(
                            value = notifMinute.toFloat(),
                            onValueChange = { notifMinute = it.toInt() },
                            valueRange = 0f..55f,
                            steps = 10
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !FestiveNotificationManager.hasNotificationPermission(context)
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    onSaveNotifications(notifEnabled, notifTitle, notifMessage, notifHour, notifMinute)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("save_notifications_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Guardar alerta")
                            }

                            FilledTonalButton(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        !FestiveNotificationManager.hasNotificationPermission(context)
                                    ) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        onSendTestNotification(notifTitle, notifMessage)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("test_notification_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Probar ahora")
                            }
                        }
                    }
                }
            }

            // 3. Cloud Sync Account & Sign Out
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.CloudDone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Sincronización en la Nube Activa", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = userEmail ?: "Cuenta de Google conectada",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (settings.preferredDeviceName.isNotBlank()) {
                            Text(
                                text = "Último ESP32 guardado: ${settings.preferredDeviceName} (${settings.preferredDeviceAddress})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = onSignOut,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("sign_out_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cerrar sesión")
                        }
                    }
                }
            }
        }
    }
}
