package com.fernandocorreia.khepri.feature.auth.domain.model

/** iOS `AuthValidation.PasswordStrength`, weakest first. */
enum class PasswordStrength {
    VeryWeak,
    Weak,
    Fair,
    Good,
    Strong,
}

/** iOS `AuthValidation`: the email pattern, the sign-up password minimum and the five-step password strength. */
object AuthValidation {
    const val MIN_PASSWORD_LENGTH = 8
    private const val SYMBOLS = "!@#$%^&*()_+-=[]{}|;':\",.<>?/~`"
    private val email = Regex("^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}$")

    fun isValidEmail(value: String): Boolean = email.matches(value)

    /** Characters as the user sees them, not UTF-16 units, as Swift's `count`. */
    fun length(password: String): Int = password.codePointCount(0, password.length)

    fun strength(password: String): PasswordStrength {
        val score =
            listOf(
                    length(password) >= MIN_PASSWORD_LENGTH,
                    password.any(Char::isUpperCase),
                    password.any(Char::isLowerCase),
                    password.any(Char::isDigit),
                    password.any { it in SYMBOLS },
                )
                .count { it }
        return PasswordStrength.entries[(score - 1).coerceAtLeast(0)]
    }
}
