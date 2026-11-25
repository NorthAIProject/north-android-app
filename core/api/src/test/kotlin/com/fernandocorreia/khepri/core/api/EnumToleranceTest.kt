package com.fernandocorreia.khepri.core.api

import kotlin.reflect.full.createType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class EnumToleranceTest {
    private val enums: List<Class<*>> =
        TestContract.modelClasses.flatMap { listOf(it) + it.declaredClasses }.filter { it.isEnum }

    /** Model enums at the pinned contract (the 9 query and path enums live in the endpoint interfaces). */
    @Test fun `55 model enums are generated`() = assertEquals(55, enums.size, enums.joinToString { it.name })

    @TestFactory
    fun `every model enum has UNKNOWN and decodes an unknown value to it`(): List<DynamicTest> = enums.map { cls ->
        DynamicTest.dynamicTest(cls.name.substringAfterLast('.')) {
            val unknown = cls.enumConstants.single { (it as Enum<*>).name == UNKNOWN_ENUM }
            @Suppress("UNCHECKED_CAST") val ser = serializer(cls.kotlin.createType()) as KSerializer<Any>
            assertEquals(unknown, ApiJson.strict.decodeFromString(ser, "\"definitely-not-a-value\""))
        }
    }
}
