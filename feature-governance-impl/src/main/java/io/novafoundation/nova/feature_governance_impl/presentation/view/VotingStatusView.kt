package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumStatusModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumTimeEstimation
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumVotingModel
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.setReferendumTimeEstimation

class VotingStatusView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle), WithContextExtensions by WithContextExtensions(context) {

    override val providedContext: Context = context

    init {
        orientation = VERTICAL

        View.inflate(context, R.layout.view_voting_status, this)

        background = getRoundedCornerDrawable(R.color.block_background)
        setPadding(0, 16.dp, 0, 16.dp)
    }

    fun setTimeEstimation(timeEstimation: ReferendumTimeEstimation?) {
        if (timeEstimation == null) {
            votingStatusTimeEstimation.makeGone()
            return
        }

        votingStatusTimeEstimation.makeVisible()
        votingStatusTimeEstimation.setReferendumTimeEstimation(timeEstimation, Gravity.END)
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

    fun setAbstainVoters(votersModel: VotersModel?) {
        abstainVotersDetails.setVotersModel(votersModel)
    }

    fun setAbstainVotersClickListener(listener: OnClickListener?) {
        abstainVotersDetails.setOnClickListener(listener)
    }

    fun setStartVoteOnClickListener(listener: OnClickListener?) {
        votingStatusStartVote.setOnClickListener(listener)
    }

    fun setVoteButtonState(state: DescriptiveButtonState) {
        votingStatusStartVote.setState(state)
    }
}
