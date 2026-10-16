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

    data class Failure(
        val title: String,
        val description: String,
        val error: String
    ) : BittensorStakeResultUi
}
