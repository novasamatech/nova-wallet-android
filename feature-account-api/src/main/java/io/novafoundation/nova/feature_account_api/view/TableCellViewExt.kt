package io.novafoundation.nova.feature_account_api.view

import androidx.core.view.setPadding
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.utils.dp
import io.novafoundation.nova.common.view.TableCellView
import io.novafoundation.nova.feature_account_api.presenatation.chain.ChainUi
import io.novafoundation.nova.feature_account_api.presenatation.chain.toDrawable

fun TableCellView.showAddress(addressModel: AddressModel) {
    setImage(addressModel.image)

    showValue(addressModel.nameOrAddress)
}

fun TableCellView.showChain(chainUi: ChainUi) {
    image.background = chainUi.gradient.toDrawable(context, cornerRadiusDp = 6)
    image.setPadding(1.5f.dp(context))
    loadImage(chainUi.icon)

    showValue(chainUi.name)
}
