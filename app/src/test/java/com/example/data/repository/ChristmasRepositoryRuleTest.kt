package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.MelodyNote
import com.example.data.model.UserSettingsDoc
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChristmasRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun saveAndObserveUserSettings_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ChristmasCloudRepository(firestore, auth)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserSettings(
                UserSettingsDoc(
                    userId = uid,
                    tempoBpm = 135L,
                    volumePercent = 75L,
                    lightsEnabled = true,
                    lightsPattern = "chase",
                    darkWinterMode = true,
                    autoOffMinutes = 45L
                )
            )
        }
        assertTrue(saveResult.isSuccess)

        val observed = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserSettings(uid).first { it != null && it.tempoBpm == 135L }
        }
        assertNotNull(observed)
        assertEquals(75L, observed?.volumePercent)
        assertEquals("chase", observed?.lightsPattern)

        // Test update branch
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.saveUserSettings(observed!!.copy(tempoBpm = 150L))
        }
        assertTrue(updateResult.isSuccess)
    }

    @Test
    fun addAndObserveCustomMelody_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = ChristmasCloudRepository(firestore, auth)

        val notes = listOf(
            MelodyNote(659, 250),
            MelodyNote(659, 250),
            MelodyNote(659, 500)
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.addCustomMelody(
                title = "Jingle Custom",
                artistOrSource = "Archivo RTTTL",
                defaultBpm = 128,
                notes = notes,
                sourceFileName = "jingle.rtttl"
            )
        }
        assertTrue(createResult.isSuccess)
        val melodyId = createResult.getOrThrow()

        val list = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCustomMelodies(uid).first { items -> items.any { it.id == melodyId } }
        }
        assertEquals("Jingle Custom", list.first { it.id == melodyId }.title)
    }

    @Test
    fun crossUserRead_isRejectedWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = ChristmasCloudRepository(firestore, auth)
        val melodyId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.addCustomMelody(
                title = "Alice Carol",
                artistOrSource = "Test",
                defaultBpm = 120,
                notes = listOf(MelodyNote(440, 300)),
                sourceFileName = "alice.txt"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = ChristmasCloudRepository(firestore, auth)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getMelodyById(aliceUid, melodyId)
        }
        assertTrue(result.isFailure)
        val exception = result.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull(exception)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, exception?.code)
    }

    @Test
    fun observeCustomMelodies_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = ChristmasCloudRepository(firestore, auth)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeCustomMelodies("some_user_123").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull(caught)
        val firestoreException = caught?.unwrapFirestoreException()
        assertNotNull("Expected nested FirebaseFirestoreException, got $caught", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_buzzer@test.com"
        const val BOB_EMAIL = "bob_buzzer@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
