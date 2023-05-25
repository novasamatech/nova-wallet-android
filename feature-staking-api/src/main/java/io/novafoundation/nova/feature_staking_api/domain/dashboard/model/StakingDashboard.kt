package io.novafoundation.nova.feature_staking_api.domain.dashboard.model

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.HasStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.WithoutStake

class StakingDashboard(
    val hasStake: List<AggregatedStakingDashboardOption<HasStake>>,
    val withoutStake: List<AggregatedStakingDashboardOption<WithoutStake>>,
)

class MoreStakingOptions(
    val inAppStaking: List<AggregatedStakingDashboardOption<WithoutStake>>,
    val browserStaking: ExtendedLoadingState<List<StakingDApp>>,
)

class StakingDApp(
    val url: String,
    val iconUrl: String,
    val name: String
)
