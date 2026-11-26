package com.fernandocorreia.khepri.feature.auth.domain

import com.fernandocorreia.khepri.feature.auth.domain.model.AuthValidation
import com.fernandocorreia.khepri.feature.auth.domain.model.PasswordStrength
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthValidationTest {
    @Test
    fun `emails follow the iOS pattern`() {
        assertTrue(AuthValidation.isValidEmail("ana.silva+khepri@example.co"))
        assertFalse(AuthValidation.isValidEmail("ana@example"))
        assertFalse(AuthValidation.isValidEmail("ana @example.com"))
        assertFalse(AuthValidation.isValidEmail(" ana@example.com"))
    }

    @Test
    fun `strength counts length, cases, digits and symbols as iOS`() {
        assertEquals(PasswordStrength.VeryWeak, AuthValidation.strength(""))
        assertEquals(PasswordStrength.VeryWeak, AuthValidation.strength("abc"))
        assertEquals(PasswordStrength.Weak, AuthValidation.strength("abcdefgh"))
        assertEquals(PasswordStrength.Fair, AuthValidation.strength("Abcdefgh"))
        assertEquals(PasswordStrength.Good, AuthValidation.strength("Abcdefg1"))
        assertEquals(PasswordStrength.Strong, AuthValidation.strength("Abcdef1!"))
    }

    @Test
    fun `length counts characters, not UTF-16 units`() {
        assertEquals(8, AuthValidation.length("pass🔑wor"))
    }
}
