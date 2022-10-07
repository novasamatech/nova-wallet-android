package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.VotesView

data class ReferendaStatusModel(val status: String, val count: String)

data class ReferendumModel(
    val id: String,
    val status: ReferendumStatus,
    val name: String,
    val timeEstimation: ReferendumTimeEstimation?,
    val track: ReferendumTrack,
    val number: String,
    val voting: ReferendumVoting?,
    val yourVote: YourVote?
)

data class ReferendumTrack(val name: String, @DrawableRes val iconRes: Int)

data class ReferendumStatus(val name: String, @ColorRes val colorRes: Int)

data class ReferendumTimeEstimation(val time: String, @DrawableRes val iconRes: Int, @ColorRes val colorRes: Int)

data class ReferendumVoting(
    val positiveFraction: Float?,
    val thresholdFraction: Float,
    val votingResultIcon: Int,
    val votingResultIconColor: Int,
    val thresholdInfo: String,
    val positivePercentage: String,
    val negativePercentage: String,
    val thresholdPercentage: String,
)

data class YourVote(val voteType: String, @ColorRes val colorRes: Int, val details: String)

fun VotesView.setModel(voting: ReferendumVoting) {
    setPositiveVotesFraction(voting.positiveFraction)
    setThreshold(voting.thresholdFraction)
}
