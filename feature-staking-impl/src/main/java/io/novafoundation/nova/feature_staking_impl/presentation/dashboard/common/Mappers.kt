package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.common

import io.novafoundation.nova.common.domain.ExtendedLoadingState
import io.novafoundation.nova.common.domain.map
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.formatting.format
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NoStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.NotYetResolved
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption.WithoutStake
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isSyncingPrimary
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isSyncingSecondary
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.model.StakingDashboardModel
import io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.view.syncingIf
import io.novafoundation.nova.feature_wallet_api.presentation.formatters.formatPlanks
import jp.co.soramitsu.fearless_utils.hash.isPositive

interface StakingDashboardPresentationMapper {

    fun mapWithoutStakeItemToUi(withoutStake: AggregatedStakingDashboardOption<WithoutStake>): StakingDashboardModel.NoStakeItem
}

class RealStakingDashboardPresentationMapper(
    private val resourceManager: ResourceManager
) : StakingDashboardPresentationMapper {

    @Suppress("UNCHECKED_CAST")
    override fun mapWithoutStakeItemToUi(withoutStake: AggregatedStakingDashboardOption<WithoutStake>): StakingDashboardModel.NoStakeItem {
        return when (withoutStake.stakingState) {
            is NoStake -> mapNoStakeItemToUi(withoutStake as AggregatedStakingDashboardOption<NoStake>)
            NotYetResolved -> mapNotYetResolvedItemToUi(withoutStake as AggregatedStakingDashboardOption<NotYetResolved>)
        }
    }

    private fun mapNotYetResolvedItemToUi(noStake: AggregatedStakingDashboardOption<NotYetResolved>): StakingDashboardModel.NoStakeItem {
        return StakingDashboardModel.NoStakeItem(
            chainUi = mapChainToUi(noStake.chain).syncingIf(isSyncing = true),
            assetId = noStake.token.configuration.id,
            earnings = ExtendedLoadingState.Loading,
            availableBalance = null,
        )
    }

    private fun mapNoStakeItemToUi(noStake: AggregatedStakingDashboardOption<NoStake>): StakingDashboardModel.NoStakeItem {
        val stats = noStake.stakingState.stats
        val syncingStage = noStake.syncingStage

        val availableBalance = noStake.stakingState.availableBalance
        val formattedAvailableBalance = if (availableBalance.isPositive()) {
            val formattedAmount = availableBalance.formatPlanks(noStake.token.configuration)
            resourceManager.getString(R.string.common_available_format, formattedAmount)
        } else {
            null
        }

        return StakingDashboardModel.NoStakeItem(
            chainUi = mapChainToUi(noStake.chain).syncingIf(syncingStage.isSyncingPrimary()),
            assetId = noStake.token.configuration.id,
            earnings = stats.map { it.estimatedEarnings.format().syncingIf(syncingStage.isSyncingSecondary()) },
            availableBalance = formattedAvailableBalance,
        )
    }
}
