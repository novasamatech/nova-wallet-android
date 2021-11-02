package io.novafoundation.nova.feature_staking_impl.presentation.staking.main.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.view.shape.addRipple
import io.novafoundation.nova.common.view.shape.getBlurDrawable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.RewardEstimation
import kotlinx.android.synthetic.main.view_estimate_earning.view.estimateEarningAmount
import kotlinx.android.synthetic.main.view_estimate_earning.view.stakeMoreActions
import kotlinx.android.synthetic.main.view_estimate_earning.view.stakingMonthGain
import kotlinx.android.synthetic.main.view_estimate_earning.view.stakingYearGain

class EstimateEarningView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {

    val amountInput: EditText
        get() = estimateEarningAmount.amountInput

    init {
        View.inflate(context, R.layout.view_estimate_earning, this)

        with(context) {
            background = addRipple(getBlurDrawable())
        }
    }

    fun setAssetImage(image: Drawable) {
        estimateEarningAmount.setAssetImage(image)
    }

    fun loadAssetImage(imageUrl: String) {
        estimateEarningAmount.loadAssetImage(imageUrl)
    }

    fun setAssetName(name: String) {
        estimateEarningAmount.setAssetName(name)
    }

    fun setAssetBalance(balance: String) {
        estimateEarningAmount.setAssetBalance(balance)
    }

    fun setAssetBalanceDollarAmount(dollarAmount: String?) {
        estimateEarningAmount.setAssetBalanceDollarAmount(dollarAmount)
    }

    fun hideAssetBalanceDollarAmount() {
        estimateEarningAmount.hideAssetDollarAmount()
    }

    fun showAssetBalanceDollarAmount() {
        estimateEarningAmount.showAssetDollarAmount()
    }

    fun showReturnsLoading() {
        stakingMonthGain.showLoading()
        stakingYearGain.showLoading()
    }

    fun hideReturnsLoading() {
        stakingMonthGain.hideLoading()
        stakingYearGain.hideLoading()
    }

    fun populateMonthEstimation(estimation: RewardEstimation) {
        populateEstimationView(stakingMonthGain, estimation)
    }

    fun populateYearEstimation(estimation: RewardEstimation) {
        populateEstimationView(stakingYearGain, estimation)
    }

    private fun populateEstimationView(view: StakingInfoView, estimation: RewardEstimation) {
        view.setBody(estimation.amount)
        if (estimation.fiatAmount == null) {
            view.hideExtraBlockValue()
        } else {
            view.showExtraBlockValue()
            view.setExtraBlockValueText(estimation.fiatAmount)
        }
        view.setExtraBlockAdditionalText(estimation.gain)
    }

    val infoActions: View
        get() = stakeMoreActions
}
