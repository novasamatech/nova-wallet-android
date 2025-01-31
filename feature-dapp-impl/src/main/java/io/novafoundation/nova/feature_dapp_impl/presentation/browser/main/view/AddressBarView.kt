package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_dapp_impl.R
import io.novafoundation.nova.feature_dapp_impl.databinding.ViewAddressBarBinding

class AddressBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions {

    private val binder = ViewAddressBarBinding.inflate(inflater(), this)

    override val providedContext: Context = context

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        background = addRipple(getRoundedCornerDrawable(R.color.dapp_blur_navigation_background, cornerSizeDp = 10), mask = getRippleMask(cornerSizeDp = 10))
    }

    fun setAddress(address: String) {
        binder.addressBarUrl.text = address
    }

    fun showSecureIcon(shouldShow: Boolean) {
        binder.addressBarIcon.setVisible(shouldShow)
    }

    fun showSecure(shouldShow: Boolean) {
        binder.addressBarIcon.setVisible(shouldShow)

        if (shouldShow) {
            binder.addressBarUrl.setTextColorRes(R.color.text_positive)
            binder.addressBarIcon.setImageTintRes(R.color.icon_positive)
        } else {
            binder.addressBarUrl.setTextColorRes(R.color.text_primary)
            binder.addressBarIcon.setImageTintRes(R.color.icon_primary)
        }
    }
}
