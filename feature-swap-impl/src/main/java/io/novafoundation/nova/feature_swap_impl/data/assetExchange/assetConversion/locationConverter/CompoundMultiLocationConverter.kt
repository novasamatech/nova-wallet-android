package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter

import io.novafoundation.nova.common.utils.tryFindNonNull
import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class CompoundMultiLocationConverter(
    private vararg val delegates: MultiLocationConverter
) : MultiLocationConverter {

    override suspend fun toMultiLocation(chainAsset: Chain.Asset): MultiLocation? {
        return delegates.tryFindNonNull { it.toMultiLocation(chainAsset) }
    }

    override suspend fun toChainAsset(multiLocation: MultiLocation): Chain.Asset? {
        return delegates.tryFindNonNull { it.toChainAsset(multiLocation) }
    }
}
