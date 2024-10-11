package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setShimmerVisible
import io.novafoundation.nova.feature_staking_impl.R

class StakingInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_staking_info, this)

        orientation = VERTICAL

        attrs?.let { applyAttributes(it) }
    }

    private fun applyAttributes(attributeSet: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.StakingInfoView)

        val title = typedArray.getString(R.styleable.StakingInfoView_title)
        title?.let { setTitle(title) }

        typedArray.recycle()
    }

    fun setTitle(title: String) {
        stakingInfoTitle.text = title
    }

    fun showLoading() {
        stakingInfoGainShimmer.setShimmerVisible(true)
        stakingInfoGain.makeGone()
    }

    fun showGain(gain: String) {
        stakingInfoGainShimmer.setShimmerVisible(false)
        stakingInfoGain.makeVisible()

        stakingInfoGain.text = gain
    }
}
