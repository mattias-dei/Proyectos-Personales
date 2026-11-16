package com.example.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.model.ChristmasSong
import com.example.data.music.ChristmasSongsCatalog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

enum class FestiveSoundType {
    CHIME_CONNECT,
    SPARKLE_LIGHT,
    TIMER_COMPLETE
}

data class AudioPlaybackTelemetry(
    val isPlaying: Boolean = false,
    val activeSong: ChristmasSong? = null,
    val currentNoteIndex: Int = -1,
    val totalNotes: Int = 0,
    val currentFrequencyHz: Int = 0,
    val currentNoteLabel: String = "—",
    val spectrumBars: List<Float> = List(24) { 0.08f }
)

class BuzzerAudioEngine(private val scope: CoroutineScope) {

    private val _telemetry = MutableStateFlow(AudioPlaybackTelemetry())
    val telemetry: StateFlow<AudioPlaybackTelemetry> = _telemetry.asStateFlow()

    @Volatile
    private var liveTempoBpm: Int = 120

    @Volatile
    private var liveVolumePercent: Int = 80

    @Volatile
    private var playOnPhoneAudio: Boolean = true

    private var playbackJob: Job? = null

    fun setTempoBpm(bpm: Int) {
        liveTempoBpm = bpm.coerceIn(40, 240)
    }

    fun setVolumePercent(volume: Int) {
        liveVolumePercent = volume.coerceIn(0, 100)
    }

    fun setPlayOnPhoneAudio(enabled: Boolean) {
        playOnPhoneAudio = enabled
    }

    fun startPlayback(
        song: ChristmasSong,
        initialBpm: Int,
        initialVolume: Int,
        onNotePlayed: (hz: Int, durationMs: Int) -> Unit = { _, _ -> },
        onCompleted: () -> Unit = {}
    ) {
        stopPlayback()
        liveTempoBpm = initialBpm.coerceIn(40, 240)
        liveVolumePercent = initialVolume.coerceIn(0, 100)

        playbackJob = scope.launch(Dispatchers.Default) {
            _telemetry.update {
                it.copy(
                    isPlaying = true,
                    activeSong = song,
                    currentNoteIndex = 0,
                    totalNotes = song.notes.size,
                    currentFrequencyHz = 0,
                    currentNoteLabel = "—"
                )
            }

            val baseBpm = song.defaultBpm.coerceAtLeast(40).toFloat()
            val notes = song.notes

            for (index in notes.indices) {
                if (!isActive) break
                val note = notes[index]
                val tempoScale = baseBpm / liveTempoBpm.coerceIn(40, 240).toFloat()
                val scaledDurationMs = (note.durationMs * tempoScale).toInt().coerceIn(30, 8000)

                onNotePlayed(note.frequencyHz, scaledDurationMs)

                _telemetry.update {
                    it.copy(
                        isPlaying = true,
                        activeSong = song,
                        currentNoteIndex = index,
                        totalNotes = notes.size,
                        currentFrequencyHz = note.frequencyHz,
                        currentNoteLabel = note.label.ifBlank {
                            ChristmasSongsCatalog.noteNameForFrequency(note.frequencyHz)
                        },
                        spectrumBars = computeSpectrumBars(note.frequencyHz, liveVolumePercent, index)
                    )
                }

                if (playOnPhoneAudio && liveVolumePercent > 0 && note.frequencyHz > 0) {
                    playSquareTone(
                        frequencyHz = note.frequencyHz,
                        durationMs = (scaledDurationMs * 0.88f).toInt().coerceAtLeast(25),
                        volumeRatio = liveVolumePercent / 100f
                    )
                    val gapMs = (scaledDurationMs * 0.12f).toLong().coerceAtLeast(10L)
                    delay(gapMs)
                } else {
                    delay(scaledDurationMs.toLong())
                }
            }

            _telemetry.update {
                AudioPlaybackTelemetry(
                    isPlaying = false,
                    activeSong = song,
                    currentNoteIndex = -1,
                    totalNotes = song.notes.size,
                    currentFrequencyHz = 0,
                    currentNoteLabel = "—",
                    spectrumBars = List(24) { 0.08f }
                )
            }
            if (isActive) {
                onCompleted()
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _telemetry.update {
            it.copy(
                isPlaying = false,
                currentFrequencyHz = 0,
                currentNoteLabel = "—",
                spectrumBars = List(24) { 0.08f }
            )
        }
    }

    fun playFestiveChime(type: FestiveSoundType) {
        scope.launch(Dispatchers.Default) {
            val seq = when (type) {
                FestiveSoundType.CHIME_CONNECT -> listOf(523 to 90, 659 to 90, 784 to 140)
                FestiveSoundType.SPARKLE_LIGHT -> listOf(880 to 65, 1047 to 95)
                FestiveSoundType.TIMER_COMPLETE -> listOf(784 to 120, 659 to 120, 523 to 220)
            }
            val vol = (liveVolumePercent / 100f).coerceIn(0.25f, 0.7f)
            for ((hz, ms) in seq) {
                playSquareTone(hz, ms, vol)
                delay(20L)
            }
        }
    }

    private fun playSquareTone(frequencyHz: Int, durationMs: Int, volumeRatio: Float) {
        val sampleRate = 22050
        val totalSamples = ((durationMs / 1000.0) * sampleRate).toInt().coerceAtLeast(1)
        val buffer = ShortArray(totalSamples)
        val amplitude = (Short.MAX_VALUE * 0.35f * volumeRatio.coerceIn(0f, 1f)).toInt()
        val fadeSamples = minOf(totalSamples / 6, 120)

        for (i in 0 until totalSamples) {
            val phase = 2.0 * PI * frequencyHz * i / sampleRate
            // Softened passive buzzer wave (fundamental + 3rd harmonic for crisp piezo timbre)
            val wave = (sin(phase) + 0.33 * sin(3.0 * phase) + 0.15 * sin(5.0 * phase)) * 0.75
            val env = when {
                fadeSamples > 0 && i < fadeSamples -> i.toFloat() / fadeSamples
                fadeSamples > 0 && i > totalSamples - fadeSamples -> (totalSamples - i).toFloat() / fadeSamples
                else -> 1f
            }
            buffer[i] = (wave * amplitude * env).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }

        runCatching {
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(buffer.size * 2)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(maxOf(minBuf, buffer.size * 2))
                .build()

            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(durationMs.toLong())
            track.stop()
            track.release()
        }
    }

    private fun computeSpectrumBars(hz: Int, volumePercent: Int, step: Int): List<Float> {
        if (hz <= 0 || volumePercent <= 0) return List(24) { 0.08f }
        val normalizedCenter = ((hz - 200f) / 900f).coerceIn(0.08f, 0.92f) * 23f
        val gain = (volumePercent / 100f).coerceIn(0.15f, 1f)
        return List(24) { idx ->
            val dist = kotlin.math.abs(idx - normalizedCenter)
            val mainPeak = kotlin.math.exp(-(dist * dist) / 4.5f)
            val harmonicPeak = kotlin.math.exp(-((idx - (normalizedCenter * 1.45f).coerceAtMost(23f)).let { it * it }) / 3.5f) * 0.45f
            val shimmer = (0.85f + 0.15f * kotlin.math.sin((step + idx).toFloat()))
            ((mainPeak + harmonicPeak) * gain * shimmer).coerceIn(0.1f, 1.0f)
        }
    }
}
