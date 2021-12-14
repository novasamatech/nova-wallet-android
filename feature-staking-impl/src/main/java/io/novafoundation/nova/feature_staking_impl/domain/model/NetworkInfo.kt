package io.novafoundation.nova.feature_staking_impl.domain.model

import java.math.BigInteger

data class NetworkInfo(
    val lockupPeriodInDays: Int,
    val minimumStake: BigInteger,
    val totalStake: BigInteger,
    val stakingPeriod: StakingPeriod,
    val nominatorsCount: Int
)

sealed class StakingPeriod {

    object Unlimited : StakingPeriod()
}
