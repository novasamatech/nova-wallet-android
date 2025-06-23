package io.novafoundation.nova.common.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.databinding.ViewAddressBinding
import io.novafoundation.nova.common.utils.WithContextExtensions
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeGone
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.utils.removeDrawableEnd
import io.novafoundation.nova.common.utils.setDrawableEnd
import io.novafoundation.nova.common.utils.setTextColorRes

class AddressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : FrameLayout(context, attrs, defStyle), WithContextExtensions {

    override val providedContext: Context = context

    private val binder = ViewAddressBinding.inflate(inflater(), this)

    init {
        setEndIcon(R.drawable.ic_info)
        attrs?.let { applyStyleAttrs(it) }
    }

    private fun applyStyleAttrs(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AddressView)

        val textColorRes = typedArray.getResourceId(R.styleable.AddressView_android_textColor, R.color.text_secondary)
        binder.addressValue.setTextColorRes(textColorRes)

        typedArray.recycle()
    }

    fun setAddress(icon: Drawable, address: String) {
        binder.addressImage.setImageDrawable(icon)
        binder.addressValue.text = address
    }

    fun setEndIcon(@DrawableRes iconRes: Int?) {
        if (iconRes == null) {
            binder.addressValue.removeDrawableEnd()
        } else {
            binder.addressValue.setDrawableEnd(iconRes, widthInDp = 16, paddingInDp = 6)
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
