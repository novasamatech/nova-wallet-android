package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes
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
        setEndIcon(R.drawable.ic_info_cicrle_filled_16)
        attrs?.let { applyStyleAttrs(it) }
    }

    private fun applyStyleAttrs(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AddressView)

        val textColorRes = typedArray.getResourceId(R.styleable.AddressView_android_textColor, R.color.text_secondary)
        addressValue.setTextColorRes(textColorRes)

        typedArray.recycle()
    }

    fun setAddress(icon: Drawable, address: String) {
        addressImage.setImageDrawable(icon)
        addressValue.text = address
    }

    fun setEndIcon(@DrawableRes iconRes: Int?) {
        if (iconRes == null) {
            addressValue.setDrawableEnd(null)
        } else {
            addressValue.setDrawableEnd(iconRes, widthInDp = 16, tint = R.color.icon_secondary, paddingInDp = 6)
        }
    }
}

fun AddressView.setAddressModel(addressModel: AddressModel) {
    setAddress(addressModel.image, addressModel.nameOrAddress)
}

fun AddressView.setAddressOrHide(addressModel: AddressModel?) {
    if (addressModel == null) {
        makeGone()
        return
    }

    makeVisible()

    setAddress(addressModel.image, addressModel.nameOrAddress)
}
