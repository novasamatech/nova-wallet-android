package io.novafoundation.nova.feature_staking_api.domain.dashboard.model

import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake

class StakingDashboard(
    val hasStake: List<AggregatedStakingDashboardOption<HasStake>>,
    val noStake: List<AggregatedStakingDashboardOption<NoStake>>,
    val resolvingItems: Int
)
