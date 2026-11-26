package com.fernandocorreia.khepri.core.datastore

/** Seals the stored session. Production uses an AES-GCM key in the Android Keystore; JVM tests use a fake. */
public interface TokenCipher {
    public fun encrypt(plain: ByteArray): ByteArray

    /**
     * Throws [SealedDataLostException] when the data can never be opened again (key gone or replaced). Any other
     * exception means "not right now".
     */
    public fun decrypt(sealed: ByteArray): ByteArray
}

/** The key that sealed the data no longer exists, so the data is useless and can be dropped. */
public class SealedDataLostException(cause: Throwable? = null) : Exception(cause)
