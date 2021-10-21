package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getCornersStateDrawable
import io.novafoundation.nova.common.view.shape.getCutCornerDrawable
import kotlinx.android.synthetic.main.view_staking_amount.view.stakingAmountInput
import kotlinx.android.synthetic.main.view_staking_amount.view.stakingAssetBalance
import kotlinx.android.synthetic.main.view_staking_amount.view.stakingAssetDollarAmount
import kotlinx.android.synthetic.main.view_staking_amount.view.stakingAssetImage
import kotlinx.android.synthetic.main.view_staking_amount.view.stakingAssetToken

class AmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    val amountInput: EditText
        get() = stakingAmountInput

    init {
        View.inflate(context, R.layout.view_staking_amount, this)

        setBackground()

        applyAttributes(attrs)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        amountInput.isEnabled = enabled
        amountInput.inputType = if (enabled) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_NULL
    }

    override fun childDrawableStateChanged(child: View?) {
        refreshDrawableState()
    }

    // Make this view be aware of amountInput state changes (i.e. state_focused)
    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val fieldState: IntArray? = amountInput.drawableState

        val need = fieldState?.size ?: 0

        val selfState = super.onCreateDrawableState(extraSpace + need)

        return mergeDrawableStates(selfState, fieldState)
    }

    private fun setBackground() {
        background = context.getCornersStateDrawable(
            focusedDrawable = context.getCutCornerDrawable(
                R.color.blurColor,
                R.color.white
            ),
            idleDrawable = context.getCutCornerDrawable(
                R.color.blurColor,
                R.color.white_40
            )
        )
    }

    private fun applyAttributes(attributeSet: AttributeSet?) {
        attributeSet?.let {
            val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.AmountView)

            val enabled = typedArray.getBoolean(R.styleable.AmountView_android_enabled, true)
            isEnabled = enabled

            typedArray.recycle()
        }
    }

    fun setAssetImage(image: Drawable) {
        stakingAssetImage.setImageDrawable(image)
    }

    fun setAssetImageResource(imageRes: Int) {
        stakingAssetImage.setImageResource(imageRes)
    }

    fun setAssetName(name: String) {
        stakingAssetToken.text = name
    }

    fun setAssetBalance(balance: String) {
        stakingAssetBalance.text = balance
    }

    fun setAssetBalanceDollarAmount(dollarAmount: String?) {
        stakingAssetDollarAmount.setTextOrHide(dollarAmount)
    }

    fun hideAssetDollarAmount() {
        stakingAssetDollarAmount.makeGone()
    }

    fun showAssetDollarAmount() {
        stakingAssetDollarAmount.makeVisible()
    }
}
