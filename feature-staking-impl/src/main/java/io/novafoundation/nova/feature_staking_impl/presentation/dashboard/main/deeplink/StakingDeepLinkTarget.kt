package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.allStakingTypes
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.parcelize.Parcelize

@Parcelize
data class StakingDeepLinkTarget(
    val chainId: String,
    val assetId: Int,
    val destination: Destination = Destination.RESOLVE_POSITION
) : Parcelable {

    enum class Destination { RESOLVE_POSITION, BITTENSOR_PORTFOLIO }
}

internal fun parseStakingDeepLinkTarget(chainId: String?, assetId: String?): StakingDeepLinkTarget? {
    if (chainId == null) return null
    require(chainId.isNotBlank()) { "Staking chain id is empty" }
    val parsedAssetId = if (assetId == null) 0 else requireNotNull(assetId.toIntOrNull()) { "Invalid staking asset id" }
    require(parsedAssetId >= 0) { "Invalid staking asset id" }
    return StakingDeepLinkTarget(chainId.removePrefix("0x"), parsedAssetId)
}

sealed interface StakingDeepLinkNavigation {
    val target: StakingDeepLinkTarget
}

class BittensorPortfolioDeepLink(override val target: StakingDeepLinkTarget) : StakingDeepLinkNavigation

class StakingDeepLinkDestination(
    override val target: StakingDeepLinkTarget,
    val option: AggregatedStakingDashboardOption<*>
) : StakingDeepLinkNavigation

internal fun resolveStakingDeepLink(target: StakingDeepLinkTarget, dashboard: StakingDashboard): StakingDeepLinkDestination? {
    val options = dashboard.hasStake + dashboard.withoutStake
    val option = options.firstOrNull { it.chain.id == target.chainId && it.token.configuration.id == target.assetId } ?: return null
    val isBittensor = when (val state = option.stakingState) {
        is AggregatedStakingDashboardOption.HasStake -> state.stakingType == Chain.Asset.StakingType.BITTENSOR_ROOT
        is AggregatedStakingDashboardOption.NoStake -> state.flowType.allStakingTypes.singleOrNull() == Chain.Asset.StakingType.BITTENSOR_ROOT
        else -> false
    }
    // Bittensor has no secondary dashboard updater. Primary sync already resolves positions and root rewards.
    val positionsReady = option.syncingStage == AggregatedStakingDashboardOption.SyncingStage.SYNCED ||
        isBittensor && option.syncingStage == AggregatedStakingDashboardOption.SyncingStage.SYNCING_SECONDARY
    if (!positionsReady) return null
    if (option.stakingState == AggregatedStakingDashboardOption.NotYetResolved) return null
    return StakingDeepLinkDestination(target, option)
}
