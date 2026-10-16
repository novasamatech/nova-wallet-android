package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.confirm.model

import io.novafoundation.nova.feature_staking_impl.presentation.bittensor.model.BittensorFeeUi

sealed interface BittensorStakeResultUi {

    data class Progress(
        val title: String,
        val description: String
    ) : BittensorStakeResultUi

    data class Success(
        val title: String,
        val description: String,
        val validator: String,
        val fee: BittensorFeeUi
    ) : BittensorStakeResultUi

    data class Recovered(
        val title: String,
        val description: String,
        val transactionHash: String,
        val metaId: Long,
        val successful: Boolean
    ) : BittensorStakeResultUi

    data class Failure(
        val title: String,
        val description: String,
        val error: String,
        val canRetry: Boolean = true
    ) : BittensorStakeResultUi
}
