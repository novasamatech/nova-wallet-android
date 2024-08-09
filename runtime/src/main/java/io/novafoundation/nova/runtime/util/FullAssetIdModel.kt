package io.novafoundation.nova.runtime.util

import android.os.Parcel
import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import io.novafoundation.nova.runtime.multiNetwork.chain.model.FullChainAssetId

class FullAssetIdModel(val chainId: ChainId, val assetId: Int) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(chainId)
        parcel.writeInt(assetId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<FullAssetIdModel> {
        override fun createFromParcel(parcel: Parcel): FullAssetIdModel {
            return FullAssetIdModel(parcel)
        }

        override fun newArray(size: Int): Array<FullAssetIdModel?> {
            return arrayOfNulls(size)
        }
    }
}

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
