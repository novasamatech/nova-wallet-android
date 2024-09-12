package io.novafoundation.nova.feature_governance_impl.presentation.referenda.vote.setup.common

import android.os.Parcelable
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import kotlinx.android.parcel.Parcelize
import java.math.BigInteger

@Parcelize
class SetupVotePayload(
    val _referendumId: BigInteger
) : Parcelable

val SetupVotePayload.referendumId: ReferendumId
    get() = ReferendumId(_referendumId)
