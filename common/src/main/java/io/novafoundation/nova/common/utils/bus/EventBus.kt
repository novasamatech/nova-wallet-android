package io.novafoundation.nova.common.utils.bus

import kotlinx.coroutines.flow.Flow

interface EventBus<T : EventBus.Event> {

    interface Event

    class SourceEvent<T : Event>(val event: T, val source: String?) : Event

    suspend fun notify(event: T, source: String?)

    fun observeEvent(): Flow<SourceEvent<T>>
}
