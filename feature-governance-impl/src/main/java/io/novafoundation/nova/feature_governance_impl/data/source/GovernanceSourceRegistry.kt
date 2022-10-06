package io.novafoundation.nova.feature_governance_impl.data.source

import io.novafoundation.nova.common.utils.Modules
import io.novafoundation.nova.common.utils.hasModule
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSource
import io.novafoundation.nova.feature_governance_api.data.source.GovernanceSourceRegistry
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.getRuntime

internal class RealGovernanceSourceRegistry(
    private val chainRegistry: ChainRegistry,
    private val governanceV2Source: GovernanceSource,
): GovernanceSourceRegistry {

    override suspend fun sourceFor(chainId: ChainId): GovernanceSource {
        val metadata = chainRegistry.getRuntime(chainId).metadata

        return when {
            metadata.hasModule(Modules.REFERENDA) -> governanceV2Source

            else -> {
                val chainName = chainRegistry.getChain(chainId).name
                throw IllegalArgumentException("Chain $chainName does not have supported governance implementation")
            }
        }
    }
}
