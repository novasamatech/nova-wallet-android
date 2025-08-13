package io.novafoundation.nova.feature_swap_core.data.assetExchange.conversion.types.hydra.sources.common

import io.novafoundation.nova.feature_swap_core_api.data.network.HydraDxAssetId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain.Asset.Type.Orml.SubType

sealed class HydrationAssetType {

    companion object;

    data object Native : HydrationAssetType()

    class Orml(val assetId: HydraDxAssetId) : HydrationAssetType()

    class Erc20(val assetId: HydraDxAssetId) : HydrationAssetType()
}


fun HydrationAssetType.Companion.fromAsset(chainAsset: Chain.Asset, hydrationAssetId: HydraDxAssetId): HydrationAssetType {
    return when (val type = chainAsset.type) {
        is Chain.Asset.Type.Native -> HydrationAssetType.Native
        is Chain.Asset.Type.Orml -> when (type.subType) {
            SubType.DEFAULT -> HydrationAssetType.Orml(hydrationAssetId)
            SubType.HYDRATION_EVM -> HydrationAssetType.Erc20(hydrationAssetId)
        }

        else -> throw IllegalArgumentException("Unsupported asset type: ${chainAsset.type}")
    }
}
