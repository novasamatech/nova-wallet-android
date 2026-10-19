package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import kotlinx.parcelize.Parcelize

@Parcelize
data class StakingDeepLinkTarget(val chainId: String, val assetId: Int) : Parcelable

internal fun parseStakingDeepLinkTarget(chainId: String?, assetId: String?): StakingDeepLinkTarget? {
    if (chainId == null) return null
    require(chainId.isNotBlank()) { "Staking chain id is empty" }
    val parsedAssetId = if (assetId == null) 0 else requireNotNull(assetId.toIntOrNull()) { "Invalid staking asset id" }
    require(parsedAssetId >= 0) { "Invalid staking asset id" }
    return StakingDeepLinkTarget(chainId.removePrefix("0x"), parsedAssetId)
}

class StakingDeepLinkDestination(
    val target: StakingDeepLinkTarget,
    val option: AggregatedStakingDashboardOption<*>
)

internal fun resolveStakingDeepLink(target: StakingDeepLinkTarget, dashboard: StakingDashboard): StakingDeepLinkDestination? {
    val options = dashboard.hasStake + dashboard.withoutStake
    val option = options.firstOrNull { it.chain.id == target.chainId && it.token.configuration.id == target.assetId } ?: return null
    if (option.syncingStage != AggregatedStakingDashboardOption.SyncingStage.SYNCED) return null
    if (option.stakingState == AggregatedStakingDashboardOption.NotYetResolved) return null
    return StakingDeepLinkDestination(target, option)
}
