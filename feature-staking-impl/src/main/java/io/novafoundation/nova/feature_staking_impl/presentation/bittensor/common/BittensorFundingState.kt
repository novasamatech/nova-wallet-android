package io.novafoundation.nova.feature_staking_impl.presentation.bittensor.common

import io.novafoundation.nova.feature_staking_impl.domain.bittensor.maximumBittensorStake
import java.math.BigInteger

internal fun bittensorNeedsFunding(available: BigInteger?, networkFee: BigInteger?, locked: Boolean): Boolean {
    if (locked || available == null) return false
    if (available == BigInteger.ZERO) return true
    if (networkFee == null) return false
    return maximumBittensorStake(available, networkFee) == BigInteger.ZERO
}
