package io.novafoundation.nova.feature_staking_impl.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getRoundedCornerDrawable
import io.novafoundation.nova.feature_staking_impl.R
import kotlinx.android.synthetic.main.view_payout_target.view.payoutTargetAmountFiat
import kotlinx.android.synthetic.main.view_payout_target.view.payoutTargetAmountGain
import kotlinx.android.synthetic.main.view_payout_target.view.payoutTargetAmountToken
import kotlinx.android.synthetic.main.view_payout_target.view.payoutTargetCheck
import kotlinx.android.synthetic.main.view_payout_target.view.payoutTargetName

class RewardDestinationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        View.inflate(context, R.layout.view_payout_target, this)

        background = context.getRoundedCornerDrawable(R.color.white_8)

        attrs?.let(this::applyAttrs)
    }

    private fun applyAttrs(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.RewardDestinationView)

        val checked = typedArray.getBoolean(R.styleable.RewardDestinationView_android_checked, false)
        setChecked(checked)

        val targetName = typedArray.getString(R.styleable.RewardDestinationView_targetName)
        targetName?.let(::setName)

        typedArray.recycle()
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
        payoutTargetCheck.isChecked = checked
    }
}
