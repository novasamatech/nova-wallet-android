package io.novafoundation.nova.feature_xcm_api.converter

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.feature_xcm_api.multiLocation.MultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation

interface MultiLocationConverter {

    suspend fun toMultiLocation(chainAsset: Chain.Asset): RelativeMultiLocation?

    suspend fun toChainAsset(multiLocation: RelativeMultiLocation): Chain.Asset?
}

suspend fun MultiLocationConverter.toMultiLocationOrThrow(chainAsset: Chain.Asset): RelativeMultiLocation {
    return toMultiLocation(chainAsset) ?: error("Failed to convert asset location")
}
