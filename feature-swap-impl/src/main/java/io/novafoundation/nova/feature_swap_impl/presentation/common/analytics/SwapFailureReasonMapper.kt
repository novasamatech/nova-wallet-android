package io.novafoundation.nova.feature_swap_impl.presentation.common.analytics

import io.novafoundation.nova.analytics.SwapFailureReason
import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.feature_swap_api.domain.model.SwapOperationSubmissionException
import java.io.IOException

fun Throwable.toSwapFailureReason(): SwapFailureReason {
    return when (this) {
        is SigningCancelledException -> SwapFailureReason.USER_CANCELLED
        is SwapOperationSubmissionException.SimulationFailed -> SwapFailureReason.EXECUTION_REVERTED
        is IOException -> SwapFailureReason.NETWORK_ERROR
        else -> SwapFailureReason.UNKNOWN
    }
}
