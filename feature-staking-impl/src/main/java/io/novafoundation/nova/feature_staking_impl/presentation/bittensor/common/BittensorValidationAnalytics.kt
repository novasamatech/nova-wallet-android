package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.analytics.StakingValidationKind
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorStakeValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorExitValidationFailure
import io.novafoundation.nova.feature_staking_impl.domain.bittensor.BittensorClaimValidationFailure

/** A missing or unreadable quote reports liquidity, a self-impacting order its own kind. */
fun BittensorStakeValidationFailure.analyticsKind(): StakingValidationKind? = when (this) {
    BittensorStakeValidationFailure.INSUFFICIENT_BALANCE -> StakingValidationKind.INSUFFICIENT_BALANCE
    is BittensorStakeValidationFailure.FeeReserve -> StakingValidationKind.FEE_RESERVE
    is BittensorStakeValidationFailure.BelowMinimumStake -> StakingValidationKind.MIN_STAKE
    BittensorStakeValidationFailure.QUOTE_UNAVAILABLE -> StakingValidationKind.LIQUIDITY
    BittensorStakeValidationFailure.SELF_IMPACT -> StakingValidationKind.ORDER_TOO_LARGE
    BittensorStakeValidationFailure.QUOTE_EXPIRED, is BittensorStakeValidationFailure.SlippageExceeded -> StakingValidationKind.RATE_UPDATED
    is BittensorStakeValidationFailure.HighPriceImpact -> StakingValidationKind.PRICE_IMPACT
    else -> null
}

fun BittensorExitValidationFailure.analyticsKind(): StakingValidationKind? = when (this) {
    BittensorExitValidationFailure.ROOT_LOCKED, BittensorExitValidationFailure.REMAINDER_BELOW_MINIMUM -> StakingValidationKind.HOLD_LOCKED
    BittensorExitValidationFailure.ORDER_TOO_LARGE -> StakingValidationKind.ORDER_TOO_LARGE
    BittensorExitValidationFailure.INSUFFICIENT_STAKE -> StakingValidationKind.INSUFFICIENT_BALANCE
    BittensorExitValidationFailure.FULL_POSITION_CHANGED -> StakingValidationKind.STALE_DATA
    BittensorExitValidationFailure.BELOW_MINIMUM -> StakingValidationKind.MIN_STAKE
    BittensorExitValidationFailure.REMAINDER_CAN_EXIT_ALL -> StakingValidationKind.DUST_REMAINDER
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
