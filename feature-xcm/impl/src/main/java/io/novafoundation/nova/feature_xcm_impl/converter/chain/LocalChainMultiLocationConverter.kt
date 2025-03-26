package io.novafoundation.nova.feature_xcm_impl.converter.chain

import io.novafoundation.nova.feature_xcm_api.converter.chain.ChainMultiLocationConverter
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.isHere
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

internal class LocalChainMultiLocationConverter(
    val chain: Chain
) : ChainMultiLocationConverter {

    override suspend fun toChain(multiLocation: RelativeMultiLocation): Chain? {
        return chain.takeIf { multiLocation.isHere() }
    }
}
