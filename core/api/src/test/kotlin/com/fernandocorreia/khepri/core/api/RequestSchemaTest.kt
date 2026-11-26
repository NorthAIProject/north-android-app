package com.fernandocorreia.khepri.core.api

import com.fasterxml.jackson.databind.JsonNode
import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * Request bodies: every request DTO, with every property filled at every depth, sends only keys its schema declares and
 * validates against the schema. Responses: every response DTO decodes from its required properties alone.
 */
class RequestSchemaTest {
    private val registry by lazy {
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12) {
            it.schemas(mapOf(BASE to TestContract.specJson))
        }
    }

    @Test
    fun `the spec has request and response bodies to check`() {
        assertTrue(TestContract.requestSchemas.size > 50, "${TestContract.requestSchemas.size} request schemas")
        assertTrue(TestContract.responseSchemas.size > 100, "${TestContract.responseSchemas.size} response schemas")
    }

    @TestFactory
    fun `a fully populated request sends only declared keys and validates`(): List<DynamicTest> =
        TestContract.requestSchemas.map { name ->
            DynamicTest.dynamicTest(name) {
                val ser = TestContract.serializerFor(name)
                val schema = TestContract.schemas.path(name)
                val value =
                    ApiJson.strict.decodeFromJsonElement(
                        ser,
                        sample(ser.descriptor, schema, requiredOnly = false, depth = 0),
                    )
                val sent = JsonConverterFactory.encodeRequest(ser, value)
                val undeclared = undeclaredKeys(sent, schema, "")
                assertEquals(emptyList<String>(), undeclared, "undeclared keys in $sent")
                assertValid(name, sent.toString())
            }
        }

    @TestFactory
    fun `a response decodes from its required properties alone`(): List<DynamicTest> =
        TestContract.responseSchemas.map { name ->
            DynamicTest.dynamicTest(name) {
                val ser: KSerializer<Any?> = TestContract.serializerFor(name)
                ApiJson.strict.decodeFromJsonElement(
                    ser,
                    sample(ser.descriptor, TestContract.schemas.path(name), requiredOnly = true, depth = 0),
                )
            }
        }

    @Test
    fun `the key check and the validator both bite`() {
        val schema = TestContract.schemas.path("CheckInRequest")
        val withMystery = JsonObject(mapOf("mood" to JsonPrimitive(4), "mystery" to JsonPrimitive(true)))
        assertEquals(listOf("mystery"), undeclaredKeys(withMystery, schema, ""))
        val nested = JsonObject(mapOf("sets" to JsonArray(listOf(JsonObject(mapOf("mystery" to JsonPrimitive(1)))))))
        val nestedSchema =
            com.fasterxml.jackson.databind
                .ObjectMapper()
                .readTree(
                    """{"type":"object","properties":{"sets":{"type":"array",""" +
                        """"items":{"type":"object","properties":{"reps":{}}}}}}"""
                )
        assertEquals(listOf("sets.0.mystery"), undeclaredKeys(nested, nestedSchema, ""))
        val errors =
            registry
                .getSchema(SchemaLocation.of("$BASE#/components/schemas/LiftSetInput"))
                .validate("""{"exerciseName":"Squat","setNumber":1,"weightKg":100}""", InputFormat.JSON)
        assertTrue(errors.isNotEmpty(), "missing required reps must fail")
    }

    private fun assertValid(name: String, json: String) {
        val errors =
            registry.getSchema(SchemaLocation.of("$BASE#/components/schemas/$name")).validate(json, InputFormat.JSON)
        assertTrue(errors.isEmpty(), "schema errors for $name: $errors in $json")
    }

    private fun undeclaredKeys(element: JsonElement, rawSchema: JsonNode?, path: String): List<String> {
        val schema = TestContract.resolve(rawSchema) ?: return emptyList()
        return when (element) {
            is JsonObject -> {
                val properties = schema.path("properties")
                if (properties.isMissingNode || properties.isEmpty) {
                    emptyList()
                } else {
                    element.flatMap { (key, child) ->
                        if (!properties.has(key)) listOf("$path$key")
                        else undeclaredKeys(child, properties.get(key), "$path$key.")
                    }
                }
            }
            is JsonArray ->
                element.flatMapIndexed { i, child -> undeclaredKeys(child, schema.get("items"), "$path$i.") }
            else -> emptyList()
        }
    }

    private fun sample(
        descriptor: SerialDescriptor,
        rawSchema: JsonNode?,
        requiredOnly: Boolean,
        depth: Int,
    ): JsonElement {
        val schema = TestContract.resolve(rawSchema)
        if (descriptor.serialName.startsWith("kotlinx.serialization.json.")) return JsonObject(emptyMap())
        return when (descriptor.kind) {
            StructureKind.CLASS,
            StructureKind.OBJECT -> {
                val properties = schema?.path("properties")
                val required = schema?.path("required")?.map { it.asText() }?.toSet().orEmpty()
                JsonObject(
                    (0 until descriptor.elementsCount)
                        .map { descriptor.getElementName(it) to it }
                        .filter { (name, i) -> !requiredOnly || name in required || !descriptor.isElementOptional(i) }
                        .filter { (_, i) -> depth < MAX_DEPTH || !descriptor.isElementOptional(i) }
                        .associate { (name, i) ->
                            val child = descriptor.getElementDescriptor(i)
                            name to sample(child, properties?.get(name), requiredOnly, depth + 1)
                        }
                )
            }
            StructureKind.LIST ->
                if (requiredOnly) JsonArray(emptyList())
                else
                    JsonArray(
                        listOf(
                            sample(descriptor.getElementDescriptor(0), schema?.get("items"), requiredOnly, depth + 1)
                        )
                    )
            StructureKind.MAP ->
                if (requiredOnly) JsonObject(emptyMap())
                else
                    JsonObject(
                        mapOf(
                            "k" to
                                sample(
                                    descriptor.getElementDescriptor(1),
                                    schema?.get("additionalProperties"),
                                    requiredOnly,
                                    depth + 1,
                                )
                        )
                    )
            SerialKind.ENUM ->
                JsonPrimitive(
                    (0 until descriptor.elementsCount)
                        .map { descriptor.getElementName(it) }
                        .first { it != UNKNOWN_WIRE_VALUE && it.isNotEmpty() }
                )
            SerialKind.CONTEXTUAL ->
                if (schema?.path("type")?.asText() == "object") JsonObject(emptyMap())
                else JsonPrimitive(stringFor(schema, descriptor))
            PrimitiveKind.STRING -> JsonPrimitive(stringFor(schema, descriptor))
            PrimitiveKind.INT,
            PrimitiveKind.LONG,
            PrimitiveKind.SHORT,
            PrimitiveKind.BYTE -> JsonPrimitive(schema?.get("minimum")?.asLong()?.coerceAtLeast(1) ?: 1L)
            PrimitiveKind.DOUBLE,
            PrimitiveKind.FLOAT -> JsonPrimitive(schema?.get("minimum")?.asDouble()?.coerceAtLeast(1.0) ?: 1.0)
            PrimitiveKind.BOOLEAN -> JsonPrimitive(true)
            is PolymorphicKind -> JsonObject(emptyMap())
            else -> JsonNull
        }
    }

    private fun stringFor(schema: JsonNode?, descriptor: SerialDescriptor): String {
        schema
            ?.get("enum")
            ?.firstOrNull { it.isTextual && it.asText().isNotEmpty() }
            ?.let {
                return it.asText()
            }
        val format = schema?.path("format")?.asText().orEmpty()
        return when {
            format == "date-time" || descriptor.serialName == "FlexibleInstant" -> "2026-09-24T07:15:00.000Z"
            format == "date" || descriptor.serialName == "LocalDate" -> "2026-09-24"
            format == "uuid" -> "11111111-1111-4111-8111-111111111111"
            format == "email" -> "someone@example.com"
            format == "uri" -> "https://example.com/a"
            else -> "x".repeat(schema?.get("minLength")?.asInt()?.coerceAtLeast(1) ?: 1)
        }
    }

    private companion object {
        const val BASE = "https://contract.khepri.test/openapi.json"
        const val MAX_DEPTH = 6
    }
}
