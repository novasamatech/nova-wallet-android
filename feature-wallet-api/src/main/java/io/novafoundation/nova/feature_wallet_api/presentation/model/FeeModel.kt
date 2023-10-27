package io.novafoundation.nova.feature_wallet_api.presentation.model

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleFee
import java.math.BigDecimal

typealias FeeModel = GenericFeeModel<SimpleFee>
typealias DecimalFee = GenericDecimalFee<SimpleFee>

class GenericFeeModel<F : GenericFee>(
    val decimalFee: GenericDecimalFee<F>,
    val display: AmountModel,
)

class GenericDecimalFee<F : GenericFee>(
    val genericFee: F,
    val networkFeeDecimalAmount: BigDecimal
) {

    @Deprecated("This field has unclear semantics in a case of custom fee structure", replaceWith = ReplaceWith("networkFeeDecimalAmount"))
    val decimalAmount: BigDecimal = networkFeeDecimalAmount

    val fee: Fee = genericFee.networkFee
}
