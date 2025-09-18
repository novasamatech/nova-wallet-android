package io.novafoundation.nova.feature_xcm_impl.converter

import io.novafoundation.nova.common.utils.TokenSymbol
import io.novafoundation.nova.feature_xcm_api.multiLocation.AbsoluteMultiLocation
import io.novafoundation.nova.feature_xcm_api.multiLocation.RelativeMultiLocation
import io.novafoundation.nova.runtime.ext.findAssetByNormalizedSymbol
import io.novafoundation.nova.runtime.ext.fullId
import io.novafoundation.nova.runtime.ext.normalizeSymbol
import io.novafoundation.nova.runtime.multiNetwork.ChainRegistry
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import io.novasama.substrate_sdk_android.extensions.tryFindNonNull

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


class RealChainAssetLocationConverter(
    // Fetched from configs, see https://github.com/novasamatech/nova-utils/blob/f7623740462f406bee58f58f05ecda0ba21af495/xcm/v8/transfers_dynamic.json#L2
    private val reservesById: Map<ChainAssetReserveId, ChainAssetReserveConfig>,

    // By default, asset reserve id is equal to its symbol
    // This mapping allows to override that for cases like multiple reserves (Statemine & Polkadot for DOT)

    // Fetched from configs, see https://github.com/novasamatech/nova-utils/blob/f7623740462f406bee58f58f05ecda0ba21af495/xcm/v8/transfers_dynamic.json#L213
    private val assetToReserveIdOverrides: Map<FullChainAssetId, ChainAssetReserveId>,

    private val chainLocationConverter: ChainLocationConverter,
    private val chainRegistry: ChainRegistry,
) : ChainAssetLocationConverter {

    private val reserveIdsByLocation = reservesById.entries.groupBy(
        keySelector = { (_, reserve) -> reserve.reserveLocation },
        valueTransform = { (_, reserve) -> reserve }
    )

    // Association works assuming multiple assets on one chain cannot map to the same reserve
    private val assetIdByReserveIdOverrideAndChain = assetToReserveIdOverrides.entries.associateBy(
        keySelector = { (assetId, reserveId) -> reserveId to assetId.chainId },
        valueTransform = { (assetId, _) -> assetId.assetId }
    )

    override suspend fun chainAssetFromRelativeLocation(
        location: RelativeMultiLocation,
        pointOfView: Chain
    ): Chain.Asset? {
        val povLocation = chainLocationConverter.absoluteLocationFromChain(pointOfView.id)
        val assetAbsoluteLocation = location.absoluteLocationViewingFrom(povLocation)

        return findAssetFromReserveLocation(assetAbsoluteLocation, pointOfView)
    }

    override suspend fun absoluteLocationFromChainAsset(chainAsset: Chain.Asset): AbsoluteMultiLocation? {
        val reserveId = getReserveId(chainAsset)
        return reservesById[reserveId]?.reserveLocation
    }

    override suspend fun relativeLocationFromChainAsset(chainAsset: Chain.Asset): RelativeMultiLocation? {
        val chainLocation = chainLocationConverter.absoluteLocationFromChain(chainAsset.chainId)
        val absoluteAssetLocation = absoluteLocationFromChainAsset(chainAsset)
        return absoluteAssetLocation?.fromPointOfViewOf(chainLocation)
    }

    private fun findAssetFromReserveLocation(
        reserveLocation: AbsoluteMultiLocation,
        povChain: Chain,
    ): Chain.Asset? {
        val allMatchingReserves = reserveIdsByLocation[reserveLocation] ?: return null

        return allMatchingReserves.tryFindNonNull { matchingReserve ->
            // We are using povChain id here as we interested in reserve override with the given reserveId that
            // happens on povChain
            val overrideKey = matchingReserve.reserveId to povChain.id
            val overriddenAssetId = assetIdByReserveIdOverrideAndChain[overrideKey]

            if (overriddenAssetId != null) {
                // We found override, this means we can return the relevant asset right away
                povChain.assetsById.getValue(overriddenAssetId)
            } else {
                // If we haven't found an override, it means that either override is not used and reserveId=asset symbol
                // or this reserve is not the right one for asset we are searching for on pov chain
                povChain.findAssetByNormalizedSymbol(TokenSymbol(matchingReserve.reserveId))
            }
        }
    }

    private fun getReserveId(chainAsset: Chain.Asset): ChainAssetReserveId {
        return assetToReserveIdOverrides[chainAsset.fullId] ?: chainAsset.normalizeSymbol()
    }
}
