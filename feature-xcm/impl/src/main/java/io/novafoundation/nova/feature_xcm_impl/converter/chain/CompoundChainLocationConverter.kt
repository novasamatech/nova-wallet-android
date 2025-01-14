package io.novafoundation.nova.feature_xcm_impl.converter.chain

import io.novafoundation.nova.common.utils.tryFindNonNull
import io.novafoundation.nova.feature_xcm_api.converter.chain.ChainMultiLocationConverter
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation

internal class CompoundChainLocationConverter(
    private vararg val delegates: ChainMultiLocationConverter
) : ChainMultiLocationConverter {

    override suspend fun toChain(multiLocation: RelativeMultiLocation): Chain? {
        return delegates.tryFindNonNull { it.toChain(multiLocation) }
    }
}
