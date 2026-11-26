package com.fernandocorreia.khepri.shell

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScopedViewModelsTest {
    @get:Rule val compose = createComposeRule()

    class Probe : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }

    @Test
    fun `the screens' ViewModels live until the screens leave, then go with the store`() {
        val store = ViewModelStore()
        val shown = mutableStateOf(true)
        val tick = mutableIntStateOf(0)
        val made = mutableListOf<Probe>()
        compose.setContent {
            if (shown.value) {
                tick.intValue
                ScopedViewModels(store) { made += viewModel<Probe>() }
            }
        }
        compose.waitForIdle()
        tick.intValue++
        compose.waitForIdle()
        val first = made.first()
        assertSame(first, made.last())
        assertFalse(first.cleared)

        shown.value = false
        compose.waitForIdle()
        assertTrue(first.cleared)

        shown.value = true
        compose.waitForIdle()
        assertNotSame(first, made.last())
    }
}
