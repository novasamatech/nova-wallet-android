package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorTimeoutException
import io.novafoundation.nova.infrastructure.attestation.BackendRefusal
import io.novafoundation.nova.infrastructure.attestation.backendRefusal
import java.net.SocketTimeoutException
import kotlinx.coroutines.TimeoutCancellationException

/** How a failed Bittensor read is explained to the user. Data sources select a kind by the exception they throw. */
enum class BittensorReadFailureKind(val retryable: Boolean) {

    /** Anything else; a retry may help. */
    GENERIC(retryable = true),

    /** A read ran out of time - typically a slow chain node ([BittensorTimeoutException]); a retry may help. */
    SLOW_NETWORK(retryable = true),

    /** This device cannot use the Nova backend ([BackendRefusal.DEVICE]); a retry cannot help. */
    DEVICE_UNSUPPORTED(retryable = false),

    /** The Nova backend refused the request, or refused this client for the rest of the process ([BackendRefusal.REQUEST]); no retry is offered. */
    REJECTED(retryable = false)
}

class BittensorReadFailure(
    val kind: BittensorReadFailureKind,
    val title: String,
    val message: String,
    private val resolve: (failure: BittensorReadFailure, retry: Boolean) -> Unit
) {

    val retryable: Boolean
        get() = kind.retryable

    fun onRetry() = resolve(this, true)

    fun onDismiss() = resolve(this, false)
}

fun Throwable.bittensorReadFailureKind(): BittensorReadFailureKind = when (backendRefusal()) {
    BackendRefusal.DEVICE -> BittensorReadFailureKind.DEVICE_UNSUPPORTED
    BackendRefusal.REQUEST -> BittensorReadFailureKind.REJECTED
    null -> if (isTimeout()) BittensorReadFailureKind.SLOW_NETWORK else BittensorReadFailureKind.GENERIC
}

private fun Throwable.isTimeout() = generateSequence(this) { it.cause }
    .take(MAX_CAUSE_DEPTH)
    .any { it is BittensorTimeoutException || it is TimeoutCancellationException || it is SocketTimeoutException }

private const val MAX_CAUSE_DEPTH = 16
