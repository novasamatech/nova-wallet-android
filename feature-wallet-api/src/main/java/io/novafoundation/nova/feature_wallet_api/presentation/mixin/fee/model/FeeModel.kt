package io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.model

import io.novafoundation.nova.feature_wallet_api.presentation.model.AmountModel

class FeeModel<F, D>(
    val fee: F,
    val display: D,
)

class FeeDisplay(
    val title: String,
    val subtitle: String?
)

fun AmountModel.toFeeDisplay(): FeeDisplay {
    return FeeDisplay(title = token, subtitle = fiat)
}
