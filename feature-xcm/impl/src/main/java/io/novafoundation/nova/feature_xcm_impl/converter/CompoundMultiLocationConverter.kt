package io.novafoundation.nova.feature_xcm_impl.converter

import io.novafoundation.nova.common.utils.tryFindNonNull
import io.novafoundation.nova.feature_xcm_api.converter.MultiLocationConverter
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

internal class CompoundMultiLocationConverter(
    private vararg val delegates: MultiLocationConverter
) : MultiLocationConverter {

    override suspend fun toMultiLocation(chainAsset: Chain.Asset): RelativeMultiLocation? {
        return delegates.tryFindNonNull { it.toMultiLocation(chainAsset) }
    }

    override suspend fun toChainAsset(multiLocation: RelativeMultiLocation): Chain.Asset? {
        return delegates.tryFindNonNull { it.toChainAsset(multiLocation) }
    }
}
