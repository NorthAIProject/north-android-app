package com.fernandocorreia.khepri.feature.today.ui.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.screenshot.ScreenshotHarness
import com.fernandocorreia.khepri.feature.today.ui.emptyToday
import com.fernandocorreia.khepri.feature.today.ui.today
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class StatGridTest {
    @get:Rule val compose = createComposeRule()

    private val shots = ScreenshotHarness(compose)

    @Test
    fun `two columns at normal text, one from font scale 1_5`() {
        shots.show { StatGrid(today, onCheckIn = {}) }
        compose.onNodeWithTag("stat-grid-two-columns").assertExists()
        shots.fontScale = 1.5f
        compose.onNodeWithTag("stat-grid-one-column").assertExists()
        compose.onNodeWithTag("stat-grid-two-columns").assertDoesNotExist()
    }

    @Test
    fun `the check-in tile opens the check-in, done or not`() {
        var opened = 0
        shots.show { StatGrid(emptyToday, onCheckIn = { opened++ }) }
        compose.onNodeWithTag("stat-check-in").performClick()
        assertEquals(1, opened)
    }
}
