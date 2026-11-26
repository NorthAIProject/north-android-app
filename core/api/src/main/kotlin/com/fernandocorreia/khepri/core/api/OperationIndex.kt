package com.fernandocorreia.khepri.core.api

/**
 * One spec operation. [pathTemplate] has no `/api/v1` prefix. [isPublic] means the spec marks it `security: []` and no
 * token is sent (`logOut` is the exception: it sends the token it revokes). [isLong] means a 300 s read timeout.
 */
data class OperationEntry(
    val method: String,
    val pathTemplate: String,
    val operationId: String,
    val isPublic: Boolean,
    val isLong: Boolean,
)

/** Every operation in the pinned spec, including the event streams and multipart operations that are hand-written. */
object OperationIndex {
    val entries: List<OperationEntry> = generatedOperations

    private val segmented = entries.map { it to it.pathTemplate.trim('/').split('/') }

    /**
     * The operation for a request path without `/api/v1` and without a query. A `{param}` matches one segment; when two
     * templates match, the one with fewer parameters wins (`/goals/active` before `/goals/{id}`).
     */
    fun find(method: String, path: String): OperationEntry? {
        val segments = path.substringBefore('?').trim('/').split('/')
        val verb = method.uppercase()
        return segmented
            .filter { (entry, template) ->
                entry.method == verb &&
                    template.size == segments.size &&
                    template.indices.all { template[it].startsWith("{") || template[it] == segments[it] }
            }
            .minByOrNull { (_, template) -> template.count { it.startsWith("{") } }
            ?.first
    }
}
