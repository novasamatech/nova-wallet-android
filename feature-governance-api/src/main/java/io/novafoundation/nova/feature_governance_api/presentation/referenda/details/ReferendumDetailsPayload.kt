package io.novafoundation.nova.feature_governance_api.presentation.referenda.details

import android.os.Parcelable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import java.math.BigInteger
import kotlinx.parcelize.Parcelize

@Parcelize
class ReferendumDetailsPayload(
    val referendumId: BigInteger,
    val allowVoting: Boolean,
    val prefilledData: PrefilledData?
) : Parcelable {

    @Parcelize
    class PrefilledData(
        val referendumNumber: String,
        val title: String,
        val status: StatusData,
        val voting: VotingData?
    ) : Parcelable

    @Parcelize
    class StatusData(
        val statusName: String,
        val statusColor: Int
    ) : Parcelable

    @Parcelize
    class VotingData(
        val positiveFraction: Float?,
        val thresholdFraction: Float?,
        @DrawableRes val votingResultIcon: Int,
        @ColorRes val votingResultIconColor: Int,
        val thresholdInfo: String?,
        val thresholdInfoVisible: Boolean,
        val positivePercentage: String,
        val negativePercentage: String,
        val thresholdPercentage: String?,
    ) : Parcelable
}

fun ReferendumDetailsPayload.toReferendumId(): ReferendumId {
    return ReferendumId(referendumId)
}
