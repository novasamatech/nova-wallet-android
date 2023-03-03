package io.novafoundation.nova.common.utils.multiResult

import io.novafoundation.nova.common.utils.multiResult.RetriableMultiResult.RetriableFailure
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class RetriableMultiResult<T>(val succeeded: List<T>, val failed: RetriableFailure<T>?) {

    companion object

    class RetriableFailure<T>(val retry: suspend () -> RetriableMultiResult<T>, val error: Throwable? = null)
}

fun <T> RetriableMultiResult.Companion.allFailed(failed: RetriableFailure<T>) = RetriableMultiResult(emptyList(), failed)

inline fun <T> RetriableMultiResult<T>.onFullSuccess(action: (successResults: List<T>) -> Unit): RetriableMultiResult<T> {
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
    val intermediateList = runCatching { intermediateListLoading() }.getOrNull()
    if (intermediateList == null) {
        val retry = suspend { runMultiCatching(intermediateListLoading, listProcessing) }

        return@coroutineScope RetriableMultiResult.allFailed(RetriableFailure(retry))
    }

    val (succeeded, failed) = intermediateList.map { item -> async { runCatching {  listProcessing(item) } } to item }
        .partition { (itemResult, _) -> itemResult.await().isSuccess }

    val retryFailure = if (failed.isNotEmpty()) {
        val failedItems = failed.map { it.second }
        val retry = suspend { runMultiCatching(intermediateListLoading = { failedItems }, listProcessing) }

        RetriableFailure(retry)
    } else {
        null
    }

    val successResults = succeeded.map { it.first.await().getOrThrow() }

    RetriableMultiResult(successResults, retryFailure)
}
