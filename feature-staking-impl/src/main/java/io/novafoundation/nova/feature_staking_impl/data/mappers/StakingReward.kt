package io.novafoundation.nova.feature_staking_impl.data.mappers

import io.novafoundation.nova.common.data.network.subquery.SubQueryResponse
import io.novafoundation.nova.common.data.network.subquery.TransactionHistoryRemote
import io.novafoundation.nova.common.utils.sumByBigInteger
import io.novafoundation.nova.core_db.model.TotalRewardLocal
import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward

fun mapSubqueryHistoryToTotalReward(response: SubQueryResponse<TransactionHistoryRemote>): TotalReward {
    return response.data.historyElements.nodes.sumByBigInteger { it.reward.amount }
}

fun mapTotalRewardLocalToTotalReward(reward: TotalRewardLocal): TotalReward {
    return reward.totalReward
}
