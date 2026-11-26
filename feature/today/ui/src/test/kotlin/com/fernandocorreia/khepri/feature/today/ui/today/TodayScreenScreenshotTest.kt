package com.fernandocorreia.khepri.feature.today.ui.today

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.designsystem.theme.Appearance
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.screenshot.ScreenshotHarness
import com.fernandocorreia.khepri.core.ui.Loadable
import com.fernandocorreia.khepri.feature.today.ui.NOW
import com.fernandocorreia.khepri.feature.today.ui.TODAY
import com.fernandocorreia.khepri.feature.today.ui.day
import com.fernandocorreia.khepri.feature.today.ui.emptyToday
import com.fernandocorreia.khepri.feature.today.ui.today
import java.time.Duration
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class TodayScreenScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val shots = ScreenshotHarness(compose)
    private val content = TodayUiState(Loadable.Content(today), Loadable.Content(day()), date = TODAY)
    private var screen by mutableStateOf(content)
    private val zone = TimeZone.getDefault()

    @Before fun fixZone() = TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

    @After fun restoreZone() = TimeZone.setDefault(zone)

    private fun show() = shots.show { TodayContent(screen, onEvent = {}) }

    @Test
    fun `content in both appearances, large text and Portuguese`() {
        show()
        shots.capture("today_content_light")
        shots.appearance = Appearance.Dark
        shots.capture("today_content_dark")
        shots.appearance = Appearance.Light
        shots.fontScale = 2f
        shots.capture("today_content_2x")
        shots.fontScale = 1f
        shots.language = "pt-PT"
        shots.capture("today_content_pt_pt")
    }

    @Test
    fun `loading, empty, failed and stale`() {
        show()
        screen = TodayUiState(date = TODAY)
        shots.capture("today_loading")
        screen = content.copy(today = Loadable.Content(emptyToday))
        shots.capture("today_empty")
        screen = TodayUiState(Loadable.Failed(AppError.Network(interrupted = false)), date = TODAY)
        shots.capture("today_failed")
        screen = content.copy(stale = Staleness(NOW.minus(Duration.ofHours(11)), showDay = true))
        shots.capture("today_stale_yesterday")
        screen = content.copy(date = TODAY.minusDays(1), isToday = false)
        shots.capture("today_previous_day")
    }
}
