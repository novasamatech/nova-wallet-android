package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.stats.MultiChainStakingStats
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import kotlinx.coroutines.Deferred

class StakingDashboardUpdaterFactory(
    private val stakingDashboardCache: StakingDashboardCache,
    private val remoteStorageSource: StorageDataSource
) {

    fun createUpdater(
        chain: Chain,
        stakingType: Chain.Asset.StakingType,
        metaAccount: MetaAccount,
        stakingStatsAsync: Deferred<MultiChainStakingStats>,
    ): Updater? {
        return when (stakingType.group()) {
            StakingTypeGroup.RELAYCHAIN -> relayChain(chain, stakingType, metaAccount, stakingStatsAsync)
            StakingTypeGroup.PARACHAIN -> null // TODO
            StakingTypeGroup.UNSUPPORTED -> null
        }
    }

    private fun relayChain(
        chain: Chain,
        stakingType: Chain.Asset.StakingType,
        metaAccount: MetaAccount,
        stakingStatsAsync: Deferred<MultiChainStakingStats>,
    ): Updater {
        return StakingDashboardRelayStakingUpdater(
            chain = chain,
            chainAsset = chain.utilityAsset,
            stakingType = stakingType,
            metaAccount = metaAccount,
            stakingStatsAsync = stakingStatsAsync,
            stakingDashboardCache = stakingDashboardCache,
            remoteStorageSource = remoteStorageSource
        )
    }
}
