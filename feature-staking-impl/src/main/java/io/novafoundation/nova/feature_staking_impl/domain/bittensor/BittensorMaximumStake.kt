package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import java.math.BigInteger

internal suspend fun maximumBittensorStake(
    available: BigInteger,
    estimateDeduction: suspend (BigInteger) -> BigInteger
): BigInteger {
    var amount = available.coerceAtLeast(BigInteger.ZERO)
    repeat(8) {
        if (amount.signum() == 0) return BigInteger.ZERO
        val deduction = estimateDeduction(amount)
        require(deduction.signum() >= 0)
        if (amount + deduction <= available) return amount
        amount = (available - deduction).coerceAtLeast(BigInteger.ZERO).min(amount)
    }
    error("Unable to determine an affordable maximum stake")
}
