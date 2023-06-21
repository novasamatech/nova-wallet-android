package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters

import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.MultiChainStakingStats

data class MultiChainOffChainSyncResult(
    val index: Int,
    val multiChainStakingStats: MultiChainStakingStats,
)
