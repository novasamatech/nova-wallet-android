package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats

import io.novafoundation.nova.common.utils.Percent
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_wallet_api.data.network.blockhain.types.Balance

typealias MultiChainStakingStats = Map<StakingOptionId, ChainStakingStats>

class ChainStakingStats(
    val estimatedEarnings: Percent,
    val accountPresentInActiveStakers: Boolean,
    val rewards: Balance
)
