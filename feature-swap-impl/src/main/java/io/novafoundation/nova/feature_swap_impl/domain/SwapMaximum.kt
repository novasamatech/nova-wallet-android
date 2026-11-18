package io.novafoundation.nova.feature_swap_impl.domain

import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.feature_swap_api.domain.model.SwapFee
import io.novafoundation.nova.feature_wallet_api.domain.model.Asset
import java.math.BigInteger

internal fun swapMaximum(transferable: BigInteger, countedTowardsEd: BigInteger, minimumBalance: BigInteger, fee: BigInteger): BigInteger {
    val alreadyRetained = (countedTowardsEd - transferable).atLeastZero()
    val additionalReserve = (minimumBalance - alreadyRetained).atLeastZero()
    return (transferable - additionalReserve - fee).atLeastZero()
}

internal fun Asset.swapMaximum(fee: SwapFee): BigInteger {
    return swapMaximum(fee.additionalMaxAmountDeduction.fromCountedTowardsEd, fee.totalFeeAmount(token.configuration))
}

internal fun Asset.swapMaximum(minimumBalance: BigInteger, fee: BigInteger = BigInteger.ZERO): BigInteger {
    return swapMaximum(transferableInPlanks, balanceCountedTowardsEDInPlanks, minimumBalance, fee)
}
