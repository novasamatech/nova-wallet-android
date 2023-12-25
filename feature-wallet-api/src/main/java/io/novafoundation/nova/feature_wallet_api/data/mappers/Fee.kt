package io.novafoundation.nova.feature_wallet_api.data.mappers

import io.novafoundation.nova.feature_account_api.data.model.Fee
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.domain.model.amountFromPlanks
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.GenericFee
import io.novafoundation.nova.feature_wallet_api.presentation.mixin.fee.SimpleFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.GenericDecimalFee
import io.novafoundation.nova.feature_wallet_api.presentation.model.GenericFeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel

fun <F : GenericFee> mapFeeToFeeModel(
    fee: F,
    token: Token,
    includeZeroFiat: Boolean = true
) = GenericFeeModel(
    decimalFee = GenericDecimalFee(
        genericFee = fee,
        networkFeeDecimalAmount = token.amountFromPlanks(fee.networkFee.amount),
    ),
    chainAsset = token.configuration,
    display = mapAmountToAmountModel(
        amountInPlanks = fee.networkFee.amount,
        token = token,
        includeZeroFiat = includeZeroFiat
    )
)

@Suppress("DeprecatedCallableAddReplaceWith")
@Deprecated("Backward-compatible adapter")
fun mapFeeToFeeModel(
    fee: Fee,
    token: Token,
    includeZeroFiat: Boolean = true
) = mapFeeToFeeModel(SimpleFee(fee), token, includeZeroFiat)
