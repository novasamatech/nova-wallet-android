package io.novafoundation.nova.feature_governance_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.feature_governance_impl.databinding.ViewVoteControlBinding

class VoteControlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {

    private val binder = ViewVoteControlBinding.inflate(inflater(), this)

    val nayButton get() = binder.setupReferendumVoteNay
    val abstainButton get() = binder.setupReferendumVoteAbstain
    val ayeButton get() = binder.setupReferendumVoteAye

    init {
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
