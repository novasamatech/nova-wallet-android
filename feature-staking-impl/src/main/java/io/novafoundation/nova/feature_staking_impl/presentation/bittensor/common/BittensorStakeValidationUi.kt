package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure

fun BittensorStakeValidationFailure.message(resources: ResourceManager): String = resources.getString(
    when (this) {
        BittensorStakeValidationFailure.WALLET_CHANGED -> R.string.bittensor_wallet_changed
        BittensorStakeValidationFailure.NON_POSITIVE_AMOUNT -> R.string.common_enter_amount
        BittensorStakeValidationFailure.INSUFFICIENT_BALANCE -> R.string.common_error_not_enough_tokens
    }
)
