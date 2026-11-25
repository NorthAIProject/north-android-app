package com.fernandocorreia.khepri.core.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import java.io.File
import kotlin.reflect.full.createType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/** The committed contract, the normalised spec and the generated classes, as the tests see them. */
object TestContract {
    val contractDir = File(System.getProperty("contract.dir"))
    private val modelsDir = File(System.getProperty("generated.models"))
    val endpointsDir = File(System.getProperty("generated.endpoints"))

    val spec: JsonNode by lazy { YAMLMapper().readTree(File(System.getProperty("normalized.spec"))) }
    val specJson: String by lazy { ObjectMapper().writeValueAsString(spec) }
    val schemas: JsonNode
        get() = spec.path("components").path("schemas")

    const val MODEL_PACKAGE = "com.fernandocorreia.khepri.core.api.model"

    /** Every generated model class, top level only. */
    val modelClasses: List<Class<*>> by lazy {
        modelsDir
            .listFiles { f -> f.extension == "kt" }
            .orEmpty()
            .sorted()
            .map { Class.forName("$MODEL_PACKAGE.${it.nameWithoutExtension}") }
    }

    @Suppress("UNCHECKED_CAST")
    fun serializerFor(schemaName: String): KSerializer<Any?> =
        serializer(Class.forName("$MODEL_PACKAGE.${schemaName}Dto").kotlin.createType()) as KSerializer<Any?>

    /** Follows `$ref`, merges `allOf`, and takes the non-null branch of `anyOf`. */
    fun resolve(node: JsonNode?): JsonNode? {
        val ref = node?.get("\$ref")
        val anyOf = node?.get("anyOf")
        return when {
            node == null || node.isMissingNode -> null
            ref != null -> resolve(schemas.path(ref.asText().substringAfterLast('/')))
            anyOf != null -> resolve(anyOf.firstOrNull { it.path("type").asText() != "null" })
            node.has("allOf") -> mergeAllOf(node as ObjectNode)
            else -> node
        }
    }

    private fun mergeAllOf(node: ObjectNode): JsonNode {
        val merged = node.deepCopy().apply { remove("allOf") }
        val properties = merged.withObject("properties")
        val required = merged.withArray("required")
        node
            .get("allOf")
            .mapNotNull { resolve(it) }
            .forEach { part ->
                part.path("properties").properties().forEach { (k, v) -> properties.set<JsonNode>(k, v) }
                part.path("required").forEach { required.add(it) }
                if (!merged.has("type") && part.has("type")) merged.set<JsonNode>("type", part.get("type"))
            }
        return merged
    }

    /** Schema names used as a JSON request body. */
    val requestSchemas: Set<String> by lazy { bodySchemas { op -> listOf(op.path("requestBody")) } }

    /** Schema names returned in a 2xx JSON response. */
    val responseSchemas: Set<String> by lazy {
        bodySchemas { op -> op.path("responses").properties().filter { it.key.startsWith("2") }.map { it.value } }
    }

    private fun bodySchemas(bodies: (JsonNode) -> List<JsonNode>): Set<String> =
        spec
            .path("paths")
            .properties()
            .flatMap { (_, item) -> item.properties().map { it.value } }
            .flatMap(bodies)
            .mapNotNull { it.path("content").path("application/json").path("schema").path("\$ref").asText(null) }
            .map { it.substringAfterLast('/') }
            .toSortedSet()
}
