package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel

fun mapNoStakeItemToUi(noStake: AggregatedStakingDashboardOption<AggregatedStakingDashboardOption.NoStake>): StakingDashboardModel.NoStakeItem {
    val stats = noStake.stakingState.stats
    val showSync = noStake.syncing && stats is ExtendedLoadingState.Loaded

    return StakingDashboardModel.NoStakeItem(
        chainUi = mapChainToUi(noStake.chain),
        assetId = noStake.token.configuration.id,
        earnings = stats.map { it.estimatedEarnings.format() },
        syncing = showSync
    )
}
