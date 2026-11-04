package io.novafoundation.nova.feature_staking_impl.data.cache

import kotlin.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Refresh immediately, then after the interval or when the app returns to foreground, whichever comes first.
 * Failures keep the last emitted value and retry sooner. Intervals start after each request completes.
 * A foreground event during a request needs no extra refresh: the current request already supplies new data.
 * Cancelling collection stops both the request and the wait; there are no independently launched jobs.
 */
internal fun <T> Flow<Unit>.refreshOnForegroundOrInterval(
    refreshInterval: Duration,
    retryInterval: Duration,
    refresh: suspend () -> Result<T>
): Flow<T> = flow {
    while (true) {
        val result = refresh()
        val failure = result.exceptionOrNull()
        if (failure is CancellationException) throw failure
        result.onSuccess { emit(it) }

        val nextRefreshAfter = if (result.isSuccess) refreshInterval else retryInterval
        withTimeoutOrNull(nextRefreshAfter.inWholeMilliseconds) {
            // A completed event stream leaves the interval timer as the only refresh trigger.
            this@refreshOnForegroundOrInterval.firstOrNull() ?: awaitCancellation()
        }
    }
}
