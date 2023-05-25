package io.novafoundation.nova.feature_staking_impl.data.dashboard.network.updaters.chain

import io.novafoundation.nova.core.updater.GlobalScopeUpdater
import io.novafoundation.nova.core_db.model.StakingDashboardItemLocal
import io.novafoundation.nova.feature_account_api.domain.model.MetaAccount
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.AggregatedStakingDashboardOption
import io.novafoundation.nova.feature_staking_api.domain.dashboard.model.StakingOptionId
import io.novafoundation.nova.feature_staking_impl.data.dashboard.cache.StakingDashboardCache
import io.novafoundation.nova.runtime.multiNetwork.chain.mappers.mapStakingTypeToStakingString
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

abstract class BaseStakingDashboardUpdater(
    protected val chain: Chain,
    protected val chainAsset: Chain.Asset,
    protected val stakingType: Chain.Asset.StakingType,
    protected val metaAccount: MetaAccount,
): GlobalScopeUpdater {

    protected val stakingTypeLocal = requireNotNull(mapStakingTypeToStakingString(stakingType))

    override val requiredModules: List<String> = emptyList()

    protected fun AggregatedStakingDashboardOption.SyncingStage.asUpdaterEvent(): StakingDashboardUpdaterEvent {
        return StakingDashboardUpdaterEvent.SyncingStageUpdated(stakingOptionId(), this)
    }

    protected fun stakingOptionId(): StakingOptionId {
        return StakingOptionId(chain.id, chainAsset.id, stakingType)
    }

    protected suspend fun StakingDashboardCache.update(updating: (StakingDashboardItemLocal?) -> StakingDashboardItemLocal) {
        update(chain.id, chainAsset.id, stakingTypeLocal, metaAccount.id, updating)
    }
}
