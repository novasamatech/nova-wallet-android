package io.novafoundation.nova.common.utils.systemCall

import android.content.Intent
import io.novafoundation.nova.common.resources.ContextManager
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class SystemCallExecutor(
    private val contextManager: ContextManager
) {

    private class PendingRequest<T>(
        val callback: (Result<T>) -> Unit,
        val systemCall: SystemCall<T>
    )

    private val ongoingRequests = ConcurrentHashMap<Int, PendingRequest<Any?>>()

    @Suppress("UNCHECKED_CAST") // type-safety is guaranteed by PendingRequest<T>
    suspend fun <T> executeSystemCall(systemCall: SystemCall<T>) = suspendCoroutine<Result<T>> { continuation ->
        try {
            val request = handleRequest(systemCall)

            ongoingRequests[request.requestCode] = PendingRequest(
                callback = { continuation.resume(it as Result<T>) },
                systemCall = systemCall as SystemCall<Any?>
            )
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }

    @Suppress("UNCHECKED_CAST") // type-safety is guaranteed by PendingRequest<T>
    fun <T> executeSystemCallNotBlocking(systemCall: SystemCall<T>, onResult: (Result<T>) -> Unit = {}): Boolean {
        try {
            val request = handleRequest(systemCall)

            ongoingRequests[request.requestCode] = PendingRequest(
                callback = onResult as (Result<Any?>) -> Unit,
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

            systemCallRequest.callback(parsedResult)
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
