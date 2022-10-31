package io.novafoundation.nova.feature_account_api.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.common.utils.useAttributes
import io.novafoundation.nova.feature_account_api.R
import kotlinx.android.synthetic.main.view_account.view.addressAction
import kotlinx.android.synthetic.main.view_account.view.addressPrimaryIcon
import kotlinx.android.synthetic.main.view_account.view.addressSubtitle
import kotlinx.android.synthetic.main.view_account.view.addressTitle

private const val SHOW_BACKGROUND_DEFAULT = true

class AccountView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), WithContextExtensions by WithContextExtensions(context) {

    init {
        View.inflate(context, R.layout.view_account, this)

        attrs?.let(::applyAttributes)
    }

    fun setAddressModel(addressModel: AddressModel) {
        if (addressModel.name != null) {
            addressTitle.text = addressModel.name
            addressSubtitle.text = addressModel.address

            addressSubtitle.makeVisible()
        } else {
            addressTitle.text = addressModel.address

            addressSubtitle.makeGone()
        }

        addressPrimaryIcon.setImageDrawable(addressModel.image)
    }

    fun setShowBackground(shouldShow: Boolean) {
        background = if (shouldShow) {
            getRoundedCornerDrawable(R.color.white_8, cornerSizeDp = 12).withRipple()
        } else {
            null
        }
    }

    fun setActionClickListener(listener: OnClickListener) {
        setOnClickListener(listener)
    }

    fun setActionIcon(icon: Drawable?) {
        addressAction.setImageDrawable(icon)
        addressAction.setVisible(icon != null)
    }

    fun setActionIcon(@DrawableRes icon: Int?) {
        setActionIcon(icon?.let(context::getDrawable))
    }

    private fun applyAttributes(attrs: AttributeSet) = context.useAttributes(attrs, R.styleable.AddressView) { typedArray ->
        val actionIcon = typedArray.getDrawable(R.styleable.AddressView_actionIcon)
        setActionIcon(actionIcon)

        val shouldShowBackground = typedArray.getBoolean(R.styleable.AddressView_showBackground, SHOW_BACKGROUND_DEFAULT)
        setShowBackground(shouldShowBackground)
    }
}
