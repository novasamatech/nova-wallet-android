package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.rewards

import io.novafoundation.nova.common.utils.Fraction
import io.novafoundation.nova.feature_staking_api.domain.nominationPool.model.PoolId

interface NominationPoolRewardCalculator {

    val maxAPY: Fraction

    fun apyFor(poolId: PoolId): Fraction?
}
