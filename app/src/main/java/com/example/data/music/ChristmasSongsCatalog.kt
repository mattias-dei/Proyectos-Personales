package com.example.data.music

import com.example.data.model.ChristmasSong
import com.example.data.model.MelodyNote

object ChristmasSongsCatalog {

    private const val REST = 0
    private const val B3 = 247
    private const val C4 = 262
    private const val CS4 = 277
    private const val D4 = 294
    private const val DS4 = 311
    private const val E4 = 330
    private const val F4 = 349
    private const val FS4 = 370
    private const val G4 = 392
    private const val GS4 = 415
    private const val A4 = 440
    private const val AS4 = 466
    private const val B4 = 494
    private const val C5 = 523
    private const val CS5 = 554
    private const val D5 = 587
    private const val DS5 = 622
    private const val E5 = 659
    private const val F5 = 698
    private const val FS5 = 740
    private const val G5 = 784
    private const val A5 = 880

    fun noteNameForFrequency(hz: Int): String {
        if (hz <= 0) return "Silencio"
        val map = listOf(
            247 to "B3", 262 to "C4", 277 to "C#4", 294 to "D4", 311 to "D#4", 330 to "E4",
            349 to "F4", 370 to "F#4", 392 to "G4", 415 to "G#4", 440 to "A4",
            466 to "A#4", 494 to "B4", 523 to "C5", 554 to "C#5", 587 to "D5",
            622 to "D#5", 659 to "E5", 698 to "F5", 740 to "F#5", 784 to "G5",
            831 to "G#5", 880 to "A5", 932 to "A#5", 988 to "B5", 1047 to "C6"
        )
        return map.minByOrNull { kotlin.math.abs(it.first - hz) }?.second ?: "${hz}Hz"
    }

