package io.novafoundation.nova.feature_staking_impl.presentation.dashboard.main.deeplink

import android.os.Parcelable
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingDashboard
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.isSyncingPrimary
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

/**
 * A staked item opens at once, an item without stake once its
 * primary (on-chain) sync is done, or as it is when [waitingForSync] is off after the pending link times out.
 * An unresolved item cannot start staking, as tapping its dashboard row cannot.
 */
internal fun resolveStakingDeepLink(target: StakingDeepLinkTarget, dashboard: StakingDashboard, waitingForSync: Boolean): StakingDeepLinkDestination? {
    dashboard.hasStake.firstOrNull { it.isTarget(target) }?.let { return StakingDeepLinkDestination(target, it) }

    val option = dashboard.withoutStake.firstOrNull { it.isTarget(target) } ?: return null
    if (option.stakingState !is AggregatedStakingDashboardOption.NoStake) return null
    if (waitingForSync && option.syncingStage.isSyncingPrimary()) return null

    return StakingDeepLinkDestination(target, option)
}

private fun AggregatedStakingDashboardOption<*>.isTarget(target: StakingDeepLinkTarget): Boolean {
    return chain.id == target.chainId && token.configuration.id == target.assetId
}
