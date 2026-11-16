package com.example.data.music

import com.example.data.model.MelodyNote
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.pow
import kotlin.math.roundToInt

data class ParsedMelodyFile(
    val title: String,
    val defaultBpm: Int,
    val notes: List<MelodyNote>
)

object MelodyFileParser {

    fun parse(rawContent: String, fallbackTitle: String = "Melodía Importada"): Result<ParsedMelodyFile> {
        val text = rawContent.trim()
        if (text.isBlank()) {
            return Result.failure(IllegalArgumentException("El archivo está vacío."))
        }

        // 1. Try JSON format
        if (text.startsWith("{") || text.startsWith("[")) {
            parseJson(text, fallbackTitle)?.let { return Result.success(it) }
        }

        // 2. Try RTTTL format (Title:d=4,o=5,b=120:c,d,e...)
        if (text.contains(":") && text.split(":").size >= 3) {
            parseRtttl(text)?.let { return Result.success(it) }
        }

        // 3. Try CSV / TXT lines (freq,duration or NOTE,duration)
        parseLinePairs(text, fallbackTitle)?.let { return Result.success(it) }

        return Result.failure(
            IllegalArgumentException(
                "Formato no reconocido. Usa RTTTL (ej. Titulo:d=4,o=5,b=120:e,e,2e), JSON o líneas 'freqHz,ms'."
            )
        )
    }

    private fun parseJson(text: String, fallbackTitle: String): ParsedMelodyFile? {
        return runCatching {
            if (text.startsWith("[")) {
                val arr = JSONArray(text)
                val notes = parseJsonNotesArray(arr)
                if (notes.isEmpty()) return null
                ParsedMelodyFile(fallbackTitle, 120, notes)
            } else {
                val obj = JSONObject(text)
                val title = obj.optString("title").ifBlank { fallbackTitle }
                val bpm = obj.optInt("bpm", 120).coerceIn(40, 240)
                val arr = obj.optJSONArray("notes") ?: return null
                val notes = parseJsonNotesArray(arr)
                if (notes.isEmpty()) return null
                ParsedMelodyFile(title, bpm, notes)
            }
        }.getOrNull()
    }

    private fun parseJsonNotesArray(arr: JSONArray): List<MelodyNote> {
        val result = mutableListOf<MelodyNote>()
        for (i in 0 until minOf(arr.length(), 400)) {
            when (val item = arr.get(i)) {
                is JSONObject -> {
                    val hz = item.optInt("hz", item.optInt("freq", item.optInt("frequency", -1)))
                    val noteStr = item.optString("note", "")
                    val resolvedHz = if (hz >= 0) hz else noteNameToHz(noteStr)
                    val ms = item.optInt("ms", item.optInt("duration", 250)).coerceIn(20, 10000)
                    if (resolvedHz >= 0) {
                        result.add(
                            MelodyNote(
                                frequencyHz = resolvedHz.coerceIn(0, 8000),
                                durationMs = ms,
                                label = ChristmasSongsCatalog.noteNameForFrequency(resolvedHz)
                            )
                        )
                    }
                }
                is JSONArray -> {
                    if (item.length() >= 2) {
                        val hz = item.optInt(0, 0).coerceIn(0, 8000)
                        val ms = item.optInt(1, 250).coerceIn(20, 10000)
                        result.add(MelodyNote(hz, ms, ChristmasSongsCatalog.noteNameForFrequency(hz)))
                    }
                }
            }
        }
        return result
    }

