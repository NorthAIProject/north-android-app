package com.fernandocorreia.khepri.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.edit
import com.fernandocorreia.khepri.core.common.TokenUnreadableException
import com.fernandocorreia.khepri.core.model.Session
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** The signed-in session, sealed with a [TokenCipher]. */
public interface SessionStore {
    /**
     * Null when signed out. Throws `TokenUnreadableException` when stored data cannot be decrypted right now, and
     * leaves the data in place. Data whose key is gone for good is dropped and reads as signed out.
     */
    public suspend fun read(): Session?

    public suspend fun write(session: Session)

    public suspend fun clear()
}

public class DataStoreSessionStore(private val dataStore: DataStore<Preferences>, private val cipher: TokenCipher) :
    SessionStore {
    override suspend fun read(): Session? = dataStore.data.first()[KEY]?.let { open(it) }

    private suspend fun open(sealed: ByteArray): Session? {
        val plain =
            try {
                cipher.decrypt(sealed)
            } catch (e: CancellationException) {
                throw e
            } catch (expected: SealedDataLostException) {
                null
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                throw TokenUnreadableException(e)
            }
        return plain?.let { decode(it) }
            ?: run {
                clear()
                null
            }
    }

    override suspend fun write(session: Session) {
        val stored = Stored(session.token, session.userId, session.expiresAt.toEpochMilli())
        val sealed = cipher.encrypt(Json.encodeToString(Stored.serializer(), stored).encodeToByteArray())
        dataStore.edit { it[KEY] = sealed }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(KEY) }
    }

    private fun decode(plain: ByteArray): Session? =
        try {
            val stored = Json.decodeFromString(Stored.serializer(), plain.decodeToString())
            Session(stored.token, stored.userId, Instant.ofEpochMilli(stored.expiresAtMillis))
        } catch (expected: SerializationException) {
            null
        }

    @Serializable private data class Stored(val token: String, val userId: String, val expiresAtMillis: Long)

    private companion object {
        val KEY = byteArrayPreferencesKey("session")
    }
}
