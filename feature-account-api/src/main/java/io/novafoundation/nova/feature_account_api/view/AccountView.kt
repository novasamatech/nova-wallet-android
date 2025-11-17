package io.novafoundation.nova.feature_account_api.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setDrawableStart
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

    sealed interface Model {
        class Address(val addressModel: AddressModel) : Model

        class NoAddress(val title: String, val subTitle: String) : Model
    }

    init {
        attrs?.let(::applyAttributes)
    }

    fun setModel(model: Model) {
        when (model) {
            is Model.Address -> setAddressModel(model.addressModel)
            is Model.NoAddress -> setNoAddress(model.title, model.subTitle)
        }
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

        binder.addressSubtitle.setDrawableStart(null)
        binder.addressSubtitle.ellipsize = TextUtils.TruncateAt.MIDDLE
        binder.addressPrimaryIcon.setImageDrawable(addressModel.image)
    }

    fun setNoAddress(title: String, subTitle: String) {
        setTitle(title)
        setIcon(ContextCompat.getDrawable(context, R.drawable.ic_identicon_placeholder))
        binder.addressSubtitle.setDrawableStart(R.drawable.ic_warning_filled, widthInDp = 16, paddingInDp = 4)
        binder.addressSubtitle.ellipsize = TextUtils.TruncateAt.END
        setSubTitle(subTitle)
    }

    fun setTitle(title: String) {
        binder.addressTitle.text = title
    }

    fun setSubTitle(subTitle: String?) {
        binder.addressSubtitle.setTextOrHide(subTitle)
        binder.addressSubtitle.setDrawableStart(null)
        binder.addressSubtitle.ellipsize = TextUtils.TruncateAt.MIDDLE
    }

    fun setIcon(icon: Drawable?) {
        binder.addressPrimaryIcon.setImageDrawable(icon)
    }

    fun setShowBackground(shouldShow: Boolean) {
        background = if (shouldShow) {
            getRoundedCornerDrawable(R.color.block_background, cornerSizeDp = 12).withRippleMask(getRippleMask(cornerSizeDp = 12))
        } else {
            null
        }
    }

    fun setActionClickListener(listener: OnClickListener?) {
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

fun AccountView.setSelectable(isSelectable: Boolean, onClickListener: View.OnClickListener) {
    if (isSelectable) {
        setActionIcon(R.drawable.ic_chevron_right)
        setActionClickListener(onClickListener)
    } else {
        setActionIcon(null as Drawable?)
        setActionClickListener(null)
    }
}
