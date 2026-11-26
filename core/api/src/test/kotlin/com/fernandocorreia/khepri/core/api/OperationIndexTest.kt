package com.fernandocorreia.khepri.core.api

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OperationIndexTest {
    private fun public(method: String, path: String) = OperationIndex.find(method, path)?.isPublic

    @Test
    fun `sign-in operations are public`() {
        assertEquals(true, public("POST", "/auth/login"))
        listOf("/auth/signup", "/auth/google", "/auth/apple", "/auth/forgot-password").forEach {
            assertEquals(true, public("POST", it), it)
        }
        listOf("register/begin", "register/finish", "login/begin", "login/finish").forEach {
            assertEquals(true, public("POST", "/auth/passkey/$it"), it)
        }
    }

    @Test fun `logout sends the token it revokes`() = assertEquals(false, public("POST", "/auth/logout"))

    @Test
    fun `templates match one segment and ignore the query`() {
        assertEquals("getConversation", OperationIndex.find("GET", "/conversations/3f1c2a")?.operationId)
        assertEquals("getTrainingWeek", OperationIndex.find("get", "/training/week?week=next")?.operationId)
        assertNull(OperationIndex.find("GET", "/conversations/3f1c2a/extra/deep"))
    }

    @Test
    fun `long operations are marked, including hand-written multipart ones`() {
        assertEquals(true, OperationIndex.find("POST", "/training/plans")?.isLong)
        assertEquals(8, OperationIndex.entries.count { it.isLong })
        assertEquals(390, OperationIndex.entries.size)
    }

    @Test
    fun `a literal segment wins over a template`() {
        val ambiguous =
            OperationIndex.entries
                .filter { e -> !e.pathTemplate.contains('{') }
                .firstOrNull { literal ->
                    OperationIndex.entries.any { t ->
                        t !== literal &&
                            t.method == literal.method &&
                            t.pathTemplate.contains('{') &&
                            t.pathTemplate.split('/').size == literal.pathTemplate.split('/').size &&
                            t.pathTemplate.split('/').zip(literal.pathTemplate.split('/')).all { (a, b) ->
                                a.startsWith("{") || a == b
                            }
                    }
                }
        if (ambiguous != null) assertEquals(ambiguous, OperationIndex.find(ambiguous.method, ambiguous.pathTemplate))
    }

    @Test fun `an unknown path is not found`() = assertNull(OperationIndex.find("GET", "/nope"))
}
