package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.MultiChainOffChainSyncResult
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.pool.PoolAccountDerivation
import io.novafoundation.nova.feature_staking_impl.data.nominationPools.repository.NominationPoolStateRepository
import io.novafoundation.nova.runtime.ext.StakingTypeGroup
import io.novafoundation.nova.runtime.ext.group
import io.novafoundation.nova.runtime.ext.utilityAsset
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import kotlinx.coroutines.flow.Flow

class StakingDashboardUpdaterFactory(
    private val stakingDashboardCache: StakingDashboardCache,
    private val remoteStorageSource: StorageDataSource,
    private val nominationPoolBalanceRepository: NominationPoolStateRepository,
    private val poolAccountDerivation: PoolAccountDerivation,
) {

    fun createUpdater(
        chain: Chain,
        stakingType: Chain.Asset.StakingType,
        metaAccount: MetaAccount,
        stakingStatsFlow: Flow<MultiChainOffChainSyncResult>,
    ): Updater? {
        return when (stakingType.group()) {
            StakingTypeGroup.RELAYCHAIN -> relayChain(chain, stakingType, metaAccount, stakingStatsFlow)
            StakingTypeGroup.PARACHAIN -> parachain(chain, stakingType, metaAccount, stakingStatsFlow)
            StakingTypeGroup.NOMINATION_POOL -> nominationPools(chain, stakingType, metaAccount, stakingStatsFlow)
            StakingTypeGroup.UNSUPPORTED -> null
        }
    }

    private fun relayChain(
        chain: Chain,
        stakingType: Chain.Asset.StakingType,
        metaAccount: MetaAccount,
        stakingStatsFlow: Flow<MultiChainOffChainSyncResult>,
    ): Updater {
        return StakingDashboardRelayStakingUpdater(
            chain = chain,
            chainAsset = chain.utilityAsset,
            stakingType = stakingType,
            metaAccount = metaAccount,
            stakingStatsFlow = stakingStatsFlow,
            stakingDashboardCache = stakingDashboardCache,
            remoteStorageSource = remoteStorageSource
        )
    }

    private fun parachain(
        chain: Chain,
        stakingType: Chain.Asset.StakingType,
        metaAccount: MetaAccount,
        stakingStatsFlow: Flow<MultiChainOffChainSyncResult>,
    ): Updater {
        return StakingDashboardParachainStakingUpdater(
            chain = chain,
            chainAsset = chain.utilityAsset,
            stakingType = stakingType,
            metaAccount = metaAccount,
            stakingStatsFlow = stakingStatsFlow,
            stakingDashboardCache = stakingDashboardCache,
            remoteStorageSource = remoteStorageSource
        )
    }

    private fun nominationPools(
        chain: Chain,
        stakingType: Chain.Asset.StakingType,
        metaAccount: MetaAccount,
        stakingStatsFlow: Flow<MultiChainOffChainSyncResult>,
    ): Updater {
        return StakingDashboardNominationPoolsUpdater(
            chain = chain,
            chainAsset = chain.utilityAsset,
            stakingType = stakingType,
            metaAccount = metaAccount,
            stakingStatsFlow = stakingStatsFlow,
            stakingDashboardCache = stakingDashboardCache,
            remoteStorageSource = remoteStorageSource,
            nominationPoolStateRepository = nominationPoolBalanceRepository,
            poolAccountDerivation = poolAccountDerivation
        )
    }
}
