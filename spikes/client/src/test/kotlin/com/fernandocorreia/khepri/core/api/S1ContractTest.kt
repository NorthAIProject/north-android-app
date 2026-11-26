package com.fernandocorreia.khepri.core.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fernandocorreia.khepri.core.api.model.CheckInRequestDto
import com.fernandocorreia.khepri.core.api.model.CrewMessageDto
import com.fernandocorreia.khepri.core.api.model.ExecutionDto
import com.fernandocorreia.khepri.core.api.model.HelpfulRequestDto
import com.fernandocorreia.khepri.core.api.model.LiftSetInputDto
import com.fernandocorreia.khepri.core.api.model.MealPlanIntakeStateDto
import com.fernandocorreia.khepri.core.api.model.MealPlanTypeDto
import com.fernandocorreia.khepri.core.api.model.ReportHelpfulRequestDto
import com.fernandocorreia.khepri.core.api.model.TargetWeightDto
import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.SpecificationVersion
import java.io.File
import java.time.Instant
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class S1ContractTest {
    private val contractDir = File(System.getProperty("contract.dir"))
    private val normalizedSpec = File(System.getProperty("normalized.spec"))

    // Step 4: every nullable site in the spec is a nullable Kotlin property.
    @Test
    fun nullableSitesAreNullable() {
        val sites: List<Pair<KClass<*>, String>> =
            listOf(
                CrewMessageDto::class to "author",
                CrewMessageDto::class to "replyTo",
                MealPlanIntakeStateDto::class to "intake",
                TargetWeightDto::class to "targetWeightKg",
                HelpfulRequestDto::class to "helpful",
                ReportHelpfulRequestDto::class to "helpful",
            )
        sites.forEach { (cls, name) ->
            val prop = cls.memberProperties.single { it.name == name }
            assertTrue(prop.returnType.isMarkedNullable, "${cls.simpleName}.$name should be nullable")
        }
    }

    // Step 5: an unknown enum value decodes to UNKNOWN, nested and top-level.
    @Test
    fun unknownEnumValuesDecodeToUnknown() {
        val nested = ApiJson.strict.decodeFromString(ExecutionDto.Outcome.serializer(), "\"retried\"")
        assertEquals(ExecutionDto.Outcome.UNKNOWN, nested)
        val topLevel = ApiJson.strict.decodeFromString(MealPlanTypeDto.serializer(), "\"keto\"")
        assertEquals(MealPlanTypeDto.UNKNOWN, topLevel)
        assertEquals(ExecutionDto.Outcome.EXECUTED, ApiJson.strict.decodeFromString(ExecutionDto.Outcome.serializer(), "\"executed\""))
    }

    // Step 6: timestamps with and without fractions and offsets.
    @Test
    fun instantsDecodeAllRfc3339Forms() {
        fun decode(s: String) = ApiJson.strict.decodeFromString(FlexibleInstantSerializer, "\"$s\"")
        assertEquals(Instant.parse("2026-09-24T07:15:00Z"), decode("2026-09-24T07:15:00Z"))
        assertEquals(Instant.parse("2026-09-24T07:15:00.123456789Z"), decode("2026-09-24T07:15:00.123456789Z"))
        assertEquals(Instant.parse("2026-09-24T06:15:00Z"), decode("2026-09-24T07:15:00+01:00"))
        assertEquals(
            "\"2026-09-24T07:15:00.123Z\"",
            ApiJson.strict.encodeToString(FlexibleInstantSerializer, Instant.parse("2026-09-24T07:15:00.123456789Z")),
        )
    }

    // Step 7: every golden the iOS contract tests map decodes with strict JSON.
    @TestFactory
    fun goldensDecodeStrictly(): List<DynamicTest> =
        File(contractDir, "golden-map.txt").readLines().filter { it.isNotBlank() }.map { line ->
            val (name, type) = line.trim().split(" ")
            DynamicTest.dynamicTest("$name -> ${type}Dto") {
                val cls = Class.forName("com.fernandocorreia.khepri.core.api.model.${type}Dto").kotlin
                @Suppress("UNCHECKED_CAST")
                val ser = serializer(cls.createType()) as KSerializer<Any>
                ApiJson.strict.decodeFromString(ser, File(contractDir, "golden/$name.golden.json").readText())
            }
        }

    // Step 8: requests carry only declared keys, no nulls, and validate against their schema.
    @Test
    fun liftSetInputMatchesItsClosedSchema() {
        val body =
            LiftSetInputDto(
                exerciseName = "Back squat",
                setNumber = 1,
                weightKg = 100.0,
                reps = 5,
                performedAt = Instant.parse("2026-09-24T07:15:00Z"),
                clientId = UUID.fromString("11111111-1111-1111-1111-111111111111"),
                kind = LiftSetInputDto.Kind.entries.first { it != LiftSetInputDto.Kind.UNKNOWN },
            )
        assertRequestMatches("LiftSetInput", ApiJson.strict.encodeToString(LiftSetInputDto.serializer(), body))
    }

    @Test
    fun checkInRequestMatchesItsSchema() {
        val body =
            CheckInRequestDto(
                mood = 4,
                energy = 3,
                wins = "Long run",
                relatedGoalId = UUID.fromString("22222222-2222-2222-2222-222222222222"),
                tags = listOf("travel"),
                source = "android",
            )
        val json = ApiJson.strict.encodeToString(CheckInRequestDto.serializer(), body)
        assertTrue(!json.contains("null"), "no nulls on the wire: $json")
        assertRequestMatches("CheckInRequest", json)
    }

    private val specJson: String by lazy {
        ObjectMapper().writeValueAsString(YAMLMapper().readTree(normalizedSpec))
    }

    private fun assertRequestMatches(schemaName: String, json: String) {
        val declared =
            ObjectMapper().readTree(specJson).path("components").path("schemas").path(schemaName).path("properties")
                .fieldNames().asSequence().toSet()
        val sent = ApiJson.strict.parseToJsonElement(json).jsonObject.keys
        assertTrue(declared.containsAll(sent), "undeclared keys ${sent - declared} in $json")

        val uri = "https://contract.khepri.test/openapi.json"
        val registry =
            SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12) { it.schemas(mapOf(uri to specJson)) }
        val schema = registry.getSchema(SchemaLocation.of("$uri#/components/schemas/$schemaName"))
        val errors = schema.validate(json, InputFormat.JSON)
        assertTrue(errors.isEmpty(), "schema errors for $schemaName: $errors in $json")
    }

    @Test
    fun strictDecodingRejectsUnknownKeysAndTolerantAcceptsThem() {
        val golden = File(contractDir, "golden/telegram.golden.json").readText()
        val withExtra = golden.trimEnd().removeSuffix("}") + ",\"addedLater\":1}"
        val ser = serializer(Class.forName("com.fernandocorreia.khepri.core.api.model.TelegramSettingsDto").kotlin.createType())
        val failure = runCatching { ApiJson.strict.decodeFromString(ser, withExtra) }.exceptionOrNull()
        assertTrue(failure is kotlinx.serialization.SerializationException, "strict must reject unknown keys, got $failure")
        ApiJson.tolerant.decodeFromString(ser, withExtra)
    }

    @Test
    fun requestCheckCatchesUndeclaredKeys() {
        val bad = """{"mood":4,"energy":3,"mystery":true}"""
        val failure = runCatching { assertRequestMatches("CheckInRequest", bad) }.exceptionOrNull()
        assertTrue(failure is AssertionError, "an undeclared key must fail the request check")
        // Only declared keys, so this reaches the schema validator; it must still fail (required `reps` missing).
        val missingRequired = """{"exerciseName":"Squat","setNumber":1,"weightKg":100}"""
        val schemaFailure = runCatching { assertRequestMatches("LiftSetInput", missingRequired) }.exceptionOrNull()
        assertTrue(schemaFailure?.message?.contains("schema errors") == true, "validator must bite: $schemaFailure")
    }

    @Test
    fun freeFormObjectsAreJsonObject() {
        val arguments = ExecutionDto::class.memberProperties.single { it.name == "arguments" }
        assertEquals(JsonObject::class.createType(), arguments.returnType)
        assertEquals(String.serializer().descriptor.kind, FlexibleInstantSerializer.descriptor.kind)
    }
}
