package com.fernandocorreia.khepri.core.common

import com.fernandocorreia.khepri.core.model.DataScope
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** Write use cases announce what they changed; screens collect [changes] and reload. A load never emits. */
public interface DataChangeBus {
    public val changes: Flow<DataScope>

    public fun changed(scope: DataScope)
}

/** Delivers only to collectors that are active at the time of [changed]; nothing is replayed. */
public class SharedFlowDataChangeBus @Inject constructor() : DataChangeBus {
    private val flow = MutableSharedFlow<DataScope>(replay = 0, extraBufferCapacity = BUFFER)
    override val changes: Flow<DataScope> = flow

    override fun changed(scope: DataScope) {
        flow.tryEmit(scope)
    }

    private companion object {
        const val BUFFER = 64
    }
}
