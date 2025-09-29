package io.novafoundation.nova.feature_xcm_api.converter.asset

import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface ChainAssetLocationConverter {

    suspend fun chainAssetFromRelativeLocation(
        location: RelativeMultiLocation,
        pointOfView: Chain,
    ): Chain.Asset?

    suspend fun absoluteLocationFromChainAsset(
        chainAsset: Chain.Asset
    ): AbsoluteMultiLocation?

    suspend fun relativeLocationFromChainAsset(
        chainAsset: Chain.Asset
    ): RelativeMultiLocation?
}

suspend fun ChainAssetLocationConverter.relativeLocationFromChainAssetOrThrow(chainAsset: Chain.Asset): RelativeMultiLocation {
    return requireNotNull(relativeLocationFromChainAsset(chainAsset)) {
        "Cannot convert ${chainAsset.symbol} on ${chainAsset.chainId} to multi-location"
    }
}
