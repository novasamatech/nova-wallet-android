package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setShimmerVisible
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.databinding.ViewStakingInfoBinding

class StakingInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val binder = ViewStakingInfoBinding.inflate(inflater(), this)

    init {
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
        binder.stakingInfoTitle.text = title
    }

    fun showLoading() {
        binder.stakingInfoGainShimmer.setShimmerVisible(true)
        binder.stakingInfoGain.makeGone()
    }

    fun showGain(gain: String) {
        binder.stakingInfoGainShimmer.setShimmerVisible(false)
        binder.stakingInfoGain.makeVisible()

        binder.stakingInfoGain.text = gain
    }
}
