package io.novafoundation.nova.feature_account_api.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextOrHide
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.databinding.ViewAccountBinding

private const val SHOW_BACKGROUND_DEFAULT = true

class AccountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    private val binder = ViewAccountBinding.inflate(inflater(), this)

    init {
        attrs?.let(::applyAttributes)
    }

    fun setAddressModel(addressModel: AddressModel) {
        if (addressModel.name != null) {
            binder.addressTitle.text = addressModel.name
            binder.addressSubtitle.text = addressModel.address

            binder.addressSubtitle.makeVisible()
        } else {
            binder.addressTitle.text = addressModel.address

            binder.addressSubtitle.makeGone()
        }

        binder.addressPrimaryIcon.setImageDrawable(addressModel.image)
    }

    fun setTitle(title: String) {
        binder.addressTitle.text = title
    }

    fun setSubTitle(subTitle: String?) {
        binder.addressSubtitle.setTextOrHide(subTitle)
    }

    fun setIcon(icon: Drawable) {
        binder.addressPrimaryIcon.setImageDrawable(icon)
    }

    fun setShowBackground(shouldShow: Boolean) {
        background = if (shouldShow) {
            getRoundedCornerDrawable(R.color.block_background, cornerSizeDp = 12).withRippleMask(getRippleMask(cornerSizeDp = 12))
        } else {
            null
        }
    }

    fun setActionClickListener(listener: OnClickListener) {
        setOnClickListener(listener)
    }

    fun setActionIcon(icon: Drawable?) {
        binder.addressAction.setImageDrawable(icon)
        binder.addressAction.setVisible(icon != null)
    }

    fun setActionIcon(@DrawableRes icon: Int?) {
        setActionIcon(icon?.let(context::getDrawable))
    }

    fun setActionTint(tintRes: Int?) {
        binder.addressAction.setImageTintRes(tintRes)
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.AccountView) { typedArray ->
        val actionIcon = typedArray.getDrawable(R.styleable.AccountView_actionIcon)
        setActionIcon(actionIcon)

        val shouldShowBackground = typedArray.getBoolean(R.styleable.AccountView_showBackground, SHOW_BACKGROUND_DEFAULT)
        setShowBackground(shouldShowBackground)
    }
}
