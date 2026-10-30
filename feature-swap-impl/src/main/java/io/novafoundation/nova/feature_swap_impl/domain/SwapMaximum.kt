package io.novafoundation.nova.feature_swap_impl.domain

import io.novafoundation.nova.common.utils.atLeastZero
import java.math.BigInteger

internal fun swapMaximum(transferable: BigInteger, countedTowardsEd: BigInteger, minimumBalance: BigInteger, fee: BigInteger): BigInteger {
    val alreadyRetained = (countedTowardsEd - transferable).atLeastZero()
    val additionalReserve = (minimumBalance - alreadyRetained).atLeastZero()
    return (transferable - additionalReserve - fee).atLeastZero()
}
