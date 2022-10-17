package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumVotingModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTimeEstimation
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

    fun setStatus(referendumStatusModel: ReferendumStatusModel) {
        votingStatus.text = referendumStatusModel.name
        votingStatus.setTextColorRes(referendumStatusModel.colorRes)
    }

    fun setVotingModel(thresholdModel: ReferendumVotingModel?) {
        votingStatusThreshold.setThresholdModel(thresholdModel)
    }

    fun setPositiveVoters(votersModel: VotersModel?) {
        positiveVotersDetails.setVotersModel(votersModel)
    }

    fun setPositiveVotersClickListener(listener: OnClickListener?) {
        positiveVotersDetails.setOnClickListener(listener)
    }

    fun setNegativeVoters(votersModel: VotersModel?) {
        negativeVotersDetails.setVotersModel(votersModel)
    }

    fun setNegativeVotersClickListener(listener: OnClickListener?) {
        negativeVotersDetails.setOnClickListener(listener)
    }

    fun setStartVoteOnClickListener(listener: OnClickListener?) {
        votingStatusStartVote.setOnClickListener(listener)
    }

    fun setVoteButtonVisible(visible: Boolean) {
        votingStatusStartVote.setVisible(visible)
    }
}
