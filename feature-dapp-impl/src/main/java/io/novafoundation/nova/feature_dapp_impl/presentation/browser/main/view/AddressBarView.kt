package io.novafoundation.nova.feature_dapp_impl.presentation.browser.main.view

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_dapp_impl.R

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

        background = addRipple(getRoundedCornerDrawable(R.color.input_background, cornerSizeDp = 10), mask = getRippleMask(cornerSizeDp = 10))
    }

    fun setAddress(address: String) {
        addressBarUrl.text = address
    }

    fun showSecureIcon(shouldShow: Boolean) {
        addressBarIcon.setVisible(shouldShow)
    }
}
