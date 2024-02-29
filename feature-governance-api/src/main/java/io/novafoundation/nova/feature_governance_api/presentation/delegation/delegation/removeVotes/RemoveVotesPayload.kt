package io.novafoundation.nova.feature_governance_api.presentation.delegation.delegation.removeVotes

import android.os.Parcelable
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.TrackId
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

@Parcelize
class RemoveVotesPayload(
    private val _trackIdsRaw: List<BigInteger>
) : Parcelable {

    val trackIds = _trackIdsRaw.map(::TrackId)
}
