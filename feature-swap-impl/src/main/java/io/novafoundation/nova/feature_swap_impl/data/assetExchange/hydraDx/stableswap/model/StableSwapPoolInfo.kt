package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.stableswap.model

import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.common.data.network.runtime.binding.bindPermill
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.Perbill
import io.novafoundation.nova.feature_swap_core.data.network.HydraDxAssetId
import java.math.BigInteger

class StableSwapPoolInfo(
    val poolAssetId: HydraDxAssetId,
    val assets: List<HydraDxAssetId>,
    val initialAmplification: BigInteger,
    val finalAmplification: BigInteger,
    val initialBlock: BigInteger,
    val finalBlock: BigInteger,
    val fee: Perbill,
)

fun bindStablePoolInfo(decoded: Any?, poolTokenId: HydraDxAssetId): StableSwapPoolInfo {
    val struct = decoded.castToStruct()

    return StableSwapPoolInfo(
        poolAssetId = poolTokenId,
        assets = bindList(decoded["assets"], ::bindNumber),
        initialAmplification = bindNumber(struct["initialAmplification"]),
        finalAmplification = bindNumber(struct["finalAmplification"]),
        initialBlock = bindNumber(struct["initialBlock"]),
        finalBlock = bindNumber(struct["finalBlock"]),
        fee = bindPermill(struct["fee"])
    )
}
