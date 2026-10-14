package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novafoundation.nova.common.utils.isNonNegative
import io.novafoundation.nova.common.utils.isZero
import java.math.BigInteger

internal suspend fun maximumBittensorStake(
    available: BigInteger,
    estimateDeduction: suspend (BigInteger) -> BigInteger
): BigInteger {
    var amount = available.coerceAtLeast(BigInteger.ZERO)
    repeat(8) {
        if (amount.isZero) return BigInteger.ZERO
        val deduction = estimateDeduction(amount)
        require(deduction.isNonNegative)
        if (amount + deduction <= available) return amount
        amount = (available - deduction).coerceAtLeast(BigInteger.ZERO).min(amount)
    }
    error("Unable to determine an affordable maximum stake")
}
