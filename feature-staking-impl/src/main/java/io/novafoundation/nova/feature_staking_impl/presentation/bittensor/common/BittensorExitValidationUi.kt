package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure

fun BittensorExitValidationFailure.message(resources: ResourceManager): String = resources.getString(
    when (this) {
        BittensorExitValidationFailure.COLDKEY_SWAP_PENDING -> R.string.bittensor_validation_coldkey_swap
        BittensorExitValidationFailure.SAFE_MODE_ACTIVE -> R.string.bittensor_validation_safe_mode
        BittensorExitValidationFailure.SUBNET_TRADING_DISABLED -> R.string.bittensor_validation_staking_disabled
        BittensorExitValidationFailure.ROOT_LOCKED -> R.string.bittensor_validation_root_locked
        BittensorExitValidationFailure.INSUFFICIENT_STAKE -> R.string.common_error_not_enough_tokens
        BittensorExitValidationFailure.FULL_POSITION_CHANGED -> R.string.bittensor_exit_position_changed
        BittensorExitValidationFailure.BELOW_MINIMUM -> R.string.bittensor_exit_below_minimum
        BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM -> R.string.bittensor_exit_remainder_below_minimum
        BittensorExitValidationFailure.INSUFFICIENT_FEE_BALANCE -> R.string.bittensor_validation_fee_reserve
        BittensorExitValidationFailure.FEE_SOURCE_CHANGED -> R.string.bittensor_exit_fee_source_changed
    }
)
