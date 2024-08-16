package io.novafoundation.nova.runtime.util

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ChainAssetParcel(val value: Chain.Asset) : Parcelable {

    constructor(parcel: Parcel) : this(readAsset(parcel))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(value)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ChainAssetParcel> {
        override fun createFromParcel(parcel: Parcel): ChainAssetParcel {
            return ChainAssetParcel(parcel)
        }

        override fun newArray(size: Int): Array<ChainAssetParcel?> {
            return arrayOfNulls(size)
        }

        private fun readAsset(parcel: Parcel): Chain.Asset {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                parcel.readSerializable(null, Chain.Asset::class.java)!!
            } else {
                parcel.readSerializable() as Chain.Asset
            }
        }
    }
}
