package io.novafoundation.nova.feature_account_api.view

import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.toDrawable

fun TableCellView.showAddress(addressModel: AddressModel) {
    setImage(addressModel.image)

    showValue(addressModel.nameOrAddress)
}

fun TableCellView.showChain(chainUi: ChainUi) {
    image.background = chainUi.gradient.toDrawable(context)
    loadImage(chainUi.icon)

    showValue(chainUi.name)
}
