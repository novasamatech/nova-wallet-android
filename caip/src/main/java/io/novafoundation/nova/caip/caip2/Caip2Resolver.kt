package io.novafoundation.nova.caip.caip2

import io.novafoundation.nova.caip.caip2.identifier.Caip2Identifier
import io.novafoundation.nova.caip.caip2.identifier.Caip2Namespace
import io.novafoundation.nova.common.utils.associateByMultiple
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import kotlinx.coroutines.flow.first

interface Caip2Resolver {

    fun caip2Of(chain: Chain, preferredNamespace: Caip2Namespace): Caip2Identifier?

    fun allCaip2Of(chain: Chain): List<Caip2Identifier>

    suspend fun chainsByCaip2(): Map<String, Chain>
}

internal class RealCaip2Resolver(
    private val chainRegistry: ChainRegistry,
) : Caip2Resolver {

    override fun caip2Of(chain: Chain, preferredNamespace: Caip2Namespace): Caip2Identifier? {
        val allCaips = allCaip2Of(chain)

        return allCaips.find { it.namespace == preferredNamespace } ?: allCaips.firstOrNull()
    }

    override fun allCaip2Of(chain: Chain): List<Caip2Identifier> {
        return buildList {
            if (chain.hasSubstrateRuntime) add(polkadotChain(requireNotNull(chain.genesisHash)))
            if (chain.isEthereumBased) add(eipChain(chain.addressPrefix))
        }
    }

    override suspend fun chainsByCaip2(): Map<String, Chain> {
        val allChains = chainRegistry.currentChains.first()

        return allChains.associateByMultiple { chain -> allCaip2Of(chain).map { it.namespaceWitId } }
    }

    private fun polkadotChain(genesisHash: String): Caip2Identifier {
        return Caip2Identifier.Polkadot(genesisHash)
    }

    private fun eipChain(eipChainId: Int): Caip2Identifier {
        return Caip2Identifier.Eip155(eipChainId.toBigInteger())
    }
}
