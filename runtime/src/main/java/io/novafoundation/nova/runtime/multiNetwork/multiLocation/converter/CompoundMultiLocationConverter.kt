package io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter

import io.novafoundation.nova.common.utils.tryFindNonNull
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation

internal class CompoundMultiLocationConverter(
    private vararg val delegates: MultiLocationConverter
) : MultiLocationConverter {

    override suspend fun toMultiLocation(chainAsset: Chain.Asset): MultiLocation? {
        return delegates.tryFindNonNull { it.toMultiLocation(chainAsset) }
    }

    override suspend fun toChainAsset(multiLocation: MultiLocation): Chain.Asset? {
        return delegates.tryFindNonNull { it.toChainAsset(multiLocation) }
    }
}
