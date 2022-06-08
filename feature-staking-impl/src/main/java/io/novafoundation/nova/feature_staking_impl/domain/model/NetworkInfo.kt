package io.novafoundation.nova.feature_staking_impl.domain.model

import java.math.BigInteger
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
data class NetworkInfo(
    val lockupPeriod: Duration,
    val minimumStake: BigInteger,
    val totalStake: BigInteger,
    val stakingPeriod: StakingPeriod,
    val nominatorsCount: Int
)

sealed class StakingPeriod {

    object Unlimited : StakingPeriod()
}
