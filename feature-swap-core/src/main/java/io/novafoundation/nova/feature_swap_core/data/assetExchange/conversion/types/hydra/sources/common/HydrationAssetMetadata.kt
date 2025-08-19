package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common

import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId

class HydrationAssetMetadata(
    val assetId: HydraDxAssetId,
    val decimals: Int,
    val assetType: String
) {

    fun determineAssetType(nativeId: HydraDxAssetId): HydrationAssetType {
        return when {
            assetId == nativeId -> HydrationAssetType.Native
            assetType == "Erc20" -> HydrationAssetType.Erc20(assetId)
            else -> HydrationAssetType.Orml(assetId)
        }
    }
}

class HydrationAssetMetadataMap(
    private val nativeId: HydraDxAssetId,
    private val metadataMap: Map<HydraDxAssetId, HydrationAssetMetadata>
) {

    fun getAssetType(assetId: HydraDxAssetId): HydrationAssetType? {
        val metadata = metadataMap[assetId] ?: return null

        return metadata.determineAssetType(nativeId)
    }

    fun getDecimals(assetId: HydraDxAssetId): Int? {
        val metadata = metadataMap[assetId] ?: return null

        return metadata.decimals
    }
}
