package com.fernandocorreia.khepri.core.network

import com.fernandocorreia.khepri.core.api.OperationEntry
import com.fernandocorreia.khepri.core.api.OperationIndex
import okhttp3.Request

private const val API_PREFIX = "/api/v1"

/** The spec operation this request calls, or null for a path outside the API. */
internal fun Request.operation(): OperationEntry? {
    val path = url.encodedPath
    if (!path.startsWith(API_PREFIX)) return null
    return OperationIndex.find(method, path.removePrefix(API_PREFIX))
}
