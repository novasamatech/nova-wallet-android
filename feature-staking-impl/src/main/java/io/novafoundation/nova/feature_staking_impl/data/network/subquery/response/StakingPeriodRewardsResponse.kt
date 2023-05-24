package io.novafoundation.nova.feature_staking_impl.data.network.subquery.response

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import java.math.BigInteger

class StakingPeriodRewardsResponse(val start: SubQueryNodes<RewardNode>, val end: SubQueryNodes<RewardNode>) {

    class RewardNode(val accumulatedAmount: BigInteger, val amount: BigInteger)
}

val StakingPeriodRewardsResponse.totalReward: BigInteger
    get() {
        val end = end.nodes.firstOrNull() ?: return BigInteger.ZERO
        val start = start.nodes.firstOrNull() ?: return BigInteger.ZERO
        return end.accumulatedAmount - start.accumulatedAmount + start.amount
    }
