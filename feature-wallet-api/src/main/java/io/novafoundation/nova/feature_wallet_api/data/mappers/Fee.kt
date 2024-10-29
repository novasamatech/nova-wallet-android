package io.novafoundation.nova.feature_wallet_api.data.mappers

import io.novafoundation.nova.feature_account_api.data.model.FeeBase
import io.novafoundation.nova.feature_wallet_api.domain.model.Token
import io.novafoundation.nova.feature_wallet_api.presentation.model.FeeModel
import io.novafoundation.nova.feature_wallet_api.presentation.model.mapAmountToAmountModel

fun <F : FeeBase> mapFeeToFeeModel(
    fee: F,
    token: Token,
    includeZeroFiat: Boolean = true
): FeeModel<F> = FeeModel(
    display = mapAmountToAmountModel(
        amountInPlanks = fee.amount,
        token = token,
        includeZeroFiat = includeZeroFiat
    ),
    fee = fee
)

