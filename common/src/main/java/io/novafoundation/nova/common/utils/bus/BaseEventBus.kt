package io.novafoundation.nova.common.utils.bus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

abstract class BaseEventBus<T : EventBus.Event> : EventBus<T> {

    private val eventFlow = MutableSharedFlow<T>()

    override suspend fun notify(event: T) {
        eventFlow.emit(event)
    }

    override fun observeEvent(): Flow<T> {
        return eventFlow
    }
}
