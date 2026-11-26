package com.fernandocorreia.khepri.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AccountStateNeedsCleanerTest {
    private val rule = AccountStateNeedsCleaner(Config.empty)

    @Test
    fun `flags a singleton holding a MutableStateFlow with no cleaner`() {
        val code =
            """
            @Singleton
            class Store @Inject constructor() {
                private val state = MutableStateFlow(0)
            }
            """
        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags the typed and qualified shapes and mutableMapOf`() {
        val code =
            """
            @Singleton
            class Store @Inject constructor() {
                private val queue = MutableStateFlow<List<String>>(emptyList())
                private val flow = kotlinx.coroutines.flow.MutableStateFlow(0)
                private val days = mutableMapOf<String, Int>()
            }
            """
        assertEquals(3, rule.lint(code).size)
    }

    @Test
    fun `passes a singleton that is a SignOutCleaner`() {
        val code =
            """
            @Singleton
            class Store @Inject constructor() : Repository, SignOutCleaner {
                private val state = MutableStateFlow(0)
            }
            """
        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `passes a class that is not a singleton`() {
        assertEquals(0, rule.lint("class Model { private val state = MutableStateFlow(0) }").size)
    }

    @Test
    fun `passes a singleton with the suppression`() {
        val code =
            """
            // Holds only the app phase, which is not account data.
            @Suppress("AccountStateNeedsCleaner")
            @Singleton
            class Session @Inject constructor() {
                private val phase = MutableStateFlow(0)
            }
            """
        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `flags state behind a delegate, a call chain, a declared type and a var`() {
        val code =
            """
            @Singleton
            class Store @Inject constructor(private val source: Source) {
                private val lazyState by lazy { MutableStateFlow(0) }
                private val chained = MutableStateFlow(0).also { it.value = 1 }
                private val typed: MutableMap<String, Int> = source.map()
                @Volatile private var last: String? = null
                private val fixed = listOf(1, 2)
            }
            """
        assertEquals(4, rule.lint(code).size)
    }
}
