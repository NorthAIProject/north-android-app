package com.fernandocorreia.khepri.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoNotNullAssertionTest {
    private val rule = NoNotNullAssertion(Config.empty)

    @Test
    fun `reports a not-null assertion`() {
        assertEquals(1, rule.lint("fun f(value: String?) = value!!.length").size)
    }

    @Test
    fun `accepts a safe call`() {
        assertEquals(0, rule.lint("fun f(value: String?) = value?.length ?: 0").size)
    }
}
