package io.novafoundation.nova.feature_wallet_api.presentation.model

import io.novafoundation.nova.common.utils.orZero
import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_account_api.data.model.requestedAccountPaysFees
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleFee
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import java.math.BigDecimal

typealias DecimalFee = GenericDecimalFee<SimpleFee>

class GenericFeeModel<F : GenericFee>(
    val decimalFee: GenericDecimalFee<F>,
    val display: AmountModel,
    val chainAsset: Chain.Asset
)

class GenericDecimalFee<F : GenericFee>(
    val genericFee: F,
    val networkFeeDecimalAmount: BigDecimal
) {

    @Deprecated("This field has unclear semantics in a case of custom fee structure", replaceWith = ReplaceWith("networkFeeDecimalAmount"))
    val decimalAmount: BigDecimal = networkFeeDecimalAmount

    val networkFee: Fee = genericFee.networkFee
}

val <F : GenericFee> GenericDecimalFee<F>.networkFeeByRequestedAccount: BigDecimal
    get() = if (networkFee.requestedAccountPaysFees) networkFeeDecimalAmount else BigDecimal.ZERO

val <F : GenericFee> GenericDecimalFee<F>?.networkFeeByRequestedAccountOrZero: BigDecimal
    get() = this?.networkFeeByRequestedAccount.orZero()

val <F : GenericFee> GenericDecimalFee<F>?.networkFeeDecimalAmount: BigDecimal
    get() = this?.networkFeeDecimalAmount.orZero()
