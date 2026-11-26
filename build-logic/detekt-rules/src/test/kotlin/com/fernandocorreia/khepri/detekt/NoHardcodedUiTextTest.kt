package com.fernandocorreia.khepri.detekt

import dev.detekt.api.Config
import dev.detekt.test.lint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NoHardcodedUiTextTest {
    private val rule = NoHardcodedUiText(Config.empty)

    @Test
    fun `reports a literal in Text`() {
        assertEquals(1, rule.lint("""fun Screen() { Text("Hi") }""").size)
    }

    @Test
    fun `reports a literal passed as text, contentDescription or label`() {
        val code =
            """
            fun Screen() {
                Text(text = "Hi")
                Icon(icon, contentDescription = "Close")
                NavigationBarItem(label = "Today")
            }
            """
        assertEquals(3, rule.lint(code).size)
    }

    @Test
    fun `accepts a string resource`() {
        assertEquals(0, rule.lint("""fun Screen() { Text(stringResource(R.string.x)) }""").size)
    }

    @Test
    fun `accepts a literal inside a Preview function`() {
        assertEquals(0, rule.lint("""@Preview fun ScreenPreview() { Text("Hi") }""").size)
    }

    @Test
    fun `ignores a literal that is not UI text`() {
        assertEquals(0, rule.lint("""fun Screen() { Text(value, modifier = Modifier.testTag("coach-orb")) }""").size)
    }
}
