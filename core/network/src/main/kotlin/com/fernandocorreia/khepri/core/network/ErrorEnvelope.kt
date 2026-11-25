package com.fernandocorreia.khepri.core.network

import com.fernandocorreia.khepri.core.model.AppError
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/** The server's error body: `{"error":{"message","fields"?,"code"?}}`. */
internal data class ErrorEnvelope(val message: String?, val fields: Map<String, String>?, val code: String?)

private val lenient = Json { ignoreUnknownKeys = true }

private fun parseObject(body: String?): JsonObject? =
    body?.takeIf { it.isNotBlank() }?.let { runCatching { lenient.parseToJsonElement(it) as? JsonObject }.getOrNull() }

internal fun parseEnvelope(json: JsonObject?): ErrorEnvelope? {
    val error = json?.get("error") as? JsonObject ?: return null
    fun text(key: String) = (error[key] as? JsonPrimitive)?.contentOrNull
    val fields =
        (error["fields"] as? JsonObject)
            ?.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.contentOrNull?.let { k to it } }
            ?.toMap()
    return ErrorEnvelope(text("message"), fields, text("code"))
}

/** Maps a non-2xx response to an [AppError], in the order the iOS client checks it (docs/design.md 7.3). */
internal fun errorFor(status: Int, body: String?): AppError {
    val json = parseObject(body)
    val envelope = parseEnvelope(json)
    val message = envelope?.message
    return when {
        status == HTTP_UNAUTHORIZED -> AppError.Unauthorized(message)
        status == HTTP_NOT_FOUND -> AppError.NotFound(message)
        status == HTTP_CONFLICT && envelope != null -> AppError.Conflict(message, null)
        status == HTTP_CONFLICT && json != null -> AppError.Conflict(null, body)
        envelope?.fields != null -> AppError.FieldValidation(message.orEmpty(), envelope.fields)
        status == HTTP_FORBIDDEN && envelope?.code != null -> AppError.Refused(envelope.code, message)
        else -> AppError.Server(status, message, envelope?.code)
    }
}

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_CONFLICT = 409
