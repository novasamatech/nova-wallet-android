package io.novafoundation.nova.feature_xcm_api.converter.asset

import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

// TODO actually support GlobalConsensus junctions and move to V9
interface ChainAssetLocationConverter {

    /**
     * Find asset on [pointOfView] chain that matches given [location]
     */
    suspend fun chainAssetFromRelativeLocation(
        location: RelativeMultiLocation,
        pointOfView: Chain,
    ): Chain.Asset?

    /**
     * Convert given [chainAsset] to absolute location
     */
    suspend fun absoluteLocationFromChainAsset(
        chainAsset: Chain.Asset
    ): AbsoluteMultiLocation?

    /**
     * Convert given [chainAsset] to relative location from the pov of its chain
     */
    suspend fun relativeLocationFromChainAsset(
        chainAsset: Chain.Asset
    ): RelativeMultiLocation?
}

suspend fun ChainAssetLocationConverter.relativeLocationFromChainAssetOrThrow(chainAsset: Chain.Asset): RelativeMultiLocation {
    return requireNotNull(relativeLocationFromChainAsset(chainAsset)) {
        "Cannot convert ${chainAsset.symbol} on ${chainAsset.chainId} to multi-location"
    }
}
