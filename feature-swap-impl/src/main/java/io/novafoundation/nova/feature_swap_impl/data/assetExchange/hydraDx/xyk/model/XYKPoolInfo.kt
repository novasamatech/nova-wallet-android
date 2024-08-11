package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.xyk.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.castToList
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetId

class XYKPoolInfo(val firstAsset: HydraDxAssetId, val secondAsset: HydraDxAssetId)

fun bindXYKPoolInfo(decoded: Any): XYKPoolInfo {
    val (first, second) = decoded.castToList()

    return XYKPoolInfo(bindNumber(first), bindNumber(second))
}
