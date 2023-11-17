package io.novafoundation.nova.runtime.multiNetwork.multiLocation.converter

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.multiLocation.MultiLocation

interface MultiLocationConverter {

    suspend fun toMultiLocation(chainAsset: Chain.Asset): MultiLocation?

    suspend fun toChainAsset(multiLocation: MultiLocation): Chain.Asset?
}

suspend fun MultiLocationConverter.toMultiLocationOrThrow(chainAsset: Chain.Asset): MultiLocation {
    return toMultiLocation(chainAsset) ?: error("Failed to convert asset location")
}
