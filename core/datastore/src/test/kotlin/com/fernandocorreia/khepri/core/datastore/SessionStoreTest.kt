package com.fernandocorreia.khepri.core.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.fernandocorreia.khepri.core.common.TokenUnreadableException
import com.fernandocorreia.khepri.core.model.Session
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class SessionStoreTest {
    @TempDir lateinit var dir: File

    private val session = Session("t0ken", "u1", Instant.parse("2026-12-25T10:00:00.123Z"))
    private val cipher = XorCipher()

    private fun TestScope.store() =
        DataStoreSessionStore(
            PreferenceDataStoreFactory.create(scope = backgroundScope) { File(dir, "session.preferences_pb") },
            cipher,
        )

    @Test
    fun `write then read returns the same session`() = runTest {
        val store = store()
        store.write(session)
        assertEquals(session, store.read())
    }

    @Test
    fun `the token is not stored in the clear`() = runTest {
        store().write(session)
        assertFalse(File(dir, "session.preferences_pb").readBytes().decodeToString().contains("t0ken"))
    }

    @Test
    fun `clear then read returns null`() = runTest {
        val store = store()
        store.write(session)
        store.clear()
        assertNull(store.read())
    }

    @Test
    fun `an unreadable cipher throws and keeps the data`() = runTest {
        val store = store()
        store.write(session)
        cipher.failure = IOException("keystore busy")
        assertThrows<TokenUnreadableException> { store.read() }
        cipher.failure = null
        assertEquals(session, store.read())
    }

    @Test
    fun `data whose key is gone reads as signed out and is dropped`() = runTest {
        val store = store()
        store.write(session)
        cipher.failure = SealedDataLostException()
        assertNull(store.read())
        cipher.failure = null
        assertNull(store.read())
    }

    private class XorCipher : TokenCipher {
        var failure: Exception? = null

        override fun encrypt(plain: ByteArray) = plain.map { (it.toInt() xor MASK).toByte() }.toByteArray()

        override fun decrypt(sealed: ByteArray): ByteArray {
            failure?.let { throw it }
            return encrypt(sealed)
        }

        private companion object {
            const val MASK = 0x5A
        }
    }
}
