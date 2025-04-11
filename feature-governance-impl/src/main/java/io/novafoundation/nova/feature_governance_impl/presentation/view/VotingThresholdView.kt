package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.isVisible
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.feature_governance_impl.databinding.ViewVotingThresholdBinding
import io.novafoundation.nova.feature_governance_impl.presentation.referenda.common.model.ReferendumVotingModel

class VotingThresholdView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val binder = ViewVotingThresholdBinding.inflate(inflater(), this)

    init {
        orientation = VERTICAL
    }

    fun setThresholdModel(maybeModel: ReferendumVotingModel?) = letOrHide(maybeModel) { model ->
        binder.thresholdInfo.isVisible = model.thresholdInfoVisible
        binder.thresholdInfo.text = model.thresholdInfo
        binder.thresholdInfo.setDrawableStart(model.votingResultIcon, widthInDp = 16, tint = model.votingResultIconColor, paddingInDp = 4)
        binder.votesView.setThreshold(model.thresholdFraction)
        binder.votesView.setPositiveVotesFraction(model.positiveFraction)
        binder.positivePercentage.text = model.positivePercentage
        binder.negativePercentage.text = model.negativePercentage
        binder.thresholdPercentage.text = model.thresholdPercentage
    }

    fun setThresholdInfoVisible(visible: Boolean?) = binder.thresholdInfo.letOrHide(visible) { value ->
        binder.thresholdInfo.isVisible = value
    }
}
