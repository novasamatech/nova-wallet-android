package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.feature_staking_impl.data.bittensor.BittensorTimeoutException
import io.novafoundation.nova.infrastructure.attestation.AttestationRefusedException
import io.novafoundation.nova.infrastructure.attestation.AttestationUnavailableException
import io.novafoundation.nova.infrastructure.attestation.AttestationUnsupportedException
import io.novafoundation.nova.infrastructure.attestation.peekErrorCode
import io.novafoundation.nova.infrastructure.attestation.profile3.isProfile3BearerDenial
import java.net.SocketTimeoutException
import kotlinx.coroutines.TimeoutCancellationException
import retrofit2.HttpException

/** How a failed Bittensor read is explained to the user. Data sources select a kind by the exception they throw. */
enum class BittensorReadFailureKind(val retryable: Boolean) {

    /** Anything else; a retry may help. */
    GENERIC(retryable = true),

    /** A read ran out of time - typically a slow chain node ([BittensorTimeoutException]); a retry may help. */
    SLOW_NETWORK(retryable = true),

    /**
     * This device cannot use the Nova backend: it cannot attest ([AttestationUnsupportedException]) or the backend does
     * not trust it; a retry cannot help.
     */
    DEVICE_UNSUPPORTED(retryable = false),

    /** The Nova backend refused the request, or refused this client for the rest of the process; no retry is offered. */
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

fun Throwable.bittensorReadFailureKind(): BittensorReadFailureKind {
    val causes = generateSequence(this) { it.cause }.take(MAX_CAUSE_DEPTH).toList()
    val refusal = causes.firstNotNullOfOrNull { it as? AttestationRefusedException }
    // The backend's answer to the read itself; below an attestation failure it answered the client's bootstrap instead.
    val responses = causes.takeWhile { it !is AttestationUnavailableException }.filterIsInstance<HttpException>()
    val deviceBound = causes.any { it is AttestationUnsupportedException } || refusal?.isDeviceBound() == true ||
        responses.any { it.isProfile3BearerDenial() }
    return when {
        deviceBound -> BittensorReadFailureKind.DEVICE_UNSUPPORTED
        refusal != null || responses.any { it.isRejectedRead() } -> BittensorReadFailureKind.REJECTED
        causes.any { it is BittensorTimeoutException || it is TimeoutCancellationException || it is SocketTimeoutException } ->
            BittensorReadFailureKind.SLOW_NETWORK
        else -> BittensorReadFailureKind.GENERIC
    }
}

// The bootstrap refusals iOS reports as device-bound, as it does a bearer the backend keeps denying.
private fun AttestationRefusedException.isDeviceBound() = when (status) {
    401 -> code == "attestation_failed"
    403 -> code == "app_not_allowed" || code == "binding_not_allowed"
    else -> false
}

// The answers iOS grades as rejected; any other error status may pass on a later attempt.
private fun HttpException.isRejectedRead() = when (code()) {
    400, 405, 413, 415, 422 -> true
    403 -> peekErrorCode() == "insufficient_scope"
    404 -> peekErrorCode() == "route_not_found"
    409 -> peekErrorCode() == "client_already_registered"
    else -> false
}

private const val MAX_CAUSE_DEPTH = 16
