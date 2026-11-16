package com.example.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.model.ChristmasSong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.OutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

enum class BtTransportType {
    BLE,
    CLASSIC_SPP
}

enum class BtConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED
}

data class DiscoveredEsp32Device(
    val name: String,
    val address: String,
    val rssi: Int,
    val transport: BtTransportType,
    val isPaired: Boolean
)

data class Esp32BluetoothUiState(
    val isBluetoothSupported: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val hasPermissions: Boolean = false,
    val connectionState: BtConnectionState = BtConnectionState.DISCONNECTED,
    val connectedDevice: DiscoveredEsp32Device? = null,
    val discoveredDevices: List<DiscoveredEsp32Device> = emptyList(),
    val statusMessage: String = "Listo para conectar a tu ESP32",
    val lastCommandSent: String = ""
)

class Esp32BluetoothManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val _state = MutableStateFlow(Esp32BluetoothUiState())
    val state: StateFlow<Esp32BluetoothUiState> = _state.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private var writableGattChar: BluetoothGattCharacteristic? = null
    private var bleMtuPayload: Int = 20
    @Volatile
    private var pendingWriteDeferred: CompletableDeferred<Int>? = null
    private var classicSocket: BluetoothSocket? = null
    private var classicOutStream: OutputStream? = null
    private var scanJob: Job? = null
    private var activeScanCallback: ScanCallback? = null
    private val commandChannel = Channel<String>(Channel.UNLIMITED)

    init {
        refreshAvailability()
        scope.launch(Dispatchers.IO) {
            for (cmd in commandChannel) {
                writeCommandBlocking(cmd)
            }
        }
    }

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun refreshAvailability() {
        val adapter = bluetoothAdapter
        val hasPerms = hasRequiredPermissions()
        val enabled = adapter?.isEnabled == true
        val pairedList = if (hasPerms && adapter != null && enabled) {
            runCatching {
                adapter.bondedDevices.orEmpty().map { dev ->
                    DiscoveredEsp32Device(
                        name = dev.name ?: "ESP32 Emparejado",
                        address = dev.address,
                        rssi = -50,
                        transport = if (dev.type == BluetoothDevice.DEVICE_TYPE_LE) {
                            BtTransportType.BLE
                        } else {
                            BtTransportType.CLASSIC_SPP
                        },
                        isPaired = true
                    )
                }
            }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        _state.update { current ->
            val merged = (pairedList + current.discoveredDevices)
                .distinctBy { it.address }
            current.copy(
                isBluetoothSupported = adapter != null,
                isBluetoothEnabled = enabled,
                hasPermissions = hasPerms,
                discoveredDevices = merged,
                statusMessage = when {
                    adapter == null -> "Este dispositivo no dispone de adaptador Bluetooth"
                    !hasPerms -> "Concede permisos de Bluetooth para buscar tu ESP32"
                    !enabled -> "Activa el Bluetooth en tu móvil para conectar el ESP32"
                    current.connectionState == BtConnectionState.CONNECTED ->
                        "Conectado a ${current.connectedDevice?.name ?: "ESP32"}"
                    else -> "Listo para escanear dispositivos ESP32"
                }
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        refreshAvailability()
        val adapter = bluetoothAdapter
        if (adapter == null) {
            _state.update { it.copy(statusMessage = "Hardware Bluetooth no disponible en este equipo") }
            return
        }
        if (!hasRequiredPermissions()) {
            _state.update { it.copy(statusMessage = "Faltan permisos de Bluetooth") }
            return
        }
        if (!adapter.isEnabled) {
            _state.update { it.copy(statusMessage = "Activa el Bluetooth para iniciar el escaneo") }
            return
        }

        stopScan()
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _state.update { it.copy(statusMessage = "Escáner BLE no disponible; mostrando dispositivos emparejados") }
            return
        }

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val dev = result.device ?: return
                val devName = dev.name ?: result.scanRecord?.deviceName ?: return
                val entry = DiscoveredEsp32Device(
                    name = devName,
                    address = dev.address,
                    rssi = result.rssi,
                    transport = BtTransportType.BLE,
                    isPaired = dev.bondState == BluetoothDevice.BOND_BONDED
                )
                _state.update { current ->
                    val updated = (listOf(entry) + current.discoveredDevices.filterNot { it.address == entry.address })
                        .sortedByDescending { it.name.contains("ESP32", ignoreCase = true) || it.name.contains("Navidad", ignoreCase = true) }
                    current.copy(discoveredDevices = updated)
                }
            }

            override fun onScanFailed(errorCode: Int) {
                _state.update {
                    it.copy(
                        connectionState = BtConnectionState.DISCONNECTED,
                        statusMessage = "Error al escanear BLE (código $errorCode)"
                    )
                }
            }
        }

        activeScanCallback = callback
        _state.update {
            it.copy(
                connectionState = BtConnectionState.SCANNING,
                statusMessage = "Buscando ESP32 navideños cercanos..."
            )
        }

        runCatching {
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()
            scanner.startScan(null, settings, callback)
        }.onFailure { err ->
            Log.w(TAG, "BLE scan failed", err)
            _state.update {
                it.copy(
                    connectionState = BtConnectionState.DISCONNECTED,
                    statusMessage = "No se pudo iniciar el escaneo: ${err.localizedMessage}"
                )
            }
            return
        }

        scanJob = scope.launch {
            delay(10_000L)
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanJob?.cancel()
        scanJob = null
        val cb = activeScanCallback
        activeScanCallback = null
        if (cb != null && hasRequiredPermissions()) {
            runCatching { bluetoothAdapter?.bluetoothLeScanner?.stopScan(cb) }
        }
        _state.update { current ->
            if (current.connectionState == BtConnectionState.SCANNING) {
                current.copy(
                    connectionState = BtConnectionState.DISCONNECTED,
                    statusMessage = if (current.discoveredDevices.isEmpty()) {
                        "No se encontraron ESP32 cercanos. Verifica que esté encendido."
                    } else {
                        "Escaneo finalizado (${current.discoveredDevices.size} encontrados)"
                    }
                )
            } else {
                current
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceInfo: DiscoveredEsp32Device) {
        if (!hasRequiredPermissions()) return
        val adapter = bluetoothAdapter ?: return
        stopScan()
        disconnect()

        _state.update {
            it.copy(
                connectionState = BtConnectionState.CONNECTING,
                statusMessage = "Conectando con ${deviceInfo.name}..."
            )
        }

        val btDevice = runCatching { adapter.getRemoteDevice(deviceInfo.address) }.getOrNull()
        if (btDevice == null) {
            _state.update {
                it.copy(
                    connectionState = BtConnectionState.DISCONNECTED,
                    statusMessage = "Dirección Bluetooth inválida"
                )
            }
            return
        }

        if (deviceInfo.transport == BtTransportType.CLASSIC_SPP) {
            connectClassicSpp(btDevice, deviceInfo)
        } else {
            connectBleGatt(btDevice, deviceInfo)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectClassicSpp(btDevice: BluetoothDevice, deviceInfo: DiscoveredEsp32Device) {
        scope.launch(Dispatchers.IO) {
            runCatching {
                val socket = btDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                bluetoothAdapter?.cancelDiscovery()
                socket.connect()
                classicSocket = socket
                classicOutStream = socket.outputStream
                _state.update {
                    it.copy(
                        connectionState = BtConnectionState.CONNECTED,
                        connectedDevice = deviceInfo,
                        statusMessage = "Conectado por SPP a ${deviceInfo.name}"
                    )
                }
            }.onFailure { err ->
                Log.w(TAG, "SPP connection failed", err)
                _state.update {
                    it.copy(
                        connectionState = BtConnectionState.DISCONNECTED,
                        connectedDevice = null,
                        statusMessage = "Fallo al conectar SPP: ${err.localizedMessage ?: "Tiempo agotado"}"
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectBleGatt(btDevice: BluetoothDevice, deviceInfo: DiscoveredEsp32Device) {
        bleMtuPayload = 20
        val servicesDiscoveryStarted = AtomicBoolean(false)

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    runCatching {
                        gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    }
                    _state.update {
                        it.copy(statusMessage = "Negociando MTU y descubriendo ESP32...")
                    }
                    val mtuRequested = runCatching { gatt.requestMtu(247) }.getOrDefault(false)
                    if (!mtuRequested && servicesDiscoveryStarted.compareAndSet(false, true)) {
                        gatt.discoverServices()
                    } else {
                        scope.launch {
                            delay(650L)
                            if (servicesDiscoveryStarted.compareAndSet(false, true)) {
                                runCatching { gatt.discoverServices() }
                            }
                        }
                    }
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    writableGattChar = null
                    bleMtuPayload = 20
                    pendingWriteDeferred?.complete(BluetoothGatt.GATT_FAILURE)
                    pendingWriteDeferred = null
                    _state.update {
                        it.copy(
                            connectionState = BtConnectionState.DISCONNECTED,
                            connectedDevice = null,
                            statusMessage = "ESP32 desconectado"
                        )
                    }
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS && mtu > 23) {
                    bleMtuPayload = (mtu - 3).coerceIn(20, 244)
                }
                if (servicesDiscoveryStarted.compareAndSet(false, true)) {
                    gatt.discoverServices()
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                pendingWriteDeferred?.complete(status)
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    disconnect()
                    return
                }
                val nusService = gatt.getService(NUS_SERVICE_UUID)
                val nusRx = nusService?.getCharacteristic(NUS_RX_CHAR_UUID)
                val fallbackChar = nusRx ?: gatt.services
                    .flatMap { it.characteristics }
                    .firstOrNull { char ->
                        (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0 ||
                            (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
                    }

                writableGattChar = fallbackChar
                if (fallbackChar != null) {
                    _state.update {
                        it.copy(
                            connectionState = BtConnectionState.CONNECTED,
                            connectedDevice = deviceInfo,
                            statusMessage = "Conectado por BLE a ${deviceInfo.name}"
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            connectionState = BtConnectionState.DISCONNECTED,
                            connectedDevice = null,
                            statusMessage = "El dispositivo no expone característica GATT de escritura"
                        )
                    }
                    gatt.disconnect()
                }
            }
        }

        activeGatt = btDevice.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        runCatching {
            classicOutStream?.close()
            classicSocket?.close()
        }
        classicOutStream = null
        classicSocket = null

        runCatching {
            activeGatt?.disconnect()
            activeGatt?.close()
        }
        activeGatt = null
        writableGattChar = null

        _state.update {
            it.copy(
                connectionState = BtConnectionState.DISCONNECTED,
                connectedDevice = null,
                statusMessage = "Desconectado del ESP32"
            )
        }
    }

    fun sendPlaySong(song: ChristmasSong, bpm: Int, volume: Int) {
        val compactNotes = song.notes.take(MAX_SEQUENCED_NOTES)
            .joinToString(",") { "${it.frequencyHz}:${it.durationMs}" }
        sendCommand("PLAY:${song.defaultBpm}|$bpm|$volume|$compactNotes\n")
    }

    fun sendLiveNote(hz: Int, durationMs: Int, volume: Int) {
        sendCommand("TONE:$hz:$durationMs:$volume\n")
    }

    fun sendTempo(bpm: Int) {
        sendCommand("TEMPO:$bpm\n")
    }

    fun sendVolume(volume: Int) {
        sendCommand("VOL:$volume\n")
    }

    fun sendLights(enabled: Boolean, pattern: String) {
        val flag = if (enabled) 1 else 0
        sendCommand("LIGHTS:$flag:$pattern\n")
    }

    fun sendAutoOffTimer(minutes: Int) {
        sendCommand("TIMER:$minutes\n")
    }

    fun sendLoopSong(enabled: Boolean) {
        sendCommand("LOOP:${if (enabled) 1 else 0}\n")
    }

    fun sendStop() {
        sendCommand("STOP\n")
    }

    private fun sendCommand(command: String) {
        _state.update { it.copy(lastCommandSent = command.trim()) }
        if (_state.value.connectionState != BtConnectionState.CONNECTED) return
        commandChannel.trySend(command)
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private suspend fun writeCommandBlocking(command: String) {
        if (_state.value.connectionState != BtConnectionState.CONNECTED) return
        val bytes = command.toByteArray(Charsets.UTF_8)

        val sppStream = classicOutStream
        if (sppStream != null) {
            runCatching {
                sppStream.write(bytes)
                sppStream.flush()
            }.onFailure { err ->
                Log.w(TAG, "SPP write failed", err)
            }
            return
        }

        val gatt = activeGatt ?: return
        val char = writableGattChar ?: return
        if (!hasRequiredPermissions()) return

        val chunkSize = bleMtuPayload.coerceIn(20, 244)
        val writeType = if ((char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        } else {
            BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        }

        var offset = 0
        while (offset < bytes.size) {
            if (_state.value.connectionState != BtConnectionState.CONNECTED) return
            val end = minOf(offset + chunkSize, bytes.size)
            val chunk = bytes.copyOfRange(offset, end)
            val deferred = CompletableDeferred<Int>()
            pendingWriteDeferred = deferred

            val started = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(char, chunk, writeType) == BluetoothGatt.GATT_SUCCESS
                } else {
                    char.value = chunk
                    char.writeType = writeType
                    gatt.writeCharacteristic(char)
                }
            }.getOrDefault(false)

            if (started) {
                withTimeoutOrNull(800L) { deferred.await() }
            } else {
                pendingWriteDeferred = null
                delay(25L)
            }
            offset = end
        }
    }

    companion object {
        private const val TAG = "Esp32BtManager"
        const val MAX_SEQUENCED_NOTES = 180
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        val NUS_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val NUS_RX_CHAR_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")

        const val ESP32_ARDUINO_SKETCH = """// ESP32 Buzzer Navideno - BLE UART (GPIO 25)
// Compatible con ESP32 Arduino Core 2.x y 3.x
// Conexion: S -> GPIO 25 | Vcc (+) -> 5V Arduino | GND (-) -> GND comun (Arduino + ESP32)

#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>

#define SERVICE_UUID "6E400001-B5A3-F393-E0A9-E50E24DCCA9E"
#define CHAR_RX_UUID "6E400002-B5A3-F393-E0A9-E50E24DCCA9E"

const int BUZZER_PIN = 25;  // Pin 'S' del modulo buzzer
const int LED_PIN    = 2;   // LED integrado del ESP32
const int LEDC_CH    = 0;   // Canal PWM 0

struct Note {
  uint16_t hz;
  uint16_t durMs;
};

const int MAX_NOTES = 200;
Note songNotes[MAX_NOTES];
int songCount = 0;
int songIndex = 0;
bool songPlaying = false;
bool noteGapActive = false;
bool loopSong = false;

int baseBpm = 120;
int currentBpm = 120;
int volumePercent = 80;
uint16_t activeHz = 0;

unsigned long notePhaseEndMs = 0;
unsigned long nextGapDurationMs = 0;
bool singleToneActive = false;
unsigned long singleToneEndMs = 0;

bool lightsEnabled = true;
String lightsPattern = "twinkle";
unsigned long lastLedMs = 0;
bool ledState = false;
unsigned long autoOffDeadlineMs = 0;

portMUX_TYPE rxMux = portMUX_INITIALIZER_UNLOCKED;
String rxBuffer = "";

void setBuzzerTone(uint16_t hz, int vol) {
  activeHz = hz;
  if (hz == 0 || vol <= 0) {
#if defined(ESP_ARDUINO_VERSION_MAJOR) && ESP_ARDUINO_VERSION_MAJOR >= 3
    ledcWriteTone(BUZZER_PIN, 0);
    ledcWrite(BUZZER_PIN, 0);
#else
    ledcWriteTone(LEDC_CH, 0);
    ledcWrite(LEDC_CH, 0);
#endif
    return;
  }

  int duty = map(constrain(vol, 0, 100), 1, 100, 12, 128);
#if defined(ESP_ARDUINO_VERSION_MAJOR) && ESP_ARDUINO_VERSION_MAJOR >= 3
  ledcWriteTone(BUZZER_PIN, hz);
  ledcWrite(BUZZER_PIN, duty);
#else
  ledcWriteTone(LEDC_CH, hz);
  ledcWrite(LEDC_CH, duty);
#endif
}

void stopAllAudio() {
  songPlaying = false;
  singleToneActive = false;
  noteGapActive = false;
  setBuzzerTone(0, 0);
}

void startSongNote(int idx) {
  if (idx >= songCount) {
    if (loopSong && songCount > 0) {
      idx = 0;
    } else {
      stopAllAudio();
      return;
    }
  }
  songIndex = idx;
  noteGapActive = false;
  unsigned long scaled = (unsigned long)songNotes[idx].durMs * (unsigned long)baseBpm / (unsigned long)max(40, currentBpm);
  scaled = constrain(scaled, 30UL, 8000UL);
  unsigned long playMs = max(22UL, (scaled * 88UL) / 100UL);
  nextGapDurationMs = max(10UL, scaled - playMs);
  setBuzzerTone(songNotes[idx].hz, volumePercent);
  notePhaseEndMs = millis() + playMs;
}

void parsePlayCommand(const String& payload) {
  int p1 = payload.indexOf('|');
  int p2 = payload.indexOf('|', p1 + 1);
  int p3 = payload.indexOf('|', p2 + 1);
  if (p1 < 0 || p2 < 0 || p3 < 0) return;

  baseBpm = constrain(payload.substring(0, p1).toInt(), 40, 240);
  currentBpm = constrain(payload.substring(p1 + 1, p2).toInt(), 40, 240);
  volumePercent = constrain(payload.substring(p2 + 1, p3).toInt(), 0, 100);

  songCount = 0;
  int start = p3 + 1;
  while (start < (int)payload.length() && songCount < MAX_NOTES) {
    int comma = payload.indexOf(',', start);
    String token = (comma < 0) ? payload.substring(start) : payload.substring(start, comma);
    int colon = token.indexOf(':');
    if (colon > 0) {
      songNotes[songCount].hz = (uint16_t)constrain(token.substring(0, colon).toInt(), 0, 8000);
      songNotes[songCount].durMs = (uint16_t)constrain(token.substring(colon + 1).toInt(), 20, 8000);
      songCount++;
    }
    if (comma < 0) break;
    start = comma + 1;
  }

  Serial.print("Cancion recibida. Notas: ");
  Serial.println(songCount);
  singleToneActive = false;
  if (songCount > 0) {
    songPlaying = true;
    startSongNote(0);
  }
}

void handleCommand(String cmd) {
  cmd.trim();
  if (cmd.length() == 0) return;
  Serial.print("RX BLE: ");
  Serial.println(cmd.substring(0, min((int)cmd.length(), 60)));

  if (cmd.startsWith("PLAY:")) {
    parsePlayCommand(cmd.substring(5));
  } else if (cmd.startsWith("TONE:")) {
    int c1 = cmd.indexOf(':', 5);
    int c2 = cmd.indexOf(':', c1 + 1);
    if (c1 > 0 && c2 > c1) {
      int hz = cmd.substring(5, c1).toInt();
      int ms = constrain(cmd.substring(c1 + 1, c2).toInt(), 25, 8000);
      volumePercent = constrain(cmd.substring(c2 + 1).toInt(), 0, 100);
      songPlaying = false;
      singleToneActive = true;
      setBuzzerTone((uint16_t)max(0, hz), volumePercent);
      singleToneEndMs = millis() + (unsigned long)((ms * 90L) / 100L);
    }
  } else if (cmd.startsWith("TEMPO:")) {
    currentBpm = constrain(cmd.substring(6).toInt(), 40, 240);
  } else if (cmd.startsWith("VOL:")) {
    volumePercent = constrain(cmd.substring(4).toInt(), 0, 100);
    if ((songPlaying && !noteGapActive) || singleToneActive) {
      setBuzzerTone(activeHz, volumePercent);
    }
  } else if (cmd == "STOP") {
    stopAllAudio();
  } else if (cmd.startsWith("LOOP:")) {
    loopSong = (cmd.charAt(5) == '1');
  } else if (cmd.startsWith("LIGHTS:")) {
    lightsEnabled = (cmd.charAt(7) == '1');
    if (cmd.length() > 9) lightsPattern = cmd.substring(9);
    if (!lightsEnabled) digitalWrite(LED_PIN, LOW);
  } else if (cmd.startsWith("TIMER:")) {
    long mins = cmd.substring(6).toInt();
    autoOffDeadlineMs = (mins > 0) ? (millis() + (unsigned long)mins * 60000UL) : 0;
  }
}

class ServerCB : public BLEServerCallbacks {
  void onConnect(BLEServer* s) override {
    Serial.println("App conectada por BLE!");
  }
  void onDisconnect(BLEServer* s) override {
    Serial.println("App desconectada, re-anunciando BLE...");
    BLEDevice::startAdvertising();
  }
};

class RxCB : public BLECharacteristicCallbacks {
  void onWrite(BLECharacteristic* pChar) override {
    uint8_t* data = pChar->getData();
    size_t len = pChar->getLength();
    if (!data || len == 0) return;
    portENTER_CRITICAL(&rxMux);
    for (size_t i = 0; i < len; i++) {
      rxBuffer += (char)data[i];
    }
    if (rxBuffer.length() > 4096) rxBuffer = "";
    portEXIT_CRITICAL(&rxMux);
  }
};

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, LOW);

#if defined(ESP_ARDUINO_VERSION_MAJOR) && ESP_ARDUINO_VERSION_MAJOR >= 3
  ledcAttach(BUZZER_PIN, 2000, 8);
#else
  ledcAttachPin(BUZZER_PIN, LEDC_CH);
#endif

  // Pitido breve de verificacion de hardware en GPIO 25
  setBuzzerTone(1000, 100);
  delay(120);
  setBuzzerTone(0, 0);

  BLEDevice::init("ESP32_Navidad_Buzzer");
  BLEDevice::setMTU(256);

  BLEServer* server = BLEDevice::createServer();
  server->setCallbacks(new ServerCB());

  BLEService* service = server->createService(SERVICE_UUID);
  BLECharacteristic* rxChar = service->createCharacteristic(
    CHAR_RX_UUID,
    BLECharacteristic::PROPERTY_WRITE | BLECharacteristic::PROPERTY_WRITE_NO_RESPONSE
  );
  rxChar->setCallbacks(new RxCB());
  service->start();

  BLEAdvertising* adv = BLEDevice::getAdvertising();
  adv->addServiceUUID(SERVICE_UUID);
  adv->setScanResponse(true);
  BLEDevice::startAdvertising();
  Serial.println("ESP32_Navidad_Buzzer listo y anunciando por BLE.");
}

void loop() {
  unsigned long now = millis();

  String readyLine = "";
  while (true) {
    int nl = -1;
    portENTER_CRITICAL(&rxMux);
    nl = rxBuffer.indexOf('\n');
    if (nl >= 0) {
      readyLine = rxBuffer.substring(0, nl);
      rxBuffer.remove(0, nl + 1);
    }
    portEXIT_CRITICAL(&rxMux);
    if (nl < 0) break;
    handleCommand(readyLine);
    readyLine = "";
  }

  if (songPlaying && (long)(now - notePhaseEndMs) >= 0) {
    if (!noteGapActive) {
      setBuzzerTone(0, 0);
      noteGapActive = true;
      notePhaseEndMs = now + nextGapDurationMs;
    } else {
      startSongNote(songIndex + 1);
    }
  } else if (singleToneActive && (long)(now - singleToneEndMs) >= 0) {
    singleToneActive = false;
    setBuzzerTone(0, 0);
  }

  if (autoOffDeadlineMs > 0 && (long)(now - autoOffDeadlineMs) >= 0) {
    autoOffDeadlineMs = 0;
    stopAllAudio();
    lightsEnabled = false;
    digitalWrite(LED_PIN, LOW);
  }

  if (lightsEnabled) {
    unsigned long interval = (lightsPattern == "party") ? 140 :
                             (lightsPattern == "chase") ? 240 :
                             (lightsPattern == "warm_glow") ? 900 : 450;
    if (now - lastLedMs >= interval) {
      lastLedMs = now;
      ledState = (lightsPattern == "warm_glow") ? true : !ledState;
      digitalWrite(LED_PIN, ledState ? HIGH : LOW);
    }
  }
}"""
    }
}
