package io.novafoundation.nova.feature_staking_impl.domain.nominationPools.common.rewards

import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.network.blockhain.models.PoolId

interface NominationPoolRewardCalculator {

    val maxAPY: Perbill

    fun apyFor(poolId: PoolId): Perbill?
}
