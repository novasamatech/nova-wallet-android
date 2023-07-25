package io.novafoundation.nova.feature_staking_impl.data.network.subquery.response

import io.novafoundation.nova.common.data.network.subquery.GroupedAggregate
import io.novafoundation.nova.common.data.network.subquery.SubQueryGroupedAggregates
import io.novafoundation.nova.common.data.network.subquery.firstSum
import io.novafoundation.nova.common.utils.atLeastZero
import io.novafoundation.nova.common.utils.orZero
import java.math.BigDecimal
import java.math.BigInteger

class StakingPeriodRewardsResponse(
    val rewards: SubQueryGroupedAggregates<GroupedAggregate.Sum<RewardNode>>,
    val slashes: SubQueryGroupedAggregates<GroupedAggregate.Sum<RewardNode>>
) {

    class RewardNode(val amount: BigDecimal)
}

val StakingPeriodRewardsResponse.RewardNode.planksAmount: BigInteger
    get() = amount.toBigInteger()

val StakingPeriodRewardsResponse.totalReward: BigInteger
    get() {
        val rewards = rewards.firstSum()?.planksAmount.orZero()
        val slashes = slashes.firstSum()?.planksAmount.orZero()

        return (rewards - slashes).atLeastZero()
    }
