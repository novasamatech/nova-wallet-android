package io.novafoundation.nova.common.utils.bus

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

interface RequestBus<T : RequestBus.Request, R : RequestBus.Response> {

    interface Request

    interface Response

    suspend fun handle(request: T): R

    fun observeEvent(): Flow<Pair<Continuation<R>, T>>
}

fun <T, R> Flow<Pair<Continuation<R>, T>>.observeBusEvent(
    action: suspend (T) -> R
): Flow<Pair<Continuation<R>, T>> {
    return this.onEach { (continuation, request) ->
        val response = action(request)
        continuation.resume(response)
    }
}
