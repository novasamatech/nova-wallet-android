package io.novafoundation.nova.common.utils.multiResult

import android.util.Log
import io.novafoundation.nova.common.base.errors.CompoundException
import io.novafoundation.nova.common.utils.multiResult.RetriableMultiResult.RetriableFailure
import io.novafoundation.nova.common.utils.requireException
import io.novafoundation.nova.common.utils.requireValue
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RetriableMultiResult<T>(val succeeded: List<T>, val failed: RetriableFailure<T>?) {

    companion object

    class RetriableFailure<T>(val retry: suspend () -> RetriableMultiResult<T>, val error: Throwable)
}

fun <T> RetriableMultiResult.Companion.allFailed(failed: RetriableFailure<T>) = RetriableMultiResult(emptyList(), failed)

suspend inline fun <T> RetriableMultiResult<T>.onFullSuccess(action: suspend (successResults: List<T>) -> Unit): RetriableMultiResult<T> {
    if (failed == null) {
        action(succeeded)
    }

    return this
}

inline fun <T> RetriableMultiResult<T>.onAnyFailure(action: (failed: RetriableFailure<T>) -> Unit): RetriableMultiResult<T> {
    if (failed != null) {
        action(failed)
    }

    return this
}

suspend fun <T, I> runMultiCatching(
    intermediateListLoading: suspend () -> List<I>,
    listProcessing: suspend (I) -> T
): RetriableMultiResult<T> = coroutineScope {
    val intermediateListResult = runCatching { intermediateListLoading() }
        .onFailure { Log.w("RetriableMultiResult", "Failed to construct multi result list", it) }

    if (intermediateListResult.isFailure) {
        val retry = suspend { runMultiCatching(intermediateListLoading, listProcessing) }

        return@coroutineScope RetriableMultiResult.allFailed(RetriableFailure(retry, intermediateListResult.requireException()))
    }

    val intermediateList = intermediateListResult.requireValue()

    val (succeeded, failed) = intermediateList.map { item ->
        val asyncProcess = async {
            runCatching { listProcessing(item) }
                .onFailure { Log.w("RetriableMultiResult", "Failed to construct multi result for item $item", it) }
        }

        asyncProcess to item
    }
        .partition { (itemResult, _) -> itemResult.await().isSuccess }

    val retryFailure = if (failed.isNotEmpty()) {
        val failedItems = failed.map { it.second }
        val exception = CompoundException(failed.map { it.first.await().requireException() })
        val retry = suspend { runMultiCatching(intermediateListLoading = { failedItems }, listProcessing) }

        RetriableFailure(retry, exception)
    } else {
        null
    }

    val successResults = succeeded.map { it.first.await().getOrThrow() }

    RetriableMultiResult(successResults, retryFailure)
}
