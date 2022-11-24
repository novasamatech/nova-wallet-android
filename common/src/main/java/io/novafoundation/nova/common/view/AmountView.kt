package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.shape.getCornersStateDrawable
import kotlinx.android.synthetic.main.view_choose_amount_old.view.stakingAmountInput
import kotlinx.android.synthetic.main.view_choose_amount_old.view.stakingAssetBalance
import kotlinx.android.synthetic.main.view_choose_amount_old.view.stakingAssetPriceAmount
import kotlinx.android.synthetic.main.view_choose_amount_old.view.stakingAssetImage
import kotlinx.android.synthetic.main.view_choose_amount_old.view.stakingAssetToken

class AmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    val amountInput: EditText
        get() = stakingAmountInput

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
        View.inflate(context, R.layout.view_choose_amount_old, this)

        setBackground()

        applyAttributes(attrs)
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        amountInput.isEnabled = enabled
        amountInput.inputType = if (enabled) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_NULL
    }
/*
    override fun childDrawableStateChanged(child: View?) {
        refreshDrawableState()
    }*/

    // Make this view be aware of amountInput state changes (i.e. state_focused)
    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        val fieldState: IntArray? = amountInput.drawableState

        val need = fieldState?.size ?: 0

        val selfState = super.onCreateDrawableState(extraSpace + need)

        return mergeDrawableStates(selfState, fieldState)
    }

    private fun setBackground() {
        background = context.getCornersStateDrawable(
            focusedDrawable = context.getBlockDrawable(
                strokeColorRes = R.color.active_border
            ),
            idleDrawable = context.getBlockDrawable()
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

    fun loadAssetImage(imageUrl: String?) {
        stakingAssetImage.load(imageUrl, imageLoader)
    }

    fun setAssetName(name: String) {
        stakingAssetToken.text = name
    }

    fun setAssetBalance(balance: String) {
        stakingAssetBalance.text = balance
    }

    fun setFiatAmount(priceAmount: String?) {
        stakingAssetPriceAmount.setTextOrHide(priceAmount)
    }
}
