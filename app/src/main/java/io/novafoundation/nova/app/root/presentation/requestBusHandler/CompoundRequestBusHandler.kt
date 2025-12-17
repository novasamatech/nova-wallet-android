package io.novafoundation.nova.app.root.presentation.requestBusHandler

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

class CompoundRequestBusHandler(
    private val handlers: Set<RequestBusHandler>
) : RequestBusHandler {
    override fun observe(): Flow<*> {
        return handlers.toList()
            .map { it.observe() }
            .merge()
    }
}
