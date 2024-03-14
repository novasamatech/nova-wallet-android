package io.novafoundation.nova.common.utils.bus

import kotlinx.coroutines.flow.Flow

interface EventBus<T : EventBus.Event> {

    interface Event

    suspend fun notify(event: T)

    fun observeEvent(): Flow<T>
}
