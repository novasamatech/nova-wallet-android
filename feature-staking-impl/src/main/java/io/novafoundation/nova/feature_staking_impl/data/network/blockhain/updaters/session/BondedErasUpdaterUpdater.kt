package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session

import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.provideContext
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScope
import io.novafoundation.nova.core.updater.GlobalScopeUpdater
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.bonded
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.bondedErasOrNull
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.erasStartSessionIndexOrNull
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.staking
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.base.StakingUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.scope.ActiveEraScope
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot

class BondedErasUpdaterUpdater(
    storageCache: StorageCache,
    stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
) : SingleStorageKeyUpdater<Unit>(GlobalScope, stakingSharedState, chainRegistry, storageCache),
    StakingUpdater<Unit> {

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: Unit): String? {
        return runtime.provideContext { metadata.staking.bondedErasOrNull?.storageKey() }
    }
}
