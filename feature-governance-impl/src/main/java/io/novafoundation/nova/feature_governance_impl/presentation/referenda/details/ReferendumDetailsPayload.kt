package io.novafoundation.nova.feature_governance_impl.presentation.referenda.details

import android.os.Parcelable
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import java.math.BigInteger
import kotlinx.android.parcel.Parcelize

@Parcelize
class ReferendumDetailsPayload(val referendumId: BigInteger, val allowVoting: Boolean = true) : Parcelable

fun ReferendumDetailsPayload.toReferendumId(): ReferendumId {
    return ReferendumId(referendumId)
}
