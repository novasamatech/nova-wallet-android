package io.novafoundation.nova.feature_staking_impl.data.mappers

import io.novafoundation.nova.core_db.model.TotalRewardLocal
import io.novafoundation.nova.feature_staking_impl.domain.model.TotalReward

fun mapTotalRewardLocalToTotalReward(reward: TotalRewardLocal): TotalReward {
    return reward.totalReward
}
