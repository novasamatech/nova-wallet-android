package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.feature_staking_impl.R
import io.novasama.substrate_sdk_android.wsrpc.logging.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean

/** Screen-local recovery for reads. Never retries a submitted transaction. */
class BittensorRetryController(private val resources: ResourceManager, private val logger: Logger) {

    // The failure awaiting the user is state, not a one-shot event: a dialog lost to a configuration change shows again,
    // and failures raised while the screen is hidden wait as one dialog instead of a stack of them.
    private val pending = MutableStateFlow<BittensorReadFailure?>(null)

    // Sources that failed while the pending dialog was up; its Retry restarts them all.
    private val pendingRetries = mutableListOf<() -> Unit>()

    // A failure no retry can fix reads the same for every source, e.g. each attested read on a device that cannot
    // attest: explain it once per screen.
    private var finalFailureReported = false

    val events: Flow<BittensorReadFailure> = pending.filterNotNull()

    /**
     * A read with [inlineRetry] is shown failed in place, as on iOS, and restarts from the screen's own Try again; only a slow
     * network still raises its dialog.
     */
    fun <T, R> observe(
        step: String,
        analytics: BittensorReadAnalytics? = null,
        inlineRetry: BittensorInlineRetry? = null,
        transform: (Flow<T>) -> Flow<R>,
        source: () -> Flow<T>
    ): Flow<R> {
        val reload = MutableStateFlow(0L)
        return reload.flatMapLatest {
            val attempt = flowOfAll { source() }
                .catch { cause ->
                    // A child request timing out is a failed read, not cancellation of this screen.
                    // Never show a retry dialog after the collecting coroutine has been cancelled.
                    currentCoroutineContext().ensureActive()
                    if (cause is CancellationException && cause !is TimeoutCancellationException) throw cause
                    analytics?.failed(cause)
                    logger.log("[Bittensor] Failed to load $step")
                    logger.log(cause)
                    // The dialog and the inline Try again may both offer this failure; only the first restarts the read.
                    val retried = AtomicBoolean(false)
                    val onRetry = {
                        if (retried.compareAndSet(false, true)) {
                            analytics?.retry()
                            reload.update { it + 1 }
                        }
                    }
                    val kind = cause.bittensorReadFailureKind()
                    inlineRetry?.add(onRetry)
                    if (inlineRetry == null || kind == BittensorReadFailureKind.SLOW_NETWORK) report(kind, onRetry)
                    throw cause
                }
            transform(attempt)
        }
    }

    private fun report(kind: BittensorReadFailureKind, onRetry: () -> Unit) {
        synchronized(pendingRetries) {
            val shown = pending.value
            if (shown != null) {
                // iOS presents one alert at a time, so a later failure adds no dialog of its own.
                if (shown.retryable && kind.retryable) pendingRetries += onRetry
                return
            }
            if (kind.retryable) {
                pendingRetries += onRetry
            } else {
                if (finalFailureReported) return
                finalFailureReported = true
            }
            pending.value = createFailure(kind)
        }
    }

    private fun createFailure(kind: BittensorReadFailureKind): BittensorReadFailure {
        val (title, message) = when (kind) {
            BittensorReadFailureKind.GENERIC -> R.string.common_error_general_title to R.string.bittensor_load_failed_message
            BittensorReadFailureKind.SLOW_NETWORK -> R.string.bittensor_load_slow_title to R.string.bittensor_load_slow_message
            BittensorReadFailureKind.DEVICE_UNSUPPORTED -> R.string.bittensor_load_device_title to R.string.bittensor_load_device_message
            BittensorReadFailureKind.REJECTED -> R.string.common_error_general_title to R.string.bittensor_load_rejected_message
        }
        return BittensorReadFailure(kind, resources.getString(title), resources.getString(message), ::resolve)
    }

    private fun resolve(failure: BittensorReadFailure, retry: Boolean) {
        val retries = synchronized(pendingRetries) {
            if (pending.value !== failure) return
            pending.value = null
            pendingRetries.toList().also { pendingRetries.clear() }
        }
        if (retry) retries.forEach { it() }
    }
}

/** The Try again of reads a screen shows failed in place; it restarts every one of them that failed since the last retry. */
class BittensorInlineRetry {

    private val pending = mutableListOf<() -> Unit>()

    fun retry() {
        val retries = synchronized(pending) { pending.toList().also { pending.clear() } }
        retries.forEach { it() }
    }

    internal fun add(onRetry: () -> Unit) {
        synchronized(pending) { pending += onRetry }
    }
}
