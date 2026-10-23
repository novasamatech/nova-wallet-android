package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorOperationOutcome
import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

sealed interface BittensorStakeResultUi {

    data class Unconfirmed(
        val title: String,
        val description: String,
        val transactionHash: String?
    ) : BittensorStakeResultUi

    data class Progress(
        val title: String,
        val description: String
    ) : BittensorStakeResultUi

    data class Success(
        val title: String,
        val description: String,
        /** Null hides the row: adding to a root position reports the stake after instead (iOS). */
        val validator: String?,
        val fee: BittensorFeeUi,
        val outcome: BittensorOperationOutcome? = null,
        val amount: AmountModel? = null,
        val stakeAfter: String? = null,
        /** "Discover subnets" is offered only after opening a new position (iOS `landingActions`). */
        val canDiscoverSubnets: Boolean = false
    ) : BittensorStakeResultUi

    data class Failure(
        val title: String,
        val description: String,
        val error: String,
        val canRetry: Boolean = true
    ) : BittensorStakeResultUi
}
