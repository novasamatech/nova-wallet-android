package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.ReferendumVotingModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.list.model.setReferendumTimeEstimation
import kotlinx.android.synthetic.main.view_voting_status.view.negativeVotersDetails
import kotlinx.android.synthetic.main.view_voting_status.view.positiveVotersDetails
import kotlinx.android.synthetic.main.view_voting_status.view.votingStatus
import kotlinx.android.synthetic.main.view_voting_status.view.votingStatusStartVote
import kotlinx.android.synthetic.main.view_voting_status.view.votingStatusThreshold
import kotlinx.android.synthetic.main.view_voting_status.view.votingStatusTimeEstimation

class VotingStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    override val providedContext: Context = context

    init {
        orientation = VERTICAL

        View.inflate(context, R.layout.view_voting_status, this)

        background = getRoundedCornerDrawable(R.color.white_8)
        setPadding(0, 16.dp, 0, 16.dp)
    }

    fun setTimeEstimation(timeEstimation: ReferendumTimeEstimation?) {
        if (timeEstimation == null) {
            votingStatusTimeEstimation.makeGone()
            return
        }

        votingStatusTimeEstimation.makeVisible()
        votingStatusTimeEstimation.setReferendumTimeEstimation(timeEstimation)
    }

    fun setStatus(status: String, @ColorRes color: Int) {
        votingStatus.text = status
        votingStatus.setTextColorRes(color)
    }

    fun setThreshold(thresholdModel: ReferendumVotingModel) {
        votingStatusThreshold.setThresholdModel(thresholdModel)
    }

    fun setPositiveVoters(votersModel: VotersView.VotersModel) {
        positiveVotersDetails.setVotersModel(votersModel)
    }

    fun setPositiveVotersClickListener(listener: OnClickListener?) {
        positiveVotersDetails.setOnClickListener(listener)
    }

    fun setNegativeVoters(votersModel: VotersView.VotersModel) {
        negativeVotersDetails.setVotersModel(votersModel)
    }

    fun setNegativeVotersClickListener(listener: OnClickListener?) {
        negativeVotersDetails.setOnClickListener(listener)
    }

    fun setStartVoteOnClickListener(listener: OnClickListener?) {
        votingStatusStartVote.setOnClickListener(listener)
    }

    fun showDetails(show: Boolean) {
        votingStatusThreshold.isVisible = show
        positiveVotersDetails.isVisible = show
        negativeVotersDetails.isVisible = show
        votingStatusStartVote.isVisible = show
    }
}