    private fun parseRtttl(text: String): ParsedMelodyFile? {
        return runCatching {
            val parts = text.replace("\n", "").replace("\r", "").split(":", limit = 3)
            if (parts.size < 3) return null
            val title = parts[0].trim().ifBlank { "Melodía RTTTL" }.take(80)
            val header = parts[1].lowercase()
            val body = parts[2].trim()

            var defaultDuration = 4
            var defaultOctave = 5
            var bpm = 120

            header.split(",").forEach { token ->
                val kv = token.trim().split("=")
                if (kv.size == 2) {
                    val v = kv[1].trim().toIntOrNull()
                    when (kv[0].trim()) {
                        "d" -> if (v != null && v in listOf(1, 2, 4, 8, 16, 32)) defaultDuration = v
                        "o" -> if (v != null && v in 3..7) defaultOctave = v
                        "b" -> if (v != null) bpm = v.coerceIn(40, 240)
                    }
                }
            }

            val wholeNoteMs = (60_000.0 / bpm) * 4.0
            val notes = mutableListOf<MelodyNote>()
            val regex = Regex("""^(\d{1,2})?([a-gpA-GP]#?|\.?)\.?(\d)?(\.)?$""")

            body.split(",").take(400).forEach { rawToken ->
                val token = rawToken.trim().lowercase()
                if (token.isEmpty()) return@forEach

                var idx = 0
                val durSb = StringBuilder()
                while (idx < token.length && token[idx].isDigit()) {
                    durSb.append(token[idx])
                    idx++
                }
                val durVal = durSb.toString().toIntOrNull() ?: defaultDuration

                val noteSb = StringBuilder()
                if (idx < token.length && token[idx] in listOf('a', 'b', 'c', 'd', 'e', 'f', 'g', 'p')) {
                    noteSb.append(token[idx])
                    idx++
                    if (idx < token.length && token[idx] == '#') {
                        noteSb.append('#')
                        idx++
                    }
                }
                var dotted = false
                if (idx < token.length && token[idx] == '.') {
                    dotted = true
                    idx++
                }
                val octave = if (idx < token.length && token[idx].isDigit()) {
                    idx++
                    token[idx - 1].digitToInt().coerceIn(3, 7)
                } else {
                    defaultOctave
                }
                if (idx < token.length && token[idx] == '.') {
                    dotted = true
                }

                val noteKey = noteSb.toString()
                if (noteKey.isNotEmpty()) {
                    var durationMs = (wholeNoteMs / durVal).roundToInt()
                    if (dotted) durationMs = (durationMs * 1.5).roundToInt()
                    durationMs = durationMs.coerceIn(25, 10000)

                    val hz = if (noteKey == "p") 0 else noteNameToHz("$noteKey$octave")
                    if (hz >= 0) {
                        notes.add(
                            MelodyNote(
                                frequencyHz = hz,
                                durationMs = durationMs,
                                label = ChristmasSongsCatalog.noteNameForFrequency(hz)
                            )
                        )
                    }
                }
            }
            if (notes.isEmpty()) return null
            ParsedMelodyFile(title, bpm, notes)
        }.getOrNull()
    }

    private fun parseLinePairs(text: String, fallbackTitle: String): ParsedMelodyFile? {
        val notes = mutableListOf<MelodyNote>()
        val lines = text.lines()
        var title = fallbackTitle
        var bpm = 120

        lines.forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            if (line.startsWith("#")) {
                val meta = line.removePrefix("#").trim()
                if (meta.startsWith("title:", ignoreCase = true)) {
                    title = meta.substringAfter(":").trim().take(80).ifBlank { fallbackTitle }
                } else if (meta.startsWith("bpm:", ignoreCase = true)) {
                    bpm = meta.substringAfter(":").trim().toIntOrNull()?.coerceIn(40, 240) ?: bpm
                }
                return@forEach
            }
            val parts = line.split(',', ';', ':', '\t', ' ').filter { it.isNotBlank() }
            if (parts.size >= 2 && notes.size < 400) {
                val first = parts[0].trim()
                val second = parts[1].trim()
                val hz = first.toIntOrNull() ?: noteNameToHz(first)
                val ms = second.toIntOrNull()?.coerceIn(20, 10000)
                if (hz >= 0 && ms != null) {
                    notes.add(
                        MelodyNote(
                            frequencyHz = hz.coerceIn(0, 8000),
                            durationMs = ms,
                            label = ChristmasSongsCatalog.noteNameForFrequency(hz)
                        )
                    )
                }
            }
        }
        if (notes.isEmpty()) return null
        return ParsedMelodyFile(title, bpm, notes)
    }

    fun noteNameToHz(raw: String): Int {
        val clean = raw.trim().uppercase()
            .replace("NOTE_", "")
            .replace("REST", "P")
            .replace("SILENCIO", "P")
        if (clean == "P" || clean == "R" || clean == "0") return 0
        val match = Regex("""^([A-G])([#S]?)(-?\d)$""").find(clean) ?: return -1
        val letter = match.groupValues[1]
        val sharp = match.groupValues[2].isNotEmpty()
        val octave = match.groupValues[3].toInt()
        val semitoneFromC = when (letter) {
            "C" -> 0
            "D" -> 2
            "E" -> 4
            "F" -> 5
            "G" -> 7
            "A" -> 9
            "B" -> 11
            else -> return -1
        } + if (sharp) 1 else 0
        val midi = (octave + 1) * 12 + semitoneFromC
        return (440.0 * 2.0.pow((midi - 69) / 12.0)).roundToInt().coerceIn(20, 8000)
    }
}
