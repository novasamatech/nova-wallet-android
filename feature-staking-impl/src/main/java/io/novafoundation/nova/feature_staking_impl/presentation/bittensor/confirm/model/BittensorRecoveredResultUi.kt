package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.RecoveredExtrinsicOutcome
import io.novafoundation.nova.feature_account_api.data.extrinsic.recovery.ExtrinsicSubmissionResolution

fun ExtrinsicSubmissionResolution.toUi(resources: ResourceManager, metaId: Long): BittensorStakeResultUi.Recovered {
    val (title, description) = when (outcome) {
        RecoveredExtrinsicOutcome.SUCCESS -> R.string.bittensor_recovery_success_title to R.string.bittensor_recovery_success_description
        RecoveredExtrinsicOutcome.FAILURE -> R.string.bittensor_staking_failed_title to R.string.bittensor_recovery_failure_description
        RecoveredExtrinsicOutcome.EXPIRED -> R.string.bittensor_recovery_expired_title to R.string.bittensor_recovery_expired_description
    }
    return BittensorStakeResultUi.Recovered(
        resources.getString(title),
        resources.getString(description),
        hash,
        metaId,
        outcome == RecoveredExtrinsicOutcome.SUCCESS
    )
}
