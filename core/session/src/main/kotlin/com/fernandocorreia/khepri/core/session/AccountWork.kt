package com.fernandocorreia.khepri.core.session

import com.fernandocorreia.khepri.core.common.AccountScope
import com.fernandocorreia.khepri.core.common.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.job
import kotlinx.coroutines.withTimeoutOrNull

/** Owns the [AccountScope]: one long-lived scope whose jobs, not the scope itself, end with the session. */
@Singleton
public class AccountWork @Inject constructor(@ApplicationScope app: CoroutineScope) {
    public val scope: CoroutineScope = CoroutineScope(app.coroutineContext + SupervisorJob(app.coroutineContext[Job]))

    /** Cancels every job and waits briefly for them to stop: a blocking call may not notice at once. */
    internal suspend fun end() {
        val job = scope.coroutineContext.job
        job.cancelChildren()
        withTimeoutOrNull(JOIN_MS) { job.children.forEach { it.join() } }
    }

    private companion object {
        const val JOIN_MS = 500L
    }
}
