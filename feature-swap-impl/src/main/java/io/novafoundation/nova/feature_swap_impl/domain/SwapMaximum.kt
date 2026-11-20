package io.novafoundation.nova.feature_swap_impl.domain

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigInteger

internal fun swapMaximum(transferable: BigInteger, countedTowardsEd: BigInteger, minimumBalance: BigInteger, fee: BigInteger): BigInteger {
    val alreadyRetained = (countedTowardsEd - transferable).atLeastZero()
    val additionalReserve = (minimumBalance - alreadyRetained).atLeastZero()
    return (transferable - additionalReserve - fee).atLeastZero()
}

internal fun BigInteger.swapMaxFeeMargin(): BigInteger = this / BigInteger.valueOf(100)

internal fun BigInteger.withSwapMaxFeeMargin(): BigInteger = this + swapMaxFeeMargin()

internal fun Asset.swapMaximum(minimumBalance: BigInteger, fee: BigInteger = BigInteger.ZERO): BigInteger {
    return swapMaximum(transferableInPlanks, balanceCountedTowardsEDInPlanks, minimumBalance, fee)
}
