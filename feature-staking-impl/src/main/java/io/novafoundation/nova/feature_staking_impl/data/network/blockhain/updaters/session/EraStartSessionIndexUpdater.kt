package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session

import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.provideContext
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.erasStartSessionIndexOrNull
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.staking
import io.novafoundation.nova.runtime.network.updaters.multiChain.SharedStateBasedUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.scope.ActiveEraScope
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot

class EraStartSessionIndexUpdater(
    activeEraScope: ActiveEraScope,
    storageCache: StorageCache,
    stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
) : SingleStorageKeyUpdater<EraIndex>(activeEraScope, stakingSharedState, chainRegistry, storageCache), SharedStateBasedUpdater<EraIndex> {

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: EraIndex): String? {
        return runtime.provideContext { metadata.staking.erasStartSessionIndexOrNull?.storageKey(scopeValue) }
    }
}
