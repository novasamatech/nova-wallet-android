package io.novafoundation.nova.feature_staking_impl.data.network.subquery.response

import io.novafoundation.nova.common.data.network.subquery.GroupedAggregate
import io.novafoundation.nova.common.data.network.subquery.SubQueryGroupedAggregates
import io.novafoundation.nova.common.utils.orZero
import java.math.BigDecimal
import java.math.BigInteger
import jp.co.soramitsu.fearless_utils.hash.isNegative

class StakingPeriodRewardsResponse(
    val rewards: SubQueryGroupedAggregates<GroupedAggregate.Sum<RewardNode>>,
    val slashes: SubQueryGroupedAggregates<GroupedAggregate.Sum<RewardNode>>
) {

    class RewardNode(val amount: BigDecimal)
}

val StakingPeriodRewardsResponse.totalReward: BigInteger
    get() {
        val end = rewards.groupedAggregates.firstOrNull()?.sum?.amount ?: return BigInteger.ZERO
        val start = slashes.groupedAggregates.firstOrNull()?.sum?.amount
        val total = end.toBigInteger() - start?.toBigInteger().orZero()
        return if (total.isNegative()) {
            return BigInteger.ZERO
        } else {
            total
        }
    }
