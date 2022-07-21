package io.novafoundation.nova.common.utils.systemCall

import android.content.Intent
import io.novafoundation.nova.common.resources.ContextManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SystemCallExecutor(
    private val contextManager: ContextManager
) {

    private class PendingRequest<T>(
        val continuation: Continuation<Result<T>>,
        val systemCall: SystemCall<T>
    )

    private val ongoingRequests = ConcurrentHashMap<Int, PendingRequest<Any?>>()

    @Suppress("UNCHECKED_CAST") // type-safety is guaranteed by PendingRequest<T>
    suspend fun <T> executeSystemCall(systemCall: SystemCall<T>) = suspendCoroutine<Result<T>> { continuation ->
        contextManager.getActivity()?.let {
            val request = systemCall.createRequest(it)

            it.startActivityForResult(request.intent, request.requestCode)

            ongoingRequests[request.requestCode] = PendingRequest(
                continuation = continuation as Continuation<Result<Any?>>,
                systemCall = systemCall as SystemCall<Any?>
            )
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        val removed = ongoingRequests.remove(requestCode)?.let { systemCallRequest ->
            val parsedResult = systemCallRequest.systemCall.parseResult(requestCode, resultCode, data)

            systemCallRequest.continuation.resume(parsedResult)
        }

        return removed != null
    }
}
