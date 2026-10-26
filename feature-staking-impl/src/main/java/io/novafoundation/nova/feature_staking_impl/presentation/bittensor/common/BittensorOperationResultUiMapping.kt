package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorFailureStage
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationResult
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isRetryable
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.isSigningCancelled
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorResultAction
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorStakeResultUi
import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model.BittensorSubmissionState

/** [landingActions] are the unconfirmed sheet buttons. */
fun BittensorOperationResult.Unknown.mapResult(
    resources: ResourceManager,
    landingActions: List<BittensorResultAction> = emptyList()
): BittensorStakeResultUi.Unconfirmed =
    BittensorStakeResultUi.Unconfirmed(
        title = resources.getString(R.string.bittensor_result_unconfirmed_title),
        description = resources.getString(R.string.bittensor_result_unconfirmed_description),
        transactionHash = transactionHash,
        actions = landingActions
    )

fun BittensorOperationResult.Failure.mapResult(
    resources: ResourceManager,
    landingActions: List<BittensorResultAction> = emptyList()
): BittensorStakeResultUi {
    if (stage == BittensorFailureStage.UNKNOWN || stage == BittensorFailureStage.DISPATCHED && transactionHash == null) {
        return BittensorOperationResult.Unknown(transactionHash).mapResult(resources, landingActions)
    }

    return BittensorStakeResultUi.Failure(
        title = resources.getString(R.string.common_failed),
        description = resources.getString(
            if (stage == BittensorFailureStage.NOT_SUBMITTED) R.string.bittensor_result_not_submitted else R.string.bittensor_result_dispatched_failure
        ),
        error = transactionHash?.let { resources.getString(R.string.bittensor_result_transaction_hash, it) }
            ?: resources.getString(R.string.common_error_general_title),
        canRetry = isRetryable
    )
}

fun BittensorOperationResult.Failure.toSubmissionState(
    resources: ResourceManager,
    landingActions: List<BittensorResultAction> = emptyList()
): BittensorSubmissionState =
    if (isSigningCancelled) BittensorSubmissionState.Ready else mapResult(resources, landingActions).toSubmissionState()

fun BittensorStakeResultUi.toSubmissionState(): BittensorSubmissionState = when (this) {
    is BittensorStakeResultUi.Progress -> BittensorSubmissionState.Submitting(this)
    is BittensorStakeResultUi.Success -> BittensorSubmissionState.Completed(this)
    is BittensorStakeResultUi.Failure -> BittensorSubmissionState.Failed(this)
    is BittensorStakeResultUi.Unconfirmed -> BittensorSubmissionState.Unconfirmed(transactionHash, this)
}
