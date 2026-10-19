package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import java.math.BigInteger

// Matches SubtensorNovaFeeConstants.feeReserve on iOS.
private val MAX_STAKE_FEE_RESERVE_IN_RAO = BigInteger.valueOf(10_000_000)

internal fun maximumBittensorStake(available: BigInteger, networkFee: BigInteger): BigInteger {
    require(networkFee.isNonNegative)

    return (available - bittensorRetainedForFees(networkFee)).coerceAtLeast(BigInteger.ZERO)
}

internal fun bittensorRetainedForFees(networkFee: BigInteger): BigInteger = networkFee + MAX_STAKE_FEE_RESERVE_IN_RAO
