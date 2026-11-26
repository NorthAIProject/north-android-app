package com.fernandocorreia.khepri.core.network.multipart

import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class OpenOrFailTest {
    @Test
    fun `an expired grant becomes an IOException`() {
        val failure = assertThrows<IOException> { openOrFail("content://x") { throw SecurityException("no grant") } }
        assertTrue(failure.cause is SecurityException)
    }

    @Test
    fun `a missing stream is FileNotFound`() {
        assertThrows<FileNotFoundException> { openOrFail("content://x") { null } }
    }

    @Test
    fun `a stream passes through`() {
        assertEquals(1, openOrFail("content://x") { ByteArrayInputStream(byteArrayOf(1)) }.read())
    }
}
