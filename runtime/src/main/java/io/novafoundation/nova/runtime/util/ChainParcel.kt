package io.novafoundation.nova.runtime.util

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ChainParcel(val chain: Chain) : Parcelable {

    constructor(parcel: Parcel) : this(readChain(parcel))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeSerializable(chain)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ChainParcel> {
        override fun createFromParcel(parcel: Parcel): ChainParcel {
            return ChainParcel(parcel)
        }

        override fun newArray(size: Int): Array<ChainParcel?> {
            return arrayOfNulls(size)
        }

        private fun readChain(parcel: Parcel): Chain {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                parcel.readSerializable(null, Chain::class.java)!!
            } else {
                parcel.readSerializable() as Chain
            }
        }
    }
}
