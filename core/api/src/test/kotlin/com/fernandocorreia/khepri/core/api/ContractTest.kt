package com.fernandocorreia.khepri.core.api

import com.fernandocorreia.khepri.core.api.model.CheckInRequestDto
import com.fernandocorreia.khepri.core.api.model.CrewMessageDto
import com.fernandocorreia.khepri.core.api.model.ExecutionDto
import com.fernandocorreia.khepri.core.api.model.HelpfulRequestDto
import com.fernandocorreia.khepri.core.api.model.MealPlanIntakeStateDto
import com.fernandocorreia.khepri.core.api.model.ReportHelpfulRequestDto
import com.fernandocorreia.khepri.core.api.model.TargetWeightDto
import kotlin.reflect.KClass
import kotlin.reflect.full.createType
import kotlin.reflect.full.memberProperties
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows

class ContractTest {
    private val goldenDir = TestContract.contractDir.resolve("golden")

    @Test
    fun `every golden has a mapping and every mapping has a golden`() {
        val files =
            checkNotNull(goldenDir.list()).map { it.removeSuffix(".golden.json") }.toSet() - TestMapping.notApiResponses
        assertEquals(emptySet<String>(), files - TestMapping.entries.keys, "goldens without a mapping")
        assertEquals(emptySet<String>(), TestMapping.entries.keys - files, "mappings without a golden")
    }

    @TestFactory
    fun `every mapped golden decodes with strict JSON`(): List<DynamicTest> =
        TestMapping.entries.map { (golden, schema) ->
            DynamicTest.dynamicTest("$golden -> ${schema}Dto") {
                ApiJson.strict.decodeFromString(
                    TestContract.serializerFor(schema),
                    goldenDir.resolve("$golden.golden.json").readText(),
                )
            }
        }

    @Test
    fun `strict decoding rejects an unknown key and default decoding accepts it`() {
        val golden = goldenDir.resolve("telegram.golden.json").readText()
        val withExtra = golden.trimEnd().removeSuffix("}") + ",\"addedLater\":1}"
        val ser = TestContract.serializerFor("TelegramSettings")
        assertThrows<SerializationException> { ApiJson.strict.decodeFromString(ser, withExtra) }
        ApiJson.default.decodeFromString(ser, withExtra)
    }

    @Test
    fun `every nullable site in the spec is a nullable property`() {
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
            assertTrue(
                cls.memberProperties.single { it.name == name }.returnType.isMarkedNullable,
                "${cls.simpleName}.$name",
            )
        }
    }

    @Test
    fun `free-form objects are JsonObject`() {
        assertEquals(
            JsonObject::class.createType(),
            ExecutionDto::class.memberProperties.single { it.name == "arguments" }.returnType,
        )
    }

    @Test
    fun `uuid fields are plain strings`() {
        assertEquals(
            String::class.createType(nullable = true),
            CheckInRequestDto::class.memberProperties.single { it.name == "relatedGoalId" }.returnType,
        )
    }
}
