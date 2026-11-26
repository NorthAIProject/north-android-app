package com.fernandocorreia.khepri.core.network.multipart

import android.content.ContentResolver
import android.content.res.AssetFileDescriptor
import android.net.Uri
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

/** Streams a content URI (photo, video, audio, import file) into a request without loading it into memory. */
public class UriRequestBody(
    private val resolver: ContentResolver,
    private val uri: Uri,
    private val contentType: MediaType,
) : RequestBody() {
    private val length: Long by lazy {
        runCatching { resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull()
            ?: AssetFileDescriptor.UNKNOWN_LENGTH
    }

    override fun contentType(): MediaType = contentType

    override fun contentLength(): Long = length

    override fun writeTo(sink: BufferedSink) {
        openOrFail(uri) { resolver.openInputStream(uri) }.source().use { sink.writeAll(it) }
    }
}

/**
 * Opens a content stream, turning every failure into an [IOException]. A `SecurityException` (a picker grant that
 * expired after process death) thrown on OkHttp's dispatcher thread would otherwise crash the app.
 */
internal fun openOrFail(uri: Any, open: () -> InputStream?): InputStream =
    try {
        open() ?: throw FileNotFoundException("Cannot open $uri")
    } catch (e: IOException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
        throw IOException("Cannot read $uri", e)
    }
