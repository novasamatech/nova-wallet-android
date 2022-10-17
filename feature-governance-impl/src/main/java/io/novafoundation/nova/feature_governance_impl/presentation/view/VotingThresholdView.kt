package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.useNonNullOrHide
import io.novafoundation.nova.feature_governance_impl.R
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumVotingModel
import kotlinx.android.synthetic.main.view_voting_threshold.view.negativePercentage
import kotlinx.android.synthetic.main.view_voting_threshold.view.positivePercentage
import kotlinx.android.synthetic.main.view_voting_threshold.view.thresholdInfo
import kotlinx.android.synthetic.main.view_voting_threshold.view.thresholdPercentage
import kotlinx.android.synthetic.main.view_voting_threshold.view.votesView

class VotingThresholdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_voting_threshold, this)
        orientation = VERTICAL
    }

    fun setThresholdModel(maybeModel: ReferendumVotingModel?) = useNonNullOrHide(maybeModel) { model ->
        thresholdInfo.text = model.thresholdInfo
        thresholdInfo.setDrawableStart(model.votingResultIcon, widthInDp = 16, tint = model.votingResultIconColor, paddingInDp = 4)
        votesView.setThreshold(model.thresholdFraction)
        votesView.setPositiveVotesFraction(model.positiveFraction)
        positivePercentage.text = model.positivePercentage
        negativePercentage.text = model.negativePercentage
        thresholdPercentage.text = model.thresholdPercentage
    }
}
