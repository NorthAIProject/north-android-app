package com.fernandocorreia.khepri.core.testing

import com.fernandocorreia.khepri.core.common.DataChangeBus
import com.fernandocorreia.khepri.core.model.DataScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** Records every [changed] call in [emitted] and also delivers it to collectors. */
public class FakeDataChangeBus : DataChangeBus {
    private val flow = MutableSharedFlow<DataScope>(extraBufferCapacity = BUFFER)
    public val emitted: MutableList<DataScope> = mutableListOf()
    override val changes: Flow<DataScope> = flow

    override fun changed(scope: DataScope) {
        emitted += scope
        flow.tryEmit(scope)
    }

    private companion object {
        const val BUFFER = 64
    }
}
