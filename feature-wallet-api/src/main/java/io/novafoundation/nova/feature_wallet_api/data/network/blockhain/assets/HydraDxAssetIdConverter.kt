package io.novafoundation.nova.feature_wallet_api.data.network.blockhain.assets

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigInteger

typealias HydraDxAssetId = BigInteger

interface HydraDxAssetIdConverter {

    val systemAssetId: HydraDxAssetId

    suspend fun toOnChainIdOrNull(chainAsset: Chain.Asset): HydraDxAssetId?

    suspend fun toChainAssetOrNull(chain: Chain, onChainId: HydraDxAssetId): Chain.Asset?

    suspend fun allOnChainIds(chain: Chain): Map<HydraDxAssetId, Chain.Asset>
}

fun HydraDxAssetIdConverter.isSystemAsset(assetId: HydraDxAssetId): Boolean {
    return assetId == systemAssetId
}


suspend fun HydraDxAssetIdConverter.toOnChainIdOrThrow(chainAsset: Chain.Asset): HydraDxAssetId {
    return requireNotNull(toOnChainIdOrNull(chainAsset))
}

suspend fun HydraDxAssetIdConverter.toChainAssetOrThrow(chain: Chain, onChainId: HydraDxAssetId):  Chain.Asset {
    return requireNotNull(toChainAssetOrNull(chain, onChainId))
}
