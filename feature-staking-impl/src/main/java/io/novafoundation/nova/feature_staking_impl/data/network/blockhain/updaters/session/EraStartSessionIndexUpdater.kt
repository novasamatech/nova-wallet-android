package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session

import io.novafoundation.nova.common.utils.metadata
import io.novafoundation.nova.common.utils.provideContext
import io.novafoundation.nova.common.utils.staking
import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.feature_staking_api.domain.model.EraIndex
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.TimelineDelegatingChainIdHolder
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.erasStartSessionIndex
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.api.staking
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.TimelineDelegatingSingleKeyUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.base.StakingUpdater
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.scope.ActiveEraScope
import io.novafoundation.nova.runtime.ext.timelineChainIdOrSelf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot
import io.novasama.substrate_sdk_android.runtime.metadata.storage
import io.novasama.substrate_sdk_android.runtime.metadata.storageKey

class EraStartSessionIndexUpdater(
    activeEraScope: ActiveEraScope,
    storageCache: StorageCache,
    // Important: StartSessionIndex storage is in staking pallet, but we need to access it on timeline chain
    private val timelineDelegatingChainIdHolder: TimelineDelegatingChainIdHolder,
    chainRegistry: ChainRegistry,
) : TimelineDelegatingSingleKeyUpdater<EraIndex>(activeEraScope, chainRegistry, storageCache, timelineDelegatingChainIdHolder) {

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: EraIndex): String {
        return runtime.provideContext { metadata.staking.erasStartSessionIndex.storageKey(scopeValue) }
    }
}
