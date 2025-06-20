package io.novafoundation.nova.common.utils.bus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

abstract class BaseRequestBus<T : RequestBus.Request, R : RequestBus.Response> : RequestBus<T, R> {

    private val eventFlow = MutableSharedFlow<Pair<Continuation<R>, T>>()

    override suspend fun handle(request: T): R = withContext(Dispatchers.Default) {
        suspendCoroutine { continuation ->
            runBlocking {
                eventFlow.emit(continuation to request)
            }
        }
    }

    override fun observeEvent(): Flow<Pair<Continuation<R>, T>> {
        return eventFlow
    }
}
