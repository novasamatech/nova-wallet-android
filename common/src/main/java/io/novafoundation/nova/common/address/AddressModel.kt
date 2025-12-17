package io.novafoundation.nova.common.address

import android.graphics.drawable.Drawable

open class AddressModel(
    val address: String,
    val image: Drawable,
    val name: String? = null
) {
    val nameOrAddress = name ?: address
}

class OptionalAddressModel(
    val address: String,
    val image: Drawable?,
    val name: String? = null
) {
    val nameOrAddress = name ?: address
}
