package io.novafoundation.nova.feature_staking_api.domain.dashboard.model

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake

class StakingDashboard(
    val hasStake: List<AggregatedStakingDashboardOption<HasStake>>,
    val noStake: List<AggregatedStakingDashboardOption<NoStake>>,
    val resolvingItems: Int,
)

class MoreStakingOptions(
    val inAppStaking: List<AggregatedStakingDashboardOption<NoStake>>,
    val resolvingInAppItems: Int,
    val browserStaking: ExtendedLoadingState<List<StakingDApp>>,
)

class StakingDApp(
    val url: String,
    val iconUrl: String,
    val name: String
)
