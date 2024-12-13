package io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.chain

import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ChainMultiLocationConverterFactory(private val chainRegistry: ChainRegistry) {

    fun resolveSelfAndChildrenParachains(self: Chain): ChainMultiLocationConverter {
        return CompoundChainLocationConverter(
            LocalChainMultiLocationConverter(self),
            ChildParachainLocationConverter(self, chainRegistry)
        )
    }
}
