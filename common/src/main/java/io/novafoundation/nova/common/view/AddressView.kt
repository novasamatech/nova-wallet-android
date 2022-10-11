package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.setDrawableEnd
import kotlinx.android.synthetic.main.view_address.view.addressImage
import kotlinx.android.synthetic.main.view_address.view.addressValue

class AddressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : FrameLayout(context, attrs, defStyle), WithContextExtensions {

    override val providedContext: Context = context

    init {
        View.inflate(context, R.layout.view_address, this)
    }

    fun setAddress(icon: Drawable, address: String) {
        addressImage.setImageDrawable(icon)
        addressValue.text = address
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        super.setOnClickListener(listener)
        if (listener != null) {
            background = getRoundedCornerDrawable(cornerSizeDp = 8).withRipple()
            addressValue.setDrawableEnd(R.drawable.ic_info_16, widthInDp = 16, tint = R.color.white_48, paddingInDp = 6)
        } else {
            background = null
            addressValue.setDrawableEnd(null)
        }
    }
}
