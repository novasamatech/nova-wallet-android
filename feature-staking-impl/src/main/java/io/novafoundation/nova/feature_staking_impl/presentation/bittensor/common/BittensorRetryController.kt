package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.flowOfAll
import io.novafoundation.nova.common.utils.sequrity.AutomaticInteractionGate
import io.novafoundation.nova.common.utils.sequrity.awaitInteractionAllowed
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/** Screen-local recovery for reads. Never retries a submitted transaction. */
class BittensorRetryController(
    private val resources: ResourceManager,
    private val logger: Logger,
    private val automaticInteractionGate: AutomaticInteractionGate
) {

    // The failure awaiting the user is state, not a one-shot event: a dialog lost to a configuration change shows again,
    // and failures raised while the screen is hidden wait as one dialog instead of a stack of them.
    private val pending = MutableStateFlow<BittensorReadFailure?>(null)

    // Sources that failed while the pending dialog was up; its Retry restarts them all.
    private val pendingRetries = mutableListOf<() -> Unit>()

    // A failure no retry can fix reads the same for every source, e.g. each attested read on a device that cannot
    // attest: explain it once per screen.
    private var finalFailureReported = false

    val events: Flow<BittensorReadFailure> = pending.filterNotNull()

    fun <T, R> observe(
        step: String,
        analytics: BittensorReadAnalytics? = null,
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
                    report(step, cause) {
                        analytics?.retry()
                        reload.update { it + 1 }
                    }
                    throw cause
                }
            transform(attempt)
        }
    }

    /**
     * Each attempt of a read whose failure raises no dialog. A failed read is requested again once the app returns from
     * the background.
     */
    fun <T> observeQuietly(step: String, load: suspend () -> T): Flow<Result<T>> = flow {
        while (true) {
            val attempt = try {
                Result.success(load())
            } catch (cause: Exception) {
                currentCoroutineContext().ensureActive()
                if (cause is CancellationException && cause !is TimeoutCancellationException) throw cause
                logFailure(step, cause)
                Result.failure(cause)
            }
            emit(attempt)
            if (attempt.isSuccess) return@flow
            automaticInteractionGate.isInteractionAllowedFlow.first { allowed -> !allowed }
            automaticInteractionGate.awaitInteractionAllowed()
        }
    }

    /**
     * Like [observe], but the screen explains the failure itself (e.g. an inline warning with "Try again") instead of a dialog.
     * [retry] reloads the source.
     */
    fun <T, R> observeInline(
        step: String,
        retry: BittensorInlineRetry,
        analytics: BittensorReadAnalytics? = null,
        transform: (Flow<T>) -> Flow<R>,
        source: () -> Flow<T>
    ): Flow<R> {
        return retry.attempts.flatMapLatest { attempt ->
            if (attempt > 0) analytics?.retry()
            val read = flowOfAll { source() }
                .catch { cause ->
                    currentCoroutineContext().ensureActive()
                    if (cause is CancellationException && cause !is TimeoutCancellationException) throw cause
                    analytics?.failed(cause)
                    logFailure(step, cause)
                    // The screen already explains a failure no retry can fix; later reads must not add a dialog for it.
                    if (!cause.bittensorReadFailureKind().retryable) synchronized(pendingRetries) { finalFailureReported = true }
                    throw cause
                }
            transform(read)
        }
    }

    /**
     * A user action (e.g. Continue) whose reads ran out of time: explained with the same slow-network dialog as a failed
     * read, whose Retry runs [onRetry]. Returns false for any other failure, which the caller presents itself.
     */
    fun reportSlowAction(step: String, cause: Throwable, onRetry: () -> Unit): Boolean {
        if (cause.bittensorReadFailureKind() != BittensorReadFailureKind.SLOW_NETWORK) return false
        report(step, cause, onRetry)
        return true
    }

    private fun report(step: String, cause: Throwable, onRetry: () -> Unit) {
        logFailure(step, cause)
        val kind = cause.bittensorReadFailureKind()
        synchronized(pendingRetries) {
            val shown = pending.value
            if (shown != null) {
                // One dialog at a time: a later failure adds no dialog of its own.
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

    private fun logFailure(step: String, cause: Throwable) {
        logger.log("[Bittensor] Failed to load $step")
        logger.log(cause)
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

/** Reload trigger for [BittensorRetryController.observeInline]. */
class BittensorInlineRetry {

    internal val attempts = MutableStateFlow(0L)

    fun retry() {
        attempts.update { it + 1 }
    }
}
