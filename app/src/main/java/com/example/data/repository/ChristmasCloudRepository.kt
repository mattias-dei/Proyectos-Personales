package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.CustomMelodyDoc
import com.example.data.model.MelodyNote
import com.example.data.model.ThemedPlaylistDoc
import com.example.data.model.UserSettingsDoc
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.tasks.await

class ChristmasCloudRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeUserSettings(userId: String): Flow<UserSettingsDoc?> = flow {
        val docRef = db.collection("users").document(userId)
            .collection("settings").document(SETTINGS_DOC_ID)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserSettingsDoc::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, docRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun saveUserSettings(settings: UserSettingsDoc): Result<Unit> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val docRef = db.collection("users").document(uid)
            .collection("settings").document(SETTINGS_DOC_ID)

        val safeSettings = settings.copy(
            userId = uid,
            tempoBpm = settings.tempoBpm.coerceIn(40L, 240L),
            volumePercent = settings.volumePercent.coerceIn(0L, 100L),
            lightsPattern = settings.lightsPattern.takeIf {
                it in setOf("twinkle", "chase", "warm_glow", "party")
            } ?: "twinkle",
            autoOffMinutes = settings.autoOffMinutes.coerceIn(0L, 240L),
            notifTitle = settings.notifTitle.ifBlank { "Hora de villancicos" }.take(80),
            notifMessage = settings.notifMessage.ifBlank { "Enciende tu ESP32 navideño" }.take(200),
            notifHour = settings.notifHour.coerceIn(0L, 23L),
            notifMinute = settings.notifMinute.coerceIn(0L, 59L),
            preferredDeviceName = settings.preferredDeviceName.take(64),
            preferredDeviceAddress = settings.preferredDeviceAddress.take(64)
        )

        return try {
            val existingSnap = docRef.get().await()
            if (!existingSnap.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "tempoBpm" to safeSettings.tempoBpm,
                    "volumePercent" to safeSettings.volumePercent,
                    "lightsEnabled" to safeSettings.lightsEnabled,
                    "lightsPattern" to safeSettings.lightsPattern,
                    "festiveSoundsEnabled" to safeSettings.festiveSoundsEnabled,
                    "darkWinterMode" to safeSettings.darkWinterMode,
                    "autoOffMinutes" to safeSettings.autoOffMinutes,
                    "notifEnabled" to safeSettings.notifEnabled,
                    "notifTitle" to safeSettings.notifTitle,
                    "notifMessage" to safeSettings.notifMessage,
                    "notifHour" to safeSettings.notifHour,
                    "notifMinute" to safeSettings.notifMinute,
                    "preferredDeviceName" to safeSettings.preferredDeviceName,
                    "preferredDeviceAddress" to safeSettings.preferredDeviceAddress,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            } else {
                val updates = mapOf(
                    "tempoBpm" to safeSettings.tempoBpm,
                    "volumePercent" to safeSettings.volumePercent,
                    "lightsEnabled" to safeSettings.lightsEnabled,
                    "lightsPattern" to safeSettings.lightsPattern,
                    "festiveSoundsEnabled" to safeSettings.festiveSoundsEnabled,
                    "darkWinterMode" to safeSettings.darkWinterMode,
                    "autoOffMinutes" to safeSettings.autoOffMinutes,
                    "notifEnabled" to safeSettings.notifEnabled,
                    "notifTitle" to safeSettings.notifTitle,
                    "notifMessage" to safeSettings.notifMessage,
                    "notifHour" to safeSettings.notifHour,
                    "notifMinute" to safeSettings.notifMinute,
                    "preferredDeviceName" to safeSettings.preferredDeviceName,
                    "preferredDeviceAddress" to safeSettings.preferredDeviceAddress,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
            Result.failure(e)
        }
    }

    fun observeCustomMelodies(userId: String): Flow<List<CustomMelodyDoc>> = flow {
        val colRef = db.collection("users").document(userId).collection("melodies")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CustomMelodyDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, colRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun addCustomMelody(
        title: String,
        artistOrSource: String,
        defaultBpm: Int,
        notes: List<MelodyNote>,
        sourceFileName: String
    ): Result<String> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val boundedNotes = notes.take(400)
        if (boundedNotes.isEmpty()) {
            return Result.failure(IllegalArgumentException("La melodía no contiene notas válidas."))
        }
        val docRef = db.collection("users").document(uid).collection("melodies").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().ifBlank { "Melodía Navideña" }.take(80),
            "artistOrSource" to artistOrSource.trim().take(80),
            "defaultBpm" to defaultBpm.toLong().coerceIn(40L, 240L),
            "frequencies" to boundedNotes.map { it.frequencyHz.toLong().coerceIn(0L, 8000L) },
            "durationsMs" to boundedNotes.map { it.durationMs.toLong().coerceIn(20L, 10000L) },
            "sourceFileName" to sourceFileName.trim().take(100),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteCustomMelody(melodyId: String): Result<Unit> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val docRef = db.collection("users").document(uid).collection("melodies").document(melodyId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    fun observePlaylists(userId: String): Flow<List<ThemedPlaylistDoc>> = flow {
        val colRef = db.collection("users").document(userId).collection("playlists")
        emitAll(
            colRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ThemedPlaylistDoc::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, colRef.path, auth)
                    }
                    throw error
                }
        )
    }

    suspend fun addPlaylist(
        title: String,
        platform: String,
        url: String,
        description: String
    ): Result<String> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("https://") && !cleanUrl.startsWith("spotify:")) {
            return Result.failure(IllegalArgumentException("La URL debe empezar por https:// o spotify:"))
        }
        val cleanPlatform = platform.takeIf {
            it in setOf("spotify", "youtube_music", "apple_music", "other")
        } ?: "other"

        val docRef = db.collection("users").document(uid).collection("playlists").document()
        val payload = mapOf(
            "userId" to uid,
            "title" to title.trim().ifBlank { "Playlist Navideña" }.take(80),
            "platform" to cleanPlatform,
            "url" to cleanUrl.take(300),
            "description" to description.trim().take(160),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val docRef = db.collection("users").document(uid).collection("playlists").document(playlistId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun getMelodyById(ownerUserId: String, melodyId: String): Result<CustomMelodyDoc?> {
        val docRef = db.collection("users").document(ownerUserId)
            .collection("melodies").document(melodyId)
        return try {
            val snap = docRef.get().await()
            val doc = snap.toObject(
                CustomMelodyDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(doc)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            Result.failure(e)
        }
    }

    companion object {
        const val SETTINGS_DOC_ID = "default"
    }
}
