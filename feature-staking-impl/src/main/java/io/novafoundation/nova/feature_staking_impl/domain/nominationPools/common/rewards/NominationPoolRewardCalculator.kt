package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.rewards

import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId

interface NominationPoolRewardCalculator {

    val maxAPY: Double

    fun apyFor(poolId: PoolId): Double?
}
