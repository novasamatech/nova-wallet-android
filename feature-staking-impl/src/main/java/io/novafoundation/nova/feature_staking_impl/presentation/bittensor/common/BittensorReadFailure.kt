package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorTimeoutException
import io.novafoundation.nova.infrastructure.attestation.AttestationUnsupportedException
import java.net.SocketTimeoutException
import kotlinx.coroutines.TimeoutCancellationException

/** How a failed Bittensor read is explained to the user. Data sources select a kind by the exception they throw. */
enum class BittensorReadFailureKind {

    /** Anything else; a retry may help. */
    GENERIC,

    /** A read ran out of time - typically a slow chain node ([BittensorTimeoutException]); a retry may help. */
    SLOW_NETWORK,

    /** This device cannot attest to the Nova backend ([AttestationUnsupportedException]); a retry cannot help. */
    DEVICE_UNSUPPORTED
}

class BittensorReadFailure(
    val kind: BittensorReadFailureKind,
    val title: String,
    val message: String,
    val onRetry: () -> Unit
) {

    val retryable: Boolean
        get() = kind != BittensorReadFailureKind.DEVICE_UNSUPPORTED
}

fun Throwable.bittensorReadFailureKind(): BittensorReadFailureKind {
    val causes = generateSequence(this) { it.cause }.take(MAX_CAUSE_DEPTH).toList()
    return when {
        causes.any { it is AttestationUnsupportedException } -> BittensorReadFailureKind.DEVICE_UNSUPPORTED
        causes.any { it is BittensorTimeoutException || it is TimeoutCancellationException || it is SocketTimeoutException } ->
            BittensorReadFailureKind.SLOW_NETWORK
        else -> BittensorReadFailureKind.GENERIC
    }
}

private const val MAX_CAUSE_DEPTH = 16
