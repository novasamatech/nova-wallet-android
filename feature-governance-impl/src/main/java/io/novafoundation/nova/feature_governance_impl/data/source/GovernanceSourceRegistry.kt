package io.novafoundation.nova.feature_governance_impl.data.source

import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId

internal class RealGovernanceSourceRegistry(
    private val chainRegistry: ChainRegistry,
    private val governanceV2Source: GovernanceSource,
    private val governanceV1Source: GovernanceSource,
) : GovernanceSourceRegistry {

    override suspend fun sourceFor(chainId: ChainId): GovernanceSource {
        val chain = chainRegistry.getChain(chainId)

        return when (chain.governance) {
            Chain.Governance.V1 -> governanceV1Source
            Chain.Governance.V2 -> governanceV2Source
            Chain.Governance.NONE -> error("Chain ${chain.name} does not have supported governance implementation")
        }
    }
}
