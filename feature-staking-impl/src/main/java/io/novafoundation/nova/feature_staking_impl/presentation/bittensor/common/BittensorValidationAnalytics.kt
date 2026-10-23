package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.StakingValidationKind
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationFailure

fun BittensorStakeValidationFailure.analyticsKind(): StakingValidationKind? = when (this) {
    BittensorStakeValidationFailure.INSUFFICIENT_BALANCE -> StakingValidationKind.INSUFFICIENT_BALANCE
    BittensorStakeValidationFailure.FEE_RESERVE -> StakingValidationKind.FEE_RESERVE
    is BittensorStakeValidationFailure.BelowMinimumStake -> StakingValidationKind.MIN_STAKE
    BittensorStakeValidationFailure.QUOTE_UNAVAILABLE -> StakingValidationKind.STALE_DATA
    BittensorStakeValidationFailure.QUOTE_EXPIRED, BittensorStakeValidationFailure.SLIPPAGE_EXCEEDED -> StakingValidationKind.RATE_UPDATED
    BittensorStakeValidationFailure.HIGH_PRICE_IMPACT -> StakingValidationKind.PRICE_IMPACT
    else -> null
}

fun BittensorExitValidationFailure.analyticsKind(): StakingValidationKind? = when (this) {
    BittensorExitValidationFailure.ROOT_LOCKED -> StakingValidationKind.HOLD_LOCKED
    BittensorExitValidationFailure.INSUFFICIENT_STAKE -> StakingValidationKind.INSUFFICIENT_BALANCE
    BittensorExitValidationFailure.FULL_POSITION_CHANGED -> StakingValidationKind.STALE_DATA
    BittensorExitValidationFailure.BELOW_MINIMUM -> StakingValidationKind.MIN_STAKE
    BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM, BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL ->
        StakingValidationKind.DUST_REMAINDER
    BittensorExitValidationFailure.INSUFFICIENT_FEE_BALANCE, BittensorExitValidationFailure.FEE_SOURCE_CHANGED ->
        StakingValidationKind.FEE_RESERVE
    BittensorExitValidationFailure.QUOTE_CHANGED -> StakingValidationKind.RATE_UPDATED
    BittensorExitValidationFailure.PRICE_IMPACT_TOO_HIGH -> StakingValidationKind.PRICE_IMPACT
    else -> null
}

fun BittensorClaimValidationFailure.analyticsKind(): StakingValidationKind? = when (this) {
    BittensorClaimValidationFailure.INSUFFICIENT_FEE_BALANCE -> StakingValidationKind.FEE_RESERVE
    BittensorClaimValidationFailure.REWARDS_DECREASED, BittensorClaimValidationFailure.FORFEITURE_INCREASED ->
        StakingValidationKind.RATE_UPDATED
    else -> null
}
