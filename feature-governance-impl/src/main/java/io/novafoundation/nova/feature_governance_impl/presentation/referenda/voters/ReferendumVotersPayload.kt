package io.novafoundation.nova.feature_governance_impl.presentation.referenda.voters

import android.os.Parcelable
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.VoteType
import java.math.BigInteger
import kotlinx.android.parcel.Parcelize

@Parcelize
class ReferendumVotersPayload(
    val referendumId: BigInteger,
    val voteType: VoteType
) : Parcelable
