package com.fernandocorreia.khepri.core.model

/** Every failure a repository can report. Nothing throws across a layer boundary; it becomes one of these. */
public sealed interface AppError {
    public data class Unauthorized(val message: String?) : AppError

    public data class NotFound(val message: String?) : AppError

    /** The server holds a newer version; [newerVersionJson] is its body when the server sent one. */
    public data class Conflict(val message: String?, val newerVersionJson: String?) : AppError

    /** A 422 with per-field messages, keyed by the request field name. */
    public data class FieldValidation(val message: String, val fields: Map<String, String>) : AppError

    public data class Server(val status: Int, val message: String?, val code: String?) : AppError

    /** The server understood the request and declined it for a known reason, named by [code]. */
    public data class Refused(val code: String, val message: String?) : AppError

    /** No response. [interrupted] is true when the connection dropped after the request was sent. */
    public data class Network(val interrupted: Boolean) : AppError

    /** A stored token exists but cannot be read now (Keystore failure). The user stays signed in. */
    public data object Locked : AppError

    /** The response could not be decoded. */
    public data object InvalidResponse : AppError
}
