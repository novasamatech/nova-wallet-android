package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationFailure

fun BittensorClaimValidationFailure.message(resources: ResourceManager): String = resources.getString(
    when (this) {
        BittensorClaimValidationFailure.COLDKEY_SWAP_PENDING -> R.string.bittensor_validation_coldkey_swap
        BittensorClaimValidationFailure.SAFE_MODE_ACTIVE -> R.string.bittensor_validation_safe_mode
        BittensorClaimValidationFailure.NOT_CLAIMABLE -> R.string.bittensor_claim_none
        BittensorClaimValidationFailure.REWARDS_DECREASED -> R.string.bittensor_claim_rewards_decreased
        BittensorClaimValidationFailure.FORFEITURE_INCREASED -> R.string.bittensor_claim_forfeiture_increased
        BittensorClaimValidationFailure.INSUFFICIENT_FEE_BALANCE -> R.string.bittensor_validation_fee_reserve
    }
)
