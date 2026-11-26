package com.fernandocorreia.khepri.core.common

import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * The server refused a call because the account has not allowed AI data sharing (403 `ai_consent_required`). The
 * network layer posts; whoever holds the consent answer reloads it. Several refusals at once arrive as one.
 */
public interface ConsentSignal {
    public val required: Flow<Unit>

    public fun post()
}

public class SharedFlowConsentSignal @Inject constructor() : ConsentSignal {
    private val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val required: Flow<Unit> = flow

    override fun post() {
        flow.tryEmit(Unit)
    }
}
