package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.feature_governance_impl.R
import kotlinx.android.synthetic.main.view_vote_control.view.setupReferendumVoteAbstain
import kotlinx.android.synthetic.main.view_vote_control.view.setupReferendumVoteAye
import kotlinx.android.synthetic.main.view_vote_control.view.setupReferendumVoteNay

class VoteControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    val nayButton get() = setupReferendumVoteNay
    val abstainButton get() = setupReferendumVoteAbstain
    val ayeButton get() = setupReferendumVoteAye

    init {
        View.inflate(context, R.layout.view_vote_control, this)
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_HORIZONTAL
    }

    fun setNayClickListener(listener: OnClickListener?) {
        nayButton.setOnClickListener(listener)
    }

    fun setAbstainClickListener(listener: OnClickListener?) {
        abstainButton.setOnClickListener(listener)
    }

    fun setAyeClickListener(listener: OnClickListener?) {
        ayeButton.setOnClickListener(listener)
    }
}
