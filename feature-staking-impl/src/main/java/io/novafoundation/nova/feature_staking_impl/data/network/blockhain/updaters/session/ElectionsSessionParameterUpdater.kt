package io.novafoundation.nova.feature_staking_impl.data.network.blockhain.updaters.session

import io.novafoundation.nova.core.storage.StorageCache
import io.novafoundation.nova.core.updater.GlobalScope
import io.novafoundation.nova.feature_staking_impl.data.StakingSharedState
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.ElectionsSession
import io.novafoundation.nova.feature_staking_impl.data.repository.consensus.ElectionsSessionRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.updaters.SingleStorageKeyUpdater
import io.novafoundation.nova.runtime.state.selectedOption
import jp.co.soramitsu.fearless_utils.runtime.RuntimeSnapshot

abstract class ElectionsSessionParameterUpdater(
    private val electionsSessionRegistry: ElectionsSessionRegistry,
    private val stakingSharedState: StakingSharedState,
    chainRegistry: ChainRegistry,
    storageCache: StorageCache
) : SingleStorageKeyUpdater<Unit>(GlobalScope, stakingSharedState, chainRegistry, storageCache) {

    protected abstract suspend fun ElectionsSession.updaterStorageKey(chainId: ChainId): String?

    override val requiredModules: List<String> = emptyList()

    override suspend fun storageKey(runtime: RuntimeSnapshot, scopeValue: Unit): String? {
        val stakingOption = stakingSharedState.selectedOption()
        val electionsSession = electionsSessionRegistry.electionsSessionFor(stakingOption)

        return electionsSession.updaterStorageKey(stakingOption.assetWithChain.chain.id)
    }
}
