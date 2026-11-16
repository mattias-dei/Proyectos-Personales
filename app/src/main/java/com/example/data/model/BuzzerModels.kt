package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

data class MelodyNote(
    val frequencyHz: Int,
    val durationMs: Int,
    val label: String = ""
)

data class ChristmasSong(
    val id: String,
    val title: String,
    val subtitle: String,
    val defaultBpm: Int,
    val notes: List<MelodyNote>,
    val isCustom: Boolean = false,
    val sourceFileName: String = ""
) {
    val totalDurationMs: Long
        get() = notes.sumOf { it.durationMs.toLong() }
}

enum class PlaybackLoopMode(val label: String) {
    ONCE("Una vez"),
    REPEAT_SONG("Repetir canción"),
    LOOP_ALL("Todas en bucle")
}

@IgnoreExtraProperties
data class UserSettingsDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val tempoBpm: Long = 120L,
    val volumePercent: Long = 80L,
    val lightsEnabled: Boolean = true,
    val lightsPattern: String = "twinkle",
    val festiveSoundsEnabled: Boolean = true,
    val darkWinterMode: Boolean = true,
    val autoOffMinutes: Long = 30L,
    val notifEnabled: Boolean = true,
    val notifTitle: String = "Hora de villancicos",
    val notifMessage: String = "Enciende tu ESP32 y disfruta de la magia navideña",
    val notifHour: Long = 20L,
    val notifMinute: Long = 0L,
    val preferredDeviceName: String = "",
    val preferredDeviceAddress: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CustomMelodyDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val artistOrSource: String = "",
    val defaultBpm: Long = 120L,
    val frequencies: List<Long> = emptyList(),
    val durationsMs: List<Long> = emptyList(),
    val sourceFileName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toChristmasSong(): ChristmasSong {
        val count = minOf(frequencies.size, durationsMs.size)
        val melodyNotes = (0 until count).map { idx ->
            val hz = frequencies[idx].toInt().coerceIn(0, 8000)
            val ms = durationsMs[idx].toInt().coerceIn(20, 10000)
            MelodyNote(frequencyHz = hz, durationMs = ms)
        }
        return ChristmasSong(
            id = id,
            title = title,
            subtitle = artistOrSource.ifBlank { "Melodía importada" },
            defaultBpm = defaultBpm.toInt().coerceIn(40, 240),
            notes = melodyNotes,
            isCustom = true,
            sourceFileName = sourceFileName
        )
    }
}

@IgnoreExtraProperties
data class ThemedPlaylistDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val platform: String = "spotify",
    val url: String = "",
    val description: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
