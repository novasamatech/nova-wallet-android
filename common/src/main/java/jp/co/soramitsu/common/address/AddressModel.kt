package jp.co.soramitsu.common.address

import android.graphics.drawable.Drawable

class AddressModel(
    val address: String,
    val image: Drawable,
    val name: String? = null
) {
    val nameOrAddress = name ?: address
}
