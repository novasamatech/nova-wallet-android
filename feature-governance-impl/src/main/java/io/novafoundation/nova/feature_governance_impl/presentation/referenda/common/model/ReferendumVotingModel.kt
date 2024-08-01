package io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.novafoundation.nova.feature_governance_api.presentation.referenda.details.ReferendumDetailsPayload

data class ReferendumVotingModel(
    val positiveFraction: Float?,
    val thresholdFraction: Float?,
    @DrawableRes val votingResultIcon: Int,
    @ColorRes val votingResultIconColor: Int,
    val thresholdInfo: String?,
    val thresholdInfoVisible: Boolean,
    val positivePercentage: String,
    val negativePercentage: String,
    val thresholdPercentage: String?,
)

fun ReferendumVotingModel.toDetailsPayload(): ReferendumDetailsPayload.VotingData {
    return ReferendumDetailsPayload.VotingData(
        positiveFraction = positiveFraction,
        thresholdFraction = thresholdFraction,
        votingResultIcon = votingResultIcon,
        votingResultIconColor = votingResultIconColor,
        thresholdInfo = thresholdInfo,
        thresholdInfoVisible = thresholdInfoVisible,
        positivePercentage = positivePercentage,
        negativePercentage = negativePercentage,
        thresholdPercentage = thresholdPercentage,
    )
}

fun ReferendumDetailsPayload.VotingData.toModel(): ReferendumVotingModel {
    return ReferendumVotingModel(
        positiveFraction = positiveFraction,
        thresholdFraction = thresholdFraction,
        votingResultIcon = votingResultIcon,
        votingResultIconColor = votingResultIconColor,
        thresholdInfo = thresholdInfo,
        thresholdInfoVisible = thresholdInfoVisible,
        positivePercentage = positivePercentage,
        negativePercentage = negativePercentage,
        thresholdPercentage = thresholdPercentage,
    )
}
