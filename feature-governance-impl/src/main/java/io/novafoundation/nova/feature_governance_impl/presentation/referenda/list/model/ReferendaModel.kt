package io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.novafoundation.nova.common.utils.formatting.TimerValue
import io.novafoundation.nova.common.R

class ReferendaStatusModel(val status: String, val count: String)

class ReferendumModel(
    val status: ReferendumStatus,
    val name: String,
    val timeEstimation: ReferendumTimeEstimation?,
    val referendumChips: List<ReferendumChip>,
    val voting: ReferendumVoting?,
    val yourVote: YourVote?
)

enum class ReferendumStatus(@StringRes val nameRes: Int, @ColorRes val colorRes: Int) {
    PASSING(R.string.referendum_status_passing, R.color.darkGreen),
    APPROVED(R.string.referendum_status_approved, R.color.darkGreen),
    IN_QUEUE(R.string.referendum_status_in_queue, R.color.white_64),
    TIMEOUT(R.string.referendum_status_timeout, R.color.white_64),
    CANCELLED(R.string.referendum_status_cancelled, R.color.white_64),
    NOT_PASSING(R.string.referendum_status_not_passing, R.color.red),
    REJECTED(R.string.referendum_status_not_rejected, R.color.red),
}

class ReferendumTimeEstimation(val time: TimerValue, @DrawableRes val iconRes: Int, @ColorRes val colorRes: Int)

class ReferendumChip(val value: String, @DrawableRes val iconRes: Int?)

class ReferendumVoting(
    val isThresholdReached: Boolean,
    val thresholdInfo: String,
    val positivePercentage: String,
    val negativePercentage: String,
    val thresholdPercentage: String,
)

class YourVote(val voteType: VoteType, val yourVoteDetails: String)

enum class VoteType(@StringRes val typeRes: Int, @ColorRes val colorRes: Int) {
    POSITIVE(R.string.referendum_vote_positive_type, R.color.multicolorGreen),
    NEGATIVE(R.string.referendum_vote_negative_type, R.color.red),
}
