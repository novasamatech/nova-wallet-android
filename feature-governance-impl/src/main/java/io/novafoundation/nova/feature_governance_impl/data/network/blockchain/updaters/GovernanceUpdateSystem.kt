package io.novafoundation.nova.feature_governance_impl.data.network.blockchain.updaters

import io.novafoundation.nova.common.utils.MultiMap
import io.novafoundation.nova.core.updater.Updater
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceAdditionalState
import io.novafoundation.nova.feature_governance_api.data.source.SupportedGovernanceOption
import io.novafoundation.nova.feature_governance_impl.data.GovernanceSharedState
import io.novafoundation.nova.runtime.ethereum.StorageSharedRequestsBuilderFactory
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.network.updaters.multiChain.MultiChainUpdateSystem
import io.novafoundation.nova.runtime.network.updaters.multiChain.SharedStateBasedUpdater
import io.novafoundation.nova.runtime.network.updaters.multiChain.groupBySyncingChain

class GovernanceUpdateSystem(
    private val chainRegistry: ChainRegistry,
    private val governanceUpdaters: List<SharedStateBasedUpdater<*>>,
    governanceSharedState: GovernanceSharedState,
    storageSharedRequestsBuilderFactory: StorageSharedRequestsBuilderFactory,
) : MultiChainUpdateSystem<GovernanceAdditionalState>(chainRegistry, governanceSharedState, storageSharedRequestsBuilderFactory) {

    override fun getUpdaters(option: SupportedGovernanceOption): MultiMap<ChainId, Updater<*>> {
        return governanceUpdaters.groupBySyncingChain(option.assetWithChain.chain)
    }
}
