package io.novafoundation.nova.feature_wallet_api.presentation.model

import io.novafoundation.nova.feature_account_api.data.model.Fee
import java.math.BigDecimal

class FeeModel(
    val decimalFee: DecimalFee,
    val display: AmountModel,
)

class DecimalFee(
    val fee: Fee,
    val decimalAmount: BigDecimal
)
