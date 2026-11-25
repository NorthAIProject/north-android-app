package com.fernandocorreia.khepri.core.testing

import com.fernandocorreia.khepri.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestDispatcher

/** Every dispatcher is the test's, so `advanceUntilIdle()` drives IO and Default work too. */
public class TestDispatcherProvider(dispatcher: TestDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher = dispatcher
    override val io: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}