    val songs: List<ChristmasSong> = listOf(
        // 1. Jingle Bells Completa (Estrofa + Estribillo completo)
        ChristmasSong(
            id = "jingle_bells",
            title = "Jingle Bells (Completa)",
            subtitle = "James Lord Pierpont · Estrofa y estribillo",
            defaultBpm = 136,
            notes = listOf(
                // Estrofa: Dashing through the snow...
                MelodyNote(G4, 250, "G4"), MelodyNote(E5, 250, "E5"), MelodyNote(D5, 250, "D5"), MelodyNote(C5, 250, "C5"),
                MelodyNote(G4, 750, "G4"), MelodyNote(G4, 125, "G4"), MelodyNote(G4, 125, "G4"),
                MelodyNote(G4, 250, "G4"), MelodyNote(E5, 250, "E5"), MelodyNote(D5, 250, "D5"), MelodyNote(C5, 250, "C5"),
                MelodyNote(A4, 850, "A4"), MelodyNote(REST, 120),
                MelodyNote(A4, 250, "A4"), MelodyNote(F5, 250, "F5"), MelodyNote(E5, 250, "E5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(B4, 850, "B4"), MelodyNote(REST, 120),
                MelodyNote(G5, 250, "G5"), MelodyNote(G5, 250, "G5"), MelodyNote(F5, 250, "F5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(E5, 850, "E5"), MelodyNote(REST, 120),
                MelodyNote(G4, 250, "G4"), MelodyNote(E5, 250, "E5"), MelodyNote(D5, 250, "D5"), MelodyNote(C5, 250, "C5"),
                MelodyNote(G4, 850, "G4"), MelodyNote(REST, 120),
                MelodyNote(G4, 250, "G4"), MelodyNote(E5, 250, "E5"), MelodyNote(D5, 250, "D5"), MelodyNote(C5, 250, "C5"),
                MelodyNote(A4, 850, "A4"), MelodyNote(REST, 120),
                MelodyNote(A4, 250, "A4"), MelodyNote(F5, 250, "F5"), MelodyNote(E5, 250, "E5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(G5, 250, "G5"), MelodyNote(G5, 250, "G5"), MelodyNote(G5, 250, "G5"), MelodyNote(G5, 250, "G5"),
                MelodyNote(A5, 250, "A5"), MelodyNote(G5, 250, "G5"), MelodyNote(F5, 250, "F5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(C5, 650, "C5"), MelodyNote(G5, 350, "G5"),
                // Estribillo: Jingle bells, jingle bells...
                MelodyNote(E5, 250, "E5"), MelodyNote(E5, 250, "E5"), MelodyNote(E5, 500, "E5"),
                MelodyNote(E5, 250, "E5"), MelodyNote(E5, 250, "E5"), MelodyNote(E5, 500, "E5"),
                MelodyNote(E5, 250, "E5"), MelodyNote(G5, 250, "G5"), MelodyNote(C5, 350, "C5"),
                MelodyNote(D5, 150, "D5"), MelodyNote(E5, 800, "E5"), MelodyNote(REST, 150),
                MelodyNote(F5, 250, "F5"), MelodyNote(F5, 250, "F5"), MelodyNote(F5, 350, "F5"),
                MelodyNote(F5, 150, "F5"), MelodyNote(F5, 250, "F5"), MelodyNote(E5, 250, "E5"),
                MelodyNote(E5, 250, "E5"), MelodyNote(E5, 125, "E5"), MelodyNote(E5, 125, "E5"),
                MelodyNote(E5, 250, "E5"), MelodyNote(D5, 250, "D5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(E5, 250, "E5"), MelodyNote(D5, 500, "D5"), MelodyNote(G5, 500, "G5"),
                MelodyNote(E5, 250, "E5"), MelodyNote(E5, 250, "E5"), MelodyNote(E5, 500, "E5"),
                MelodyNote(E5, 250, "E5"), MelodyNote(E5, 250, "E5"), MelodyNote(E5, 500, "E5"),
                MelodyNote(E5, 250, "E5"), MelodyNote(G5, 250, "G5"), MelodyNote(C5, 350, "C5"),
                MelodyNote(D5, 150, "D5"), MelodyNote(E5, 800, "E5"), MelodyNote(REST, 150),
                MelodyNote(F5, 250, "F5"), MelodyNote(F5, 250, "F5"), MelodyNote(F5, 350, "F5"),
                MelodyNote(F5, 150, "F5"), MelodyNote(F5, 250, "F5"), MelodyNote(E5, 250, "E5"),
                MelodyNote(E5, 250, "E5"), MelodyNote(E5, 125, "E5"), MelodyNote(E5, 125, "E5"),
                MelodyNote(G5, 250, "G5"), MelodyNote(G5, 250, "G5"), MelodyNote(F5, 250, "F5"),
                MelodyNote(D5, 250, "D5"), MelodyNote(C5, 1000, "C5"), MelodyNote(REST, 400)
            )
        ),
        // 2. Noche de Paz (Silent Night - Completa)
        ChristmasSong(
            id = "silent_night",
            title = "Noche de Paz (Completa)",
            subtitle = "Franz Xaver Gruber · Melodía tradicional",
            defaultBpm = 88,
            notes = listOf(
                MelodyNote(G4, 480, "G4"), MelodyNote(A4, 160, "A4"), MelodyNote(G4, 320, "G4"),
                MelodyNote(E4, 960, "E4"),
                MelodyNote(G4, 480, "G4"), MelodyNote(A4, 160, "A4"), MelodyNote(G4, 320, "G4"),
                MelodyNote(E4, 960, "E4"),
                MelodyNote(D5, 640, "D5"), MelodyNote(D5, 320, "D5"), MelodyNote(B4, 960, "B4"),
                MelodyNote(C5, 640, "C5"), MelodyNote(C5, 320, "C5"), MelodyNote(G4, 960, "G4"),
                MelodyNote(A4, 640, "A4"), MelodyNote(A4, 320, "A4"), MelodyNote(C5, 480, "C5"),
                MelodyNote(B4, 160, "B4"), MelodyNote(A4, 320, "A4"),
                MelodyNote(G4, 480, "G4"), MelodyNote(A4, 160, "A4"), MelodyNote(G4, 320, "G4"),
                MelodyNote(E4, 960, "E4"),
                MelodyNote(A4, 640, "A4"), MelodyNote(A4, 320, "A4"), MelodyNote(C5, 480, "C5"),
                MelodyNote(B4, 160, "B4"), MelodyNote(A4, 320, "A4"),
                MelodyNote(G4, 480, "G4"), MelodyNote(A4, 160, "A4"), MelodyNote(G4, 320, "G4"),
                MelodyNote(E4, 960, "E4"),
                MelodyNote(D5, 640, "D5"), MelodyNote(D5, 320, "D5"), MelodyNote(F5, 480, "F5"),
                MelodyNote(D5, 160, "D5"), MelodyNote(B4, 320, "B4"),
                MelodyNote(C5, 960, "C5"), MelodyNote(E5, 960, "E5"),
                MelodyNote(C5, 480, "C5"), MelodyNote(G4, 160, "G4"), MelodyNote(E4, 320, "E4"),
                MelodyNote(G4, 480, "G4"), MelodyNote(F4, 160, "F4"), MelodyNote(D4, 320, "D4"),
                MelodyNote(C4, 1400, "C4"), MelodyNote(REST, 450)
            )
        ),
        // 3. We Wish You a Merry Christmas (Completa con puente)
        ChristmasSong(
            id = "we_wish_you",
            title = "We Wish You a Merry Christmas",
            subtitle = "Tradicional inglés · Versión completa",
            defaultBpm = 140,
            notes = listOf(
                // Parte A
                MelodyNote(D4, 300, "D4"),
                MelodyNote(G4, 300, "G4"), MelodyNote(G4, 150, "G4"), MelodyNote(A4, 150, "A4"),
                MelodyNote(G4, 150, "G4"), MelodyNote(FS4, 150, "F#4"),
                MelodyNote(E4, 300, "E4"), MelodyNote(E4, 300, "E4"), MelodyNote(E4, 300, "E4"),
                MelodyNote(A4, 300, "A4"), MelodyNote(A4, 150, "A4"), MelodyNote(B4, 150, "B4"),
                MelodyNote(A4, 150, "A4"), MelodyNote(G4, 150, "G4"),
                MelodyNote(FS4, 300, "F#4"), MelodyNote(D4, 300, "D4"), MelodyNote(D4, 300, "D4"),
                MelodyNote(B4, 300, "B4"), MelodyNote(B4, 150, "B4"), MelodyNote(C5, 150, "C5"),
                MelodyNote(B4, 150, "B4"), MelodyNote(A4, 150, "A4"),
                MelodyNote(G4, 300, "G4"), MelodyNote(E4, 300, "E4"),
                MelodyNote(D4, 150, "D4"), MelodyNote(D4, 150, "D4"),
                MelodyNote(E4, 300, "E4"), MelodyNote(A4, 300, "A4"),
                MelodyNote(FS4, 300, "F#4"), MelodyNote(G4, 650, "G4"),
                // Puente: Good tidings we bring...
                MelodyNote(D4, 300, "D4"),
                MelodyNote(G4, 300, "G4"), MelodyNote(G4, 300, "G4"), MelodyNote(G4, 300, "G4"),
                MelodyNote(FS4, 600, "F#4"), MelodyNote(FS4, 300, "F#4"),
                MelodyNote(G4, 300, "G4"), MelodyNote(FS4, 300, "F#4"), MelodyNote(E4, 300, "E4"),
                MelodyNote(D4, 600, "D4"), MelodyNote(A4, 300, "A4"),
                MelodyNote(B4, 300, "B4"), MelodyNote(A4, 300, "A4"), MelodyNote(G4, 300, "G4"),
                MelodyNote(D5, 300, "D5"), MelodyNote(D4, 300, "D4"),
                MelodyNote(D4, 150, "D4"), MelodyNote(D4, 150, "D4"),
                MelodyNote(E4, 300, "E4"), MelodyNote(A4, 300, "A4"),
                MelodyNote(FS4, 300, "F#4"), MelodyNote(G4, 650, "G4"),
                // Reprise final
                MelodyNote(D4, 300, "D4"),
                MelodyNote(B4, 300, "B4"), MelodyNote(B4, 150, "B4"), MelodyNote(C5, 150, "C5"),
                MelodyNote(B4, 150, "B4"), MelodyNote(A4, 150, "A4"),
                MelodyNote(G4, 300, "G4"), MelodyNote(E4, 300, "E4"),
                MelodyNote(D4, 150, "D4"), MelodyNote(D4, 150, "D4"),
                MelodyNote(E4, 300, "E4"), MelodyNote(A4, 300, "A4"),
                MelodyNote(FS4, 300, "F#4"), MelodyNote(G4, 950, "G4"), MelodyNote(REST, 400)
            )
        ),
        // 4. Deck the Halls (Completa A-A-B-A)
        ChristmasSong(
            id = "deck_the_halls",
            title = "Deck the Halls (Completa)",
            subtitle = "Tradicional galés · Estructura A-A-B-A",
            defaultBpm = 132,
            notes = listOf(
                // A1
                MelodyNote(G5, 375, "G5"), MelodyNote(F5, 125, "F5"), MelodyNote(E5, 250, "E5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(C5, 250, "C5"), MelodyNote(D5, 250, "D5"), MelodyNote(E5, 250, "E5"), MelodyNote(C5, 250, "C5"),
                MelodyNote(D5, 125, "D5"), MelodyNote(E5, 125, "E5"), MelodyNote(F5, 125, "F5"), MelodyNote(D5, 125, "D5"),
                MelodyNote(E5, 375, "E5"), MelodyNote(D5, 125, "D5"), MelodyNote(C5, 250, "C5"), MelodyNote(B4, 250, "B4"),
                MelodyNote(C5, 500, "C5"), MelodyNote(REST, 100),
                // A2
                MelodyNote(G5, 375, "G5"), MelodyNote(F5, 125, "F5"), MelodyNote(E5, 250, "E5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(C5, 250, "C5"), MelodyNote(D5, 250, "D5"), MelodyNote(E5, 250, "E5"), MelodyNote(C5, 250, "C5"),
                MelodyNote(D5, 125, "D5"), MelodyNote(E5, 125, "E5"), MelodyNote(F5, 125, "F5"), MelodyNote(D5, 125, "D5"),
                MelodyNote(E5, 375, "E5"), MelodyNote(D5, 125, "D5"), MelodyNote(C5, 250, "C5"), MelodyNote(B4, 250, "B4"),
                MelodyNote(C5, 500, "C5"), MelodyNote(REST, 100),
                // B
                MelodyNote(D5, 375, "D5"), MelodyNote(E5, 125, "E5"), MelodyNote(F5, 250, "F5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(E5, 375, "E5"), MelodyNote(F5, 125, "F5"), MelodyNote(G5, 250, "G5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(E5, 125, "E5"), MelodyNote(FS5, 125, "F#5"), MelodyNote(G5, 250, "G5"),
                MelodyNote(A5, 125, "A5"), MelodyNote(G5, 125, "G5"), MelodyNote(FS5, 250, "F#5"),
                MelodyNote(G5, 500, "G5"), MelodyNote(REST, 100),
                // A3 final
                MelodyNote(G5, 375, "G5"), MelodyNote(F5, 125, "F5"), MelodyNote(E5, 250, "E5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(C5, 250, "C5"), MelodyNote(D5, 250, "D5"), MelodyNote(E5, 250, "E5"), MelodyNote(C5, 250, "C5"),
                MelodyNote(D5, 125, "D5"), MelodyNote(E5, 125, "E5"), MelodyNote(F5, 125, "F5"), MelodyNote(D5, 125, "D5"),
                MelodyNote(E5, 375, "E5"), MelodyNote(D5, 125, "D5"), MelodyNote(C5, 250, "C5"), MelodyNote(B4, 250, "B4"),
                MelodyNote(C5, 850, "C5"), MelodyNote(REST, 400)
            )
        ),
        // 5. Joy to the World (Completa)
        ChristmasSong(
            id = "joy_to_the_world",
            title = "Joy to the World (Completa)",
            subtitle = "Lowell Mason & Handel · Coral completo",
            defaultBpm = 118,
            notes = listOf(
                // Joy to the world, the Lord is come!
                MelodyNote(C5, 500, "C5"), MelodyNote(B4, 375, "B4"), MelodyNote(A4, 125, "A4"),
                MelodyNote(G4, 750, "G4"), MelodyNote(F4, 250, "F4"),
                MelodyNote(E4, 500, "E4"), MelodyNote(D4, 500, "D4"), MelodyNote(C4, 750, "C4"),
                // Let earth receive her King
                MelodyNote(G4, 250, "G4"), MelodyNote(A4, 750, "A4"),
                MelodyNote(A4, 250, "A4"), MelodyNote(B4, 750, "B4"),
                MelodyNote(B4, 250, "B4"), MelodyNote(C5, 950, "C5"), MelodyNote(REST, 150),
                // Let every heart prepare Him room
                MelodyNote(C5, 250, "C5"), MelodyNote(C5, 250, "C5"), MelodyNote(B4, 250, "B4"),
                MelodyNote(A4, 250, "A4"), MelodyNote(G4, 250, "G4"), MelodyNote(G4, 375, "G4"),
                MelodyNote(F4, 125, "F4"), MelodyNote(E4, 250, "E4"),
                MelodyNote(C5, 250, "C5"), MelodyNote(C5, 250, "C5"), MelodyNote(B4, 250, "B4"),
                MelodyNote(A4, 250, "A4"), MelodyNote(G4, 250, "G4"), MelodyNote(G4, 375, "G4"),
                MelodyNote(F4, 125, "F4"), MelodyNote(E4, 250, "E4"),
                // And heaven and nature sing
                MelodyNote(E4, 250, "E4"), MelodyNote(E4, 250, "E4"), MelodyNote(E4, 250, "E4"),
                MelodyNote(E4, 125, "E4"), MelodyNote(F4, 125, "F4"), MelodyNote(G4, 750, "G4"),
                MelodyNote(F4, 125, "F4"), MelodyNote(E4, 125, "E4"),
                MelodyNote(D4, 250, "D4"), MelodyNote(D4, 250, "D4"), MelodyNote(D4, 250, "D4"),
                MelodyNote(D4, 125, "D4"), MelodyNote(E4, 125, "E4"), MelodyNote(F4, 750, "F4"),
                MelodyNote(E4, 125, "E4"), MelodyNote(D4, 125, "D4"),
                // And heaven, and heaven and nature sing
                MelodyNote(E4, 250, "E4"), MelodyNote(C5, 500, "C5"), MelodyNote(A4, 250, "A4"),
                MelodyNote(G4, 375, "G4"), MelodyNote(F4, 125, "F4"), MelodyNote(E4, 250, "E4"),
                MelodyNote(F4, 250, "F4"), MelodyNote(E4, 500, "E4"), MelodyNote(D4, 500, "D4"),
                MelodyNote(C4, 1100, "C4"), MelodyNote(REST, 400)
            )
        ),
        // 6. O Tannenbaum / El Abeto (Completa A-A-B-A)
        ChristmasSong(
            id = "o_christmas_tree",
            title = "O Tannenbaum (Completa)",
            subtitle = "Ernst Anschütz · Versión completa",
            defaultBpm = 104,
            notes = listOf(
                // A1
                MelodyNote(C4, 300, "C4"), MelodyNote(F4, 450, "F4"), MelodyNote(F4, 150, "F4"),
                MelodyNote(F4, 550, "F4"), MelodyNote(G4, 250, "G4"),
                MelodyNote(A4, 450, "A4"), MelodyNote(A4, 150, "A4"), MelodyNote(A4, 550, "A4"),
                MelodyNote(A4, 250, "A4"), MelodyNote(G4, 250, "G4"), MelodyNote(A4, 250, "A4"),
                MelodyNote(AS4, 500, "A#4"), MelodyNote(E4, 350, "E4"), MelodyNote(G4, 350, "G4"),
                MelodyNote(F4, 700, "F4"),
                // A2
                MelodyNote(C4, 300, "C4"), MelodyNote(F4, 450, "F4"), MelodyNote(F4, 150, "F4"),
                MelodyNote(F4, 550, "F4"), MelodyNote(G4, 250, "G4"),
                MelodyNote(A4, 450, "A4"), MelodyNote(A4, 150, "A4"), MelodyNote(A4, 550, "A4"),
                MelodyNote(A4, 250, "A4"), MelodyNote(G4, 250, "G4"), MelodyNote(A4, 250, "A4"),
                MelodyNote(AS4, 500, "A#4"), MelodyNote(E4, 350, "E4"), MelodyNote(G4, 350, "G4"),
                MelodyNote(F4, 700, "F4"),
                // B
                MelodyNote(C5, 250, "C5"), MelodyNote(C5, 250, "C5"), MelodyNote(A4, 250, "A4"),
                MelodyNote(D5, 550, "D5"), MelodyNote(C5, 250, "C5"), MelodyNote(C5, 250, "C5"),
                MelodyNote(AS4, 250, "A#4"), MelodyNote(AS4, 550, "A#4"),
                MelodyNote(AS4, 250, "A#4"), MelodyNote(AS4, 250, "A#4"), MelodyNote(G4, 250, "G4"),
                MelodyNote(C5, 550, "C5"), MelodyNote(AS4, 250, "A#4"), MelodyNote(AS4, 250, "A#4"),
                MelodyNote(A4, 250, "A4"), MelodyNote(A4, 550, "A4"),
                // A3 final
                MelodyNote(C4, 300, "C4"), MelodyNote(F4, 450, "F4"), MelodyNote(F4, 150, "F4"),
                MelodyNote(F4, 550, "F4"), MelodyNote(G4, 250, "G4"),
                MelodyNote(A4, 450, "A4"), MelodyNote(A4, 150, "A4"), MelodyNote(A4, 550, "A4"),
                MelodyNote(A4, 250, "A4"), MelodyNote(G4, 250, "G4"), MelodyNote(A4, 250, "A4"),
                MelodyNote(AS4, 500, "A#4"), MelodyNote(E4, 350, "E4"), MelodyNote(G4, 350, "G4"),
                MelodyNote(F4, 1000, "F4"), MelodyNote(REST, 400)
            )
        ),
        // 7. Frosty the Snowman (Completa con puente)
        ChristmasSong(
            id = "frosty_snowman",
            title = "Frosty the Snowman (Completa)",
            subtitle = "Walter Rollins & Steve Nelson",
            defaultBpm = 126,
            notes = listOf(
                // A1
                MelodyNote(G4, 500, "G4"), MelodyNote(E4, 375, "E4"), MelodyNote(F4, 125, "F4"),
                MelodyNote(G4, 250, "G4"), MelodyNote(C5, 500, "C5"),
                MelodyNote(B4, 125, "B4"), MelodyNote(C5, 125, "C5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(C5, 250, "C5"), MelodyNote(B4, 250, "B4"), MelodyNote(A4, 250, "A4"),
                MelodyNote(G4, 750, "G4"),
                MelodyNote(B4, 125, "B4"), MelodyNote(C5, 125, "C5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(C5, 250, "C5"), MelodyNote(B4, 250, "B4"), MelodyNote(A4, 125, "A4"),
                MelodyNote(A4, 125, "A4"), MelodyNote(G4, 250, "G4"), MelodyNote(C5, 250, "C5"),
                MelodyNote(E4, 250, "E4"), MelodyNote(G4, 125, "G4"), MelodyNote(A4, 125, "A4"),
                MelodyNote(G4, 250, "G4"), MelodyNote(F4, 250, "F4"), MelodyNote(E4, 250, "E4"),
                MelodyNote(D4, 250, "D4"), MelodyNote(C4, 850, "C4"), MelodyNote(REST, 120),
                // Puente: There must have been some magic...
                MelodyNote(C5, 250, "C5"), MelodyNote(A4, 250, "A4"), MelodyNote(A4, 250, "A4"), MelodyNote(C5, 250, "C5"),
                MelodyNote(B4, 250, "B4"), MelodyNote(G4, 250, "G4"), MelodyNote(E4, 250, "E4"), MelodyNote(G4, 250, "G4"),
                MelodyNote(A4, 250, "A4"), MelodyNote(G4, 250, "G4"), MelodyNote(F4, 250, "F4"), MelodyNote(E4, 250, "E4"),
                MelodyNote(D4, 750, "D4"), MelodyNote(REST, 120),
                MelodyNote(D4, 250, "D4"), MelodyNote(G4, 250, "G4"), MelodyNote(G4, 250, "G4"), MelodyNote(B4, 250, "B4"),
                MelodyNote(D5, 250, "D5"), MelodyNote(D5, 250, "D5"), MelodyNote(B4, 250, "B4"), MelodyNote(D5, 250, "D5"),
                MelodyNote(C5, 250, "C5"), MelodyNote(B4, 250, "B4"), MelodyNote(A4, 250, "A4"), MelodyNote(F4, 250, "F4"),
                MelodyNote(G4, 750, "G4"), MelodyNote(REST, 120),
                // A2 final
                MelodyNote(G4, 500, "G4"), MelodyNote(E4, 375, "E4"), MelodyNote(F4, 125, "F4"),
                MelodyNote(G4, 250, "G4"), MelodyNote(C5, 500, "C5"),
                MelodyNote(B4, 125, "B4"), MelodyNote(C5, 125, "C5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(C5, 250, "C5"), MelodyNote(B4, 250, "B4"), MelodyNote(A4, 250, "A4"),
                MelodyNote(G4, 750, "G4"),
                MelodyNote(B4, 125, "B4"), MelodyNote(C5, 125, "C5"), MelodyNote(D5, 250, "D5"),
                MelodyNote(C5, 250, "C5"), MelodyNote(B4, 250, "B4"), MelodyNote(A4, 125, "A4"),
                MelodyNote(A4, 125, "A4"), MelodyNote(G4, 250, "G4"), MelodyNote(C5, 250, "C5"),
                MelodyNote(E4, 250, "E4"), MelodyNote(G4, 125, "G4"), MelodyNote(A4, 125, "A4"),
                MelodyNote(G4, 250, "G4"), MelodyNote(F4, 250, "F4"), MelodyNote(E4, 250, "E4"),
                MelodyNote(D4, 250, "D4"), MelodyNote(C4, 1000, "C4"), MelodyNote(REST, 400)
            )
        ),
        // 8. Rodolfo el Reno (Completa A-A-B-A)
        ChristmasSong(
            id = "rudolph",
            title = "Rodolfo el Reno (Completa)",
            subtitle = "Johnny Marks · Versión completa",
            defaultBpm = 132,
            notes = listOf(
                // A1
                MelodyNote(G4, 220, "G4"), MelodyNote(A4, 220, "A4"), MelodyNote(G4, 220, "G4"),
                MelodyNote(E4, 420, "E4"), MelodyNote(C5, 420, "C5"), MelodyNote(A4, 420, "A4"),
                MelodyNote(G4, 780, "G4"),
                MelodyNote(G4, 200, "G4"), MelodyNote(A4, 200, "A4"), MelodyNote(G4, 200, "G4"),
                MelodyNote(A4, 200, "A4"), MelodyNote(G4, 420, "G4"), MelodyNote(C5, 420, "C5"),
                MelodyNote(B4, 820, "B4"),
                MelodyNote(F4, 220, "F4"), MelodyNote(G4, 220, "G4"), MelodyNote(F4, 220, "F4"),
                MelodyNote(D4, 420, "D4"), MelodyNote(B4, 420, "B4"), MelodyNote(A4, 420, "A4"),
                MelodyNote(G4, 780, "G4"),
                MelodyNote(G4, 200, "G4"), MelodyNote(A4, 200, "A4"), MelodyNote(G4, 200, "G4"),
                MelodyNote(A4, 200, "A4"), MelodyNote(G4, 420, "G4"), MelodyNote(A4, 420, "A4"),
                MelodyNote(E4, 820, "E4"), MelodyNote(REST, 150),
                // B: Then one foggy Christmas Eve...
                MelodyNote(A4, 420, "A4"), MelodyNote(A4, 420, "A4"), MelodyNote(C5, 420, "C5"),
                MelodyNote(A4, 420, "A4"), MelodyNote(G4, 420, "G4"), MelodyNote(E4, 220, "E4"),
                MelodyNote(G4, 650, "G4"),
                MelodyNote(F4, 420, "F4"), MelodyNote(A4, 420, "A4"), MelodyNote(G4, 420, "G4"),
                MelodyNote(F4, 420, "F4"), MelodyNote(E4, 850, "E4"),
                MelodyNote(D4, 420, "D4"), MelodyNote(E4, 420, "E4"), MelodyNote(G4, 420, "G4"),
                MelodyNote(A4, 420, "A4"), MelodyNote(B4, 420, "B4"), MelodyNote(B4, 420, "B4"),
                MelodyNote(B4, 650, "B4"),
                MelodyNote(C5, 420, "C5"), MelodyNote(C5, 420, "C5"), MelodyNote(B4, 420, "B4"),
                MelodyNote(A4, 420, "A4"), MelodyNote(G4, 420, "G4"), MelodyNote(F4, 220, "F4"),
                MelodyNote(D4, 700, "D4"),
                // A2 final
                MelodyNote(G4, 220, "G4"), MelodyNote(A4, 220, "A4"), MelodyNote(G4, 220, "G4"),
                MelodyNote(E4, 420, "E4"), MelodyNote(C5, 420, "C5"), MelodyNote(A4, 420, "A4"),
                MelodyNote(G4, 780, "G4"),
                MelodyNote(G4, 200, "G4"), MelodyNote(A4, 200, "A4"), MelodyNote(G4, 200, "G4"),
                MelodyNote(A4, 200, "A4"), MelodyNote(G4, 420, "G4"), MelodyNote(D5, 420, "D5"),
                MelodyNote(C5, 1050, "C5"), MelodyNote(REST, 400)
            )
        )
    )
}
