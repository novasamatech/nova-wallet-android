package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import jp.co.soramitsu.fearless_utils.hash.isPositive

interface StakingDashboardPresentationMapper {

    fun mapNoStakeItemToUi(noStake: AggregatedStakingDashboardOption<AggregatedStakingDashboardOption.NoStake>): StakingDashboardModel.NoStakeItem
}

class RealStakingDashboardPresentationMapper(
    private val resourceManager: ResourceManager
) : StakingDashboardPresentationMapper {

    override fun mapNoStakeItemToUi(noStake: AggregatedStakingDashboardOption<AggregatedStakingDashboardOption.NoStake>): StakingDashboardModel.NoStakeItem {
        val stats = noStake.stakingState.stats
        val showSync = noStake.syncingStage && stats is ExtendedLoadingState.Loaded

        val availableBalance = noStake.stakingState.availableBalance
        val formattedAvailableBalance = if (availableBalance.isPositive()) {
            val formattedAmount = availableBalance.formatPlanks(noStake.token.configuration)
            resourceManager.getString(R.string.common_available_format, formattedAmount)
        } else {
            null
        }

        return StakingDashboardModel.NoStakeItem(
            chainUi = mapChainToUi(noStake.chain),
            assetId = noStake.token.configuration.id,
            earnings = stats.map { it.estimatedEarnings.format() },
            availableBalance = formattedAvailableBalance,
            syncing = showSync
        )
    }
}
