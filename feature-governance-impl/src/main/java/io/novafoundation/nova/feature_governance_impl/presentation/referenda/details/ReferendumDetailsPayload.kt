package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details

import android.os.Parcel
import android.os.Parcelable
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import java.math.BigInteger

class ReferendumDetailsPayload : Parcelable {

    val referendumId: ReferendumId

    constructor(referendumId: ReferendumId) {
        this.referendumId = referendumId
    }

    constructor(parcel: Parcel) {
        val bigInteger = BigInteger(parcel.readString()!!)
        referendumId = ReferendumId(bigInteger)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(referendumId.toString())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ReferendumDetailsPayload> {
        override fun createFromParcel(parcel: Parcel): ReferendumDetailsPayload {
            return ReferendumDetailsPayload(parcel)
        }

        override fun newArray(size: Int): Array<ReferendumDetailsPayload?> {
            return arrayOfNulls(size)
        }
    }
}
