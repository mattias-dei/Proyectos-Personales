package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.AudioPlaybackTelemetry
import com.example.data.audio.BuzzerAudioEngine
import com.example.data.audio.FestiveSoundType
import com.example.data.bluetooth.BtConnectionState
import com.example.data.bluetooth.DiscoveredEsp32Device
import com.example.data.bluetooth.Esp32BluetoothManager
import com.example.data.bluetooth.Esp32BluetoothUiState
import com.example.data.model.ChristmasSong
import com.example.data.model.CustomMelodyDoc
import com.example.data.model.PlaybackLoopMode
import com.example.data.model.ThemedPlaylistDoc
import com.example.data.model.UserSettingsDoc
import com.example.data.music.ChristmasSongsCatalog
import com.example.data.music.MelodyFileParser
import com.example.data.preferences.ThemePreferenceStore
import com.example.data.repository.ChristmasCloudRepository
import com.example.notifications.FestiveNotificationManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

class ChristmasBuzzerViewModel(
    private val repository: ChristmasCloudRepository,
    private val bluetoothManager: Esp32BluetoothManager,
    private val audioEngine: BuzzerAudioEngine,
    private val themeStore: ThemePreferenceStore,
    private val appContext: Context,
    private val currentUserId: String
) : ViewModel() {

    val bluetoothState: StateFlow<Esp32BluetoothUiState> = bluetoothManager.state
    val playbackTelemetry: StateFlow<AudioPlaybackTelemetry> = audioEngine.telemetry

    val settingsState: StateFlow<CloudUiState<UserSettingsDoc>> = repository.observeUserSettings(currentUserId)
        .map<UserSettingsDoc?, CloudUiState<UserSettingsDoc>> { doc ->
            val resolved = doc ?: UserSettingsDoc(userId = currentUserId)
            audioEngine.setTempoBpm(resolved.tempoBpm.toInt())
            audioEngine.setVolumePercent(resolved.volumePercent.toInt())
            viewModelScope.launch { themeStore.setDarkWinterMode(resolved.darkWinterMode) }
            CloudUiState.Success(resolved)
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Error observing settings", root)
            emit(CloudUiState.Error(root.message ?: "Error al sincronizar configuración"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = CloudUiState.Loading
        )

    val customMelodiesState: StateFlow<CloudUiState<List<CustomMelodyDoc>>> =
        repository.observeCustomMelodies(currentUserId)
            .map<List<CustomMelodyDoc>, CloudUiState<List<CustomMelodyDoc>>> { list ->
                CloudUiState.Success(list)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing custom melodies", root)
                emit(CloudUiState.Error(root.message ?: "Error al cargar melodías de la nube"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    val playlistsState: StateFlow<CloudUiState<List<ThemedPlaylistDoc>>> =
        repository.observePlaylists(currentUserId)
            .map<List<ThemedPlaylistDoc>, CloudUiState<List<ThemedPlaylistDoc>>> { list ->
                CloudUiState.Success(list)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w(TAG, "Error observing playlists", root)
                emit(CloudUiState.Error(root.message ?: "Error al cargar playlists"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = CloudUiState.Loading
            )

    val loopMode: StateFlow<PlaybackLoopMode> = themeStore.loopModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PlaybackLoopMode.LOOP_ALL
        )

    private val _selectedSong = MutableStateFlow(ChristmasSongsCatalog.songs.first())
    val selectedSong: StateFlow<ChristmasSong> = _selectedSong.asStateFlow()

    private val _localSettings = MutableStateFlow(UserSettingsDoc(userId = currentUserId))
    val localSettings: StateFlow<UserSettingsDoc> = _localSettings.asStateFlow()

    private val _remainingAutoOffSeconds = MutableStateFlow(0L)
    val remainingAutoOffSeconds: StateFlow<Long> = _remainingAutoOffSeconds.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private var saveSettingsJob: Job? = null
    private var autoOffJob: Job? = null

    init {
        viewModelScope.launch {
            settingsState.collect { state ->
                if (state is CloudUiState.Success) {
                    _localSettings.value = state.data
                }
            }
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun selectSong(song: ChristmasSong) {
        _selectedSong.value = song
    }

    fun setLoopMode(mode: PlaybackLoopMode) {
        viewModelScope.launch {
            themeStore.setLoopMode(mode)
        }
        if (bluetoothState.value.connectionState == BtConnectionState.CONNECTED) {
            bluetoothManager.sendLoopSong(mode == PlaybackLoopMode.REPEAT_SONG)
        }
    }

    private fun allAvailableSongs(): List<ChristmasSong> {
        val custom = (customMelodiesState.value as? CloudUiState.Success)?.data
            ?.map { it.toChristmasSong() }
            .orEmpty()
        return ChristmasSongsCatalog.songs + custom
    }

    fun nextSong() {
        val all = allAvailableSongs()
        if (all.isEmpty()) return
        val currentIdx = all.indexOfFirst { it.id == _selectedSong.value.id }
        val next = all[(currentIdx + 1) % all.size]
        playSong(next)
    }

    fun previousSong() {
        val all = allAvailableSongs()
        if (all.isEmpty()) return
        val currentIdx = all.indexOfFirst { it.id == _selectedSong.value.id }
        val prevIdx = if (currentIdx <= 0) all.lastIndex else currentIdx - 1
        playSong(all[prevIdx])
    }

    fun playSong(song: ChristmasSong = _selectedSong.value) {
        playSongInternal(song = song, sendPlayToEsp = true)
    }

    private fun playSongInternal(song: ChristmasSong, sendPlayToEsp: Boolean) {
        _selectedSong.value = song
        val current = _localSettings.value
        val bpm = current.tempoBpm.toInt().coerceIn(40, 240)
        val vol = current.volumePercent.toInt().coerceIn(0, 100)
        val isEsp32Connected = bluetoothState.value.connectionState == BtConnectionState.CONNECTED
        val currentLoopMode = loopMode.value

        if (current.festiveSoundsEnabled && !isEsp32Connected && sendPlayToEsp) {
            audioEngine.playFestiveChime(FestiveSoundType.SPARKLE_LIGHT)
        }

        val useHardwareSequencer = isEsp32Connected &&
            song.notes.size <= Esp32BluetoothManager.MAX_SEQUENCED_NOTES

        audioEngine.setPlayOnPhoneAudio(!isEsp32Connected)

        if (useHardwareSequencer && sendPlayToEsp) {
            bluetoothManager.sendLoopSong(currentLoopMode == PlaybackLoopMode.REPEAT_SONG)
            bluetoothManager.sendPlaySong(song, bpm, vol)
        }

        audioEngine.startPlayback(
            song = song,
            initialBpm = bpm,
            initialVolume = vol,
            onNotePlayed = { hz, ms ->
                if (bluetoothState.value.connectionState == BtConnectionState.CONNECTED && !useHardwareSequencer) {
                    bluetoothManager.sendLiveNote(hz, ms, _localSettings.value.volumePercent.toInt())
                }
            },
            onCompleted = {
                viewModelScope.launch {
                    when (loopMode.value) {
                        PlaybackLoopMode.REPEAT_SONG -> {
                            // If ESP32 is looping in hardware, keep phone visualizer looping without re-triggering PLAY:
                            playSongInternal(song = song, sendPlayToEsp = !useHardwareSequencer)
                        }
                        PlaybackLoopMode.LOOP_ALL -> {
                            nextSong()
                        }
                        PlaybackLoopMode.ONCE -> {
                            if (bluetoothState.value.connectionState == BtConnectionState.CONNECTED && !useHardwareSequencer) {
                                bluetoothManager.sendStop()
                            }
                        }
                    }
                }
            }
        )
    }

    fun stopPlayback() {
        audioEngine.stopPlayback()
        bluetoothManager.sendStop()
    }

    fun updateTempo(bpm: Int) {
        val clamped = bpm.coerceIn(40, 240)
        audioEngine.setTempoBpm(clamped)
        bluetoothManager.sendTempo(clamped)
        updateAndSyncSettings { it.copy(tempoBpm = clamped.toLong()) }
    }

    fun updateVolume(volumePercent: Int) {
        val clamped = volumePercent.coerceIn(0, 100)
        audioEngine.setVolumePercent(clamped)
        bluetoothManager.sendVolume(clamped)
        updateAndSyncSettings { it.copy(volumePercent = clamped.toLong()) }
    }

    fun toggleLights() {
        val next = !_localSettings.value.lightsEnabled
        val pattern = _localSettings.value.lightsPattern
        bluetoothManager.sendLights(next, pattern)
        if (_localSettings.value.festiveSoundsEnabled) {
            audioEngine.playFestiveChime(FestiveSoundType.SPARKLE_LIGHT)
        }
        updateAndSyncSettings(immediate = true) { it.copy(lightsEnabled = next) }
    }

    fun setLightsPattern(pattern: String) {
        bluetoothManager.sendLights(_localSettings.value.lightsEnabled, pattern)
        if (_localSettings.value.festiveSoundsEnabled) {
            audioEngine.playFestiveChime(FestiveSoundType.SPARKLE_LIGHT)
        }
        updateAndSyncSettings(immediate = true) { it.copy(lightsPattern = pattern) }
    }

    fun toggleFestiveSounds() {
        val next = !_localSettings.value.festiveSoundsEnabled
        if (next) {
            audioEngine.playFestiveChime(FestiveSoundType.CHIME_CONNECT)
        }
        updateAndSyncSettings(immediate = true) { it.copy(festiveSoundsEnabled = next) }
    }

    fun toggleDarkWinterMode() {
        val next = !_localSettings.value.darkWinterMode
        viewModelScope.launch { themeStore.setDarkWinterMode(next) }
        updateAndSyncSettings(immediate = true) { it.copy(darkWinterMode = next) }
    }

    fun startAutoOffTimer(minutes: Int) {
        val clamped = minutes.coerceIn(0, 240)
        autoOffJob?.cancel()
        updateAndSyncSettings(immediate = true) { it.copy(autoOffMinutes = clamped.toLong()) }
        bluetoothManager.sendAutoOffTimer(clamped)

        if (clamped == 0) {
            _remainingAutoOffSeconds.value = 0L
            _statusBanner.value = "Temporizador de apagado automático desactivado"
            return
        }

        _remainingAutoOffSeconds.value = clamped * 60L
        _statusBanner.value = "Apagado automático programado en $clamped min"
        autoOffJob = viewModelScope.launch {
            while (isActive && _remainingAutoOffSeconds.value > 0L) {
                delay(1000L)
                _remainingAutoOffSeconds.update { (it - 1L).coerceAtLeast(0L) }
            }
            if (isActive && _remainingAutoOffSeconds.value == 0L) {
                stopPlayback()
                bluetoothManager.sendLights(false, _localSettings.value.lightsPattern)
                if (_localSettings.value.festiveSoundsEnabled) {
                    audioEngine.playFestiveChime(FestiveSoundType.TIMER_COMPLETE)
                }
                FestiveNotificationManager.showAutoOffCompletedNotification(appContext)
                _statusBanner.value = "Apagado automático ejecutado: música y luces detenidas"
            }
        }
    }

    fun cancelAutoOffTimer() {
        autoOffJob?.cancel()
        autoOffJob = null
        _remainingAutoOffSeconds.value = 0L
        bluetoothManager.sendAutoOffTimer(0)
        _statusBanner.value = "Temporizador cancelado"
    }

    fun importMelodyFromUri(uri: Uri) {
        viewModelScope.launch {
            val fileName = resolveFileName(uri) ?: "melodia_externa.txt"
            val fallbackTitle = fileName.substringBeforeLast(".").replace("_", " ").ifBlank {
                "Melodía Navideña"
            }

            val rawText = withContext(Dispatchers.IO) {
                runCatching {
                    appContext.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull()
            }

            if (rawText.isNullOrBlank()) {
                _statusBanner.value = "No se pudo leer el archivo seleccionado"
                return@launch
            }

            val parsed = MelodyFileParser.parse(rawText, fallbackTitle)
            parsed.onSuccess { melody ->
                val res = repository.addCustomMelody(
                    title = melody.title,
                    artistOrSource = "Archivo externo ($fileName)",
                    defaultBpm = melody.defaultBpm,
                    notes = melody.notes,
                    sourceFileName = fileName
                )
                res.onSuccess { newId ->
                    val newSong = ChristmasSong(
                        id = newId,
                        title = melody.title,
                        subtitle = "Archivo externo ($fileName)",
                        defaultBpm = melody.defaultBpm,
                        notes = melody.notes,
                        isCustom = true,
                        sourceFileName = fileName
                    )
                    _selectedSong.value = newSong
                    _statusBanner.value = "¡Melodía '${melody.title}' cargada y sincronizada en la nube!"
                }.onFailure { err ->
                    _statusBanner.value = "Error al guardar en la nube: ${err.localizedMessage}"
                }
            }.onFailure { err ->
                _statusBanner.value = err.message ?: "Error al interpretar el archivo de melodía"
            }
        }
    }

    fun deleteCustomMelody(melodyId: String) {
        viewModelScope.launch {
            repository.deleteCustomMelody(melodyId)
                .onSuccess { _statusBanner.value = "Melodía eliminada de la nube" }
                .onFailure { _statusBanner.value = "No se pudo eliminar: ${it.localizedMessage}" }
        }
    }

    fun addPlaylist(title: String, platform: String, url: String, description: String) {
        viewModelScope.launch {
            repository.addPlaylist(title, platform, url, description)
                .onSuccess { _statusBanner.value = "Playlist guardada y sincronizada en la nube" }
                .onFailure { _statusBanner.value = it.localizedMessage ?: "Error al guardar playlist" }
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
                .onSuccess { _statusBanner.value = "Playlist eliminada" }
                .onFailure { _statusBanner.value = "No se pudo eliminar: ${it.localizedMessage}" }
        }
    }

    fun saveNotificationSettings(
        enabled: Boolean,
        title: String,
        message: String,
        hour: Int,
        minute: Int
    ) {
        val cleanTitle = title.trim().ifBlank { "Hora de villancicos" }.take(80)
        val cleanMsg = message.trim().ifBlank { "¡Enciende tu ESP32 navideño!" }.take(200)
        FestiveNotificationManager.scheduleDailyReminder(
            context = appContext,
            enabled = enabled,
            hour = hour,
            minute = minute,
            title = cleanTitle,
            message = cleanMsg
        )
        updateAndSyncSettings(immediate = true) {
            it.copy(
                notifEnabled = enabled,
                notifTitle = cleanTitle,
                notifMessage = cleanMsg,
                notifHour = hour.toLong(),
                notifMinute = minute.toLong()
            )
        }
        _statusBanner.value = if (enabled) {
            String.format("Notificación programada a las %02d:%02d", hour, minute)
        } else {
            "Notificaciones diarias desactivadas"
        }
    }

    fun sendTestNotification(title: String, message: String) {
        FestiveNotificationManager.showTestNotification(appContext, title, message)
        _statusBanner.value = "Notificación de prueba enviada"
    }

    fun refreshBluetooth() = bluetoothManager.refreshAvailability()
    fun startBluetoothScan() = bluetoothManager.startScan()
    fun stopBluetoothScan() = bluetoothManager.stopScan()

    fun connectEsp32Device(device: DiscoveredEsp32Device) {
        bluetoothManager.connectToDevice(device)
        if (_localSettings.value.festiveSoundsEnabled) {
            audioEngine.playFestiveChime(FestiveSoundType.CHIME_CONNECT)
        }
        updateAndSyncSettings(immediate = true) {
            it.copy(
                preferredDeviceName = device.name.take(64),
                preferredDeviceAddress = device.address.take(64)
            )
        }
    }

    fun disconnectEsp32() = bluetoothManager.disconnect()

    private fun updateAndSyncSettings(
        immediate: Boolean = false,
        transform: (UserSettingsDoc) -> UserSettingsDoc
    ) {
        val updated = transform(_localSettings.value).copy(userId = currentUserId)
        _localSettings.value = updated
        saveSettingsJob?.cancel()
        saveSettingsJob = viewModelScope.launch {
            if (!immediate) delay(450L)
            repository.saveUserSettings(updated)
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        return runCatching {
            appContext.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }.getOrNull()
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.stopPlayback()
        bluetoothManager.stopScan()
    }

    private companion object {
        const val TAG = "ChristmasBuzzerVM"
    }
}
