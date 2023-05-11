package io.novafoundation.nova.feature_staking_impl.data.network.subquery.response

import io.novafoundation.nova.common.data.network.subquery.SubQueryNodes
import java.math.BigInteger

class SubQueryTotalRewardResponse(val start: SubQueryNodes<RewardNode>, val end: SubQueryNodes<RewardNode>) {

    class RewardNode(val accumulatedAmount: BigInteger)
}

val SubQueryTotalRewardResponse.totalReward: BigInteger
    get() {
        val end = end.nodes.firstOrNull()?.accumulatedAmount ?: return BigInteger.ZERO
        val start = start.nodes.firstOrNull()?.accumulatedAmount ?: return BigInteger.ZERO
        return end - start
    }
