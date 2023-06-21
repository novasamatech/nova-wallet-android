package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

class StakingStatsResponse(
    val activeStakers: SubQueryNodes<ActiveStaker>,
    val stakingApies: SubQueryNodes<StakingApy>,
    val accumulatedRewards: SubQueryNodes<AccumulatedReward>
) {

    interface WithStakingId {
        val networkId: String
        val stakingType: String
    }

    class ActiveStaker(override val networkId: String, override val stakingType: String, val address: String) : WithStakingId

    class StakingApy(override val networkId: String, override val stakingType: String, val maxAPY: Double) : WithStakingId

    class AccumulatedReward(override val networkId: String, override val stakingType: String, val amount: Balance) : WithStakingId
}
