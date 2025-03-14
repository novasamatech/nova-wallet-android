package io.novafoundation.nova.common.utils.systemCall

import android.content.Intent
import io.novafoundation.nova.common.resources.ContextManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class SystemCallExecutor(
    private val contextManager: ContextManager
) {

    private sealed interface PendingRequest<T> {
        val systemCall: SystemCall<T>

        fun onResult(result: Result<T>)

        class Suspendable<T>(
            private val continuation: Continuation<Result<T>>,
            override val systemCall: SystemCall<T>
        ) : PendingRequest<T> {
            override fun onResult(result: Result<T>) {
                continuation.resume(result)
            }
        }

        class Normal<T>(
            private val onResult: (Result<T>) -> Unit,
            override val systemCall: SystemCall<T>
        ) : PendingRequest<T> {
            override fun onResult(result: Result<T>) {
                onResult.invoke(result)
            }
        }
    }

    private val ongoingRequests = ConcurrentHashMap<Int, PendingRequest<Any?>>()

    @Suppress("UNCHECKED_CAST") // type-safety is guaranteed by PendingRequest<T>
    suspend fun <T> executeSystemCall(systemCall: SystemCall<T>) = suspendCoroutine<Result<T>> { continuation ->
        try {
            val request = handleRequest(systemCall)

            ongoingRequests[request.requestCode] = PendingRequest.Suspendable(
                continuation = continuation as Continuation<Result<Any?>>,
                systemCall = systemCall as SystemCall<Any?>
            )
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    @Suppress("UNCHECKED_CAST") // type-safety is guaranteed by PendingRequest<T>
    fun <T> executeSystemCallNotBlocking(systemCall: SystemCall<T>, onResult: (Result<T>) -> Unit): Boolean {
        try {
            val request = handleRequest(systemCall)

            ongoingRequests[request.requestCode] = PendingRequest.Normal(
                onResult = onResult as (Result<Any?>) -> Unit,
                systemCall = systemCall as SystemCall<Any?>
            )
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        val removed = ongoingRequests.remove(requestCode)?.let { systemCallRequest ->
            val parsedResult = systemCallRequest.systemCall.parseResult(requestCode, resultCode, data)

            systemCallRequest.onResult(parsedResult)
        }

        return removed != null
    }

    private fun <T> handleRequest(systemCall: SystemCall<T>): SystemCall.Request {
        val activity = contextManager.getActivity()!!
        val request = systemCall.createRequest(activity)
        activity.startActivityForResult(request.intent, request.requestCode)
        return request
    }
}
