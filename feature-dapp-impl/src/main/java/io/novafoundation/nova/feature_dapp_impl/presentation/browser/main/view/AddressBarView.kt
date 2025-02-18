package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setImageTintRes
import io.novafoundation.nova.common.utils.setTextColorRes
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_dapp_impl.R
import kotlinx.android.synthetic.main.view_address_bar.view.addressBarIcon
import kotlinx.android.synthetic.main.view_address_bar.view.addressBarUrl

class AddressBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr), WithContextExtensions {

    override val providedContext: Context = context

    init {
        View.inflate(context, R.layout.view_address_bar, this)

        orientation = HORIZONTAL
        gravity = Gravity.CENTER

        background = addRipple(getRoundedCornerDrawable(R.color.dapp_blur_navigation_background, cornerSizeDp = 10), mask = getRippleMask(cornerSizeDp = 10))
    }

    fun setAddress(address: String) {
        addressBarUrl.text = address
    }

    fun showSecure(shouldShow: Boolean) {
        addressBarIcon.setVisible(shouldShow)

        if (shouldShow) {
            addressBarUrl.setTextColorRes(R.color.text_positive)
            addressBarIcon.setImageTintRes(R.color.icon_positive)
        } else {
            addressBarUrl.setTextColorRes(R.color.text_primary)
            addressBarIcon.setImageTintRes(R.color.icon_primary)
        }
    }
}
