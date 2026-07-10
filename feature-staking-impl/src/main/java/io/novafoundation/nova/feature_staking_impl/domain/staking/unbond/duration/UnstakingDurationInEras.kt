package io.novafoundation.nova.feature_staking_impl.domain.staking.unbond.duration

import java.math.BigInteger

data class UnstakingDurationInEras(
    val validator: BigInteger,
    val nominator: BigInteger
) {

    fun valueFor(variant: UnstakingDurationVariant): BigInteger = when (variant) {
        UnstakingDurationVariant.FULL -> validator
        UnstakingDurationVariant.NOMINATOR -> nominator
    }
}
