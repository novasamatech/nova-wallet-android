package io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter.chain

import io.novafoundation.nova.common.utils.tryFindNonNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation

internal class CompoundChainLocationConverter(
    private vararg val delegates: ChainMultiLocationConverter
) : ChainMultiLocationConverter {

    override suspend fun toChain(multiLocation: MultiLocation): Chain? {
        return delegates.tryFindNonNull { it.toChain(multiLocation) }
    }
}
