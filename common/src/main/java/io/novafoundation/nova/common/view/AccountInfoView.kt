package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.databinding.ViewAccountInfoBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.getColorOrNull
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setImageTint

class AccountInfoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    private val binder = ViewAccountInfoBinding.inflate(inflater(), this)

    init {
        background = getRoundedCornerDrawable(fillColorRes = R.color.block_background).withRippleMask()

        isFocusable = true
        isClickable = true

        applyAttributes(attrs)
    }

    private fun applyAttributes(attrs: AttributeSet?) {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AccountInfoView)

            val actionIcon = typedArray.getDrawable(R.styleable.AccountInfoView_accountActionIcon)
            actionIcon?.let(::setActionIcon)

            val actionIconTint = typedArray.getColorOrNull(R.styleable.AccountInfoView_accountActionIconTint)
            setActionIconTint(actionIconTint)

            val textVisible = typedArray.getBoolean(R.styleable.AccountInfoView_textVisible, true)
            binder.accountAddressText.visibility = if (textVisible) View.VISIBLE else View.GONE

            typedArray.recycle()
        }
    }

    fun setActionIcon(icon: Drawable) {
        binder.accountAction.setImageDrawable(icon)
    }

    fun setActionIconTint(@ColorInt color: Int?) {
        binder.accountAction.setImageTint(color)
    }

    fun setActionListener(clickListener: (View) -> Unit) {
        binder.accountAction.setOnClickListener(clickListener)
    }

    fun setWholeClickListener(listener: (View) -> Unit) {
        setOnClickListener(listener)

        setActionListener(listener)
    }

    fun setTitle(accountName: String) {
        binder.accountTitle.text = accountName
    }

    fun setText(address: String) {
        binder.accountAddressText.text = address
    }

    fun setAccountIcon(icon: Drawable) {
        binder.accountIcon.setImageDrawable(icon)
    }

    fun hideBody() {
        binder.accountAddressText.makeGone()
    }

    fun showBody() {
        binder.accountAddressText.makeVisible()
    }
}
