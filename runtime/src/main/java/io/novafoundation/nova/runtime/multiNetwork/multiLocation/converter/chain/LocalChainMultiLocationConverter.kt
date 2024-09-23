package io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.chain

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.isHere

internal class LocalChainMultiLocationConverter(
    val chain: Chain
) : ChainMultiLocationConverter {

    override suspend fun toChain(multiLocation: MultiLocation): Chain? {
        return chain.takeIf { multiLocation.isHere() }
    }
}
