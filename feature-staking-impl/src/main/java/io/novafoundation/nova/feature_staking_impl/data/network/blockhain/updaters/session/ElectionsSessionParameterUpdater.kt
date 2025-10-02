package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session

import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.chain
import io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.TimelineDelegatingSingleKeyUpdater
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.ElectionsSession
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.ElectionsSessionRegistry
import io.novafoundation.nova.runtime.ext.timelineChainIdOrSelf
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.updaters.multiChain.DelegateToTimelineChainIdHolder
import io.novafoundation.nova.runtime.state.selectedOption
import io.novasama.substrate_sdk_android.runtime.RuntimeSnapshot

abstract class ElectionsSessionParameterUpdater(
    private val electionsSessionRegistry: ElectionsSessionRegistry,
    private val stakingSharedState: StakingSharedState,
    timelineDelegatingChainIdHolder: DelegateToTimelineChainIdHolder,
    chainRegistry: ChainRegistry,
    storageCache: StorageCache
) : TimelineDelegatingSingleKeyUpdater<Unit>(GlobalScope, chainRegistry, storageCache, timelineDelegatingChainIdHolder) {

    protected abstract suspend fun ElectionsSession.updaterStorageKey(chainId: ChainId): String?

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: Unit): String? {
        // We lookup election session from the staking chain asset itself
        val stakingOption = stakingSharedState.selectedOption()
        val electionsSession = electionsSessionRegistry.electionsSessionFor(stakingOption)

        // But delegate sync to timeline chain
        val timelineChainId = stakingOption.chain.timelineChainIdOrSelf()
        return electionsSession.updaterStorageKey(timelineChainId)
    }
}
