package io.novafoundation.nova.common.view

import io.novafoundation.nova.common.address.AddressModel

fun LabeledTextView.setAddress(addressModel: AddressModel) {
    setTextIcon(addressModel.image)
    setMessage(addressModel.nameOrAddress)
}
