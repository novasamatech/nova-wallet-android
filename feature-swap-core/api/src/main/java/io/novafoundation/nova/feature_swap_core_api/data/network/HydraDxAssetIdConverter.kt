package io.novafoundation.nova.feature_swap_core_api.data.network

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

typealias HydraDxAssetId = BigInteger
typealias HydraRemoteToLocalMapping = Map<HydraDxAssetId, Chain.Asset>

interface HydraDxAssetIdConverter {

    val systemAssetId: HydraDxAssetId

    suspend fun toOnChainIdOrNull(chainAsset: Chain.Asset): HydraDxAssetId?

    suspend fun toChainAssetOrNull(chain: Chain, onChainId: HydraDxAssetId): Chain.Asset?

    suspend fun allOnChainIds(chain: Chain): HydraRemoteToLocalMapping
}

fun HydraDxAssetIdConverter.isSystemAsset(assetId: HydraDxAssetId): Boolean {
    return assetId == systemAssetId
}

suspend fun HydraDxAssetIdConverter.toOnChainIdOrThrow(chainAsset: Chain.Asset): HydraDxAssetId {
    return requireNotNull(toOnChainIdOrNull(chainAsset))
}

suspend fun HydraDxAssetIdConverter.toChainAssetOrThrow(chain: Chain, onChainId: HydraDxAssetId): Chain.Asset {
    return requireNotNull(toChainAssetOrNull(chain, onChainId))
}
