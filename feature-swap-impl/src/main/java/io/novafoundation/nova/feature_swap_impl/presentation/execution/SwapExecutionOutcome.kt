package io.novafoundation.nova.feature_swap_impl.presentation.execution

import io.novafoundation.nova.common.base.errors.SigningCancelledException
import io.novafoundation.nova.common.data.network.runtime.binding.DispatchError
import io.novafoundation.nova.feature_swap_api.domain.model.SwapOperationSubmissionException
import io.novafoundation.nova.feature_swap_api.domain.model.SwapProgress

/** A transport error alone cannot prove that a transaction failed to execute. */
internal fun SwapProgress.Failure.hasKnownOutcome(): Boolean {
    return transaction?.failedOnChain == true || error is DispatchError ||
        error is SwapOperationSubmissionException.SimulationFailed || error is SigningCancelledException
}
