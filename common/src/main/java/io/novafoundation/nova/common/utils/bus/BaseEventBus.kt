package io.novafoundation.nova.common.utils.bus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

abstract class BaseEventBus<T : EventBus.Event> : EventBus<T> {

    private val eventFlow = MutableSharedFlow<EventBus.SourceEvent<T>>()

    override suspend fun notify(event: T, source: String?) {
        eventFlow.emit(EventBus.SourceEvent(event, source))
    }

    override fun observeEvent(): Flow<EventBus.SourceEvent<T>> {
        return eventFlow
    }
}
