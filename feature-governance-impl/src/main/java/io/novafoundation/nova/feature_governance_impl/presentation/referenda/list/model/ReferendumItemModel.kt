package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.feature_governance_api.data.network.blockhain.model.ReferendumId
import io.novafoundation.nova.feature_governance_impl.presentation.view.VotesView
import io.novafoundation.nova.feature_governance_impl.presentation.view.VotingThresholdView

data class ReferendaGroupModel(val name: String, val badge: String)

data class ReferendumModel(
    val id: ReferendumId,
    val status: ReferendumStatusModel,
    val name: String,
    val timeEstimation: ReferendumTimeEstimation?,
    val track: ReferendumTrackModel?,
    val number: String,
    val voting: ReferendumVotingModel?,
    val yourVote: YourVoteModel?
)

data class ReferendumTrackModel(val name: String, @DrawableRes val iconRes: Int)

data class ReferendumStatusModel(val name: String, @ColorRes val colorRes: Int)

typealias ReferendumTimeEstimationStyleRefresher = () -> ReferendumTimeEstimation.TextStyle

sealed class ReferendumTimeEstimation {

    data class TextStyle(
        @DrawableRes val iconRes: Int,
        @ColorRes val colorRes: Int
    ) {
        companion object
    }

    data class Timer(
        val time: TimerValue,
        @StringRes val timeFormat: Int,
        val textStyleRefresher: ReferendumTimeEstimationStyleRefresher,
    ) : ReferendumTimeEstimation()

    data class Text(
        val text: String,
        val textStyle: TextStyle
    ) : ReferendumTimeEstimation()
}

data class ReferendumVotingModel(
    val positiveFraction: Float?,
    val thresholdFraction: Float,
    @DrawableRes val votingResultIcon: Int,
    @ColorRes val votingResultIconColor: Int,
    val thresholdInfo: String,
    val positivePercentage: String,
    val negativePercentage: String,
    val thresholdPercentage: String,
)

data class YourVoteModel(val voteType: String, @ColorRes val colorRes: Int, val details: String)

fun VotingThresholdView.setModel(voting: ReferendumVotingModel) {
    val thresholdModel = VotingThresholdView.ThresholdModel(
        voting.thresholdInfo,
        voting.votingResultIcon,
        voting.votingResultIconColor,
        voting.positiveFraction,
        voting.thresholdFraction,
        voting.positivePercentage,
        voting.negativePercentage,
        voting.thresholdPercentage
    )
    setThresholdModel(thresholdModel)
}

fun VotesView.setModel(voting: ReferendumVotingModel) {
    setPositiveVotesFraction(voting.positiveFraction)
    setThreshold(voting.thresholdFraction)
}
