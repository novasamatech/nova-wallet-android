package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.InputType
import android.util.AttributeSet
import android.widget.EditText
import androidx.constraintlayout.widget.ConstraintLayout
import coil.ImageLoader
import coil.load
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewChooseAmountOldBinding
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.view.shape.getBlockDrawable
import io.novafoundation.nova.common.view.shape.getCornersStateDrawable

class AmountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : ConstraintLayout(context, attrs, defStyle) {

    private val binder = ViewChooseAmountOldBinding.inflate(inflater(), this)

    val amountInput: EditText
        get() = binder.stakingAmountInput

    private val imageLoader: ImageLoader by lazy(LazyThreadSafetyMode.NONE) {
        FeatureUtils.getCommonApi(context).imageLoader()
    }

    init {
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
        binder.stakingAssetImage.setImageDrawable(image)
    }

    fun loadAssetImage(imageUrl: String?) {
        binder.stakingAssetImage.load(imageUrl, imageLoader)
    }

    fun setAssetName(name: String) {
        binder.stakingAssetToken.text = name
    }

    fun setAssetBalance(balance: String) {
        binder.stakingAssetBalance.text = balance
    }

    fun setFiatAmount(priceAmount: String?) {
        binder.stakingAssetPriceAmount.setTextOrHide(priceAmount)
    }
}
