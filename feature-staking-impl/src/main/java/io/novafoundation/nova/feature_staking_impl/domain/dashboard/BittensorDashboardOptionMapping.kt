package io.novafoundation.nova.feature_staking_impl.domain.dashboard

import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.BittensorDashboardDetails
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboardValue
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.allStakingTypes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

internal fun StakingDashboard.withBittensorDetails(details: Map<String, BittensorDashboardDetails>): StakingDashboard {
    return StakingDashboard(
        hasStake = hasStake.map { it.withBittensorDetails(details) },
        withoutStake = withoutStake.map { it.withBittensorDetails(details) }
    )
}

internal fun <S> AggregatedStakingDashboardOption<S>.withBittensorDetails(
    detailsByChain: Map<String, BittensorDashboardDetails>
): AggregatedStakingDashboardOption<S> {
    val matches = when (val state = stakingState) {
        is AggregatedStakingDashboardOption.HasStake -> state.stakingType == Chain.Asset.StakingType.BITTENSOR_ROOT
        is AggregatedStakingDashboardOption.NoStake -> state.flowType.allStakingTypes.singleOrNull() == Chain.Asset.StakingType.BITTENSOR_ROOT
        else -> false
    }
    if (!matches) return this
    var details = detailsByChain[chain.id] ?: loadingBittensorDashboardDetails()
    // The updater's HasStake classification includes claimable root rewards without stake; those get
    // their own panel. A confirmed empty stake stays loading until the updater has read the rewards and
    // reclassified the card, never an active zero stake.
    if (stakingState is AggregatedStakingDashboardOption.HasStake &&
        details.composition == BittensorDashboardDetails.Composition.NONE &&
        details.totalStake is StakingDashboardValue.Known &&
        !details.showsClaimableRewardsPanel
    ) {
        details = details.copy(totalStake = StakingDashboardValue.Loading)
    }
    return AggregatedStakingDashboardOption(chain, token, stakingState, syncingStage, details)
}
