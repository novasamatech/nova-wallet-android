package io.novafoundation.nova.runtime.util

import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId
import kotlinx.android.parcel.Parcelize

@Parcelize
class FullAssetIdModel(val chainId: ChainId, val assetId: Int) : Parcelable

fun FullChainAssetId.toModel(): FullAssetIdModel {
    return FullAssetIdModel(
        chainId = chainId,
        assetId = assetId
    )
}

fun FullAssetIdModel.toDomain(): FullChainAssetId {
    return FullChainAssetId(
        chainId = chainId,
        assetId = assetId
    )
}
