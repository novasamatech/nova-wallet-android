package io.novafoundation.nova.feature_swap_impl.data.assetExchange.assetConversion.locationConverter

import io.novafoundation.nova.feature_wallet_api.domain.model.MultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface MultiLocationConverter {

    suspend fun toMultiLocation(chainAsset: Chain.Asset): MultiLocation?

    suspend fun toChainAsset(multiLocation: MultiLocation): Chain.Asset?
}


suspend fun MultiLocationConverter.toMultiLocationOrThrow(chainAsset: Chain.Asset): MultiLocation {
    return toMultiLocation(chainAsset) ?: error("Failed to convert asset location")
}
