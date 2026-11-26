package com.fernandocorreia.khepri.core.common

import app.cash.turbine.test
import com.fernandocorreia.khepri.core.model.DataScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DataChangeBusTest {
    private val bus = SharedFlowDataChangeBus()

    @Test
    fun `two active collectors both receive a change`() = runTest {
        val first = mutableListOf<DataScope>()
        val second = mutableListOf<DataScope>()
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val a = launch(dispatcher) { bus.changes.collect { first += it } }
        val b = launch(dispatcher) { bus.changes.collect { second += it } }

        bus.changed(DataScope.Day)

        assertEquals(listOf(DataScope.Day), first)
        assertEquals(listOf(DataScope.Day), second)
        a.cancel()
        b.cancel()
    }

    @Test
    fun `a collector that subscribes after the change does not receive it`() = runTest {
        bus.changed(DataScope.Day)
        bus.changes.test { expectNoEvents() }
    }

    @Test
    fun `a change with no collector returns at once`() {
        repeat(1_000) { bus.changed(DataScope.Training) }
    }

    @Test
    fun `a slow collector never stops the newest change from arriving`() = runTest {
        val seen = mutableListOf<DataScope>()
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val collector =
            launch(UnconfinedTestDispatcher(testScheduler)) {
                bus.changes.collect {
                    seen += it
                    gate.await()
                }
            }
        repeat(200) { bus.changed(DataScope.Training) }
        bus.changed(DataScope.Goals)
        gate.complete(Unit)
        testScheduler.advanceUntilIdle()
        assertEquals(DataScope.Goals, seen.last())
        collector.cancel()
    }

    @Test
    fun `reloads keeps only the wanted scopes and folds a burst into one`() = runTest {
        var reloads = 0
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val collector =
            launch(UnconfinedTestDispatcher(testScheduler)) {
                bus.reloads(DataScope.Day, DataScope.Goals).collect {
                    reloads++
                    gate.await()
                }
            }
        bus.changed(DataScope.Social)
        assertEquals(0, reloads)
        bus.changed(DataScope.Day)
        assertEquals(1, reloads)
        repeat(10) { bus.changed(DataScope.Goals) }
        gate.complete(Unit)
        testScheduler.advanceUntilIdle()
        assertEquals(2, reloads)
        collector.cancel()
    }
}
