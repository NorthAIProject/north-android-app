package com.fernandocorreia.khepri.core.network.multipart

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UriRequestBodyTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val jpeg = "image/jpeg".toMediaType()

    @Test
    fun `streams the bytes and reports the known length`() {
        val bytes = ByteArray(SIZE) { it.toByte() }
        val file = File.createTempFile("photo", ".jpg", context.cacheDir).apply { writeBytes(bytes) }
        val body = UriRequestBody(context.contentResolver, Uri.fromFile(file), jpeg)
        val sink = Buffer()
        body.writeTo(sink)
        assertArrayEquals(bytes, sink.readByteArray())
        assertEquals(SIZE.toLong(), body.contentLength())
        assertEquals(jpeg, body.contentType())
    }

    @Test
    fun `reports -1 when the resolver does not know the length`() {
        val body = UriRequestBody(context.contentResolver, Uri.parse("content://nobody/photo"), jpeg)
        assertEquals(-1L, body.contentLength())
    }

    private companion object {
        const val SIZE = 70_000
    }
}
