package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import io.novafoundation.nova.common.presentation.DescriptiveButtonState
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.view.setState
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.databinding.ViewVotingStatusBinding
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

    private val binder = ViewVotingStatusBinding.inflate(inflater(), this)

    init {
        orientation = VERTICAL

        background = getRoundedCornerDrawable(R.color.block_background)
        setPadding(0, 16.dp, 0, 16.dp)
    }

    fun setTimeEstimation(timeEstimation: ReferendumTimeEstimation?) {
        if (timeEstimation == null) {
            binder.votingStatusTimeEstimation.makeGone()
            return
        }

        binder.votingStatusTimeEstimation.makeVisible()
        binder.votingStatusTimeEstimation.setReferendumTimeEstimation(timeEstimation, Gravity.END)
    }

    fun setStatus(referendumStatusModel: ReferendumStatusModel) {
        binder.votingStatus.text = referendumStatusModel.name
        binder.votingStatus.setTextColorRes(referendumStatusModel.colorRes)
    }

    fun setVotingModel(thresholdModel: ReferendumVotingModel?) {
        binder.votingStatusThreshold.setThresholdModel(thresholdModel)
    }

    fun setPositiveVoters(votersModel: VotersModel?) {
        binder.positiveVotersDetails.setVotersModel(votersModel)
    }

    fun setPositiveVotersClickListener(listener: OnClickListener?) {
        binder.positiveVotersDetails.setOnClickListener(listener)
    }

    fun setNegativeVoters(votersModel: VotersModel?) {
        binder.negativeVotersDetails.setVotersModel(votersModel)
    }

    fun setNegativeVotersClickListener(listener: OnClickListener?) {
        binder.negativeVotersDetails.setOnClickListener(listener)
    }

    fun setAbstainVoters(votersModel: VotersModel?) {
        binder.abstainVotersDetails.setVotersModel(votersModel)
    }

    fun setAbstainVotersClickListener(listener: OnClickListener?) {
        binder.abstainVotersDetails.setOnClickListener(listener)
    }

    fun setStartVoteOnClickListener(listener: OnClickListener?) {
        binder.votingStatusStartVote.setOnClickListener(listener)
    }

    fun setVoteButtonState(state: DescriptiveButtonState) {
        binder.votingStatusStartVote.setState(state)
    }
}
