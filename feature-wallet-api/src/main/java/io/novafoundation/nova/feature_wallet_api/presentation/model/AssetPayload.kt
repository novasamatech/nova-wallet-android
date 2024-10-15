package io.novafoundation.nova.feature_wallet_api.presentation.model

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.parcelize.Parcelize

@Parcelize
class AssetPayload(val chainId: ChainId, val chainAssetId: Int) : Parcelable

val AssetPayload.fullChainAssetId: FullChainAssetId
    get() = FullChainAssetId(chainId, chainAssetId)

fun FullChainAssetId.toAssetPayload(): AssetPayload = AssetPayload(chainId, assetId)
