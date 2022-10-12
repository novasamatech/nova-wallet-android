package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.utils.setDrawableStart
import io.novafoundation.nova.feature_governance_impl.R
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

    fun setThresholdModel(model: ThresholdModel) {
        thresholdInfo.text = model.thresholdInfo
        thresholdInfo.setDrawableStart(model.thresholdIconRes, widthInDp = 16, tint = model.thresholdIconColorRes, paddingInDp = 4)
        votesView.setThreshold(model.thresholdFraction)
        votesView.setPositiveVotesFraction(model.positiveVotesFraction)
        positivePercentage.text = model.positiveVotesPercentage
        negativePercentage.text = model.negativeVotesPercentage
        thresholdPercentage.text = model.thresholdPercentage
    }

    class ThresholdModel(
        val thresholdInfo: String,
        @DrawableRes val thresholdIconRes: Int,
        @ColorRes val thresholdIconColorRes: Int,
        val positiveVotesFraction: Float?,
        val thresholdFraction: Float,
        val positiveVotesPercentage: String,
        val negativeVotesPercentage: String,
        val thresholdPercentage: String
    )
}
