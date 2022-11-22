package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.view.PrimaryButton
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.shape.getRippleMask
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.view_estimate_earning.view.estimateEarningTitle
import kotlinx.android.synthetic.main.view_estimate_earning.view.stakeMoreActions
import kotlinx.android.synthetic.main.view_estimate_earning.view.stakingMonthGain
import kotlinx.android.synthetic.main.view_estimate_earning.view.stakingYearGain
import kotlinx.android.synthetic.main.view_estimate_earning.view.startStakingBtn

class EstimateEarningView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_estimate_earning, this)

        with(context) {
            background = addRipple(getBlockDrawable(), mask = getRippleMask())
        }
    }

    fun setTitle(title: String) {
        estimateEarningTitle.text = title
    }

    fun showLoading() {
        stakingMonthGain.showLoading()
        stakingYearGain.showLoading()
    }

    fun showGains(
        monthlyGain: String,
        yearlyGain: String,
    ) {
        stakingMonthGain.showGain(monthlyGain)
        stakingYearGain.showGain(yearlyGain)
    }

    val infoActions: View
        get() = stakeMoreActions

    val startStakingButton: PrimaryButton
        get() = startStakingBtn
}
