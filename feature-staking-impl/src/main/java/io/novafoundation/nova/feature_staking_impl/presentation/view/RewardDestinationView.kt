package io.novafoundation.nova.feature_staking_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_staking_impl.R
import io.novafoundation.nova.feature_staking_impl.presentation.staking.main.model.RewardEstimation

private const val CHECKABLE_DEFAULT = true

class RewardDestinationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var checkable = CHECKABLE_DEFAULT

    init {
        View.inflate(context, R.layout.view_payout_target, this)

        background = context.getRoundedCornerDrawable(R.color.block_background)

        attrs?.let(this::applyAttrs)
    }

    private fun applyAttrs(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.RewardDestinationView) { typedArray ->
        checkable = typedArray.getBoolean(R.styleable.RewardDestinationView_android_checkable, CHECKABLE_DEFAULT)

        if (checkable) {
            val checked = typedArray.getBoolean(R.styleable.RewardDestinationView_android_checked, false)
            setChecked(checked)
        }
        payoutTargetCheck.setVisible(checkable)

        val targetName = typedArray.getString(R.styleable.RewardDestinationView_targetName)
        targetName?.let(::setName)
    }

    fun setName(name: String) {
        payoutTargetName.text = name
    }

    fun setTokenAmount(amount: String) {
        payoutTargetAmountToken.text = amount
    }

    fun setPercentageGain(gain: String) {
        payoutTargetAmountGain.text = gain
    }

    fun setFiatAmount(amount: String?) {
        payoutTargetAmountFiat.setTextOrHide(amount)
    }

    fun setChecked(checked: Boolean) {
        require(checkable) {
            "Cannot check non-checkable view"
        }

        payoutTargetCheck.isChecked = checked
    }
}

fun RewardDestinationView.showRewardEstimation(rewardEstimation: RewardEstimation) {
    setTokenAmount(rewardEstimation.amount)
    setFiatAmount(rewardEstimation.fiatAmount)
    setPercentageGain(rewardEstimation.gain)
}
