package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.core.updater.GlobalScopeUpdater
import io.novafoundation.nova.core.updater.SharedRequestsBuilder
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapStakingTypeToStakingString
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.Flow

abstract class BaseStakingDashboardUpdater(
    protected val chain: Chain,
    protected val chainAsset: Chain.Asset,
    protected val stakingType: Chain.Asset.StakingType,
    protected val metaAccount: MetaAccount,
) : GlobalScopeUpdater {

    protected val stakingTypeLocal = requireNotNull(mapStakingTypeToStakingString(stakingType))

    override val requiredModules: List<String> = emptyList()

    abstract suspend fun listenForUpdates(storageSubscriptionBuilder: SharedRequestsBuilder): Flow<Updater.SideEffect>

    override suspend fun listenForUpdates(
        storageSubscriptionBuilder: SharedRequestsBuilder,
        scopeValue: Unit
    ): Flow<Updater.SideEffect> {
        return listenForUpdates(storageSubscriptionBuilder)
    }

    protected fun primarySynced(): StakingDashboardUpdaterEvent {
        return StakingDashboardUpdaterEvent.PrimarySynced(stakingOptionId())
    }

    protected fun secondarySynced(indexOfUsedOffChainSync: Int): StakingDashboardUpdaterEvent {
        return StakingDashboardUpdaterEvent.AllSynced(stakingOptionId(), indexOfUsedOffChainSync)
    }

    protected fun stakingOptionId(): StakingOptionId {
        return StakingOptionId(chain.id, chainAsset.id, stakingType)
    }

    protected suspend fun StakingDashboardCache.update(updating: (StakingDashboardItemLocal?) -> StakingDashboardItemLocal) {
        update(chain.id, chainAsset.id, stakingTypeLocal, metaAccount.id, updating)
    }
}
