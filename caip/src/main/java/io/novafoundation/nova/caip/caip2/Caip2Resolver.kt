package io.novafoundation.nova.caip.caip2

import io.novafoundation.nova.caip.caip2.identifier.Caip2Identifier
import io.novafoundation.nova.caip.caip2.identifier.Caip2Namespace
import io.novafoundation.nova.runtime.ext.genesisHash
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface Caip2Resolver {

    fun caip2Of(chain: Chain, preferredNamespace: Caip2Namespace): Caip2Identifier?
}

internal class RealCaip2Resolver : Caip2Resolver {

    override fun caip2Of(chain: Chain, preferredNamespace: Caip2Namespace): Caip2Identifier? {
        return when {
            chain.hasSubstrateRuntime && chain.isEthereumBased -> when (preferredNamespace) {
                Caip2Namespace.EIP155 -> eipChain(chain.addressPrefix)
                Caip2Namespace.POLKADOT -> polkadotChain(requireNotNull(chain.genesisHash))
            }

            chain.hasSubstrateRuntime -> polkadotChain(requireNotNull(chain.genesisHash))

            chain.isEthereumBased -> eipChain(chain.addressPrefix)

            else -> null
        }
    }

    private fun polkadotChain(genesisHash: String): Caip2Identifier {
        return Caip2Identifier.Polkadot(genesisHash)
    }

    private fun eipChain(eipChainId: Int): Caip2Identifier {
        return Caip2Identifier.Eip155(eipChainId.toBigInteger())
    }
}
