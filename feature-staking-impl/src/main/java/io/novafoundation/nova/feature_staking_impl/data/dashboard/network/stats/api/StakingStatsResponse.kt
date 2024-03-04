package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.api

import io.novafoundation.nova.common.data.network.subquery.GroupedAggregate
import io.novafoundation.nova.common.data.network.subquery.SubQueryGroupedAggregates
import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import java.math.BigDecimal

typealias StakingStatsRewards = SubQueryGroupedAggregates<GroupedAggregate.Sum<StakingStatsResponse.AccumulatedReward>>

class StakingStatsResponse(
    val activeStakers: SubQueryNodes<ActiveStaker>?,
    val stakingApies: SubQueryNodes<StakingApy>,
    val rewards: SubQueryGroupedAggregates<GroupedAggregate.Sum<AccumulatedReward>>?,
    val slashes: SubQueryGroupedAggregates<GroupedAggregate.Sum<AccumulatedReward>>?
) {

    interface WithStakingId {
        val networkId: String
        val stakingType: String
    }

    class ActiveStaker(override val networkId: String, override val stakingType: String, val address: String) : WithStakingId

    class StakingApy(override val networkId: String, override val stakingType: String, val maxAPY: Double) : WithStakingId

    class AccumulatedReward(val amount: BigDecimal) // We use BigDecimal to support scientific notations
}
