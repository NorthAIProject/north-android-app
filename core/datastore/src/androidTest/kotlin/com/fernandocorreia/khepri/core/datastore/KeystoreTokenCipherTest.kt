package com.fernandocorreia.khepri.core.datastore

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

/** Device-only (Phase 6 suite): the Keystore does not exist on the JVM. */
@RunWith(AndroidJUnit4::class)
class KeystoreTokenCipherTest {
    @Test
    fun roundTrip() {
        val cipher = KeystoreTokenCipher()
        val plain = "t0ken".encodeToByteArray()
        val sealed = cipher.encrypt(plain)
        assertFalse(sealed.decodeToString().contains("t0ken"))
        assertArrayEquals(plain, cipher.decrypt(sealed))
    }
}
